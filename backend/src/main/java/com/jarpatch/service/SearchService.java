package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.SearchResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 项目文件搜索服务。
 * <p>
 * 搜索入口来自 /api/projects/{id}/search。该服务扫描 sources 与 extracted 两个工作区，
 * 搜索文件名、原始文本内容和中文 Unicode 转义还原后的内容，结果返回给前端搜索面板。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class SearchService {

    private final WorkspaceService workspaceService;
    private final FileKindService fileKindService;
    private final JavaUnicodeEscapeService javaUnicodeEscapeService;

    /**
     * 创建项目文件搜索服务。
     *
     * @param workspaceService        工作区服务
     * @param fileKindService         文件类型识别服务
     * @param javaUnicodeEscapeService Java 中文 Unicode 转义还原服务
     */
    public SearchService(WorkspaceService workspaceService,
                         FileKindService fileKindService,
                         JavaUnicodeEscapeService javaUnicodeEscapeService) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
        this.javaUnicodeEscapeService = javaUnicodeEscapeService;
    }

    /**
     * 搜索项目中的 Java 和文本资源文件。
     *
     * @param project 项目记录
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     * @throws IOException 读取工作区失败时抛出
     */
    public List<SearchResult> search(ProjectRecord project, String keyword) throws IOException {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_SEARCH_KEYWORD_EMPTY);
        }
        List<SearchResult> results = new ArrayList<>();
        searchRoot(workspaceService.sourceDir(project), JarPatchConstants.TREE_SOURCE_PREFIX, keyword.trim(), results);
        searchRoot(workspaceService.extractedDir(project), JarPatchConstants.TREE_EXTRACTED_PREFIX, keyword.trim(), results);
        return results;
    }

    /**
     * 扫描指定根目录。
     *
     * @param root    根目录
     * @param prefix  文件树路径前缀
     * @param keyword 搜索关键词
     * @param results 搜索结果集合
     * @throws IOException 读取目录失败时抛出
     */
    private void searchRoot(Path root, String prefix, String keyword, List<SearchResult> results) throws IOException {
        if (!Files.exists(root) || results.size() >= JarPatchConstants.SEARCH_MAX_RESULTS) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> searchFile(root, prefix, path, keyword, results));
        }
    }

    /**
     * 搜索单个文件。
     *
     * @param root    根目录
     * @param prefix  文件树路径前缀
     * @param file    文件路径
     * @param keyword 搜索关键词
     * @param results 搜索结果集合
     */
    private void searchFile(Path root, String prefix, Path file, String keyword, List<SearchResult> results) {
        if (results.size() >= JarPatchConstants.SEARCH_MAX_RESULTS) {
            return;
        }
        FileKind kind = fileKindService.detect(file);
        if (!kind.isEditable()) {
            return;
        }
        String treePath = prefix + root.relativize(file).toString().replace('\\', '/');
        if (file.getFileName().toString().contains(keyword)) {
            results.add(new SearchResult(treePath, 1, file.getFileName().toString()));
            return;
        }
        searchFileContent(treePath, file, keyword, results);
    }

    /**
     * 搜索文件内容。
     *
     * @param treePath 文件树路径
     * @param file     文件路径
     * @param keyword  搜索关键词
     * @param results  搜索结果集合
     */
    private void searchFileContent(String treePath, Path file, String keyword, List<SearchResult> results) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String rawLine = lines.get(index);
                String decodedLine = javaUnicodeEscapeService.decodeChineseEscapes(rawLine);
                if (rawLine.contains(keyword) || decodedLine.contains(keyword)) {
                    results.add(new SearchResult(treePath, index + 1, preview(decodedLine)));
                    if (results.size() >= JarPatchConstants.SEARCH_MAX_RESULTS) {
                        return;
                    }
                }
            }
        } catch (IOException ignored) {
            // 单个文件读取失败不影响其他文件搜索，失败文件不会出现在结果中。
        }
    }

    /**
     * 截断搜索预览文本。
     *
     * @param line 命中行
     * @return 预览文本
     */
    private String preview(String line) {
        String trimmed = line.trim();
        if (trimmed.length() <= JarPatchConstants.SEARCH_PREVIEW_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, JarPatchConstants.SEARCH_PREVIEW_MAX_LENGTH);
    }
}
