package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.JavaVersionInfo;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Java class 目标版本识别服务。
 * <p>
 * 导入阶段读取主业务 class 文件头形成项目版本记录；编译阶段按每个修改源码对应的原始
 * class 再次复核，CompileService 只使用该明确版本生成 --release 参数。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class JavaVersionService {

    private static final int CLASS_MAGIC = 0xCAFEBABE;
    private static final int CLASS_MAJOR_TO_FEATURE_OFFSET = 44;
    private static final int MINIMUM_RELEASE_VERSION = 7;
    private static final String CLASS_SUFFIX = "." + JarPatchConstants.CLASS_EXTENSION;
    private static final String JAVA_SUFFIX = "." + JarPatchConstants.JAVA_EXTENSION;
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String MODULE_INFO_CLASS = "module-info.class";
    private static final String ARCHIVE_ENTRY_SEPARATOR = "!/";
    private static final String MESSAGE_NO_MAIN_CLASS = "包内没有主业务 class";
    private static final String MESSAGE_INVALID_CLASS = "不是有效的 class 文件";
    private static final String MESSAGE_UNSUPPORTED_MAJOR_VERSION = "不支持的 class major version";
    private static final String MESSAGE_JAVA_VERSION_PREFIX = "Java ";
    private static final int UNKNOWN_VERSION = 0;

    private final WorkspaceService workspaceService;

    /**
     * 创建 Java 版本识别服务。
     *
     * @param workspaceService 工作区服务
     */
    public JavaVersionService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * 从导入临时工作区的主业务 class 中识别项目目标 Java 版本。
     *
     * @param extractedDir 解压目录
     * @param packageType  包类型码
     * @return 版本识别结果；没有 class 时返回零版本说明
     * @throws IOException class 文件读取失败时抛出
     */
    public JavaVersionInfo detectProjectVersion(Path extractedDir, String packageType) throws IOException {
        Path classRoot = classRoot(extractedDir, packageType);
        if (!Files.isDirectory(classRoot)) {
            return new JavaVersionInfo(UNKNOWN_VERSION, UNKNOWN_VERSION, MESSAGE_NO_MAIN_CLASS);
        }
        try (var stream = Files.walk(classRoot)) {
            var iterator = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(CLASS_SUFFIX))
                    .filter(path -> !isMultiReleaseClass(classRoot, path))
                    .filter(path -> !MODULE_INFO_CLASS.equals(path.getFileName().toString()))
                    .iterator();
            JavaVersionInfo selected = null;
            while (iterator.hasNext()) {
                Path classFile = iterator.next();
                int majorVersion = readMajorVersion(classFile);
                if (selected == null || majorVersion > selected.getClassMajorVersion()) {
                    selected = new JavaVersionInfo(toFeatureVersion(majorVersion), majorVersion,
                            classRoot.relativize(classFile).toString().replace('\\', '/'));
                }
            }
            return selected == null
                    ? new JavaVersionInfo(UNKNOWN_VERSION, UNKNOWN_VERSION, MESSAGE_NO_MAIN_CLASS) : selected;
        }
    }

    /**
     * 识别同一编译目标内所有修改源码对应的原始 class 版本，并要求版本完全一致。
     *
     * @param project       项目记录
     * @param compileTarget 主 classes 标识或嵌套 Jar 相对路径
     * @param javaPaths     本组修改源码路径
     * @return 严格目标版本
     * @throws IOException 原 class 读取失败时抛出
     */
    public JavaVersionInfo detectCompileTargetVersion(ProjectRecord project,
                                                      String compileTarget,
                                                      List<String> javaPaths) throws IOException {
        JavaVersionInfo selected = null;
        for (String javaPath : javaPaths) {
            String classEntry = resolveClassEntry(compileTarget, javaPath);
            int majorVersion = JarPatchConstants.COMPILE_TARGET_MAIN.equals(compileTarget)
                    ? readMainClassMajor(project, classEntry)
                    : readNestedClassMajor(project, compileTarget, classEntry);
            if (selected != null && selected.getClassMajorVersion() != majorVersion) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_CLASS_VERSION_CONFLICT);
            }
            selected = new JavaVersionInfo(toFeatureVersion(majorVersion), majorVersion,
                    compileTarget + ARCHIVE_ENTRY_SEPARATOR + classEntry);
        }
        if (selected == null || selected.getFeatureVersion() < MINIMUM_RELEASE_VERSION) {
            throw new IllegalArgumentException(selected == null
                    ? JarPatchConstants.MESSAGE_CLASS_VERSION_NOT_FOUND
                    : JarPatchConstants.MESSAGE_JAVA_RELEASE_UNSUPPORTED
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + MESSAGE_JAVA_VERSION_PREFIX
                    + selected.getFeatureVersion());
        }
        return selected;
    }

    /**
     * 读取主 classes 目录内的原始 class major version。
     *
     * @param project    项目记录
     * @param classEntry class 相对路径
     * @return class major version
     * @throws IOException 读取失败时抛出
     */
    private int readMainClassMajor(ProjectRecord project, String classEntry) throws IOException {
        Path classFile = classRoot(workspaceService.extractedDir(project), project.getPackageType()).resolve(classEntry).normalize();
        if (!Files.isRegularFile(classFile)) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_CLASS_VERSION_NOT_FOUND
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + classEntry);
        }
        return readMajorVersion(classFile);
    }

    /**
     * 读取嵌套 Jar 指定条目的原始 class major version。
     *
     * @param project       项目记录
     * @param compileTarget 嵌套 Jar 相对路径
     * @param classEntry    Jar 内 class 条目
     * @return class major version
     * @throws IOException 读取失败时抛出
     */
    private int readNestedClassMajor(ProjectRecord project, String compileTarget, String classEntry) throws IOException {
        Path jarFile = workspaceService.resolveExtracted(project, compileTarget);
        try (ZipFile zipFile = new ZipFile(jarFile.toFile())) {
            ZipEntry entry = zipFile.getEntry(classEntry);
            if (entry == null || entry.isDirectory()) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_CLASS_VERSION_NOT_FOUND
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + compileTarget
                        + ARCHIVE_ENTRY_SEPARATOR + classEntry);
            }
            try (InputStream inputStream = zipFile.getInputStream(entry)) {
                return readMajorVersion(inputStream, compileTarget + ARCHIVE_ENTRY_SEPARATOR + classEntry);
            }
        }
    }

    /**
     * 把源码树路径转换为原始 class 条目路径。
     *
     * @param compileTarget 编译目标
     * @param javaPath      源码树路径
     * @return class 条目路径
     */
    private String resolveClassEntry(String compileTarget, String javaPath) {
        if (javaPath == null || !javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)
                || !javaPath.endsWith(JAVA_SUFFIX)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_CLASS_VERSION_NOT_FOUND
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + javaPath);
        }
        String sourceRelativePath = javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length());
        if (!JarPatchConstants.COMPILE_TARGET_MAIN.equals(compileTarget)) {
            String prefix = JarPatchConstants.SOURCE_NESTED_JAR_DIR + JarPatchConstants.ZIP_SEPARATOR
                    + compileTarget + JarPatchConstants.ZIP_SEPARATOR;
            if (!sourceRelativePath.startsWith(prefix)) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH);
            }
            sourceRelativePath = sourceRelativePath.substring(prefix.length());
        }
        return sourceRelativePath.substring(0, sourceRelativePath.length() - JAVA_SUFFIX.length()) + CLASS_SUFFIX;
    }

    /**
     * 读取磁盘 class 文件 major version。
     *
     * @param classFile class 文件
     * @return class major version
     * @throws IOException 读取失败时抛出
     */
    private int readMajorVersion(Path classFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(classFile)) {
            return readMajorVersion(inputStream, classFile.toString());
        }
    }

    /**
     * 从 class 输入流读取并校验文件头。
     *
     * @param inputStream class 输入流
     * @param evidence    检测依据说明
     * @return class major version
     * @throws IOException 读取失败时抛出
     */
    private int readMajorVersion(InputStream inputStream, String evidence) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream))) {
            if (dataInputStream.readInt() != CLASS_MAGIC) {
                throw new IllegalArgumentException(MESSAGE_INVALID_CLASS
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + evidence);
            }
            dataInputStream.readUnsignedShort();
            return dataInputStream.readUnsignedShort();
        }
    }

    /**
     * 把 class major version 转换为 Java 特性版本。
     *
     * @param majorVersion class major version
     * @return Java 特性版本
     */
    private int toFeatureVersion(int majorVersion) {
        if (majorVersion <= CLASS_MAJOR_TO_FEATURE_OFFSET) {
            throw new IllegalArgumentException(MESSAGE_UNSUPPORTED_MAJOR_VERSION
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + majorVersion);
        }
        return majorVersion - CLASS_MAJOR_TO_FEATURE_OFFSET;
    }

    /**
     * 判断 class 是否位于多版本 Jar 的版本目录。
     *
     * @param classRoot 主 class 根目录
     * @param classFile class 文件
     * @return 位于 META-INF/versions 时返回 true
     */
    private boolean isMultiReleaseClass(Path classRoot, Path classFile) {
        return classRoot.relativize(classFile).toString().replace('\\', '/').startsWith(MULTI_RELEASE_PREFIX);
    }

    /**
     * 按包类型解析主业务 class 根目录。
     *
     * @param extractedDir 解压目录
     * @param packageType  包类型码
     * @return class 根目录
     */
    private Path classRoot(Path extractedDir, String packageType) {
        if ("SPRING_BOOT_JAR".equals(packageType)) {
            return extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR);
        }
        if ("WAR".equals(packageType)) {
            return extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR);
        }
        return extractedDir;
    }
}
