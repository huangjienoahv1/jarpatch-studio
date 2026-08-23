package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 编译产物原子提交与失败恢复服务。
 * <p>
 * CompileService 在全部 javac 目标成功后调用本服务。实际写回点在 extracted 主 classes 或
 * 嵌套 Jar；写回前完整备份被替换文件，调用方失败时使用同一提交对象恢复。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class CompileArtifactCommitter {

    private static final String BACKUP_MAIN_DIR = "main";
    private static final String BACKUP_NESTED_DIR = "nested";
    private static final char WINDOWS_PATH_SEPARATOR = '\\';

    private final WorkspaceService workspaceService;
    private final ArchiveService archiveService;

    /**
     * 创建编译产物提交服务。
     *
     * @param workspaceService 工作区服务
     * @param archiveService   嵌套 Jar 重建服务
     */
    public CompileArtifactCommitter(WorkspaceService workspaceService, ArchiveService archiveService) {
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
    }

    /**
     * 备份所有可能被本次提交替换的主 class 和嵌套 Jar。
     *
     * @param project         项目记录
     * @param compiledOutputs 各目标编译输出
     * @param backupRoot      本次备份根目录
     * @return 失败恢复信息
     * @throws IOException 备份失败时抛出
     */
    public CompileCommit prepare(ProjectRecord project,
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
     * 按目标应用全部已成功编译的 class。
     *
     * @param project         项目记录
     * @param compiledOutputs 各目标编译输出
     * @param cancelRequested 取消检查回调
     * @throws IOException 写回失败时抛出
     */
    public void apply(ProjectRecord project,
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
     * 恢复提交前所有原文件并删除本次新增 class。
     *
     * @param commit 提交恢复信息
     * @throws IOException 恢复失败时抛出
     */
    public void restore(CompileCommit commit) throws IOException {
        for (Map.Entry<Path, Path> entry : commit.originalFiles.entrySet()) {
            atomicReplaceFile(entry.getValue(), entry.getKey());
        }
        for (Path newFile : commit.newFiles) {
            Files.deleteIfExists(newFile);
        }
    }

    /**
     * 收集编译目录内全部 class 文件。
     *
     * @param compiledDir 编译输出目录
     * @return 按路径排序的 class 文件
     * @throws IOException 遍历失败时抛出
     */
    public List<Path> collectClassFiles(Path compiledDir) throws IOException {
        try (var stream = Files.walk(compiledDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    /**
     * 删除单次编译 staging 目录。
     *
     * @param root staging 根目录
     * @throws IOException 删除失败时抛出
     */
    public void deleteTree(Path root) throws IOException {
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
     * 把嵌套 Jar 路径转换为 staging 目录安全名称。
     *
     * @param compileTarget 编译目标
     * @return 安全目录名
     */
    public String safeTargetName(String compileTarget) {
        return compileTarget.replace(JarPatchConstants.ZIP_SEPARATOR, "_")
                .replace(WINDOWS_PATH_SEPARATOR, '_');
    }

    /**
     * 获取不同包类型的主 class 根目录。
     *
     * @param project 项目记录
     * @return 主 class 根目录
     */
    public Path classRoot(ProjectRecord project) {
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
     * 备份主 classes 中本次会被覆盖的文件，并记录原先不存在的新增文件。
     *
     * @param project     当前项目
     * @param compiledDir 编译产物目录
     * @param backupRoot  本次提交的备份目录
     * @param commit      提交恢复信息
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
     * @param project       当前项目
     * @param compileTarget 嵌套 Jar 相对路径
     * @param backupRoot    本次提交的备份目录
     * @param commit        提交恢复信息
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
        Path backup = backupRoot.resolve(safeTargetName(compileTarget) + ".backup");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        commit.originalFiles.put(target, backup);
    }

    /**
     * 把主目标 class 逐文件原子替换到 class 根目录。
     *
     * @param compiledDir    编译产物目录
     * @param targetRoot     class 写回根目录
     * @param cancelRequested 取消检查回调
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
     * 使用目标目录内临时文件原子替换单个文件。
     *
     * @param source 待提交文件
     * @param target 正式目标文件
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
     * 单次编译提交恢复信息，由本服务创建、应用并恢复。
     */
    public static final class CompileCommit {

        private final Map<Path, Path> originalFiles = new LinkedHashMap<>();
        private final Set<Path> newFiles = new LinkedHashSet<>();

        private CompileCommit() {
        }
    }
}
