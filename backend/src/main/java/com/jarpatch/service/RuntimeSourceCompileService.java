package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.JdkCompilerInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipFile;

/**
 * 在本机按运行中 class 的 Java 版本编译单类源码，使用本地部署包解析依赖。
 * 产物在临时目录校验后返回运行时工作台，由工作台完成目标校验、备份和热替换。
 *
 * @author 黄杰
 */
@Service
public class RuntimeSourceCompileService {

    private static final Duration COMPILE_TIMEOUT = Duration.ofSeconds(90);
    private static final int MINIMUM_RELEASE_COMPILER_VERSION = 9;
    private static final String JAVA_SUFFIX = ".java";
    private static final String CLASS_SUFFIX = ".class";
    private static final String JAR_SUFFIX = ".jar";
    private static final List<String> CLASS_ROOTS = List.of(JarPatchConstants.SPRING_BOOT_CLASSES_DIR,
            JarPatchConstants.WAR_CLASSES_DIR);
    private static final List<String> LIB_ROOTS = List.of(JarPatchConstants.SPRING_BOOT_LIB_DIR,
            JarPatchConstants.WAR_LIB_DIR, "WEB-INF/lib-provided");

    private final RuntimeClassComparisonService runtimeClasses;
    private final ArchiveExtractor archiveExtractor;
    private final JavaVersionService javaVersionService;
    private final JdkService jdkService;
    private final CompileProcessRunner compileProcessRunner;

    /**
     * 复用归档校验、原 class 版本识别及本地项目的 JDK 配置和 javac 执行器。
     */
    public RuntimeSourceCompileService(RuntimeClassComparisonService runtimeClasses, ArchiveExtractor archiveExtractor,
                                       JavaVersionService javaVersionService, JdkService jdkService,
                                       CompileProcessRunner compileProcessRunner) {
        this.runtimeClasses = runtimeClasses;
        this.archiveExtractor = archiveExtractor;
        this.javaVersionService = javaVersionService;
        this.jdkService = jdkService;
        this.compileProcessRunner = compileProcessRunner;
    }

    /**
     * 编译当前源码并返回唯一的目标 class。编辑稿、依赖以及 javac 产物仅写入临时目录。
     *
     * @param archivePath 本地部署包路径
     * @param className 当前 JVM 加载的类全名
     * @param contents 打开编辑器时采集的原 class 的 Base64 内容
     * @param source 用户在工作台中修改的 Java 源码
     * @return 编译产物的 Base64 内容
     * @throws IOException 部署包、临时文件或编译器无法读取
     * @throws InterruptedException 编译进程等待被中断
     */
    public String compile(String archivePath, String className, String contents, String source)
            throws IOException, InterruptedException {
        String classEntry = RuntimeClassComparisonService.classEntry(className);
        byte[] snapshot = RuntimeClassComparisonService.decodeSnapshot(contents);
        if (source == null || source.isBlank()
                || source.getBytes(StandardCharsets.UTF_8).length > RuntimeClassComparisonService.MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("请输入 Java 源码，内容须小于或等于 2 MiB");
        }
        Path archive = runtimeClasses.requireArchive(archivePath);
        int release = javaVersionService.detectClassFeatureVersion(snapshot, className);
        JdkCompilerInfo compiler = jdkService.findCompiler();
        if (compiler.getFeatureVersion() < Math.max(release, MINIMUM_RELEASE_COMPILER_VERSION)) {
            throw new IllegalArgumentException("配置的 javac 不支持目标 Java " + release + "，请在「开发能力」配置兼容 JDK");
        }

        Path working = Files.createTempDirectory("jarpatch-runtime-compile-");
        long deadline = System.nanoTime() + COMPILE_TIMEOUT.toNanos();
        BooleanSupplier timedOut = () -> System.nanoTime() - deadline >= 0;
        try {
            String javaEntry = classEntry.substring(0, classEntry.length() - CLASS_SUFFIX.length()) + JAVA_SUFFIX;
            Path javaFile = working.resolve("source").resolve(javaEntry);
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, source);
            Path output = Files.createDirectory(working.resolve("output"));
            String classpath = buildClasspath(archive, working.resolve("dependencies"), timedOut);
            List<String> command = compileProcessRunner.prepareCommand(compiler.getJavacPath(), output, release,
                    classpath, List.of(javaFile));
            compileProcessRunner.run(command, working, timedOut);
            return readCompiledClass(output, classEntry);
        } catch (IllegalStateException exception) {
            if (timedOut.getAsBoolean() && JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(exception.getMessage())) {
                throw new IllegalStateException("准备依赖或编译超过 " + COMPILE_TIMEOUT.toSeconds() + " 秒", exception);
            }
            throw exception;
        } finally {
            FileSystemUtils.deleteRecursively(working);
        }
    }

    /**
     * 普通 Jar 直接提供依赖，Spring Boot 和 WAR 解包后使用主类目录及标准依赖目录。
     */
    private String buildClasspath(Path archive, Path extracted, BooleanSupplier timedOut) throws IOException {
        List<String> roots = new ArrayList<>(CLASS_ROOTS);
        roots.addAll(LIB_ROOTS);
        boolean nested;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            nested = zip.stream().anyMatch(entry -> roots.stream().anyMatch(root -> entry.getName().startsWith(root + "/")));
        }
        if (!nested) {
            archiveExtractor.validate(archive);
            return archive.toString();
        }
        archiveExtractor.unzip(archive, extracted, timedOut);
        List<String> paths = new ArrayList<>();
        CLASS_ROOTS.stream().map(extracted::resolve).filter(Files::isDirectory).map(Path::toString).forEach(paths::add);
        for (String libRoot : LIB_ROOTS) {
            Path library = extracted.resolve(libRoot);
            if (!Files.isDirectory(library)) continue;
            try (var files = Files.list(library)) {
                files.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().endsWith(JAR_SUFFIX))
                        .sorted().map(Path::toString).forEach(paths::add);
            }
        }
        paths.add(archive.toString());
        return String.join(File.pathSeparator, paths);
    }

    /**
     * 仅返回目标 class。若编译还生成了其他 class，停止本次替换，避免只应用了补丁的一部分。
     */
    private String readCompiledClass(Path output, String classEntry) throws IOException {
        Path target = output.resolve(classEntry);
        if (!Files.isRegularFile(target)) throw new IllegalStateException("编译产物的类名与当前 JVM 目标不一致");
        try (var files = Files.walk(output)) {
            if (files.anyMatch(file -> !file.equals(target) && Files.isRegularFile(file)
                    && file.getFileName().toString().endsWith(CLASS_SUFFIX))) {
                throw new IllegalStateException("编译生成了多个 class，当前在线编辑只支持单 class 补丁");
            }
        }
        try (InputStream input = Files.newInputStream(target)) {
            byte[] bytes = input.readNBytes(RuntimeClassComparisonService.MAX_CLASS_BYTES + 1);
            if (bytes.length > RuntimeClassComparisonService.MAX_CLASS_BYTES) {
                throw new IllegalStateException("编译后的 class 超过 1 MiB");
            }
            return Base64.getEncoder().encodeToString(bytes);
        }
    }
}
