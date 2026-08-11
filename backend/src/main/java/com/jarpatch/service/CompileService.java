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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
    private static final String BACKUP_MAIN_DIR = "main";
    private static final String BACKUP_NESTED_DIR = "nested";
    private static final int MINIMUM_RELEASE_COMPILER_VERSION = 9;
    private static final long PROCESS_POLL_INTERVAL_MILLIS = 200L;
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
    private final ArchiveService archiveService;
    private final FileChangeRepository fileChangeRepository;
    private final TaskService taskService;
    private final JdkService jdkService;
    private final JavaVersionService javaVersionService;
    private final CompiledArtifactRepository compiledArtifactRepository;
    private final ClockService clockService;

    /**
     * 创建严格编译服务。
     *
     * @param workspaceService     工作区服务
     * @param archiveService       压缩包服务
     * @param fileChangeRepository 修改记录仓储
     * @param taskService          任务服务
     * @param jdkService           JDK 工具服务
     * @param javaVersionService   原 class 版本识别服务
     * @param compiledArtifactRepository 已提交编译产物仓储
     * @param clockService         时间服务
     */
    public CompileService(WorkspaceService workspaceService,
                          ArchiveService archiveService,
                          FileChangeRepository fileChangeRepository,
                          TaskService taskService,
                          JdkService jdkService,
                          JavaVersionService javaVersionService,
                          CompiledArtifactRepository compiledArtifactRepository,
                          ClockService clockService) {
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
        this.fileChangeRepository = fileChangeRepository;
        this.taskService = taskService;
        this.jdkService = jdkService;
        this.javaVersionService = javaVersionService;
        this.compiledArtifactRepository = compiledArtifactRepository;
        this.clockService = clockService;
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
        CompileCommit compileCommit = null;
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
                Path targetOutput = compileRunDir.resolve(OUTPUT_DIR).resolve(safeCompileTargetName(entry.getKey()));
                Files.createDirectories(targetOutput);
                List<String> command = buildCommand(project, compilerInfo.getJavacPath(), targetOutput,
                        entry.getValue(), targetVersion.getFeatureVersion());
                processOutput.append(runProcess(command, workspaceService.sourceDir(project),
                        () -> taskService.isCancelled(task.getId())));
                compiledOutputs.put(entry.getKey(), targetOutput);
            }

            taskService.running(task, PROGRESS_PREPARE_COMMIT, MESSAGE_COMPILE_PREPARE_COMMIT);
            taskService.ensureNotCancelled(task.getId());
            compileCommit = prepareCompileCommit(project, compiledOutputs, compileRunDir.resolve(BACKUP_DIR));

            // 实际写回只发生在全部 javac 成功之后；提交异常或任务取消由 finally 恢复备份。
            applyCompiledOutputs(project, compiledOutputs, () -> taskService.isCancelled(task.getId()));
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
                restoreCompileCommit(compileCommit);
            }
            if (!completed && artifactRecordsUpdated) {
                compiledArtifactRepository.replaceProjectArtifacts(project.getId(), previousArtifacts, clockService.now());
            }
            if (compileRunDir != null) {
                deleteTree(compileRunDir);
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
        entries.add(classRoot(project).toString());
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
     * 在写回前备份所有可能被替换的 class 和嵌套 Jar。
     *
     * @param project         项目记录
     * @param compiledOutputs 各目标编译输出
     * @param backupRoot      本次备份根目录
     * @return 可用于失败恢复的提交信息
     * @throws IOException 备份失败时抛出
     */
    private CompileCommit prepareCompileCommit(ProjectRecord project,
                                               Map<String, Path> compiledOutputs,
                                               Path backupRoot) throws IOException {
        CompileCommit commit = new CompileCommit();
        Files.createDirectories(backupRoot);
        for (Map.Entry<String, Path> entry : compiledOutputs.entrySet()) {
            if (JarPatchConstants.COMPILE_TARGET_MAIN.equals(entry.getKey())) {
                backupMainClasses(project, entry.getValue(), backupRoot.resolve(BACKUP_MAIN_DIR), commit);
            } else {
                backupNestedJar(project, entry.getKey(), backupRoot.resolve(BACKUP_NESTED_DIR), commit);
            }
        }
        return commit;
    }

    /**
     * 备份主 classes 目录中本次会被覆盖的文件，并记录原先不存在的新增文件。
     *
     * @param project      项目记录
     * @param compiledDir  主目标编译输出
     * @param backupRoot   主 class 备份目录
     * @param commit       提交恢复信息
     * @throws IOException 备份失败时抛出
     */
    private void backupMainClasses(ProjectRecord project,
                                   Path compiledDir,
                                   Path backupRoot,
                                   CompileCommit commit) throws IOException {
        Path targetRoot = classRoot(project);
        for (Path classFile : collectClassFiles(compiledDir)) {
            Path relativePath = compiledDir.relativize(classFile);
            Path target = targetRoot.resolve(relativePath).normalize();
            if (Files.exists(target)) {
                Path backup = backupRoot.resolve(relativePath).normalize();
                Files.createDirectories(backup.getParent());
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                commit.originalFiles.put(target, backup);
            } else {
                commit.newFiles.add(target);
            }
        }
    }

    /**
     * 完整备份本次会重建的嵌套 Jar。
     *
     * @param project       项目记录
     * @param compileTarget 嵌套 Jar 相对路径
     * @param backupRoot    嵌套 Jar 备份目录
     * @param commit        提交恢复信息
     * @throws IOException 备份失败时抛出
     */
    private void backupNestedJar(ProjectRecord project,
                                 String compileTarget,
                                 Path backupRoot,
                                 CompileCommit commit) throws IOException {
        Path target = workspaceService.resolveExtracted(project, compileTarget);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH);
        }
        Files.createDirectories(backupRoot);
        Path backup = backupRoot.resolve(safeCompileTargetName(compileTarget) + ".backup");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        commit.originalFiles.put(target, backup);
    }

    /**
     * 按目标应用全部已成功编译的 class。
     *
     * @param project         项目记录
     * @param compiledOutputs 各目标编译输出
     * @param cancelRequested 取消检查回调
     * @throws IOException 写回失败时抛出
     */
    private void applyCompiledOutputs(ProjectRecord project,
                                      Map<String, Path> compiledOutputs,
                                      BooleanSupplier cancelRequested) throws IOException {
        for (Map.Entry<String, Path> entry : compiledOutputs.entrySet()) {
            ensureNotCancelled(cancelRequested);
            if (JarPatchConstants.COMPILE_TARGET_MAIN.equals(entry.getKey())) {
                copyCompiledClasses(entry.getValue(), classRoot(project), cancelRequested);
            } else {
                Path nestedJar = workspaceService.resolveExtracted(project, entry.getKey());
                archiveService.replaceClassesInJar(nestedJar, entry.getValue(), cancelRequested);
            }
        }
    }

    /**
     * 把主目标 class 逐文件原子替换到 class 根目录。
     *
     * @param compiledDir     编译输出目录
     * @param targetRoot      class 根目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 写回失败时抛出
     */
    private void copyCompiledClasses(Path compiledDir,
                                     Path targetRoot,
                                     BooleanSupplier cancelRequested) throws IOException {
        for (Path classFile : collectClassFiles(compiledDir)) {
            ensureNotCancelled(cancelRequested);
            Path target = targetRoot.resolve(compiledDir.relativize(classFile)).normalize();
            atomicReplaceFile(classFile, target);
        }
    }

    /**
     * 恢复提交前所有原文件并删除本次新增 class。
     *
     * @param commit 提交恢复信息
     * @throws IOException 恢复失败时抛出
     */
    private void restoreCompileCommit(CompileCommit commit) throws IOException {
        for (Map.Entry<Path, Path> entry : commit.originalFiles.entrySet()) {
            atomicReplaceFile(entry.getValue(), entry.getKey());
        }
        for (Path newFile : commit.newFiles) {
            Files.deleteIfExists(newFile);
        }
    }

    /**
     * 使用目标目录内临时文件原子替换单个文件。
     *
     * @param source 完整源文件
     * @param target 最终目标文件
     * @throws IOException 文件系统不支持原子移动或复制失败时抛出
     */
    private void atomicReplaceFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporaryFile = Files.createTempFile(target.getParent(), "." + target.getFileName() + ".", ".tmp");
        try {
            Files.copy(source, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(JarPatchConstants.MESSAGE_EXPORT_ATOMIC_MOVE_REQUIRED, exception);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * 收集编译目录内全部 class 文件。
     *
     * @param compiledDir 编译输出目录
     * @return 按路径排序的 class 文件
     * @throws IOException 遍历失败时抛出
     */
    private List<Path> collectClassFiles(Path compiledDir) throws IOException {
        try (var stream = Files.walk(compiledDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
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
            for (Path classFile : collectClassFiles(entry.getValue())) {
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

    /**
     * 执行 javac 并在取消时终止子进程。
     *
     * @param command         命令参数
     * @param workDir        工作目录
     * @param cancelRequested 取消检查回调
     * @return javac 输出
     * @throws IOException          进程启动失败时抛出
     * @throws InterruptedException 进程等待被中断时抛出
     */
    private String runProcess(List<String> command,
                              Path workDir,
                              BooleanSupplier cancelRequested) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        Thread readerThread = new Thread(() -> readProcessOutput(process, output), "jarpatch-javac-output");
        readerThread.setDaemon(true);
        readerThread.start();
        try {
            while (process.isAlive()) {
                if (cancelRequested.getAsBoolean()) {
                    process.destroyForcibly();
                    readerThread.join();
                    throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
                }
                process.waitFor(PROCESS_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            }
            readerThread.join();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString();
    }

    /**
     * 持续读取 javac 合并输出流。
     *
     * @param process javac 进程
     * @param output  输出缓冲
     */
    private void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (IOException ignored) {
            // 取消会关闭进程流，读取线程在此正常结束，主线程负责输出最终任务状态。
        }
    }

    /**
     * 删除本次编译 staging 目录。
     *
     * @param root staging 根目录
     * @throws IOException 删除失败时抛出
     */
    private void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * 检查任务取消状态。
     *
     * @param cancelRequested 取消检查回调
     */
    private void ensureNotCancelled(BooleanSupplier cancelRequested) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
        }
    }

    /**
     * 把嵌套 Jar 路径转换为 staging 目录安全名称。
     *
     * @param compileTarget 编译目标
     * @return 安全目录名
     */
    private String safeCompileTargetName(String compileTarget) {
        return compileTarget.replace(JarPatchConstants.ZIP_SEPARATOR, "_")
                .replace(WINDOWS_PATH_SEPARATOR, '_');
    }

    /**
     * 按包类型解析主 class 根目录。
     *
     * @param project 项目记录
     * @return 主 class 根目录
     */
    private Path classRoot(ProjectRecord project) {
        Path extractedDir = workspaceService.extractedDir(project);
        if (PackageType.SPRING_BOOT_JAR.getCode().equals(project.getPackageType())) {
            return extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR);
        }
        if (PackageType.WAR.getCode().equals(project.getPackageType())) {
            return extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR);
        }
        return extractedDir;
    }

    /**
     * 单次编译提交恢复信息。
     */
    private static final class CompileCommit {

        private final Map<Path, Path> originalFiles = new LinkedHashMap<>();
        private final Set<Path> newFiles = new LinkedHashSet<>();
    }
}
