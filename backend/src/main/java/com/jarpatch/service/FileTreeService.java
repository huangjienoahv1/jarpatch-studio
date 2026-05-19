package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.model.FileNode;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件树服务。
 * <p>
 * 工作台加载项目时调用该服务读取 extracted 目录结构，返回给 Electron 前端展示目录树。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class FileTreeService {

    private final WorkspaceService workspaceService;
    private final FileKindService fileKindService;

    /**
     * 创建文件树服务。
     *
     * @param workspaceService 工作区服务
     * @param fileKindService  文件类型服务
     */
    public FileTreeService(WorkspaceService workspaceService, FileKindService fileKindService) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
    }

    /**
     * 构建项目文件树。
     *
     * @param project 项目记录
     * @return 文件树根节点
     * @throws IOException 读取目录失败时抛出
     */
    public FileNode buildTree(ProjectRecord project) throws IOException {
        FileNode root = new FileNode();
        root.setName("workspace");
        root.setPath("");
        root.setKind(FileKind.DIRECTORY.getCode());
        root.setEditable(false);
        List<FileNode> children = new ArrayList<>();
        children.add(buildNode(workspaceService.sourceDir(project), workspaceService.sourceDir(project), "sources"));
        children.add(buildNode(workspaceService.extractedDir(project), workspaceService.extractedDir(project), "extracted"));
        root.setChildren(children);
        return root;
    }

    /**
     * 递归构建文件树节点。
     *
     * @param root 当前项目解压根目录
     * @param path 当前文件路径
     * @return 文件树节点
     * @throws IOException 读取目录失败时抛出
     */
    private FileNode buildNode(Path root, Path path, String prefix) throws IOException {
        FileKind kind = fileKindService.detect(path);
        FileNode node = new FileNode();
        node.setName(root.equals(path) ? prefix : path.getFileName().toString());
        node.setPath(root.equals(path) ? prefix : prefix + "/" + root.relativize(path).toString().replace('\\', '/'));
        node.setKind(kind.getCode());
        node.setEditable(kind.isEditable());
        if (Files.isDirectory(path)) {
            List<FileNode> children = Files.list(path)
                    .sorted(Comparator.comparing(candidate -> candidate.getFileName().toString().toLowerCase()))
                    .map(candidate -> buildChild(root, candidate, prefix))
                    .collect(Collectors.toList());
            node.setChildren(children);
        }
        return node;
    }

    /**
     * 包装子节点构建异常，便于 stream 递归调用。
     *
     * @param root      解压根目录
     * @param candidate 子路径
     * @return 子节点
     */
    private FileNode buildChild(Path root, Path candidate, String prefix) {
        try {
            return buildNode(root, candidate, prefix);
        } catch (IOException e) {
            throw new IllegalStateException("构建文件树失败: " + candidate, e);
        }
    }
}
