package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.DiffReport;
import com.jarpatch.model.FileContentView;
import com.jarpatch.model.FileDiff;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.repository.CompiledArtifactRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 项目导出前可靠差异服务。
 * <p>
 * 服务从导入时固定 baseline 与当前工作区收集可编辑文件并按原始字节 SHA-256 比较，
 * 返回真实源码/资源差异；已提交 class 清单直接读取 compiled_artifacts，不用文件名猜测。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class DiffService {

    private static final String CATEGORY_SOURCE = "SOURCE";
    private static final String CATEGORY_RESOURCE = "RESOURCE";
    private static final String STATUS_ADDED = "ADDED";
    private static final String STATUS_MODIFIED = "MODIFIED";
    private static final String STATUS_DELETED = "DELETED";

    private final WorkspaceService workspaceService;
    private final FileKindService fileKindService;
    private final FileContentService fileContentService;
    private final CompiledArtifactRepository compiledArtifactRepository;

    /**
     * 创建项目差异服务。
     *
     * @param workspaceService          工作区服务
     * @param fileKindService           文件类型服务
     * @param fileContentService        保真文件读取服务
     * @param compiledArtifactRepository 编译产物仓储
     */
    public DiffService(WorkspaceService workspaceService,
                       FileKindService fileKindService,
                       FileContentService fileContentService,
                       CompiledArtifactRepository compiledArtifactRepository) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
        this.fileContentService = fileContentService;
        this.compiledArtifactRepository = compiledArtifactRepository;
    }

    /**
     * 生成项目当前相对导入基线的完整差异报告。
     *
     * @param project 项目记录
     * @return 源码、资源和 class 产物清单
     * @throws IOException 遍历或读取文件失败时抛出
     */
    public DiffReport compare(ProjectRecord project) throws IOException {
        Set<String> paths = new TreeSet<>();
        collectEditablePaths(workspaceService.baselineDir(project), paths);
        collectEditablePaths(workspaceService.projectRoot(project), paths);

        List<FileDiff> sourceDiffs = new ArrayList<>();
        List<FileDiff> resourceDiffs = new ArrayList<>();
        for (String path : paths) {
            FileDiff fileDiff = compareFile(project, path);
            if (fileDiff == null) {
                continue;
            }
            if (CATEGORY_SOURCE.equals(fileDiff.getCategory())) {
                sourceDiffs.add(fileDiff);
            } else {
                resourceDiffs.add(fileDiff);
            }
        }

        DiffReport report = new DiffReport();
        report.setSourceDiffs(sourceDiffs);
        report.setResourceDiffs(resourceDiffs);
        report.setCompiledArtifacts(compiledArtifactRepository.findPaths(project.getId()));
        return report;
    }

    /**
     * 收集工作区或 baseline 下 sources、extracted 两类可编辑文件路径。
     *
     * @param root  项目根或 baseline 根
     * @param paths 输出路径集合
     * @throws IOException 遍历失败时抛出
     */
    private void collectEditablePaths(Path root, Set<String> paths) throws IOException {
        collectEditableRoot(root.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR),
                JarPatchConstants.WORKSPACE_SOURCE_DIR, paths);
        collectEditableRoot(root.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR),
                JarPatchConstants.WORKSPACE_EXTRACTED_DIR, paths);
    }

    /**
     * 收集单个文件树根目录的可编辑文件。
     *
     * @param fileRoot 文件根目录
     * @param prefix   文件树前缀
     * @param paths    输出路径集合
     * @throws IOException 遍历失败时抛出
     */
    private void collectEditableRoot(Path fileRoot, String prefix, Set<String> paths) throws IOException {
        if (!Files.isDirectory(fileRoot)) {
            return;
        }
        try (var stream = Files.walk(fileRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> fileKindService.detect(path).isEditable())
                    .map(path -> prefix + JarPatchConstants.ZIP_SEPARATOR
                            + fileRoot.relativize(path).toString().replace('\\', '/'))
                    .forEach(paths::add);
        }
    }

    /**
     * 比较单个文件的 baseline 与当前内容。
     *
     * @param project 项目记录
     * @param path    文件树路径
     * @return 有差异时返回差异模型，无差异时返回 null
     * @throws IOException 文件读取失败时抛出
     */
    private FileDiff compareFile(ProjectRecord project, String path) throws IOException {
        Path baselinePath = workspaceService.resolveBaseline(project, path);
        Path currentPath = resolveCurrentPath(project, path);
        boolean baselineExists = Files.isRegularFile(baselinePath);
        boolean currentExists = Files.isRegularFile(currentPath);
        FileContentView baseline = baselineExists ? fileContentService.readPath(baselinePath) : null;
        FileContentView current = currentExists ? fileContentService.readPath(currentPath) : null;
        if (baseline != null && current != null && baseline.getContentHash().equals(current.getContentHash())) {
            return null;
        }

        FileDiff diff = new FileDiff();
        diff.setPath(path);
        diff.setCategory(path.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX) ? CATEGORY_SOURCE : CATEGORY_RESOURCE);
        diff.setStatus(!baselineExists ? STATUS_ADDED : !currentExists ? STATUS_DELETED : STATUS_MODIFIED);
        diff.setOriginalHash(baseline == null ? null : baseline.getContentHash());
        diff.setCurrentHash(current == null ? null : current.getContentHash());
        diff.setOriginalContent(baseline == null ? null : baseline.getContent());
        diff.setCurrentContent(current == null ? null : current.getContent());
        return diff;
    }

    /**
     * 解析当前工作区内的 sources 或 extracted 文件路径。
     *
     * @param project 项目记录
     * @param path    文件树路径
     * @return 当前文件绝对路径
     */
    private Path resolveCurrentPath(ProjectRecord project, String path) {
        if (path.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            return workspaceService.resolveSource(project,
                    path.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length()));
        }
        if (path.startsWith(JarPatchConstants.TREE_EXTRACTED_PREFIX)) {
            return workspaceService.resolveExtracted(project,
                    path.substring(JarPatchConstants.TREE_EXTRACTED_PREFIX.length()));
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
    }
}
