package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import com.jarpatch.model.NestedJarCandidate;
import com.jarpatch.model.ProjectImportInspection;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 导入前包结构预解析服务。
 * <p>
 * 前端选择 Jar/War 后先调用该服务。服务会读取包内 pom.xml、主 classes 包名前缀和嵌套 Jar，
 * 返回可供用户勾选的反编译候选项；正式导入时用户选择结果再传给 ProjectService。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ProjectInspectionService {

    private static final String POM_FILE_NAME = "pom.xml";
    private static final String MODULES_TAG_NAME = "modules";
    private static final String MODULE_TAG_NAME = "module";
    private static final String SPRING_BOOT_CLASSES_PREFIX = JarPatchConstants.SPRING_BOOT_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
    private static final String WAR_CLASSES_PREFIX = JarPatchConstants.WAR_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR;
    private static final String SPRING_BOOT_LIB_PREFIX = JarPatchConstants.SPRING_BOOT_LIB_DIR + JarPatchConstants.ZIP_SEPARATOR;
    private static final String WAR_LIB_PREFIX = JarPatchConstants.WAR_LIB_DIR + JarPatchConstants.ZIP_SEPARATOR;
    private static final String CLASS_SUFFIX = "." + JarPatchConstants.CLASS_EXTENSION;
    private static final String JAR_SUFFIX = "." + JarPatchConstants.JAR_EXTENSION;
    private static final String MODULE_INFO_CLASS_FILE = "module-info.class";
    private static final String REASON_POM_MATCH = "pom.xml 模块匹配";
    private static final String REASON_PACKAGE_MATCH = "应用包名前缀匹配";
    private static final String REASON_MANUAL = "等待手动选择";
    private static final String MESSAGE_POM_PARSE_FAILED = "解析 pom.xml 失败";
    private static final int APPLICATION_PACKAGE_PREFIX_DEPTH = 3;

    private final ArchiveService archiveService;

    /**
     * 创建导入前包结构预解析服务。
     *
     * @param archiveService 共享压缩包安全校验服务
     */
    public ProjectInspectionService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    /**
     * 预解析 Jar 或 War 文件。
     * <p>
     * 入口在 /api/projects/inspect，实际读取点是用户选择的原始包，结果只返回给前端，
     * 不创建工作区、不写 SQLite。
     * </p>
     *
     * @param filePath 原始包路径
     * @return 预解析结果
     * @throws IOException 读取包失败时抛出
     */
    public ProjectImportInspection inspect(String filePath) throws IOException {
        Path archiveFile = Paths.get(filePath).toAbsolutePath().normalize();
        validateArchive(archiveFile);
        archiveService.validateArchive(archiveFile);
        try (ZipFile zipFile = new ZipFile(archiveFile.toFile())) {
            PackageType packageType = detectPackageType(archiveFile, zipFile);
            Set<String> pomModules = readPomModules(zipFile);
            Set<String> applicationPackagePrefixes = findApplicationPackagePrefixes(zipFile, packageType);
            List<NestedJarCandidate> candidates = findNestedJarCandidates(zipFile, packageType, pomModules, applicationPackagePrefixes);
            ProjectImportInspection inspection = new ProjectImportInspection();
            inspection.setFilePath(archiveFile.toString());
            inspection.setPackageType(packageType.getCode());
            inspection.setPomModules(new ArrayList<>(pomModules));
            inspection.setCandidates(candidates);
            inspection.setSelectedCount((int) candidates.stream().filter(NestedJarCandidate::isSelected).count());
            return inspection;
        }
    }

    /**
     * 校验原始包是否存在且类型受支持。
     *
     * @param archiveFile 原始包路径
     */
    private void validateArchive(Path archiveFile) {
        String fileName = archiveFile.getFileName().toString().toLowerCase();
        boolean supported = fileName.endsWith("." + JarPatchConstants.JAR_EXTENSION)
                || fileName.endsWith("." + JarPatchConstants.WAR_EXTENSION);
        if (!Files.exists(archiveFile) || !supported) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_UNSUPPORTED_PACKAGE);
        }
    }

    /**
     * 根据文件后缀和 Zip 条目识别包类型。
     *
     * @param archiveFile 原始包路径
     * @param zipFile     Zip 文件
     * @return 包类型
     */
    private PackageType detectPackageType(Path archiveFile, ZipFile zipFile) {
        String fileName = archiveFile.getFileName().toString().toLowerCase();
        if (fileName.endsWith("." + JarPatchConstants.WAR_EXTENSION)) {
            return PackageType.WAR;
        }
        if (zipFile.getEntry(JarPatchConstants.SPRING_BOOT_CLASSES_DIR + JarPatchConstants.ZIP_SEPARATOR) != null) {
            return PackageType.SPRING_BOOT_JAR;
        }
        return PackageType.STANDARD_JAR;
    }

    /**
     * 读取包内 pom.xml 的模块名和 artifactId。
     *
     * @param zipFile Zip 文件
     * @return pom.xml 中可用于匹配嵌套 Jar 的模块名集合
     * @throws IOException 读取 pom.xml 失败时抛出
     */
    private Set<String> readPomModules(ZipFile zipFile) throws IOException {
        Set<String> modules = new TreeSet<>();
        for (ZipEntry entry : zipFile.stream().toList()) {
            if (!entry.isDirectory() && entry.getName().endsWith(POM_FILE_NAME)) {
                String content = readEntryAsString(zipFile, entry);
                modules.addAll(readPomModuleValues(content));
            }
        }
        return modules;
    }

    /**
     * 把 Zip 条目读取为 UTF-8 文本。
     *
     * @param zipFile Zip 文件
     * @param entry   Zip 条目
     * @return 条目内容
     * @throws IOException 读取失败时抛出
     */
    private String readEntryAsString(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 使用 XML 解析器读取 pom.xml 中 modules 下的直接 module 值。
     *
     * @param xml XML 文本
     * @return 标签值集合
     */
    private Set<String> readPomModuleValues(String xml) {
        Set<String> values = new TreeSet<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            NodeList modulesNodes = document.getElementsByTagName(MODULES_TAG_NAME);
            for (int modulesIndex = 0; modulesIndex < modulesNodes.getLength(); modulesIndex++) {
                collectDirectModuleValues(modulesNodes.item(modulesIndex), values);
            }
        } catch (Exception e) {
            throw new IllegalStateException(MESSAGE_POM_PARSE_FAILED, e);
        }
        return values;
    }

    /**
     * 收集 modules 节点下的直接 module 子节点值。
     *
     * @param modulesNode modules 节点
     * @param values      模块名集合
     */
    private void collectDirectModuleValues(Node modulesNode, Set<String> values) {
        NodeList children = modulesNode.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (MODULE_TAG_NAME.equals(child.getNodeName())) {
                String value = child.getTextContent() == null ? JarPatchConstants.EMPTY_TEXT
                        : child.getTextContent().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
    }

    /**
     * 从主 class 条目推导应用包名前缀。
     *
     * @param zipFile     Zip 文件
     * @param packageType 包类型
     * @return 应用包名前缀集合
     */
    private Set<String> findApplicationPackagePrefixes(ZipFile zipFile, PackageType packageType) {
        String classPrefix = resolveClassPrefix(packageType);
        Set<String> prefixes = new HashSet<>();
        for (ZipEntry entry : zipFile.stream().toList()) {
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.startsWith(classPrefix) && isNormalClassEntry(entryName)) {
                String packagePrefix = resolvePackagePrefix(entryName.substring(classPrefix.length()));
                if (!packagePrefix.isEmpty()) {
                    prefixes.add(packagePrefix);
                }
            }
        }
        return prefixes;
    }

    /**
     * 按包类型解析主 class 条目前缀。
     *
     * @param packageType 包类型
     * @return 主 class 条目前缀
     */
    private String resolveClassPrefix(PackageType packageType) {
        if (PackageType.SPRING_BOOT_JAR == packageType) {
            return SPRING_BOOT_CLASSES_PREFIX;
        }
        if (PackageType.WAR == packageType) {
            return WAR_CLASSES_PREFIX;
        }
        return JarPatchConstants.EMPTY_TEXT;
    }

    /**
     * 判断条目是否是普通 class。
     *
     * @param entryName Zip 条目名称
     * @return 是普通 class 时返回 true
     */
    private boolean isNormalClassEntry(String entryName) {
        return entryName.endsWith(CLASS_SUFFIX) && !entryName.endsWith(MODULE_INFO_CLASS_FILE);
    }

    /**
     * 解析 class 条目对应的应用包名前缀。
     *
     * @param classEntryName class 条目相对主 class 根目录的路径
     * @return 包名前缀
     */
    private String resolvePackagePrefix(String classEntryName) {
        String[] parts = classEntryName.split(JarPatchConstants.ZIP_SEPARATOR);
        if (parts.length <= 1) {
            return JarPatchConstants.EMPTY_TEXT;
        }
        int depth = Math.min(APPLICATION_PACKAGE_PREFIX_DEPTH, parts.length - 1);
        return String.join(JarPatchConstants.ZIP_SEPARATOR, List.of(parts).subList(0, depth))
                + JarPatchConstants.ZIP_SEPARATOR;
    }

    /**
     * 查找嵌套 Jar 候选项。
     *
     * @param zipFile                    Zip 文件
     * @param packageType                包类型
     * @param pomModules                 pom.xml 模块名集合
     * @param applicationPackagePrefixes 应用包名前缀集合
     * @return 候选项列表
     * @throws IOException 读取嵌套 Jar 失败时抛出
     */
    private List<NestedJarCandidate> findNestedJarCandidates(ZipFile zipFile,
                                                             PackageType packageType,
                                                             Set<String> pomModules,
                                                             Set<String> applicationPackagePrefixes) throws IOException {
        String libPrefix = resolveLibPrefix(packageType);
        List<NestedJarCandidate> candidates = new ArrayList<>();
        if (libPrefix.isEmpty()) {
            return candidates;
        }
        for (ZipEntry entry : zipFile.stream().toList()) {
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.startsWith(libPrefix) && entryName.endsWith(JAR_SUFFIX)) {
                candidates.add(buildCandidate(zipFile, entry, pomModules, applicationPackagePrefixes));
            }
        }
        candidates.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        return candidates;
    }

    /**
     * 按包类型解析依赖 Jar 条目前缀。
     *
     * @param packageType 包类型
     * @return 依赖 Jar 条目前缀
     */
    private String resolveLibPrefix(PackageType packageType) {
        if (PackageType.SPRING_BOOT_JAR == packageType) {
            return SPRING_BOOT_LIB_PREFIX;
        }
        if (PackageType.WAR == packageType) {
            return WAR_LIB_PREFIX;
        }
        return JarPatchConstants.EMPTY_TEXT;
    }

    /**
     * 构建单个嵌套 Jar 候选项。
     *
     * @param zipFile                    Zip 文件
     * @param entry                      嵌套 Jar 条目
     * @param pomModules                 pom.xml 模块名集合
     * @param applicationPackagePrefixes 应用包名前缀集合
     * @return 候选项
     * @throws IOException 读取嵌套 Jar 失败时抛出
     */
    private NestedJarCandidate buildCandidate(ZipFile zipFile,
                                              ZipEntry entry,
                                              Set<String> pomModules,
                                              Set<String> applicationPackagePrefixes) throws IOException {
        String name = entry.getName().substring(entry.getName().lastIndexOf(JarPatchConstants.ZIP_SEPARATOR) + 1);
        NestedJarStats stats = inspectNestedJar(zipFile, entry, applicationPackagePrefixes);
        boolean pomMatched = matchesPomModule(name, pomModules);
        NestedJarCandidate candidate = new NestedJarCandidate();
        candidate.setPath(entry.getName());
        candidate.setName(name);
        candidate.setClassCount(stats.classCount());
        candidate.setSelected(pomMatched || stats.containsApplicationClass());
        candidate.setReason(resolveCandidateReason(pomMatched, stats.containsApplicationClass()));
        return candidate;
    }

    /**
     * 判断 Jar 文件名是否匹配 pom.xml 中的模块或 artifactId。
     *
     * @param jarName    Jar 文件名
     * @param pomModules pom.xml 模块名集合
     * @return 匹配时返回 true
     */
    private boolean matchesPomModule(String jarName, Set<String> pomModules) {
        String lowerName = jarName.toLowerCase();
        return pomModules.stream()
                .map(String::toLowerCase)
                .anyMatch(module -> lowerName.equals(module + JAR_SUFFIX) || lowerName.startsWith(module + "-"));
    }

    /**
     * 读取嵌套 Jar 的 class 数量和应用包名前缀命中状态。
     *
     * @param zipFile                    Zip 文件
     * @param entry                      嵌套 Jar 条目
     * @param applicationPackagePrefixes 应用包名前缀集合
     * @return 嵌套 Jar 统计
     * @throws IOException 读取嵌套 Jar 失败时抛出
     */
    private NestedJarStats inspectNestedJar(ZipFile zipFile,
                                            ZipEntry entry,
                                            Set<String> applicationPackagePrefixes) throws IOException {
        int classCount = 0;
        boolean containsApplicationClass = false;
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream(entry))) {
            ZipEntry nestedEntry;
            while ((nestedEntry = zipInputStream.getNextEntry()) != null) {
                String nestedEntryName = nestedEntry.getName();
                if (!nestedEntry.isDirectory() && isNormalClassEntry(nestedEntryName)) {
                    classCount++;
                    if (applicationPackagePrefixes.stream().anyMatch(nestedEntryName::startsWith)) {
                        containsApplicationClass = true;
                    }
                }
                zipInputStream.closeEntry();
            }
        }
        return new NestedJarStats(classCount, containsApplicationClass);
    }

    /**
     * 解析候选项默认选择原因。
     *
     * @param pomMatched               是否命中 pom.xml 模块
     * @param containsApplicationClass 是否命中应用包名前缀
     * @return 原因文案
     */
    private String resolveCandidateReason(boolean pomMatched, boolean containsApplicationClass) {
        if (pomMatched) {
            return REASON_POM_MATCH;
        }
        if (containsApplicationClass) {
            return REASON_PACKAGE_MATCH;
        }
        return REASON_MANUAL;
    }

    /**
     * 嵌套 Jar 统计结果。
     *
     * @param classCount               普通 class 数量
     * @param containsApplicationClass 是否包含应用 class
     */
    private record NestedJarStats(int classCount, boolean containsApplicationClass) {
    }
}
