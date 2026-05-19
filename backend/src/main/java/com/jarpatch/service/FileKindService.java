package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件类型识别服务。
 * <p>
 * 文件树、内容读取和保存接口通过该服务统一判断 Java、文本资源、签名文件和二进制文件，
 * 保证前后端编辑权限口径一致。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class FileKindService {

    /**
     * 根据文件路径判断文件类型。
     *
     * @param path 文件路径
     * @return 文件类型枚举
     */
    public FileKind detect(Path path) {
        if (Files.isDirectory(path)) {
            return FileKind.DIRECTORY;
        }
        String extension = extension(path.getFileName().toString());
        if (JarPatchConstants.JAVA_EXTENSION.equals(extension)) {
            return FileKind.JAVA;
        }
        if (JarPatchConstants.SIGNATURE_EXTENSIONS.contains(extension)) {
            return FileKind.SIGNATURE;
        }
        if (JarPatchConstants.EDITABLE_RESOURCE_EXTENSIONS.contains(extension)) {
            return FileKind.RESOURCE;
        }
        return FileKind.BINARY;
    }

    /**
     * 获取小写文件后缀。
     *
     * @param fileName 文件名
     * @return 小写后缀；没有后缀时返回空字符串
     */
    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase();
    }
}
