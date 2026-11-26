package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.JdkCompilerInfo;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.PatchDirectoryPreview;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.CompiledArtifactRepository;
import com.jarpatch.service.PatchDirectoryPlanner.Input;
import com.jarpatch.service.PatchDirectoryPlanner.Plan;
import com.jarpatch.service.PatchDirectoryPlanner.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 应用已预览的本地类补丁。在项目操作锁内复核输入，将 javac 产物和现成 class 暂存后统一写回。
 * 产物清单与任务成功状态同时落库，失败或取消时由编译产物提交器恢复工作区。
 *
 * @author 黄杰
 */
@Service
public class PatchDirectoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatchDirectoryService.class);
    private static final String TASK_TYPE = "PATCH_DIRECTORY";
    private static final String MESSAGE_START = "开始应用补丁目录";
    private static final String MESSAGE_STAGE = "验证并暂存补丁文件";
    private static final String MESSAGE_COMPILE = "按目标类版本编译补丁源码";
    private static final String MESSAGE_COMMIT = "校验成功，准备统一写回 class";
    private static final String MESSAGE_SUCCESS = "补丁已应用，点击「导出」生成新包";
    private static final String MESSAGE_FAILED = "应用补丁失败";
    private static final String STAGING_PREFIX = ".patch-directory-";
    private static final int PROGRESS_STAGE = 15;
    private static final int PROGRESS_COMPILE = 35;
    private static final int PROGRESS_COMMIT = 75;
    private static final int MINIMUM_RELEASE = 7;
    private static final int MINIMUM_COMPILER_RELEASE = 9;

    private final WorkspaceService workspaceService;
    private final ProjectOperationLockService projectOperationLockService;
    private final PatchDirectoryPlanner planner;
    private final CompileArtifactCommitter committer;
    private final CompiledArtifactRepository artifacts;
    private final TaskService taskService;
    private final JdkService jdkService;
    private final CompileProcessRunner compiler;
    private final ClockService clockService;
    private final TransactionTemplate transactionTemplate;

    /** 创建补丁目录服务，接入现有项目锁、编译、提交及任务记录。 */
    public PatchDirectoryService(WorkspaceService workspaceService,
                                 ProjectOperationLockService projectOperationLockService,
                                 PatchDirectoryPlanner planner, CompileArtifactCommitter committer,
                                 CompiledArtifactRepository artifacts, TaskService taskService,
                                 JdkService jdkService, CompileProcessRunner compiler,
                                 ClockService clockService, TransactionTemplate transactionTemplate) {
        this.workspaceService = workspaceService;
        this.projectOperationLockService = projectOperationLockService;
        this.planner = planner;
        this.committer = committer;
        this.artifacts = artifacts;
        this.taskService = taskService;
        this.jdkService = jdkService;
        this.compiler = compiler;
        this.clockService = clockService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 在同一项目锁下生成只读预览。
     *
     * @param project 已导入的包
     * @param directory 用户选择的本机目录
     * @return 匹配清单和确认标识
     */
    public PatchDirectoryPreview preview(ProjectRecord project, String directory) throws IOException {
        return projectOperationLockService.runExclusiveIo(project.getId(),
                () -> planner.inspect(project, directory, () -> { }).preview());
    }

    /**
     * 应用已确认的预览，将 class 提交到 extracted 并记录导出待校验条目。
     *
     * @param project 当前项目
     * @param directory 预览时用户选择的本机目录
     * @param confirmation 预览内容确认标识
     * @param taskId 预创建的可取消任务 ID
     * @return 成功写回的包内 class 清单
     */
    public OperationResult apply(ProjectRecord project, String directory, String confirmation, String taskId)
            throws IOException, InterruptedException {
        try {
            return projectOperationLockService.runExclusive(project.getId(),
                    () -> applyWithinLock(project, directory, confirmation, taskId));
        } catch (IllegalStateException exception) {
            if (JarPatchConstants.MESSAGE_PROJECT_OPERATION_IN_PROGRESS.equals(exception.getMessage())) {
                taskService.failRejectedTask(taskId, project.getId(), exception.getMessage());
            }
            throw exception;
        }
    }

    /** 重新匹配、暂存并编译全部输入，之后才允许写入工作区。 */
    private OperationResult applyWithinLock(ProjectRecord project, String directory, String confirmation,
                                            String taskId) throws IOException, InterruptedException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE, MESSAGE_START);
        Path staging = null;
        boolean preserveBackup = false;
        try {
            taskService.running(task, PROGRESS_STAGE, MESSAGE_STAGE);
            Plan plan = planner.inspect(project, directory, () -> taskService.ensureNotCancelled(task.getId()));
            if (confirmation == null || !confirmation.equals(plan.preview().confirmation())) {
                throw new IllegalArgumentException(PatchDirectoryPlanner.MESSAGE_STALE);
            }
            staging = Files.createTempDirectory(workspaceService.compiledDir(project), STAGING_PREFIX);
            Map<String, TargetFiles> targets = stageFiles(staging, plan, task);
            compileSources(project, task, targets);
            Map<String, Path> outputs = new LinkedHashMap<>();
            targets.forEach((name, target) -> outputs.put(name, target.output()));
            List<String> paths = collectPaths(project, outputs);
            OperationResult result = new OperationResult();
            result.setTaskId(task.getId());
            result.setChangedFiles(paths);
            result.setMessage(MESSAGE_SUCCESS);

            taskService.running(task, PROGRESS_COMMIT, MESSAGE_COMMIT);
            var commit = committer.prepare(project, outputs, staging.resolve("backup"));
            try {
                committer.apply(project, outputs, () -> taskService.isCancelled(task.getId()));
                transactionTemplate.executeWithoutResult(status -> {
                    artifacts.addPatchArtifacts(project.getId(), paths, clockService.now());
                    taskService.success(task, MESSAGE_SUCCESS);
                });
            } catch (IOException | RuntimeException exception) {
                try {
                    committer.restore(commit);
                } catch (IOException | RuntimeException recoveryFailure) {
                    preserveBackup = true;
                    IOException failure = new IOException("补丁回滚失败，备份保留在 " + staging, recoveryFailure);
                    failure.addSuppressed(exception);
                    throw failure;
                }
                throw exception;
            }
            return result;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (!taskService.isCancelled(task.getId())) {
                taskService.failed(task, MESSAGE_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                        + exception.getMessage());
            }
            throw exception;
        } finally {
            if (staging != null && !preserveBackup) {
                try {
                    committer.deleteTree(staging);
                } catch (IOException exception) {
                    LOGGER.warn("无法清理补丁暂存目录 {}", staging, exception);
                }
            }
        }
    }

    /** 将预览文件复制到本次独立目录，按主包或原嵌套 JAR 分组。 */
    private Map<String, TargetFiles> stageFiles(Path staging, Plan plan, TaskRecord task) throws IOException {
        Map<String, TargetFiles> groups = new LinkedHashMap<>();
        for (Map.Entry<Input, Target> match : plan.matches().entrySet()) {
            taskService.ensureNotCancelled(task.getId());
            Input input = match.getKey();
            Target target = match.getValue();
            TargetFiles group = groups.get(target.group());
            if (group == null) {
                Path groupDir = Files.createTempDirectory(staging, "target-");
                group = new TargetFiles(Files.createDirectory(groupDir.resolve("classes")),
                        groupDir.resolve("sources"), groupDir.resolve("compiled"), new LinkedHashMap<>());
                groups.put(target.group(), group);
            }
            String entry = input.identity().entry();
            byte[] bytes = PatchDirectoryPlanner.readConfirmed(input);
            if (input.source()) {
                String javaPath = entry.substring(0, entry.length() - PatchDirectoryPlanner.CLASS_SUFFIX.length())
                        + PatchDirectoryPlanner.JAVA_SUFFIX;
                Path source = group.sources().resolve(javaPath);
                Files.createDirectories(source.getParent());
                Files.writeString(source, PatchDirectoryPlanner.sourceText(bytes));
                group.sourceTargets().put(entry, target.major());
            } else {
                Path output = group.output().resolve(entry);
                Files.createDirectories(output.getParent());
                Files.write(output, bytes);
            }
        }
        return groups;
    }

    /** 直接 class 优先进入 classpath，以便同批的 Java 源码引用它们。 */
    private void compileSources(ProjectRecord project, TaskRecord task, Map<String, TargetFiles> groups)
            throws IOException, InterruptedException {
        if (groups.values().stream().allMatch(group -> group.sourceTargets().isEmpty())) {
            return;
        }
        taskService.running(task, PROGRESS_COMPILE, MESSAGE_COMPILE);
        JdkCompilerInfo javac = jdkService.findCompiler();
        Path extracted = workspaceService.extractedDir(project);
        String originalClasspath = ArchiveService.classpath(extracted, committer.classRoot(project));
        for (Map.Entry<String, TargetFiles> entry : groups.entrySet()) {
            taskService.ensureNotCancelled(task.getId());
            TargetFiles group = entry.getValue();
            if (group.sourceTargets().isEmpty()) {
                continue;
            }
            Set<Integer> classVersions = new LinkedHashSet<>(group.sourceTargets().values());
            if (classVersions.size() != 1) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_CLASS_VERSION_CONFLICT);
            }
            int release = PatchDirectoryPlanner.javaRelease(classVersions.iterator().next());
            if (release < MINIMUM_RELEASE || javac.getFeatureVersion() < Math.max(release, MINIMUM_COMPILER_RELEASE)) {
                throw new IllegalArgumentException("当前 JDK 无法按原包目标 Java " + release + " 编译");
            }
            List<Path> sources = new ArrayList<>();
            try (var stream = Files.walk(group.sources())) {
                stream.filter(Files::isRegularFile).sorted().forEach(sources::add);
            }
            List<String> classpath = new ArrayList<>();
            classpath.add(group.output().toString());
            groups.values().stream().filter(other -> other != group)
                    .map(other -> other.output().toString()).forEach(classpath::add);
            if (!PatchDirectoryPlanner.MAIN.equals(entry.getKey())) {
                classpath.add(workspaceService.resolveExtracted(project, entry.getKey()).toString());
            }
            classpath.add(originalClasspath);
            Path output = Files.createDirectory(group.compilerOutput());
            List<String> command = compiler.prepareCommand(javac.getJavacPath(), output, release,
                    String.join(File.pathSeparator, classpath), sources);
            String compilerMessage = compiler.run(command, group.sources(), () -> taskService.isCancelled(task.getId()));
            if (!compilerMessage.isBlank()) {
                taskService.running(task, PROGRESS_COMPILE, compilerMessage);
            }
            mergeSourceOutput(group);
        }
    }

    /** 核对 javac 必须产出已确认的类，另外只接受该类所生成的内部 class。 */
    private void mergeSourceOutput(TargetFiles group) throws IOException {
        Set<String> compiled = new LinkedHashSet<>();
        for (Path file : committer.collectClassFiles(group.compilerOutput())) {
            var identity = PatchDirectoryPlanner.classIdentity(Files.readAllBytes(file));
            String entry = PatchDirectoryPlanner.entryPath(group.compilerOutput(), file);
            Integer expected = group.sourceTargets().get(entry);
            if (expected == null) {
                expected = findSourceOwner(group, entry);
            }
            if (!identity.entry().equals(entry) || expected == null || expected != identity.major()) {
                throw new IllegalArgumentException("源码生成了预览范围以外或版本不符的 class：" + entry);
            }
            compiled.add(entry);
            Path target = group.output().resolve(entry);
            Files.createDirectories(target.getParent());
            Files.move(file, target);
        }
        for (String expected : group.sourceTargets().keySet()) {
            if (!compiled.contains(expected)) {
                throw new IllegalArgumentException("源码没有生成预览所对应的 class：" + expected);
            }
        }
    }

    /** 将编译生成的内部 class 归属到同包、已确认的源码。 */
    private Integer findSourceOwner(TargetFiles group, String entry) {
        for (int index = entry.lastIndexOf('$'); index > entry.lastIndexOf('/'); index = entry.lastIndexOf('$', index - 1)) {
            Integer version = group.sourceTargets().get(entry.substring(0, index) + PatchDirectoryPlanner.CLASS_SUFFIX);
            if (version != null) {
                return version;
            }
        }
        return null;
    }

    /** 计算待写回和导出校验的真实 class 路径，并检查主包目标的文件系统边界。 */
    private List<String> collectPaths(ProjectRecord project, Map<String, Path> outputs) throws IOException {
        Path extracted = workspaceService.extractedDir(project);
        Path root = committer.classRoot(project);
        Path realRoot = extracted.toRealPath();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Path> target : outputs.entrySet()) {
            for (Path file : committer.collectClassFiles(target.getValue())) {
                String entry = PatchDirectoryPlanner.entryPath(target.getValue(), file);
                if (PatchDirectoryPlanner.MAIN.equals(target.getKey())) {
                    Path destination = root.resolve(entry);
                    String location = PatchDirectoryPlanner.entryPath(extracted, destination);
                    Path expected = realRoot.resolve(location);
                    if (!destination.getParent().toRealPath().toString().equals(expected.getParent().toString())
                            || Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                            && (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
                            || !destination.toRealPath().toString().equals(expected.toString()))) {
                        throw new IllegalArgumentException("工作区实际路径与目标 class 不一致：" + location);
                    }
                    result.add(location);
                } else {
                    result.add(target.getKey() + JarPatchConstants.ARCHIVE_ENTRY_SEPARATOR + entry);
                }
            }
        }
        return result.stream().sorted().toList();
    }

    /** 单一写回目标对应的独立暂存目录与源码预期版本。 */
    private record TargetFiles(Path output, Path sources, Path compilerOutput, Map<String, Integer> sourceTargets) {
    }
}
