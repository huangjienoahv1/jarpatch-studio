package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.repository.FileChangeRepository;
import com.jarpatch.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件内容读写服务。
 * <p>
 * 前端打开文件时调用读取方法；用户保存 Java 或资源文件时调用保存方法，内容写入实际
 * 工作区文件，同时修改记录写入 SQLite file_changes 表。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class FileContentService {

    private final WorkspaceService workspaceService;
    private final FileKindService fileKindService;
    private final FileChangeRepository fileChangeRepository;
    private final ProjectRepository projectRepository;
    private final ClockService clockService;
    private final JavaUnicodeEscapeService javaUnicodeEscapeService;

    /**
     * 创建文件内容服务。
     *
     * @param workspaceService     工作区服务
     * @param fileKindService      文件类型服务
     * @param fileChangeRepository 修改记录仓储
     * @param projectRepository    项目仓储
     * @param clockService         时间服务
     * @param javaUnicodeEscapeService Java 中文 Unicode 转义还原服务
     */
    public FileContentService(WorkspaceService workspaceService,
                              FileKindService fileKindService,
                              FileChangeRepository fileChangeRepository,
                              ProjectRepository projectRepository,
                              ClockService clockService,
                              JavaUnicodeEscapeService javaUnicodeEscapeService) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
        this.fileChangeRepository = fileChangeRepository;
        this.projectRepository = projectRepository;
        this.clockService = clockService;
        this.javaUnicodeEscapeService = javaUnicodeEscapeService;
    }

    /**
     * 读取可编辑文件内容。
     *
     * @param project      项目记录
     * @param relativePath 文件相对路径
     * @return UTF-8 文件内容
     * @throws IOException 读取失败时抛出
     */
    public String read(ProjectRecord project, String relativePath) throws IOException {
        Path path = resolveEditablePath(project, relativePath);
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return javaUnicodeEscapeService.decodeChineseEscapes(content);
    }

    /**
     * 保存可编辑文件内容并记录修改。
     *
     * @param project      项目记录
     * @param relativePath 文件相对路径
     * @param content      新文件内容
     * @throws IOException 写入失败时抛出
     */
    public void save(ProjectRecord project, String relativePath, String content) throws IOException {
        Path path = resolveEditablePath(project, relativePath);
        FileKind kind = fileKindService.detect(path);
        Files.writeString(path, content, StandardCharsets.UTF_8);

        // 入口在保存文件接口，实际写入点是工作区文件，结果同时写入 file_changes 修改记录表。
        String now = clockService.now();
        fileChangeRepository.upsert(project.getId(), relativePath, kind.getCode(), now);
        projectRepository.touch(project.getId(), now);
    }

    /**
     * 解析并校验可编辑文件路径。
     *
     * @param project      项目记录
     * @param relativePath 文件相对路径
     * @return 可编辑文件绝对路径
     */
    private Path resolveEditablePath(ProjectRecord project, String relativePath) {
        Path path = resolveWorkspacePath(project, relativePath);
        FileKind kind = fileKindService.detect(path);
        if (!kind.isEditable()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_NOT_EDITABLE);
        }
        return path;
    }

    /**
     * 根据文件树前缀解析真实工作区路径。
     *
     * @param project      项目记录
     * @param relativePath 带 sources 或 extracted 前缀的文件树路径
     * @return 工作区真实文件路径
     */
    private Path resolveWorkspacePath(ProjectRecord project, String relativePath) {
        if (relativePath != null && relativePath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            return workspaceService.resolveSource(project, relativePath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length()));
        }
        if (relativePath != null && relativePath.startsWith(JarPatchConstants.TREE_EXTRACTED_PREFIX)) {
            return workspaceService.resolveExtracted(project, relativePath.substring(JarPatchConstants.TREE_EXTRACTED_PREFIX.length()));
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
    }
}
