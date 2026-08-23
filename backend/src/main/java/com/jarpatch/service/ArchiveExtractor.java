package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.ArchiveLimitsProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Jar/War 输入校验与受限解压服务。
 * <p>
 * 导入预解析和正式解压共用中央目录资源门禁；实际解压时再次检查路径、重复条目、单条目
 * 与累计展开大小，所有目标路径必须严格位于指定工作区目录。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ArchiveExtractor {

    private final ArchiveLimitsProperties archiveLimits;

    /**
     * 创建受限解压服务。
     *
     * @param archiveLimits 压缩包资源限制配置
     */
    public ArchiveExtractor(ArchiveLimitsProperties archiveLimits) {
        this.archiveLimits = archiveLimits;
    }

    /**
     * 解压 Jar 或 War 到目标目录并支持任务取消。
     *
     * @param archiveFile    原始压缩包
     * @param targetDir      解压目标目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 解压失败时抛出
     */
    public void unzip(Path archiveFile, Path targetDir, BooleanSupplier cancelRequested) throws IOException {
        validate(archiveFile);
        Files.createDirectories(targetDir);
        long totalWritten = 0L;
        Set<String> extractedEntries = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(archiveFile)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                ensureNotCancelled(cancelRequested);
                validateEntryName(entry.getName(), extractedEntries);
                Path target = safeResolve(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    totalWritten = safeAdd(totalWritten,
                            copyEntry(zipInputStream, target, cancelRequested, totalWritten));
                }
                zipInputStream.closeEntry();
            }
        }
    }

    /**
     * 校验压缩包中央目录的大小、条目数、路径深度、重复项和压缩比。
     *
     * @param archiveFile 待校验 Jar 或 War
     * @throws IOException 压缩包不可读取时抛出
     */
    public void validate(Path archiveFile) throws IOException {
        if (Files.size(archiveFile) > archiveLimits.getMaxArchiveBytes()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_TOO_LARGE);
        }
        int entryCount = 0;
        long totalSize = 0L;
        Set<String> entries = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(archiveFile.toFile())) {
            var enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                entryCount++;
                if (entryCount > archiveLimits.getMaxEntryCount()) {
                    throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_ENTRY_LIMIT);
                }
                validateEntryName(entry.getName(), entries);
                if (entry.isDirectory()) {
                    continue;
                }
                long size = entry.getSize();
                long compressedSize = entry.getCompressedSize();
                if (size > archiveLimits.getMaxEntryBytes()) {
                    throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_ENTRY_TOO_LARGE
                            + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entry.getName());
                }
                if (size >= 0L) {
                    totalSize = safeAdd(totalSize, size);
                    if (totalSize > archiveLimits.getMaxUncompressedBytes()) {
                        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_UNCOMPRESSED_LIMIT);
                    }
                }
                if ((size > 0L && compressedSize == 0L)
                        || (compressedSize > 0L
                        && (double) size / compressedSize > archiveLimits.getMaxCompressionRatio())) {
                    throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_RATIO_LIMIT
                            + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entry.getName());
                }
            }
        }
    }

    /**
     * 校验条目名称唯一且路径深度受控。
     *
     * @param entryName 当前压缩条目名称
     * @param entries   已校验条目名称集合
     */
    private void validateEntryName(String entryName, Set<String> entries) {
        String normalizedName = entryName.replace('\\', '/');
        if (!entries.add(normalizedName)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_DUPLICATE_ENTRY
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entryName);
        }
        long depth = normalizedName.chars().filter(character -> character == '/').count();
        if (depth > archiveLimits.getMaxPathDepth()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_PATH_DEPTH_LIMIT
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entryName);
        }
    }

    /**
     * 流式写出单个压缩条目并实时执行大小门禁。
     *
     * @param inputStream     压缩条目输入流
     * @param target          条目输出路径
     * @param cancelRequested 取消检查回调
     * @param totalBefore     当前条目前的累计展开字节数
     * @return 当前条目实际写入字节数
     */
    private long copyEntry(InputStream inputStream,
                           Path target,
                           BooleanSupplier cancelRequested,
                           long totalBefore) throws IOException {
        long entryWritten = 0L;
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                ensureNotCancelled(cancelRequested);
                entryWritten = safeAdd(entryWritten, length);
                if (entryWritten > archiveLimits.getMaxEntryBytes()) {
                    throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_ENTRY_TOO_LARGE
                            + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + target.getFileName());
                }
                if (safeAdd(totalBefore, entryWritten) > archiveLimits.getMaxUncompressedBytes()) {
                    throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_UNCOMPRESSED_LIMIT);
                }
                outputStream.write(buffer, 0, length);
            }
        }
        return entryWritten;
    }

    /**
     * 安全累加展开大小，数值溢出按超过上限处理。
     *
     * @param current 当前累计值
     * @param value   本次增量
     * @return 未溢出的累加结果
     */
    private long safeAdd(long current, long value) {
        try {
            return Math.addExact(current, value);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_UNCOMPRESSED_LIMIT, exception);
        }
    }

    /**
     * 解析目标目录内路径并阻止 Zip 路径逃逸。
     *
     * @param targetDir 目标根目录
     * @param entryName 压缩条目名称
     * @return 已验证仍位于目标根目录内的路径
     */
    private Path safeResolve(Path targetDir, String entryName) {
        Path normalizedTargetDir = targetDir.toAbsolutePath().normalize();
        Path target = normalizedTargetDir.resolve(entryName).normalize();
        if (!target.startsWith(normalizedTargetDir)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        return target;
    }

    /**
     * 检查任务取消状态。
     *
     * @param cancelRequested 取消检查回调
     */
    private void ensureNotCancelled(BooleanSupplier cancelRequested) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
        }
    }
}
