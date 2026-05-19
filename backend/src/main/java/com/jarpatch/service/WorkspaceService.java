package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.JarPatchProperties;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path root = Paths.get(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
        Path projectRoot = root.resolve(projectId).normalize();
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_ORIGINAL_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_COMPILED_DIR));
        Files.createDirectories(projectRoot.resolve(JarPatchConstants.WORKSPACE_EXPORT_DIR));
        return projectRoot;
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
        Path resolved = normalizedRoot.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        return resolved;
    }
}
