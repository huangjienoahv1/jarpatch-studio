package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 包类型识别服务。
 * <p>
 * 导入服务解压完成后调用该服务，根据文件后缀和目录结构识别普通 Jar、Spring Boot Jar
 * 和 War，后续分析、编译和导出都以该结果作为分支依据。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class PackageDetectService {

    /**
     * 根据原始文件和解压目录识别包类型。
     *
     * @param archiveFile 原始包路径
     * @param extractedDir 解压目录
     * @return 包类型
     */
    public PackageType detect(Path archiveFile, Path extractedDir) {
        String fileName = archiveFile.getFileName().toString().toLowerCase();
        if (fileName.endsWith("." + JarPatchConstants.WAR_EXTENSION)) {
            return PackageType.WAR;
        }
        if (Files.exists(extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR))) {
            return PackageType.SPRING_BOOT_JAR;
        }
        return PackageType.STANDARD_JAR;
    }
}
