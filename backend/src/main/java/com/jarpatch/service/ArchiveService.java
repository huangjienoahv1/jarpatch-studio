package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    private final ArchiveExtractor archiveExtractor;

    /**
     * 创建压缩包服务。
     *
     * @param archiveExtractor 输入校验与受限解压服务
     */
    public ArchiveService(ArchiveExtractor archiveExtractor) {
        this.archiveExtractor = archiveExtractor;
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
        archiveExtractor.unzip(archiveFile, targetDir, cancelRequested);
    }

    /**
     * 在预解析和解压前校验压缩包中央目录资源指标。
     *
     * @param archiveFile 待校验 Jar 或 War
     * @throws IOException 压缩包不可读取时抛出
     */
    public void validateArchive(Path archiveFile) throws IOException {
        archiveExtractor.validate(archiveFile);
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
            // 每层只排序直接子节点，目录条目先于其内容写入，避免把全目录路径同时保存在内存。
            writeDirectoryContents(sourceDir, sourceDir, zipOutputStream, springBootLayout,
                    removeSignatures, cancelRequested);
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
     * 以确定顺序递归写入目录内容，每次只持有当前目录的直接子节点。
     *
     * @param sourceDir 打包根目录
     * @param currentDir 当前遍历目录
     * @param zipOutputStream Zip 输出流
     * @param springBootLayout 是否使用 Spring Boot 嵌套 Jar 规则
     * @param removeSignatures 是否移除根签名条目
     * @param cancelRequested 取消检查回调
     * @throws IOException 目录读取或文件写入失败时抛出
     */
    private void writeDirectoryContents(Path sourceDir,
                                        Path currentDir,
                                        ZipOutputStream zipOutputStream,
                                        boolean springBootLayout,
                                        boolean removeSignatures,
                                        BooleanSupplier cancelRequested) throws IOException {
        List<Path> children;
        try (var stream = Files.list(currentDir)) {
            children = stream.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        }
        for (Path child : children) {
            ensureNotCancelled(cancelRequested);
            if (Files.isDirectory(child)) {
                addDirectory(sourceDir, child, zipOutputStream, cancelRequested);
                writeDirectoryContents(sourceDir, child, zipOutputStream, springBootLayout,
                        removeSignatures, cancelRequested);
            }
            else if (Files.isRegularFile(child)
                    && !(removeSignatures && isSignatureEntry(sourceDir, child))) {
                addFile(sourceDir, child, zipOutputStream, springBootLayout, cancelRequested);
            }
        }
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
