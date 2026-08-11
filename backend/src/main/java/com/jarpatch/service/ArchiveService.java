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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

/**
 * Jar/War 压缩包处理服务。
 * <p>
 * 导入流程调用 unzip 解压原始包到工作区；导出流程调用 zipDirectory 把 extracted 目录重新
 * 打包。Spring Boot 嵌套 Jar 在导出时以 STORED 方式写入，避免破坏启动器读取规则。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ArchiveService {

    private static final String MESSAGE_WRITE_ARCHIVE_ENTRY_FAILED = "写入压缩条目失败";
    private static final String MESSAGE_WRITE_DIRECTORY_ENTRY_FAILED = "写入目录条目失败";

    private final ArchiveLimitsProperties archiveLimits;

    /**
     * 创建压缩包服务。
     *
     * @param archiveLimits 压缩包资源限制配置
     */
    public ArchiveService(ArchiveLimitsProperties archiveLimits) {
        this.archiveLimits = archiveLimits;
    }

    /**
     * 解压 Jar 或 War 到目标目录。
     *
     * @param archiveFile 原始压缩包
     * @param targetDir   解压目标目录
     * @throws IOException 解压失败时抛出
     */
    public void unzip(Path archiveFile, Path targetDir) throws IOException {
        unzip(archiveFile, targetDir, () -> false);
    }

    /**
     * 解压 Jar 或 War 到目标目录，并支持任务取消检查。
     *
     * @param archiveFile    原始压缩包
     * @param targetDir      解压目标目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 解压失败时抛出
     */
    public void unzip(Path archiveFile, Path targetDir, BooleanSupplier cancelRequested) throws IOException {
        validateArchive(archiveFile);
        Files.createDirectories(targetDir);
        long totalWritten = 0L;
        Set<String> extractedEntries = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archiveFile)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                ensureNotCancelled(cancelRequested);
                validateEntryName(entry.getName(), extractedEntries);
                Path target = safeResolve(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    long entryWritten = copyArchiveEntry(zipInputStream, target, cancelRequested, totalWritten);
                    totalWritten += entryWritten;
                }
                zipInputStream.closeEntry();
            }
        }
    }

    /**
     * 在预解析和解压前校验压缩包中央目录资源指标。
     *
     * @param archiveFile 待校验 Jar 或 War
     * @throws IOException 压缩包不可读取时抛出
     */
    public void validateArchive(Path archiveFile) throws IOException {
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
     * @param entryName 条目名称
     * @param entries   当前压缩包已出现条目
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
     * 安全累加压缩包展开大小，溢出时按超过限制处理。
     *
     * @param current 当前累计值
     * @param value   本次增加值
     * @return 新累计值
     */
    private long safeAdd(long current, long value) {
        try {
            return Math.addExact(current, value);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_ARCHIVE_UNCOMPRESSED_LIMIT, exception);
        }
    }

    /**
     * 流式写出单个压缩条目并执行实时大小门禁。
     *
     * @param inputStream     Zip 输入流
     * @param target          目标文件
     * @param cancelRequested 取消检查回调
     * @param totalBefore     当前已展开总字节数
     * @return 本条目实际写入字节数
     * @throws IOException 文件写入失败时抛出
     */
    private long copyArchiveEntry(InputStream inputStream,
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
     * 把目录重新打包为 Jar 或 War。
     *
     * @param sourceDir        待打包目录
     * @param outputFile       输出文件
     * @param springBootLayout 是否按 Spring Boot 可执行 Jar 规则处理嵌套 Jar
     * @throws IOException 打包失败时抛出
     */
    public void zipDirectory(Path sourceDir, Path outputFile, boolean springBootLayout) throws IOException {
        zipDirectory(sourceDir, outputFile, springBootLayout, () -> false);
    }

    /**
     * 把目录重新打包为 Jar 或 War，并支持任务取消检查。
     *
     * @param sourceDir        待打包目录
     * @param outputFile       输出文件
     * @param springBootLayout 是否按 Spring Boot 可执行 Jar 规则处理嵌套 Jar
     * @param cancelRequested  取消检查回调
     * @throws IOException 打包失败时抛出
     */
    public void zipDirectory(Path sourceDir, Path outputFile, boolean springBootLayout, BooleanSupplier cancelRequested) throws IOException {
        zipDirectory(sourceDir, outputFile, springBootLayout, false, cancelRequested);
    }

    /**
     * 把目录重新打包为 Jar 或 War，并按明确策略排除失效签名。
     *
     * @param sourceDir          待打包目录
     * @param outputFile         输出文件
     * @param springBootLayout   是否按 Spring Boot 规则处理嵌套 Jar
     * @param removeSignatures   是否移除根 META-INF 签名条目
     * @param cancelRequested    取消检查回调
     * @throws IOException 打包失败时抛出
     */
    public void zipDirectory(Path sourceDir,
                             Path outputFile,
                             boolean springBootLayout,
                             boolean removeSignatures,
                             BooleanSupplier cancelRequested) throws IOException {
        Files.createDirectories(outputFile.getParent());
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputFile)))) {
            List<Path> paths = collectPaths(sourceDir);

            // Spring Boot 启动器会把 BOOT-INF/classes/ 作为 classpath 根，必须先保留目录条目。
            for (Path directory : paths) {
                ensureNotCancelled(cancelRequested);
                if (Files.isDirectory(directory) && !sourceDir.equals(directory)) {
                    addDirectory(sourceDir, directory, zipOutputStream, cancelRequested);
                }
            }
            for (Path file : paths) {
                ensureNotCancelled(cancelRequested);
                if (Files.isRegularFile(file) && !(removeSignatures && isSignatureEntry(sourceDir, file))) {
                    addFile(sourceDir, file, zipOutputStream, springBootLayout, cancelRequested);
                }
            }
        }
    }

    /**
     * 将编译后的 class 文件替换回指定嵌套 Jar。
     * <p>
     * 编译入口来自 CompileService，实际写入点是 extracted 中的原嵌套 Jar。该方法重建 Jar：
     * 先复制原有条目并跳过同名 class，再追加 compiledDir 中的新 class，结果覆盖原 Jar 文件。
     * </p>
     *
     * @param jarFile     extracted 目录内的嵌套 Jar
     * @param compiledDir 当前嵌套 Jar 对应的编译输出目录
     * @throws IOException 读取或写入 Jar 失败时抛出
     */
    public void replaceClassesInJar(Path jarFile, Path compiledDir) throws IOException {
        replaceClassesInJar(jarFile, compiledDir, () -> false);
    }

    /**
     * 将编译后的 class 文件替换回指定嵌套 Jar，并支持任务取消检查。
     *
     * @param jarFile         extracted 目录内的嵌套 Jar
     * @param compiledDir     当前嵌套 Jar 对应的编译输出目录
     * @param cancelRequested  取消检查回调
     * @throws IOException 读取或写入 Jar 失败时抛出
     */
    public void replaceClassesInJar(Path jarFile, Path compiledDir, BooleanSupplier cancelRequested) throws IOException {
        Path tempFile = Files.createTempFile(jarFile.getParent(), jarFile.getFileName().toString(), ".tmp");
        Set<String> replacementEntries = collectClassEntries(compiledDir);
        try {
            try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(jarFile)));
                 ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                copyOriginalEntries(zipInputStream, zipOutputStream, replacementEntries, cancelRequested);
                addReplacementClasses(compiledDir, zipOutputStream, cancelRequested);
            }
            try {
                Files.move(tempFile, jarFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(JarPatchConstants.MESSAGE_WORKSPACE_ATOMIC_MOVE_REQUIRED, exception);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 收集并排序待打包路径。
     *
     * @param sourceDir 源目录
     * @return 排序后的路径列表
     * @throws IOException 读取目录失败时抛出
     */
    private List<Path> collectPaths(Path sourceDir) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (var stream = Files.walk(sourceDir)) {
            stream.sorted(Comparator.comparing(Path::toString)).forEach(paths::add);
        }
        return paths;
    }

    /**
     * 收集编译输出目录中的 class 条目名称。
     *
     * @param compiledDir 编译输出目录
     * @return Jar 内部 class 条目集合
     * @throws IOException 读取目录失败时抛出
     */
    private Set<String> collectClassEntries(Path compiledDir) throws IOException {
        Set<String> entries = new HashSet<>();
        try (var stream = Files.walk(compiledDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .map(path -> compiledDir.relativize(path).toString().replace('\\', '/'))
                    .forEach(entries::add);
        }
        return entries;
    }

    /**
     * 复制原 Jar 中不被替换的条目。
     *
     * @param zipInputStream     原 Jar 输入流
     * @param zipOutputStream    新 Jar 输出流
     * @param replacementEntries 需要被新 class 覆盖的条目名称
     * @param cancelRequested    取消检查回调
     * @throws IOException 复制失败时抛出
     */
    private void copyOriginalEntries(ZipInputStream zipInputStream,
                                     ZipOutputStream zipOutputStream,
                                     Set<String> replacementEntries,
                                     BooleanSupplier cancelRequested) throws IOException {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            ensureNotCancelled(cancelRequested);
            if (!replacementEntries.contains(entry.getName())) {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zipOutputStream.putNextEntry(newEntry);
                if (!entry.isDirectory()) {
                    copy(zipInputStream, zipOutputStream, cancelRequested);
                }
                zipOutputStream.closeEntry();
            }
            zipInputStream.closeEntry();
        }
    }

    /**
     * 追加本次编译产生的新 class 条目。
     *
     * @param compiledDir     编译输出目录
     * @param zipOutputStream 新 Jar 输出流
     * @param cancelRequested  取消检查回调
     * @throws IOException 写入失败时抛出
     */
    private void addReplacementClasses(Path compiledDir, ZipOutputStream zipOutputStream, BooleanSupplier cancelRequested) throws IOException {
        try (var stream = Files.walk(compiledDir)) {
            List<Path> classFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path classFile : classFiles) {
                ensureNotCancelled(cancelRequested);
                addReplacementClass(compiledDir, classFile, zipOutputStream, cancelRequested);
            }
        }
    }

    /**
     * 写入单个替换 class 条目。
     *
     * @param compiledDir     编译输出目录
     * @param classFile       class 文件路径
     * @param zipOutputStream 新 Jar 输出流
     * @param cancelRequested  取消检查回调
     * @throws IOException 写入失败时抛出
     */
    private void addReplacementClass(Path compiledDir, Path classFile, ZipOutputStream zipOutputStream, BooleanSupplier cancelRequested) throws IOException {
        ensureNotCancelled(cancelRequested);
        String entryName = compiledDir.relativize(classFile).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (InputStream inputStream = Files.newInputStream(classFile)) {
            copy(inputStream, zipOutputStream, cancelRequested);
        }
        zipOutputStream.closeEntry();
    }

    /**
     * 安全解析 Zip 条目输出路径，防止恶意条目逃逸目标目录。
     *
     * @param targetDir 目标目录
     * @param entryName Zip 条目名称
     * @return 安全输出路径
     */
    private Path safeResolve(Path targetDir, String entryName) {
        Path normalizedTarget = targetDir.toAbsolutePath().normalize();
        Path resolved = normalizedTarget.resolve(entryName).normalize();
        if (!resolved.startsWith(normalizedTarget)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
        }
        return resolved;
    }

    /**
     * 从 Zip 输入流复制单个文件。
     *
     * @param inputStream Zip 输入流
     * @param target      目标文件路径
     * @param cancelRequested 取消检查回调
     * @throws IOException 文件写入失败时抛出
     */
    private void copy(InputStream inputStream, Path target) throws IOException {
        copy(inputStream, target, () -> false);
    }

    /**
     * 从 Zip 输入流复制单个文件，并支持任务取消检查。
     *
     * @param inputStream    Zip 输入流
     * @param target         目标文件路径
     * @param cancelRequested 取消检查回调
     * @throws IOException 文件写入失败时抛出
     */
    private void copy(InputStream inputStream, Path target, BooleanSupplier cancelRequested) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                ensureNotCancelled(cancelRequested);
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * 从一个流复制内容到另一个流。
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @param cancelRequested 取消检查回调
     * @throws IOException 复制失败时抛出
     */
    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        copy(inputStream, outputStream, () -> false);
    }

    /**
     * 从一个流复制内容到另一个流，并支持任务取消检查。
     *
     * @param inputStream    输入流
     * @param outputStream   输出流
     * @param cancelRequested 取消检查回调
     * @throws IOException 复制失败时抛出
     */
    private void copy(InputStream inputStream, OutputStream outputStream, BooleanSupplier cancelRequested) throws IOException {
        byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            ensureNotCancelled(cancelRequested);
            outputStream.write(buffer, 0, length);
        }
    }

    /**
     * 将目录中的单个文件写入 Zip 输出流。
     *
     * @param sourceDir        源目录
     * @param file             当前文件
     * @param zipOutputStream  Zip 输出流
     * @param springBootLayout 是否按 Spring Boot 规则处理
     */
    private void addFile(Path sourceDir, Path file, ZipOutputStream zipOutputStream, boolean springBootLayout) {
        addFile(sourceDir, file, zipOutputStream, springBootLayout, () -> false);
    }

    /**
     * 将目录中的单个文件写入 Zip 输出流，并支持任务取消检查。
     *
     * @param sourceDir        源目录
     * @param file             当前文件
     * @param zipOutputStream  Zip 输出流
     * @param springBootLayout 是否按 Spring Boot 规则处理
     * @param cancelRequested   取消检查回调
     */
    private void addFile(Path sourceDir, Path file, ZipOutputStream zipOutputStream, boolean springBootLayout, BooleanSupplier cancelRequested) {
        ensureNotCancelled(cancelRequested);
        String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
        try {
            ZipEntry entry = new ZipEntry(entryName);
            if (springBootLayout && isSpringBootNestedJar(entryName)) {
                configureStoredEntry(file, entry);
            }
            zipOutputStream.putNextEntry(entry);
            try (InputStream inputStream = Files.newInputStream(file)) {
                copy(inputStream, zipOutputStream, cancelRequested);
            }
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException(MESSAGE_WRITE_ARCHIVE_ENTRY_FAILED
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entryName, e);
        }
    }

    /**
     * 将目录条目写入 Zip 输出流。
     *
     * @param sourceDir       源目录
     * @param directory       当前目录
     * @param zipOutputStream Zip 输出流
     */
    private void addDirectory(Path sourceDir, Path directory, ZipOutputStream zipOutputStream) {
        addDirectory(sourceDir, directory, zipOutputStream, () -> false);
    }

    /**
     * 将目录条目写入 Zip 输出流，并支持任务取消检查。
     *
     * @param sourceDir       源目录
     * @param directory       当前目录
     * @param zipOutputStream Zip 输出流
     * @param cancelRequested  取消检查回调
     */
    private void addDirectory(Path sourceDir, Path directory, ZipOutputStream zipOutputStream, BooleanSupplier cancelRequested) {
        ensureNotCancelled(cancelRequested);
        String entryName = sourceDir.relativize(directory).toString().replace('\\', '/') + JarPatchConstants.ZIP_SEPARATOR;
        try {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException(MESSAGE_WRITE_DIRECTORY_ENTRY_FAILED
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + entryName, e);
        }
    }

    /**
     * 判断当前条目是否为 Spring Boot 嵌套依赖 Jar。
     *
     * @param entryName Zip 条目名称
     * @return 是嵌套依赖 Jar 时返回 true
     */
    private boolean isSpringBootNestedJar(String entryName) {
        return entryName.startsWith(JarPatchConstants.SPRING_BOOT_LIB_DIR + JarPatchConstants.ZIP_SEPARATOR)
                && entryName.endsWith("." + JarPatchConstants.JAR_EXTENSION);
    }

    /**
     * 判断文件是否为根 META-INF 下需移除的标准签名条目。
     *
     * @param sourceDir 打包根目录
     * @param file      当前文件
     * @return 是签名条目时返回 true
     */
    private boolean isSignatureEntry(Path sourceDir, Path file) {
        String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
        if (!entryName.startsWith("META-INF/") || entryName.substring("META-INF/".length()).contains("/")) {
            return false;
        }
        String fileName = file.getFileName().toString().toUpperCase();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex < 0 ? JarPatchConstants.EMPTY_TEXT
                : fileName.substring(dotIndex + 1).toLowerCase();
        return fileName.startsWith("SIG-") || JarPatchConstants.SIGNATURE_EXTENSIONS.contains(extension);
    }

    /**
     * 配置 STORED Zip 条目所需的大小和 CRC。
     *
     * @param file  文件路径
     * @param entry Zip 条目
     * @throws IOException 读取文件失败时抛出
     */
    private void configureStoredEntry(Path file, ZipEntry entry) throws IOException {
        CRC32 crc32 = new CRC32();
        long size = Files.size(file);
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                crc32.update(buffer, 0, length);
            }
        }
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(size);
        entry.setCompressedSize(size);
        entry.setCrc(crc32.getValue());
    }

    /**
     * 检查任务是否已取消。
     *
     * @param cancelRequested 取消检查回调
     */
    private void ensureNotCancelled(BooleanSupplier cancelRequested) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
        }
    }
}
