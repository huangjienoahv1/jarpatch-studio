package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import com.jarpatch.model.JavaVersionInfo;
import com.jarpatch.model.JdkCompilerInfo;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.FileChangeRepository;
import com.jarpatch.repository.CompiledArtifactRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Java 修改文件严格编译与提交服务。
 * <p>
 * 编译入口来自 /api/projects/{id}/compile。服务先按主 classes 或嵌套 Jar 分组，从每个
 * 修改源码对应的原始 class 读取目标 Java 版本，再使用已验证 javac 和 --release 把所有
 * 产物写入本次 staging 目录；全部目标编译成功后才备份并统一写回 extracted，任一失败或
 * 取消都会恢复本次提交前文件。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class CompileService {

    private static final String TASK_TYPE_COMPILE = "COMPILE";
    private static final String JAVAC_ARGUMENT_FILE_NAME = "javac-arguments.txt";
    private static final String JAVAC_ARGUMENT_FILE_PREFIX = "@";
    private static final String JAVAC_RELEASE_ARGUMENT = "--release";
    private static final String NESTED_JAR_SOURCE_PREFIX = JarPatchConstants.SOURCE_NESTED_JAR_DIR
            + JarPatchConstants.ZIP_SEPARATOR;
    private static final String NESTED_JAR_SOURCE_MARKER = "." + JarPatchConstants.JAR_EXTENSION
            + JarPatchConstants.ZIP_SEPARATOR;
    private static final String COMPILE_STAGING_PREFIX = ".staging-";
    private static final String OUTPUT_DIR = "outputs";
    private static final String BACKUP_DIR = "backup";
    private static final int MINIMUM_RELEASE_COMPILER_VERSION = 9;
    private static final char WINDOWS_PATH_SEPARATOR = '\\';
    private static final char JAVAC_ARGUMENT_PATH_SEPARATOR = '/';
    private static final String JAVAC_ARGUMENT_QUOTE = "\"";
    private static final String JAVAC_ARGUMENT_ESCAPED_QUOTE = "\\\"";
    private static final String MESSAGE_COMPILE_START = "开始编译修改过的 Java 文件";
    private static final String MESSAGE_COMPILE_LOCATE_JAVAC = "定位并执行 javac -version";
    private static final String MESSAGE_JAVAC_RELEASE_REQUIRED = "当前 javac 不支持 --release，请配置 Java 9 或更高版本的完整 JDK";
    private static final String MESSAGE_COMPILE_ALL_TARGETS = "按原始 class 版本编译全部目标";
    private static final String MESSAGE_COMPILE_PREPARE_COMMIT = "全部目标编译成功，准备统一提交";
    private static final String MESSAGE_COMPILE_COMPLETE = "编译完成";
    private static final String MESSAGE_COMPILE_SUCCESS = "编译完成，全部 class 已统一写入 extracted";
    private static final String MESSAGE_COMPILE_FAILED = "编译失败";
    private static final String MESSAGE_JAVAC_PREFIX = "javac ";
    private static final String MESSAGE_TARGET_JAVA_PREFIX = "，目标 Java ";
    private static final int PROGRESS_LOCATE_JAVAC = 15;
    private static final int PROGRESS_COMPILE_TARGETS = 30;
    private static final int PROGRESS_PREPARE_COMMIT = 75;

    private final WorkspaceService workspaceService;
    private final FileChangeRepository fileChangeRepository;
    private final TaskService taskService;
    private final JdkService jdkService;
    private final JavaVersionService javaVersionService;
    private final CompiledArtifactRepository compiledArtifactRepository;
    private final ClockService clockService;
    private final CompileProcessRunner compileProcessRunner;
    private final CompileArtifactCommitter compileArtifactCommitter;

    /**
     * 创建严格编译服务。
     *
     * @param workspaceService     工作区服务
     * @param fileChangeRepository 修改记录仓储
     * @param taskService          任务服务
     * @param jdkService           JDK 工具服务
     * @param javaVersionService   原 class 版本识别服务
     * @param compiledArtifactRepository 已提交编译产物仓储
     * @param clockService         时间服务
     * @param compileProcessRunner javac 子进程执行器
     * @param compileArtifactCommitter 编译产物提交与恢复服务
     */
    public CompileService(WorkspaceService workspaceService,
                          FileChangeRepository fileChangeRepository,
                          TaskService taskService,
                          JdkService jdkService,
                          JavaVersionService javaVersionService,
                          CompiledArtifactRepository compiledArtifactRepository,
                          ClockService clockService,
                          CompileProcessRunner compileProcessRunner,
                          CompileArtifactCommitter compileArtifactCommitter) {
        this.workspaceService = workspaceService;
        this.fileChangeRepository = fileChangeRepository;
        this.taskService = taskService;
        this.jdkService = jdkService;
        this.javaVersionService = javaVersionService;
        this.compiledArtifactRepository = compiledArtifactRepository;
        this.clockService = clockService;
        this.compileProcessRunner = compileProcessRunner;
        this.compileArtifactCommitter = compileArtifactCommitter;
    }

    /**
     * 编译已修改 Java 文件并提交 class。
     *
     * @param project 项目记录
     * @return 编译结果
     * @throws IOException          文件读写失败时抛出
     * @throws InterruptedException javac 被中断时抛出
     */
    public OperationResult compile(ProjectRecord project) throws IOException, InterruptedException {
        return compile(project, null);
    }

    /**
     * 编译已修改 Java 文件并以可回滚提交阶段统一写回。
     *
     * @param project 项目记录
     * @param taskId  预创建任务 ID，可为空
     * @return 编译结果
     * @throws IOException          文件读写失败时抛出
     * @throws InterruptedException javac 被中断时抛出
     */
    public OperationResult compile(ProjectRecord project, String taskId) throws IOException, InterruptedException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE_COMPILE, MESSAGE_COMPILE_START);
        Path compileRunDir = null;
        CompileArtifactCommitter.CompileCommit compileCommit = null;
        boolean completed = false;
        boolean artifactRecordsUpdated = false;
        List<String> previousArtifacts = compiledArtifactRepository.findPaths(project.getId());
        try {
            List<String> javaPaths = fileChangeRepository.findJavaPaths(project.getId());
            if (javaPaths.isEmpty()) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_NO_MODIFIED_JAVA);
            }

            taskService.running(task, PROGRESS_LOCATE_JAVAC, MESSAGE_COMPILE_LOCATE_JAVAC);
            JdkCompilerInfo compilerInfo = jdkService.findCompiler();
            if (compilerInfo.getFeatureVersion() < MINIMUM_RELEASE_COMPILER_VERSION) {
                throw new IllegalArgumentException(MESSAGE_JAVAC_RELEASE_REQUIRED);
            }

            compileRunDir = createCompileRunDirectory(project);
            Map<String, List<String>> pathsByTarget = groupJavaPathsByTarget(javaPaths);
            Map<String, Path> compiledOutputs = new LinkedHashMap<>();
            StringBuilder processOutput = new StringBuilder();

            taskService.running(task, PROGRESS_COMPILE_TARGETS, MESSAGE_COMPILE_ALL_TARGETS);
            for (Map.Entry<String, List<String>> entry : pathsByTarget.entrySet()) {
                taskService.ensureNotCancelled(task.getId());
                JavaVersionInfo targetVersion = javaVersionService.detectCompileTargetVersion(
                        project, entry.getKey(), entry.getValue());
                validateCompilerVersion(compilerInfo, targetVersion);
                Path targetOutput = compileRunDir.resolve(OUTPUT_DIR)
                        .resolve(compileArtifactCommitter.safeTargetName(entry.getKey()));
                Files.createDirectories(targetOutput);
                List<String> command = buildCommand(project, compilerInfo.getJavacPath(), targetOutput,
                        entry.getValue(), targetVersion.getFeatureVersion());
                processOutput.append(compileProcessRunner.run(command, workspaceService.sourceDir(project),
                        () -> taskService.isCancelled(task.getId())));
                compiledOutputs.put(entry.getKey(), targetOutput);
            }

            taskService.running(task, PROGRESS_PREPARE_COMMIT, MESSAGE_COMPILE_PREPARE_COMMIT);
            taskService.ensureNotCancelled(task.getId());
            compileCommit = compileArtifactCommitter.prepare(
                    project, compiledOutputs, compileRunDir.resolve(BACKUP_DIR));

            // 实际写回只发生在全部 javac 成功之后；提交异常或任务取消由 finally 恢复备份。
            compileArtifactCommitter.apply(project, compiledOutputs, () -> taskService.isCancelled(task.getId()));
            List<String> compiledArtifacts = collectArtifactPaths(project, compiledOutputs);
            compiledArtifactRepository.replaceProjectArtifacts(project.getId(), compiledArtifacts, clockService.now());
            artifactRecordsUpdated = true;

            OperationResult result = new OperationResult();
            result.setTaskId(task.getId());
            result.setChangedFiles(javaPaths);
            result.setMessage(processOutput.isEmpty() ? MESSAGE_COMPILE_COMPLETE : processOutput.toString());
            taskService.success(task, MESSAGE_COMPILE_SUCCESS);
            completed = true;
            return result;
        } catch (IllegalStateException exception) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(exception.getMessage())) {
                throw exception;
            }
            taskService.failed(task, MESSAGE_COMPILE_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                    + exception.getMessage());
            throw exception;
        } catch (RuntimeException | IOException | InterruptedException exception) {
            taskService.failed(task, MESSAGE_COMPILE_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                    + exception.getMessage());
            throw exception;
        } finally {
            if (!completed && compileCommit != null) {
                compileArtifactCommitter.restore(compileCommit);
            }
            if (!completed && artifactRecordsUpdated) {
                compiledArtifactRepository.replaceProjectArtifacts(project.getId(), previousArtifacts, clockService.now());
            }
            if (compileRunDir != null) {
                compileArtifactCommitter.deleteTree(compileRunDir);
            }
        }
    }

    /**
     * 校验 javac 能覆盖原 class 的目标 Java 版本。
     *
     * @param compilerInfo 已验证编译器
     * @param targetVersion 原 class 目标版本
     */
    private void validateCompilerVersion(JdkCompilerInfo compilerInfo, JavaVersionInfo targetVersion) {
        if (compilerInfo.getFeatureVersion() < targetVersion.getFeatureVersion()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_JDK_VERSION_TOO_LOW
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + MESSAGE_JAVAC_PREFIX
                    + compilerInfo.getFeatureVersion()
                    + MESSAGE_TARGET_JAVA_PREFIX + targetVersion.getFeatureVersion());
        }
    }

    /**
     * 创建本次编译独立 staging 目录。
     *
     * @param project 项目记录
     * @return 编译 staging 目录
     * @throws IOException 创建失败时抛出
     */
    private Path createCompileRunDirectory(ProjectRecord project) throws IOException {
        Path runDir = workspaceService.compiledDir(project).resolve(COMPILE_STAGING_PREFIX + UUID.randomUUID()).normalize();
        Files.createDirectories(runDir.resolve(OUTPUT_DIR));
        return runDir;
    }

    /**
     * 构建使用 javac 参数文件的严格编译命令。
     *
     * @param project        项目记录
     * @param javac          javac 路径
     * @param compiledDir    本目标 staging 输出目录
     * @param javaPaths      修改源码路径
     * @param releaseVersion --release 版本
     * @return 外部进程命令
     * @throws IOException 参数文件写入失败时抛出
     */
    private List<String> buildCommand(ProjectRecord project,
                                      Path javac,
                                      Path compiledDir,
                                      List<String> javaPaths,
                                      int releaseVersion) throws IOException {
        Path argumentFile = compiledDir.resolve(JAVAC_ARGUMENT_FILE_NAME);
        writeArgumentFile(project, compiledDir, javaPaths, releaseVersion, argumentFile);
        return List.of(javac.toString(), JAVAC_ARGUMENT_FILE_PREFIX + argumentFile);
    }

    /**
     * 写入包含 --release、classpath、输出目录和明确源码清单的 javac 参数文件。
     *
     * @param project        项目记录
     * @param compiledDir    编译输出目录
     * @param javaPaths      修改源码路径
     * @param releaseVersion --release 版本
     * @param argumentFile   参数文件
     * @throws IOException 写入失败时抛出
     */
    private void writeArgumentFile(ProjectRecord project,
                                   Path compiledDir,
                                   List<String> javaPaths,
                                   int releaseVersion,
                                   Path argumentFile) throws IOException {
        List<String> arguments = new ArrayList<>();
        arguments.add(JAVAC_RELEASE_ARGUMENT);
        arguments.add(String.valueOf(releaseVersion));
        arguments.add("-encoding");
        arguments.add(JarPatchConstants.UTF_8);
        arguments.add("-classpath");
        arguments.add(formatArgumentFileValue(buildClasspath(project)));
        arguments.add("-d");
        arguments.add(formatArgumentFileValue(compiledDir.toString()));
        for (String javaPath : javaPaths) {
            if (!javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
            }
            String relativePath = javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length());
            arguments.add(formatArgumentFileValue(workspaceService.resolveSource(project, relativePath).toString()));
        }
        Files.write(argumentFile, arguments, StandardCharsets.UTF_8);
    }

    /**
     * 把路径值转为 javac 参数文件要求的带引号格式。
     *
     * @param value 原始参数值
     * @return 参数文件值
     */
    private String formatArgumentFileValue(String value) {
        return JAVAC_ARGUMENT_QUOTE
                + value.replace(WINDOWS_PATH_SEPARATOR, JAVAC_ARGUMENT_PATH_SEPARATOR)
                .replace(JAVAC_ARGUMENT_QUOTE, JAVAC_ARGUMENT_ESCAPED_QUOTE)
                + JAVAC_ARGUMENT_QUOTE;
    }

    /**
     * 构建原包 classes 和全部嵌套 Jar 组成的编译 classpath。
     *
     * @param project 项目记录
     * @return classpath 字符串
     * @throws IOException 遍历依赖失败时抛出
     */
    private String buildClasspath(ProjectRecord project) throws IOException {
        List<String> entries = new ArrayList<>();
        Path extractedDir = workspaceService.extractedDir(project);
        entries.add(extractedDir.toString());
        entries.add(compileArtifactCommitter.classRoot(project).toString());
        try (var stream = Files.walk(extractedDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase()
                            .endsWith("." + JarPatchConstants.JAR_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> entries.add(path.toString()));
        }
        return String.join(File.pathSeparator, entries);
    }

    /**
     * 按主 classes 或嵌套 Jar 写回目标分组修改源码。
     *
     * @param javaPaths 修改源码路径
     * @return 保持原顺序的目标分组
     */
    private Map<String, List<String>> groupJavaPathsByTarget(List<String> javaPaths) {
        Map<String, List<String>> groupedPaths = new LinkedHashMap<>();
        for (String javaPath : javaPaths) {
            groupedPaths.computeIfAbsent(resolveCompileTarget(javaPath), key -> new ArrayList<>()).add(javaPath);
        }
        return groupedPaths;
    }

    /**
     * 解析源码对应的主 classes 或嵌套 Jar 目标。
     *
     * @param javaPath 修改源码路径
     * @return 编译目标标识
     */
    private String resolveCompileTarget(String javaPath) {
        if (javaPath == null || !javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        String sourceRelativePath = javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length());
        if (!sourceRelativePath.startsWith(NESTED_JAR_SOURCE_PREFIX)) {
            return JarPatchConstants.COMPILE_TARGET_MAIN;
        }
        String nestedRelativePath = sourceRelativePath.substring(NESTED_JAR_SOURCE_PREFIX.length());
        int markerIndex = nestedRelativePath.indexOf(NESTED_JAR_SOURCE_MARKER);
        if (markerIndex < 0) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH);
        }
        return nestedRelativePath.substring(0, markerIndex + ("." + JarPatchConstants.JAR_EXTENSION).length());
    }

    /**
     * 把 staging class 文件转换为最终包内可校验的产物路径。
     *
     * @param project         项目记录
     * @param compiledOutputs 各目标编译输出
     * @return 主包条目或 nested.jar!/entry 格式的产物路径
     * @throws IOException 遍历编译输出失败时抛出
     */
    private List<String> collectArtifactPaths(ProjectRecord project,
                                              Map<String, Path> compiledOutputs) throws IOException {
        List<String> artifacts = new ArrayList<>();
        for (Map.Entry<String, Path> entry : compiledOutputs.entrySet()) {
            for (Path classFile : compileArtifactCommitter.collectClassFiles(entry.getValue())) {
                String classEntry = entry.getValue().relativize(classFile).toString().replace('\\', '/');
                if (JarPatchConstants.COMPILE_TARGET_MAIN.equals(entry.getKey())) {
                    artifacts.add(mainArchivePrefix(project) + classEntry);
                } else {
                    artifacts.add(entry.getKey() + JarPatchConstants.ARCHIVE_ENTRY_SEPARATOR + classEntry);
                }
            }
        }
        artifacts.sort(String::compareTo);
        return artifacts;
    }

    /**
     * 获取不同包类型的主 class 包内路径前缀。
     *
     * @param project 项目记录
     * @return 空字符串、BOOT-INF/classes/ 或 WEB-INF/classes/
     */
    private String mainArchivePrefix(ProjectRecord project) {
        if (PackageType.SPRING_BOOT_JAR.getCode().equals(project.getPackageType())) {
            return JarPatchConstants.SPRING_BOOT_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
        }
        if (PackageType.WAR.getCode().equals(project.getPackageType())) {
            return JarPatchConstants.WAR_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
        }
        return JarPatchConstants.EMPTY_TEXT;
    }

}
