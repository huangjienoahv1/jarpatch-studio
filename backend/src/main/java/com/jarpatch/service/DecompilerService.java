package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.benf.cfr.reader.Main;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CFR 反编译服务。
 * <p>
 * 导入流程在解压完成后调用该服务，将主 classes 目录和包内嵌套 Jar 中的 class 文件反编译到
 * sources 目录。反编译结果是用户编辑 Java 代码的入口，后续保存接口会把修改记录写入 SQLite。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class DecompilerService {

    private static final String MODULE_INFO_CLASS_FILE = "module-info.class";
    private static final String NESTED_JAR_TEMP_PREFIX = "jarpatch-nested-classes-";

    private final ArchiveService archiveService;

    /**
     * 创建反编译服务。
     *
     * @param archiveService 压缩包服务，用于把嵌套 Jar 展开为临时 class 目录
     */
    public DecompilerService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    /**
     * 根据包类型选择主 class 根目录，并继续反编译用户选中的嵌套 Jar。
     * <p>
     * 对 Spring Boot 多模块项目，入口模块通常位于 BOOT-INF/classes，各业务模块会以 Jar 形式
     * 放在 BOOT-INF/lib。前端预解析后会把用户勾选的嵌套 Jar 路径传入这里，反编译结果输出到
     * sources/nested-jars/{原 Jar 相对路径}，后续编译服务可以据此把 class 写回原嵌套 Jar。
     * </p>
     *
     * @param extractedDir       解压目录
     * @param sourceDir          源码输出目录
     * @param packageType        包类型编码
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @throws IOException 目录创建或反编译失败时抛出
     */
    public void decompile(Path extractedDir, Path sourceDir, String packageType, Set<String> selectedNestedJars) throws IOException {
        Files.createDirectories(sourceDir);
        Path classesRoot = resolveClassesRoot(extractedDir, packageType);
        if (Files.exists(classesRoot)) {
            decompileClassesRoot(classesRoot, sourceDir);
        }

        decompileNestedJars(extractedDir, selectedNestedJars, sourceDir);
    }

    /**
     * 反编译一个 class 根目录下的全部 class 文件。
     *
     * @param classesRoot class 根目录
     * @param sourceDir   源码输出目录
     * @throws IOException 读取 class 文件失败时抛出
     */
    private void decompileClassesRoot(Path classesRoot, Path sourceDir) throws IOException {
        for (Path classFile : findClassFiles(classesRoot)) {
            decompileClassFile(classFile, sourceDir);
        }
    }

    /**
     * 反编译用户选择的嵌套 Jar。
     * <p>
     * 入口在导入项目接口，实际执行点是 CFR Main，结果写入 sources/nested-jars。
     * 选择范围来自导入前预解析弹窗，用户未勾选时不反编译任何嵌套 Jar，只保留主 classes 源码。
     * </p>
     *
     * @param extractedDir       解压目录
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @param sourceDir          源码输出目录
     * @throws IOException 查找嵌套 Jar 失败时抛出
     */
    private void decompileNestedJars(Path extractedDir, Set<String> selectedNestedJars, Path sourceDir) throws IOException {
        if (selectedNestedJars == null || selectedNestedJars.isEmpty()) {
            return;
        }
        for (Path nestedJar : findSelectedNestedJars(extractedDir, selectedNestedJars)) {
            Path nestedSourceDir = sourceDir.resolve(JarPatchConstants.SOURCE_NESTED_JAR_DIR)
                    .resolve(extractedDir.relativize(nestedJar).toString());
            Files.createDirectories(nestedSourceDir);
            decompileArchive(nestedJar, nestedSourceDir);
        }
    }

    /**
     * 按包类型解析 class 文件根目录。
     *
     * @param extractedDir 解压目录
     * @param packageType  包类型编码
     * @return class 文件根目录
     */
    private Path resolveClassesRoot(Path extractedDir, String packageType) {
        if ("SPRING_BOOT_JAR".equals(packageType)) {
            return extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR);
        }
        if ("WAR".equals(packageType)) {
            return extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR);
        }
        return extractedDir;
    }

    /**
     * 查找 class 文件列表。
     *
     * @param classesRoot class 根目录
     * @return class 文件列表
     * @throws IOException 读取目录失败时抛出
     */
    private List<Path> findClassFiles(Path classesRoot) throws IOException {
        try (Stream<Path> stream = Files.walk(classesRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::isDecompilableClassFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 判断当前 class 是否应该交给 CFR 反编译。
     * <p>
     * Java 9 之后的 module-info.class 是模块描述文件，不是普通业务类。CFR 按普通 class
     * 处理它会抛出 ACC_MODULE 异常，因此导入流程会跳过该文件，结果不写入 sources。
     * </p>
     *
     * @param path class 文件路径
     * @return 可反编译时返回 true
     */
    private boolean isDecompilableClassFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith("." + JarPatchConstants.CLASS_EXTENSION)
                && !MODULE_INFO_CLASS_FILE.equals(fileName);
    }

    /**
     * 根据用户选择结果解析真实嵌套 Jar 路径。
     *
     * @param extractedDir       解压目录
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @return 需要反编译的嵌套 Jar 路径列表
     */
    private List<Path> findSelectedNestedJars(Path extractedDir, Set<String> selectedNestedJars) {
        Path normalizedExtractedDir = extractedDir.toAbsolutePath().normalize();
        return selectedNestedJars.stream()
                .map(path -> normalizedExtractedDir.resolve(path).normalize())
                .filter(path -> isValidSelectedNestedJar(normalizedExtractedDir, path))
                .collect(Collectors.toList());
    }

    /**
     * 校验用户选择的嵌套 Jar 是否仍位于解压目录内。
     *
     * @param extractedDir 解压目录
     * @param nestedJar    用户选择的嵌套 Jar 真实路径
     * @return 可反编译时返回 true
     */
    private boolean isValidSelectedNestedJar(Path extractedDir, Path nestedJar) {
        return nestedJar.startsWith(extractedDir)
                && Files.isRegularFile(nestedJar)
                && nestedJar.getFileName().toString().endsWith("." + JarPatchConstants.JAR_EXTENSION);
    }

    /**
     * 反编译单个 class 文件。
     *
     * @param classFile class 文件路径
     * @param sourceDir Java 源码输出目录
     */
    private void decompileClassFile(Path classFile, Path sourceDir) {
        // 入口在导入项目接口，实际执行点在 CFR Main，结果写入项目工作区 sources 目录。
        String[] args = new String[]{
                classFile.toAbsolutePath().toString(),
                "--outputdir", sourceDir.toAbsolutePath().toString(),
                "--silent", "true"
        };
        Main.main(args);
    }

    /**
     * 反编译单个嵌套 Jar。
     *
     * @param archiveFile 嵌套 Jar 文件
     * @param sourceDir   当前嵌套 Jar 的源码输出目录
     * @throws IOException 解压或读取嵌套 Jar 失败时抛出
     */
    private void decompileArchive(Path archiveFile, Path sourceDir) throws IOException {
        Path tempDir = Files.createTempDirectory(NESTED_JAR_TEMP_PREFIX);
        try {
            archiveService.unzip(archiveFile, tempDir);
            // 嵌套 Jar 先展开再逐个 class 反编译，确保 module-info.class 不进入 CFR 普通类处理链路。
            decompileClassesRoot(tempDir, sourceDir);
        } finally {
            deleteDirectoryQuietly(tempDir);
        }
    }

    /**
     * 删除嵌套 Jar 反编译使用的临时目录。
     *
     * @param root 临时目录根路径
     */
    private void deleteDirectoryQuietly(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePathQuietly);
        } catch (IOException e) {
            throw new IllegalStateException("删除反编译临时目录失败: " + root, e);
        }
    }

    /**
     * 删除单个临时路径。
     *
     * @param path 待删除路径
     */
    private void deletePathQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("删除反编译临时文件失败: " + path, e);
        }
    }
}
