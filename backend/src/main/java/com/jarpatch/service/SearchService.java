package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.SearchResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.Charset;
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
    private final ProjectSettingsService projectSettingsService;
    private static final int UTF_8_BOM_FIRST = 0xEF;
    private static final int UTF_8_BOM_SECOND = 0xBB;
    private static final int UTF_8_BOM_THIRD = 0xBF;
    private static final int UTF_16_LE_BOM_FIRST = 0xFF;
    private static final int UTF_16_LE_BOM_SECOND = 0xFE;
    private static final int UTF_16_BE_BOM_FIRST = 0xFE;
    private static final int UTF_16_BE_BOM_SECOND = 0xFF;

    /**
     * 创建项目文件搜索服务。
     *
     * @param workspaceService        工作区服务
     * @param fileKindService         文件类型识别服务
     * @param javaUnicodeEscapeService Java 中文 Unicode 转义还原服务
     * @param projectSettingsService 项目设置服务
     */
    public SearchService(WorkspaceService workspaceService,
                         FileKindService fileKindService,
                         JavaUnicodeEscapeService javaUnicodeEscapeService,
                         ProjectSettingsService projectSettingsService) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
        this.javaUnicodeEscapeService = javaUnicodeEscapeService;
        this.projectSettingsService = projectSettingsService;
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
        long maxFileBytes = projectSettingsService.maxEditableFileBytes(project.getId());
        searchRoot(workspaceService.sourceDir(project), JarPatchConstants.TREE_SOURCE_PREFIX,
                keyword.trim(), maxFileBytes, results);
        searchRoot(workspaceService.extractedDir(project), JarPatchConstants.TREE_EXTRACTED_PREFIX,
                keyword.trim(), maxFileBytes, results);
        return results;
    }

    /**
     * 扫描指定根目录。
     *
     * @param root    根目录
     * @param prefix  文件树路径前缀
     * @param keyword 搜索关键词
     * @param maxFileBytes 允许搜索内容的最大文件字节数
     * @param results 搜索结果集合
     * @throws IOException 读取目录失败时抛出
     */
    private void searchRoot(Path root,
                            String prefix,
                            String keyword,
                            long maxFileBytes,
                            List<SearchResult> results) throws IOException {
        if (!Files.exists(root) || results.size() >= JarPatchConstants.SEARCH_MAX_RESULTS) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            var iterator = stream.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext() && results.size() < JarPatchConstants.SEARCH_MAX_RESULTS) {
                searchFile(root, prefix, iterator.next(), keyword, maxFileBytes, results);
            }
        }
    }

    /**
     * 搜索单个文件。
     *
     * @param root    根目录
     * @param prefix  文件树路径前缀
     * @param file    文件路径
     * @param keyword 搜索关键词
     * @param maxFileBytes 允许搜索内容的最大文件字节数
     * @param results 搜索结果集合
     */
    private void searchFile(Path root,
                            String prefix,
                            Path file,
                            String keyword,
                            long maxFileBytes,
                            List<SearchResult> results) throws IOException {
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
        if (Files.size(file) > maxFileBytes) {
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
    private void searchFileContent(String treePath,
                                   Path file,
                                   String keyword,
                                   List<SearchResult> results) throws IOException {
        Charset charset = detectCharset(file);
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String rawLine;
            int lineNumber = 0;
            while ((rawLine = reader.readLine()) != null) {
                lineNumber++;
                String decodedLine = javaUnicodeEscapeService.decodeChineseEscapes(rawLine);
                if (rawLine.contains(keyword) || decodedLine.contains(keyword)) {
                    results.add(new SearchResult(treePath, lineNumber, preview(decodedLine)));
                    if (results.size() >= JarPatchConstants.SEARCH_MAX_RESULTS) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * 根据 BOM 识别搜索文本编码，未带 BOM 时严格按 UTF-8 读取。
     *
     * @param file 文本文件
     * @return 搜索使用的字符集
     * @throws IOException 读取 BOM 失败时抛出
     */
    private Charset detectCharset(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            int first = inputStream.read();
            int second = inputStream.read();
            int third = inputStream.read();
            if (first == UTF_8_BOM_FIRST && second == UTF_8_BOM_SECOND && third == UTF_8_BOM_THIRD) {
                return StandardCharsets.UTF_8;
            }
            if ((first == UTF_16_LE_BOM_FIRST && second == UTF_16_LE_BOM_SECOND)
                    || (first == UTF_16_BE_BOM_FIRST && second == UTF_16_BE_BOM_SECOND)) {
                return StandardCharsets.UTF_16;
            }
            return StandardCharsets.UTF_8;
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
