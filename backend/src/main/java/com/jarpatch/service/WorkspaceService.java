package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.JarPatchProperties;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 项目工作区服务。
 * <p>
 * 该服务负责创建和定位项目工作区目录。导入服务把原始包写入 original，解压服务把内容
 * 写入 extracted，反编译服务把源码写入 sources，编译服务把 class 写入 compiled，导出服务
 * 把新包写入 exports。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class WorkspaceService {

    private final JarPatchProperties properties;

    /**
     * 创建工作区服务。
     *
     * @param properties JarPatch 配置属性
     */
    public WorkspaceService(JarPatchProperties properties) {
        this.properties = properties;
    }

    /**
     * 为项目创建完整工作区目录。
     *
     * @param projectId 项目 ID
     * @return 项目根工作区路径
     * @throws IOException 创建目录失败时抛出
     */
    public Path createProjectWorkspace(String projectId) throws IOException {
        Path projectRoot = projectPath(projectId);
        createWorkspaceDirectories(projectRoot);
        return projectRoot;
    }

    /**
     * 创建带未完成标记的导入工作区。
     * <p>
     * 导入入口直接使用项目最终路径，但在完整导入前只存在 .importing 标记且不写项目记录；
     * 失败时删除本次目录，避免依赖 Windows 对非空目录的原子重命名能力。
     * </p>
     *
     * @param projectId 项目 ID
     * @return 尚未发布的项目工作区路径
     * @throws IOException 创建目录失败时抛出
     */
    public Path createImportWorkspace(String projectId) throws IOException {
        Path projectRoot = projectPath(projectId);
        Files.createDirectories(workspaceRoot());
        try {
            Files.createDirectory(projectRoot);
            Files.createFile(projectRoot.resolve(JarPatchConstants.WORKSPACE_IMPORTING_MARKER));
            createWorkspaceDirectories(projectRoot);
            return projectRoot;
        } catch (IOException | RuntimeException exception) {
            try {
                deleteWorkspace(projectRoot);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    /**
     * 原子发布已完整构建的导入工作区状态。
     * <p>
     * 仅把同目录的 .importing 标记原子移动为 .ready，不移动非空目录；项目服务随后才写入
     * SQLite 项目记录。任一步失败时整个目录仍由导入回滚流程删除。
     * </p>
     *
     * @param importRoot 已完整构建但尚未发布的工作区
     * @return 已就绪项目工作区路径
     * @throws IOException 原子移动失败时抛出
     */
    public Path markImportReady(Path importRoot) throws IOException {
        Path normalizedRoot = requireInsideWorkspaceRoot(importRoot);
        Path importingMarker = normalizedRoot.resolve(JarPatchConstants.WORKSPACE_IMPORTING_MARKER);
        Path readyMarker = normalizedRoot.resolve(JarPatchConstants.WORKSPACE_READY_MARKER);
        if (!Files.isRegularFile(importingMarker) || Files.exists(readyMarker)) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_IMPORT_STATE_INVALID);
        }
        try {
            Files.move(importingMarker, readyMarker, StandardCopyOption.ATOMIC_MOVE);
            return normalizedRoot;
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(JarPatchConstants.MESSAGE_WORKSPACE_ATOMIC_MOVE_REQUIRED, exception);
        }
    }

    /**
     * 启动时清理上次进程中断留下且未写入数据库的导入工作区。
     * <p>
     * 入口在应用就绪恢复服务；只处理带 .importing/.ready 标记的新协议目录和旧版
     * .staging- 目录；带 .ready 标记但已无历史记录的目录交给孤立工作区预览入口，
     * 启动过程不会推断或删除。
     * </p>
     *
     * @param registeredWorkspacePaths SQLite 已登记的工作区路径
     * @throws IOException 扫描或清理失败时抛出
     */
    public void recoverIncompleteImports(Set<String> registeredWorkspacePaths) throws IOException {
        Path root = workspaceRoot();
        Files.createDirectories(root);
        Set<Path> registeredPaths = new HashSet<>();
        for (String registeredPath : registeredWorkspacePaths) {
            registeredPaths.add(Paths.get(registeredPath).toAbsolutePath().normalize());
        }
        List<Path> candidates;
        try (Stream<Path> stream = Files.list(root)) {
            candidates = stream.filter(Files::isDirectory).toList();
        }
        for (Path path : candidates) {
            Path candidate = path.toAbsolutePath().normalize();
            boolean legacyStaging = candidate.getFileName().toString()
                    .startsWith(JarPatchConstants.WORKSPACE_STAGING_PREFIX);
            boolean importing = Files.exists(candidate.resolve(JarPatchConstants.WORKSPACE_IMPORTING_MARKER));
            if (legacyStaging || (importing && !registeredPaths.contains(candidate))) {
                deleteWorkspace(candidate);
            }
        }
    }

    /**
     * 删除失败操作留下的单个临时或正式项目工作区。
     *
     * @param workspacePath 待删除工作区路径
     * @throws IOException 删除失败时抛出
     */
    public void deleteWorkspace(Path workspacePath) throws IOException {
        Path normalizedPath = requireInsideWorkspaceRoot(workspacePath);
        if (normalizedPath.equals(workspaceRoot()) || !Files.exists(normalizedPath)) {
            return;
        }
        try (var stream = Files.walk(normalizedPath)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * 创建项目工作区固定目录。
     *
     * @param projectRoot 项目或临时工作区根目录
     * @throws IOException 创建目录失败时抛出
     */
    private void createWorkspaceDirectories(Path projectRoot) throws IOException {
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_ORIGINAL_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_COMPILED_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_EXPORT_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_BASELINE_DIR));
    }

    /**
     * 获取规范化工作区总根目录。
     *
     * @return 工作区总根目录
     */
    public Path workspaceRoot() {
        return Paths.get(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
    }

    /**
     * 获取指定项目的正式工作区路径。
     *
     * @param projectId 项目 ID
     * @return 正式工作区路径
     */
    public Path projectPath(String projectId) {
        return workspaceRoot().resolve(projectId).normalize();
    }

    /**
     * 校验路径严格位于工作区总根目录内。
     *
     * @param path 待校验路径
     * @return 规范化路径
     */
    private Path requireInsideWorkspaceRoot(Path path) {
        Path root = workspaceRoot();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.equals(root) || !normalizedPath.startsWith(root)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        return normalizedPath;
    }

    /**
     * 获取项目工作区根目录。
     *
     * @param project 项目记录
     * @return 项目工作区路径
     */
    public Path projectRoot(ProjectRecord project) {
        return Paths.get(project.getWorkspacePath()).toAbsolutePath().normalize();
    }

    /**
     * 获取项目解压目录。
     *
     * @param project 项目记录
     * @return 解压目录路径
     */
    public Path extractedDir(ProjectRecord project) {
        return projectRoot(project).resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR).normalize();
    }

    /**
     * 获取项目反编译源码目录。
     *
     * @param project 项目记录
     * @return 反编译源码目录路径
     */
    public Path sourceDir(ProjectRecord project) {
        return projectRoot(project).resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR).normalize();
    }

    /**
     * 获取项目编译输出目录。
     *
     * @param project 项目记录
     * @return 编译输出目录路径
     */
    public Path compiledDir(ProjectRecord project) {
        return projectRoot(project).resolve(JarPatchConstants.WORKSPACE_COMPILED_DIR).normalize();
    }

    /**
     * 获取项目导出目录。
     *
     * @param project 项目记录
     * @return 导出目录路径
     */
    public Path exportDir(ProjectRecord project) {
        return projectRoot(project).resolve(JarPatchConstants.WORKSPACE_EXPORT_DIR).normalize();
    }

    /**
     * 获取项目导入基线目录。
     *
     * @param project 项目记录
     * @return 基线目录路径
     */
    public Path baselineDir(ProjectRecord project) {
        return projectRoot(project).resolve(JarPatchConstants.WORKSPACE_BASELINE_DIR).normalize();
    }

    /**
     * 解析项目基线目录内的安全文件树路径。
     *
     * @param project      项目记录
     * @param relativePath 带 sources 或 extracted 前缀的文件树路径
     * @return 基线文件绝对路径
     */
    public Path resolveBaseline(ProjectRecord project, String relativePath) {
        return resolveInside(baselineDir(project), relativePath);
    }

    /**
     * 解析解压目录内的安全相对路径。
     *
     * @param project      项目记录
     * @param relativePath 相对路径
     * @return 解压目录内的绝对路径
     */
    public Path resolveExtracted(ProjectRecord project, String relativePath) {
        return resolveInside(extractedDir(project), relativePath);
    }

    /**
     * 解析源码目录内的安全相对路径。
     *
     * @param project      项目记录
     * @param relativePath 相对路径
     * @return 源码目录内的绝对路径
     */
    public Path resolveSource(ProjectRecord project, String relativePath) {
        return resolveInside(sourceDir(project), relativePath);
    }

    /**
     * 解析指定根目录内的安全路径，防止相对路径逃逸工作区。
     *
     * @param root         根目录
     * @param relativePath 相对路径
     * @return 根目录内绝对路径
     */
    private Path resolveInside(Path root, String relativePath) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath == null
                ? JarPatchConstants.EMPTY_TEXT : relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        return resolved;
    }
}
