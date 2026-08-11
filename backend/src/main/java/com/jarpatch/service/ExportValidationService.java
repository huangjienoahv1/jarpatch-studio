package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ExportValidationResult;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 导出临时包结构校验服务。
 * <p>
 * 导出服务在原子发布前调用本服务，依次校验压缩包可读性、Manifest、包布局、修改资源、
 * 已编译 class、Spring Boot 嵌套 Jar 存储方式和签名策略；任何错误都会阻止目标文件发布。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ExportValidationService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String NESTED_ARTIFACT_SEPARATOR = "!/";
    private static final String META_INF_PREFIX = "META-INF/";
    private static final String SIGNATURE_PREFIX = "SIG-";
    private static final String CHECK_ARCHIVE_READABLE = "压缩包中央目录可读取";
    private static final String CHECK_MANIFEST = "Manifest 与工作区一致";
    private static final String CHECK_LAYOUT = "包布局目录完整";
    private static final String CHECK_RESOURCES = "修改资源已进入导出包";
    private static final String CHECK_CLASSES = "编译 class 已进入正确目标";
    private static final String CHECK_STORED_JARS = "Spring Boot 嵌套 Jar 均为 STORED";
    private static final String CHECK_SIGNATURES = "签名条目符合导出策略";
    private static final String ERROR_ARCHIVE_UNREADABLE = "导出包不可读取";
    private static final String ERROR_MANIFEST_MISMATCH = "Manifest 缺失或内容不一致";
    private static final String ERROR_MANIFEST_UNEXPECTED = "导出包出现工作区不存在的 Manifest";
    private static final String ERROR_LAYOUT_PREFIX = "缺少包布局目录";
    private static final String ERROR_RESOURCE_CATEGORY = "修改资源";
    private static final String ERROR_RESOURCE_PREFIX = "修改资源未正确进入导出包";
    private static final String ERROR_COMPILED_CATEGORY = "编译产物";
    private static final String ERROR_COMPILED_PREFIX = "编译产物未正确进入导出包";
    private static final String ERROR_STORED_CATEGORY = "Spring Boot 嵌套";
    private static final String ERROR_STORED_PREFIX = "Spring Boot 嵌套 Jar 未使用 STORED";
    private static final String ERROR_SIGNATURE_CATEGORY = "失效签名";
    private static final String ERROR_SIGNATURE_PREFIX = "失效签名仍存在";

    private final WorkspaceService workspaceService;

    /**
     * 创建导出结构校验服务。
     *
     * @param workspaceService 工作区服务
     */
    public ExportValidationService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * 校验尚未发布的临时 Jar/War。
     *
     * @param project          项目记录
     * @param archiveFile      临时导出包
     * @param changedFiles     真实修改文件清单
     * @param compiledArtifacts 已提交 class 清单
     * @param signaturesRemoved 是否选择移除失效签名
     * @return 完整校验结果
     */
    public ExportValidationResult validate(ProjectRecord project,
                                           Path archiveFile,
                                           List<String> changedFiles,
                                           List<String> compiledArtifacts,
                                           boolean signaturesRemoved) {
        ExportValidationResult result = new ExportValidationResult();
        try (ZipFile zipFile = new ZipFile(archiveFile.toFile())) {
            result.getChecks().add(CHECK_ARCHIVE_READABLE);
            validateManifest(project, zipFile, result);
            validateLayout(project, zipFile, result);
            validateChangedResources(project, zipFile, changedFiles, result);
            validateCompiledArtifacts(project, zipFile, compiledArtifacts, result);
            validateStoredNestedJars(project, zipFile, result);
            validateSignatures(zipFile, signaturesRemoved, result);
        } catch (RuntimeException | IOException exception) {
            result.getErrors().add(ERROR_ARCHIVE_UNREADABLE
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + exception.getMessage());
        }
        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    /**
     * 校验 Manifest 是否存在且字节与工作区一致。
     *
     * @param project 项目记录
     * @param zipFile 导出包
     * @param result  校验结果
     * @throws IOException 读取失败时抛出
     */
    private void validateManifest(ProjectRecord project,
                                  ZipFile zipFile,
                                  ExportValidationResult result) throws IOException {
        Path currentManifest = workspaceService.extractedDir(project).resolve(JarPatchConstants.MANIFEST_PATH);
        ZipEntry exportedManifest = zipFile.getEntry(JarPatchConstants.MANIFEST_PATH);
        if (Files.isRegularFile(currentManifest)) {
            if (exportedManifest == null || !sameContent(currentManifest, zipFile.getInputStream(exportedManifest))) {
                result.getErrors().add(ERROR_MANIFEST_MISMATCH);
                return;
            }
        } else if (exportedManifest != null) {
            result.getErrors().add(ERROR_MANIFEST_UNEXPECTED);
            return;
        }
        result.getChecks().add(CHECK_MANIFEST);
    }

    /**
     * 校验 Spring Boot 或 War 必需主 class 目录。
     *
     * @param project 项目记录
     * @param zipFile 导出包
     * @param result  校验结果
     */
    private void validateLayout(ProjectRecord project, ZipFile zipFile, ExportValidationResult result) {
        String requiredPrefix = null;
        if ("SPRING_BOOT_JAR".equals(project.getPackageType())) {
            requiredPrefix = JarPatchConstants.SPRING_BOOT_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
        } else if ("WAR".equals(project.getPackageType())) {
            requiredPrefix = JarPatchConstants.WAR_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
        }
        if (requiredPrefix != null && !containsEntryPrefix(zipFile, requiredPrefix)) {
            result.getErrors().add(ERROR_LAYOUT_PREFIX
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + requiredPrefix);
            return;
        }
        result.getChecks().add(CHECK_LAYOUT);
    }

    /**
     * 校验所有 extracted 资源修改均以当前字节进入导出包。
     *
     * @param project      项目记录
     * @param zipFile      导出包
     * @param changedFiles 修改文件清单
     * @param result       校验结果
     * @throws IOException 读取失败时抛出
     */
    private void validateChangedResources(ProjectRecord project,
                                          ZipFile zipFile,
                                          List<String> changedFiles,
                                          ExportValidationResult result) throws IOException {
        for (String changedFile : changedFiles) {
            if (!changedFile.startsWith(JarPatchConstants.TREE_EXTRACTED_PREFIX)) {
                continue;
            }
            String entryName = changedFile.substring(JarPatchConstants.TREE_EXTRACTED_PREFIX.length());
            Path currentFile = workspaceService.resolveExtracted(project, entryName);
            ZipEntry exportedEntry = zipFile.getEntry(entryName);
            if (!Files.isRegularFile(currentFile) || exportedEntry == null
                    || !sameContent(currentFile, zipFile.getInputStream(exportedEntry))) {
                result.getErrors().add(ERROR_RESOURCE_PREFIX
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + changedFile);
            }
        }
        if (result.getErrors().stream().noneMatch(error -> error.startsWith(ERROR_RESOURCE_CATEGORY))) {
            result.getChecks().add(CHECK_RESOURCES);
        }
    }

    /**
     * 校验主包和嵌套 Jar 内的全部编译产物。
     *
     * @param project   项目记录
     * @param zipFile   导出包
     * @param artifacts 编译产物路径
     * @param result    校验结果
     * @throws IOException 读取失败时抛出
     */
    private void validateCompiledArtifacts(ProjectRecord project,
                                           ZipFile zipFile,
                                           List<String> artifacts,
                                           ExportValidationResult result) throws IOException {
        for (String artifact : artifacts) {
            int separatorIndex = artifact.indexOf(NESTED_ARTIFACT_SEPARATOR);
            boolean valid = separatorIndex < 0
                    ? validateMainArtifact(project, zipFile, artifact)
                    : validateNestedArtifact(project, zipFile,
                    artifact.substring(0, separatorIndex),
                    artifact.substring(separatorIndex + NESTED_ARTIFACT_SEPARATOR.length()));
            if (!valid) {
                result.getErrors().add(ERROR_COMPILED_PREFIX
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + artifact);
            }
        }
        if (result.getErrors().stream().noneMatch(error -> error.startsWith(ERROR_COMPILED_CATEGORY))) {
            result.getChecks().add(CHECK_CLASSES);
        }
    }

    /**
     * 校验主包 class 条目与 extracted 当前文件一致。
     *
     * @param project  项目记录
     * @param zipFile  导出包
     * @param artifact 主包 class 条目
     * @return 校验通过时返回 true
     * @throws IOException 读取失败时抛出
     */
    private boolean validateMainArtifact(ProjectRecord project, ZipFile zipFile, String artifact) throws IOException {
        ZipEntry exportedEntry = zipFile.getEntry(artifact);
        Path currentFile = workspaceService.extractedDir(project).resolve(artifact).normalize();
        return exportedEntry != null && Files.isRegularFile(currentFile)
                && sameContent(currentFile, zipFile.getInputStream(exportedEntry));
    }

    /**
     * 校验导出包与当前 extracted 嵌套 Jar 内同一 class 内容一致。
     *
     * @param project    项目记录
     * @param exported   导出包
     * @param outerEntry 嵌套 Jar 条目
     * @param innerEntry 嵌套 class 条目
     * @return 校验通过时返回 true
     * @throws IOException 读取失败时抛出
     */
    private boolean validateNestedArtifact(ProjectRecord project,
                                           ZipFile exported,
                                           String outerEntry,
                                           String innerEntry) throws IOException {
        ZipEntry exportedOuter = exported.getEntry(outerEntry);
        Path currentNestedJar = workspaceService.resolveExtracted(project, outerEntry);
        if (exportedOuter == null || !Files.isRegularFile(currentNestedJar)) {
            return false;
        }
        byte[] exportedDigest;
        try (InputStream inputStream = exported.getInputStream(exportedOuter)) {
            exportedDigest = digestNestedEntry(inputStream, innerEntry);
        }
        try (ZipFile currentZip = new ZipFile(currentNestedJar.toFile())) {
            ZipEntry currentEntry = currentZip.getEntry(innerEntry);
            if (currentEntry == null || exportedDigest == null) {
                return false;
            }
            try (InputStream inputStream = currentZip.getInputStream(currentEntry)) {
                return Arrays.equals(exportedDigest, digest(inputStream));
            }
        }
    }

    /**
     * 从嵌套 Jar 流中查找并计算指定条目摘要。
     *
     * @param nestedJarStream 嵌套 Jar 输入流
     * @param entryName       目标条目
     * @return SHA-256；不存在时返回 null
     * @throws IOException 读取失败时抛出
     */
    private byte[] digestNestedEntry(InputStream nestedJarStream, String entryName) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(nestedJarStream))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entryName.equals(entry.getName())) {
                    return digest(inputStream);
                }
                inputStream.closeEntry();
            }
        }
        return null;
    }

    /**
     * 校验 Spring Boot 嵌套 Jar 均使用 STORED 压缩方式。
     *
     * @param project 项目记录
     * @param zipFile 导出包
     * @param result  校验结果
     */
    private void validateStoredNestedJars(ProjectRecord project,
                                          ZipFile zipFile,
                                          ExportValidationResult result) {
        if (!"SPRING_BOOT_JAR".equals(project.getPackageType())) {
            result.getChecks().add(CHECK_STORED_JARS);
            return;
        }
        String prefix = JarPatchConstants.SPRING_BOOT_LIB_DIR + JarPatchConstants.ZIP_SEPARATOR;
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.getName().startsWith(prefix)
                    && entry.getName().endsWith("." + JarPatchConstants.JAR_EXTENSION)
                    && entry.getMethod() != ZipEntry.STORED) {
                result.getErrors().add(ERROR_STORED_PREFIX
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entry.getName());
            }
        }
        if (result.getErrors().stream().noneMatch(error -> error.startsWith(ERROR_STORED_CATEGORY))) {
            result.getChecks().add(CHECK_STORED_JARS);
        }
    }

    /**
     * 校验选择移除签名时导出包不再包含标准签名条目。
     *
     * @param zipFile           导出包
     * @param signaturesRemoved 是否要求移除签名
     * @param result            校验结果
     */
    private void validateSignatures(ZipFile zipFile,
                                    boolean signaturesRemoved,
                                    ExportValidationResult result) {
        if (!signaturesRemoved) {
            result.getChecks().add(CHECK_SIGNATURES);
            return;
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            if (isSignatureEntry(entryName)) {
                result.getErrors().add(ERROR_SIGNATURE_PREFIX
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entryName);
            }
        }
        if (result.getErrors().stream().noneMatch(error -> error.startsWith(ERROR_SIGNATURE_CATEGORY))) {
            result.getChecks().add(CHECK_SIGNATURES);
        }
    }

    /**
     * 判断 Zip 中是否存在指定目录前缀。
     *
     * @param zipFile 导出包
     * @param prefix  条目前缀
     * @return 至少一个条目匹配时返回 true
     */
    private boolean containsEntryPrefix(ZipFile zipFile, String prefix) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断根 META-INF 条目是否为标准签名文件。
     *
     * @param entryName Zip 条目名
     * @return 是签名条目时返回 true
     */
    private boolean isSignatureEntry(String entryName) {
        if (!entryName.startsWith(META_INF_PREFIX)
                || entryName.substring(META_INF_PREFIX.length()).contains("/")) {
            return false;
        }
        String fileName = entryName.substring(META_INF_PREFIX.length()).toUpperCase(Locale.ROOT);
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex < 0 ? JarPatchConstants.EMPTY_TEXT
                : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        return fileName.startsWith(SIGNATURE_PREFIX) || JarPatchConstants.SIGNATURE_EXTENSIONS.contains(extension);
    }

    /**
     * 比较磁盘文件和 Zip 条目流的 SHA-256。
     *
     * @param file        磁盘文件
     * @param inputStream Zip 条目流
     * @return 内容一致时返回 true
     * @throws IOException 读取失败时抛出
     */
    private boolean sameContent(Path file, InputStream inputStream) throws IOException {
        try (InputStream fileInput = Files.newInputStream(file); InputStream zipInput = inputStream) {
            return Arrays.equals(digest(fileInput), digest(zipInput));
        }
    }

    /**
     * 流式计算 SHA-256，避免把嵌套 Jar 或大文件完整读入内存。
     *
     * @param inputStream 输入流
     * @return SHA-256 字节
     * @throws IOException 读取失败时抛出
     */
    private byte[] digest(InputStream inputStream) throws IOException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_SHA256_UNAVAILABLE, exception);
        }
        byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            messageDigest.update(buffer, 0, length);
        }
        return messageDigest.digest();
    }
}
