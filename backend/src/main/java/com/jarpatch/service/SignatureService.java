package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Jar/War 根签名文件识别服务。
 * <p>
 * 导出门禁使用该服务检查 extracted/META-INF 下的标准签名文件；检测结果决定是否阻止
 * 已修改包保留签名，或按用户明确策略在打包时排除失效签名条目。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class SignatureService {

    private static final String SIGNATURE_PREFIX = "SIG-";

    private final WorkspaceService workspaceService;

    /**
     * 创建签名识别服务。
     *
     * @param workspaceService 工作区服务
     */
    public SignatureService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * 查询项目根 META-INF 下全部标准签名文件。
     *
     * @param project 项目记录
     * @return 按路径排序的签名文件
     * @throws IOException 目录读取失败时抛出
     */
    public List<String> findSignatureFiles(ProjectRecord project) throws IOException {
        Path metaInf = workspaceService.extractedDir(project).resolve("META-INF");
        if (!Files.isDirectory(metaInf)) {
            return List.of();
        }
        try (var stream = Files.list(metaInf)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isSignatureFile)
                    .map(path -> "META-INF/" + path.getFileName())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    /**
     * 判断 META-INF 文件是否为签名文件。
     *
     * @param path 文件路径
     * @return 是签名文件时返回 true
     */
    private boolean isSignatureFile(Path path) {
        String fileName = path.getFileName().toString().toUpperCase();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex < 0 ? JarPatchConstants.EMPTY_TEXT
                : fileName.substring(dotIndex + 1).toLowerCase();
        return fileName.startsWith(SIGNATURE_PREFIX) || JarPatchConstants.SIGNATURE_EXTENSIONS.contains(extension);
    }
}
