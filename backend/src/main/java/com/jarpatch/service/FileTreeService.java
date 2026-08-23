package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.FileNode;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
     * 构建只包含工作区根节点的懒加载文件树。
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
        root.setHasChildren(true);
        root.setChildrenLoaded(true);
        List<FileNode> children = new ArrayList<>();
        children.add(buildNode(workspaceService.sourceDir(project), workspaceService.sourceDir(project), "sources"));
        children.add(buildNode(workspaceService.extractedDir(project), workspaceService.extractedDir(project), "extracted"));
        root.setChildren(children);
        return root;
    }

    /**
     * 按前端展开动作只读取一个目录的直接子节点。
     *
     * @param project 项目记录
     * @param parentPath 文件树父目录路径，必须位于 sources 或 extracted
     * @return 已排序的直接子节点
     * @throws IOException 目录读取失败时抛出
     */
    public List<FileNode> loadChildren(ProjectRecord project, String parentPath) throws IOException {
        ResolvedTreePath resolved = resolveTreePath(project, parentPath);
        if (!Files.isDirectory(resolved.path())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_TREE_DIRECTORY_REQUIRED);
        }
        try (Stream<Path> stream = Files.list(resolved.path())) {
            return stream
                    .sorted(Comparator
                            .comparing((Path candidate) -> candidate.getFileName().toString(),
                                    String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(candidate -> candidate.getFileName().toString()))
                    .map(candidate -> buildChild(resolved.root(), candidate, resolved.prefix()))
                    .toList();
        }
    }

    /**
     * 构建单个文件树节点；目录只标记是否存在子节点，不递归读取。
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
            node.setHasChildren(hasChildren(path));
            node.setChildrenLoaded(false);
        }
        else {
            node.setHasChildren(false);
            node.setChildrenLoaded(true);
        }
        return node;
    }

    /**
     * 判断目录是否至少包含一个直接子节点。
     *
     * @param path 目录路径
     * @return 存在子节点时返回 true
     * @throws IOException 目录读取失败时抛出
     */
    private boolean hasChildren(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.findAny().isPresent();
        }
    }

    /**
     * 解析并校验前端请求的文件树目录路径。
     *
     * @param project 项目记录
     * @param parentPath 前端文件树路径
     * @return 安全根目录、实际目录和树前缀
     */
    private ResolvedTreePath resolveTreePath(ProjectRecord project, String parentPath) {
        if ("sources".equals(parentPath)) {
            Path root = workspaceService.sourceDir(project);
            return new ResolvedTreePath(root, root, "sources");
        }
        if (parentPath != null && parentPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            Path root = workspaceService.sourceDir(project);
            return new ResolvedTreePath(root,
                    workspaceService.resolveSource(project,
                            parentPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length())),
                    "sources");
        }
        if ("extracted".equals(parentPath)) {
            Path root = workspaceService.extractedDir(project);
            return new ResolvedTreePath(root, root, "extracted");
        }
        if (parentPath != null && parentPath.startsWith(JarPatchConstants.TREE_EXTRACTED_PREFIX)) {
            Path root = workspaceService.extractedDir(project);
            return new ResolvedTreePath(root,
                    workspaceService.resolveExtracted(project,
                            parentPath.substring(JarPatchConstants.TREE_EXTRACTED_PREFIX.length())),
                    "extracted");
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_TREE_PATH_INVALID);
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
            throw new IllegalStateException(JarPatchConstants.MESSAGE_FILE_TREE_BUILD_FAILED + candidate, e);
        }
    }

    /**
     * 文件树路径解析结果。
     *
     * @param root sources 或 extracted 根目录
     * @param path 当前实际目录
     * @param prefix 文件树前缀
     */
    private record ResolvedTreePath(Path root, Path path, String prefix) {
    }
}
