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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /**
     * 解压 Jar 或 War 到目标目录。
     *
     * @param archiveFile 原始压缩包
     * @param targetDir   解压目标目录
     * @throws IOException 解压失败时抛出
     */
    public void unzip(Path archiveFile, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archiveFile)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = safeResolve(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    copy(zipInputStream, target);
                }
                zipInputStream.closeEntry();
            }
        }
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
        Files.createDirectories(outputFile.getParent());
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputFile)))) {
            List<Path> paths = collectPaths(sourceDir);

            // Spring Boot 启动器会把 BOOT-INF/classes/ 作为 classpath 根，必须先保留目录条目。
            for (Path directory : paths) {
                if (Files.isDirectory(directory) && !sourceDir.equals(directory)) {
                    addDirectory(sourceDir, directory, zipOutputStream);
                }
            }
            for (Path file : paths) {
                if (Files.isRegularFile(file)) {
                    addFile(sourceDir, file, zipOutputStream, springBootLayout);
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
        Path tempFile = Files.createTempFile(jarFile.getParent(), jarFile.getFileName().toString(), ".tmp");
        Set<String> replacementEntries = collectClassEntries(compiledDir);
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(jarFile)));
             ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
            copyOriginalEntries(zipInputStream, zipOutputStream, replacementEntries);
            addReplacementClasses(compiledDir, zipOutputStream);
        }
        Files.move(tempFile, jarFile, StandardCopyOption.REPLACE_EXISTING);
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
        Files.walk(sourceDir)
                .sorted(Comparator.comparing(Path::toString))
                .forEach(paths::add);
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
     * @throws IOException 复制失败时抛出
     */
    private void copyOriginalEntries(ZipInputStream zipInputStream,
                                     ZipOutputStream zipOutputStream,
                                     Set<String> replacementEntries) throws IOException {
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!replacementEntries.contains(entry.getName())) {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zipOutputStream.putNextEntry(newEntry);
                if (!entry.isDirectory()) {
                    copy(zipInputStream, zipOutputStream);
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
     * @throws IOException 写入失败时抛出
     */
    private void addReplacementClasses(Path compiledDir, ZipOutputStream zipOutputStream) throws IOException {
        try (var stream = Files.walk(compiledDir)) {
            List<Path> classFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path classFile : classFiles) {
                addReplacementClass(compiledDir, classFile, zipOutputStream);
            }
        }
    }

    /**
     * 写入单个替换 class 条目。
     *
     * @param compiledDir     编译输出目录
     * @param classFile       class 文件路径
     * @param zipOutputStream 新 Jar 输出流
     * @throws IOException 写入失败时抛出
     */
    private void addReplacementClass(Path compiledDir, Path classFile, ZipOutputStream zipOutputStream) throws IOException {
        String entryName = compiledDir.relativize(classFile).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(classFile, zipOutputStream);
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
     * @throws IOException 文件写入失败时抛出
     */
    private void copy(InputStream inputStream, Path target) throws IOException {
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    /**
     * 从一个流复制内容到另一个流。
     *
     * @param inputStream  输入流
     * @param outputStream 输出流
     * @throws IOException 复制失败时抛出
     */
    private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
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
        String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
        try {
            ZipEntry entry = new ZipEntry(entryName);
            if (springBootLayout && isSpringBootNestedJar(entryName)) {
                configureStoredEntry(file, entry);
            }
            zipOutputStream.putNextEntry(entry);
            Files.copy(file, zipOutputStream);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("写入压缩条目失败: " + entryName, e);
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
        String entryName = sourceDir.relativize(directory).toString().replace('\\', '/') + JarPatchConstants.ZIP_SEPARATOR;
        try {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("写入目录条目失败: " + entryName, e);
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
     * 配置 STORED Zip 条目所需的大小和 CRC。
     *
     * @param file  文件路径
     * @param entry Zip 条目
     * @throws IOException 读取文件失败时抛出
     */
    private void configureStoredEntry(Path file, ZipEntry entry) throws IOException {
        CRC32 crc32 = new CRC32();
        byte[] bytes = Files.readAllBytes(file);
        crc32.update(bytes);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc32.getValue());
    }
}
