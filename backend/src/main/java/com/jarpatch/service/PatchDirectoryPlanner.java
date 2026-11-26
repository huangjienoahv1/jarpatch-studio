package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.PatchDirectoryPreview;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.repository.FileChangeRepository;
import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassWriter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * 只读匹配外部补丁和包内 class，生成桌面端预览。写回服务在确认后重新验证同一计划。
 *
 * @author 黄杰
 */
@Service
class PatchDirectoryPlanner {

    static final String JAVA_SUFFIX = "." + JarPatchConstants.JAVA_EXTENSION;
    static final String CLASS_SUFFIX = "." + JarPatchConstants.CLASS_EXTENSION;
    static final String MAIN = JarPatchConstants.COMPILE_TARGET_MAIN;
    static final String MESSAGE_STALE = "补丁目录或目标内容已变化，请重新预览";
    private static final String JAR_SUFFIX = "." + JarPatchConstants.JAR_EXTENSION;
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";
    private static final String META_INF_PREFIX = "META-INF/";
    private static final int MAX_INPUT_FILES = 1000;
    private static final int MAX_FILE_BYTES = 8 * 1024 * 1024;
    private static final int CLASS_MAGIC = 0xCAFEBABE;
    private static final int JAVA_FEATURE_OFFSET = 44;
    private static final int PREVIEW_MINOR_VERSION = 0xffff;
    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern CLASS_ENTRY = Pattern.compile("(?:" + IDENTIFIER + "/)*" + IDENTIFIER + "\\.class");
    private static final String JAVA_TRIVIA = "(?:\\p{javaWhitespace}++|//[^\\r\\n]*+|/\\*(?s:.*?)\\*/)";
    private static final Pattern JAVA_IGNORED = Pattern.compile(JAVA_TRIVIA);
    private static final String SKIP_JAVA_TRIVIA = JAVA_TRIVIA + "*+";
    private static final Pattern JAVA_PACKAGE = Pattern.compile(SKIP_JAVA_TRIVIA + "package\\b" + SKIP_JAVA_TRIVIA
            + "(" + IDENTIFIER + "(?:" + SKIP_JAVA_TRIVIA + "\\." + SKIP_JAVA_TRIVIA + IDENTIFIER + ")*)"
            + SKIP_JAVA_TRIVIA + ";");
    private static final String MESSAGE_INVALID_CLASS = "class 文件损坏、类名无效或字节码版本超出解析范围";
    private static final String MESSAGE_PREVIEW_CLASS = "暂不支持启用了 Java 预览特性的 class";
    private static final String MESSAGE_MULTI_RELEASE = "该类存在多版本条目，无法确定应替换的版本";
    private static final String MESSAGE_SIGNED_NESTED = "目标嵌套 JAR 带有签名";
    private static final String MESSAGE_EXISTING_EDIT = "对应源码已有保存的编辑，请先处理该编辑";

    private final WorkspaceService workspaceService;
    private final CompileArtifactCommitter committer;
    private final FileChangeRepository fileChangeRepository;

    /** 创建只读匹配服务，复用工作区布局和手工源码修改记录。 */
    PatchDirectoryPlanner(WorkspaceService workspaceService, CompileArtifactCommitter committer,
                          FileChangeRepository fileChangeRepository) {
        this.workspaceService = workspaceService;
        this.committer = committer;
        this.fileChangeRepository = fileChangeRepository;
    }

    /**
     * 读取本机目录及当前包，为每个补丁文件生成候选位置和阻塞原因。
     *
     * @param project 当前包工作区
     * @param directory 选择的目录
     * @param checkCancelled 应用时的取消检查，预览时为空操作
     * @return 界面预览和成功匹配的应用计划
     */
    Plan inspect(ProjectRecord project, String directory, Runnable checkCancelled) throws IOException {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("请选择补丁目录");
        }
        Path root = Path.of(directory);
        if (!root.isAbsolute() || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("请选择存在的本机补丁目录");
        }
        root = root.toRealPath();
        Path extracted = workspaceService.extractedDir(project);
        Path classRoot = committer.classRoot(project);
        Path workspace = Path.of(project.getWorkspacePath()).toRealPath();
        if (root.startsWith(workspace) || workspace.startsWith(root)) {
            throw new IllegalArgumentException("请选择与项目工作区分开的补丁目录");
        }

        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(JAVA_SUFFIX) || name.endsWith(CLASS_SUFFIX);
                    })
                    .limit(MAX_INPUT_FILES + 1L).sorted().toList();
        }
        if (files.isEmpty() || files.size() > MAX_INPUT_FILES) {
            throw new IllegalArgumentException("补丁目录需要包含 1～" + MAX_INPUT_FILES + " 个 .java 或 .class 文件");
        }

        List<Input> inputs = new ArrayList<>();
        List<PatchDirectoryPreview.Entry> rows = new ArrayList<>();
        for (Path file : files) {
            checkCancelled.run();
            String relative = entryPath(root, file);
            try {
                byte[] bytes = readBounded(file);
                boolean source = relative.endsWith(JAVA_SUFFIX);
                ClassIdentity identity = source ? sourceIdentity(file, bytes) : classIdentity(bytes);
                inputs.add(new Input(file, relative, source, identity, digest(bytes)));
            } catch (IllegalArgumentException exception) {
                rows.add(new PatchDirectoryPreview.Entry(relative, "", List.of(), exception.getMessage()));
            }
        }

        Map<String, List<Target>> targets = findTargets(extracted, classRoot, inputs, checkCancelled);
        Set<String> conflicts = overlappingInputs(inputs);
        Set<String> edited = new HashSet<>(fileChangeRepository.findJavaPaths(project.getId()));
        Map<Input, Target> matches = new LinkedHashMap<>();
        MessageDigest fingerprint = sha256();
        update(fingerprint, project.getId());
        update(fingerprint, root.toString());
        for (Input input : inputs) {
            String entry = input.identity().entry();
            List<Target> candidates = targets.getOrDefault(entry, List.of());
            String problem = conflicts.contains(entry.toLowerCase(Locale.ROOT))
                    ? "补丁目录中有重复或重叠的类" : matchProblem(input, candidates, edited);
            List<String> paths = candidates.stream().map(Target::location).toList();
            rows.add(new PatchDirectoryPreview.Entry(input.relative(), stem(entry).replace('/', '.'), paths, problem));
            if (problem == null) {
                Target target = candidates.get(0);
                matches.put(input, target);
                update(fingerprint, input.relative());
                update(fingerprint, input.hash());
                update(fingerprint, target.location());
                update(fingerprint, target.hash());
            }
        }
        rows.sort(Comparator.comparing(PatchDirectoryPreview.Entry::input));
        boolean applicable = matches.size() == files.size();
        String confirmation = applicable ? HexFormat.of().formatHex(fingerprint.digest()) : null;
        return new Plan(new PatchDirectoryPreview(root.toString(), confirmation, applicable, List.copyOf(rows)), matches);
    }

    /** 重新读取并校验已确认的输入，避免扫描与暂存之间发生修改。 */
    static byte[] readConfirmed(Input input) throws IOException {
        byte[] bytes = readBounded(input.path());
        if (!input.hash().equals(digest(bytes))) {
            throw new IllegalArgumentException(MESSAGE_STALE);
        }
        return bytes;
    }

    /** 校验 class 结构，读取实际包名、类名和原始字节码版本。 */
    static ClassIdentity classIdentity(byte[] bytes) {
        ClassReader reader;
        String entry;
        try {
            originalMajor(bytes);
            reader = new ClassReader(bytes);
            entry = reader.getClassName() + CLASS_SUFFIX;
            reader.accept(new ClassWriter(0), 0);
        } catch (IllegalArgumentException exception) {
            if (MESSAGE_PREVIEW_CLASS.equals(exception.getMessage())) {
                throw exception;
            }
            throw new IllegalArgumentException(MESSAGE_INVALID_CLASS, exception);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(MESSAGE_INVALID_CLASS, exception);
        }
        if (!CLASS_ENTRY.matcher(entry).matches()) {
            throw new IllegalArgumentException(MESSAGE_INVALID_CLASS);
        }
        return new ClassIdentity(entry, reader.readUnsignedShort(6));
    }

    /** 将 class major 版本转换成 javac 的 release 参数。 */
    static int javaRelease(int major) {
        return major - JAVA_FEATURE_OFFSET;
    }

    /** 转为包内使用的斜杠路径。 */
    static String entryPath(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    /** 解码 UTF-8 源码并去掉可选 BOM，供预览和 javac 使用。 */
    static String sourceText(byte[] bytes) throws IOException {
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        return text.startsWith("\ufeff") ? text.substring(1) : text;
    }

    /** 从 UTF-8 源码开头的包声明和文件名定位 class，编译后还会验证实际产物。 */
    private static ClassIdentity sourceIdentity(Path file, byte[] bytes) {
        String text;
        try {
            text = sourceText(bytes);
        } catch (IOException exception) {
            throw new IllegalArgumentException("请使用 UTF-8 编码的 Java 源码", exception);
        }
        var declaration = JAVA_PACKAGE.matcher(text);
        String name = file.getFileName().toString();
        String prefix = declaration.lookingAt()
                ? JAVA_IGNORED.matcher(declaration.group(1)).replaceAll("").replace('.', '/') + "/" : "";
        String entry = prefix + name.substring(0, name.length() - JAVA_SUFFIX.length()) + CLASS_SUFFIX;
        if (!CLASS_ENTRY.matcher(entry).matches()) {
            throw new IllegalArgumentException("Java 文件名或包名无法匹配类");
        }
        return new ClassIdentity(entry, 0);
    }

    /** 检索主 class 与单层嵌套 JAR，只读取请求涉及的 class 字节。 */
    private Map<String, List<Target>> findTargets(Path extracted, Path classRoot, List<Input> inputs,
                                                   Runnable checkCancelled) throws IOException {
        Map<String, List<Target>> matches = new HashMap<>();
        Set<String> entries = new HashSet<>();
        Set<String> sources = new HashSet<>();
        for (Input input : inputs) {
            entries.add(input.identity().entry());
            if (input.source()) {
                sources.add(input.identity().entry());
            }
        }
        if (entries.isEmpty()) {
            return matches;
        }
        Path realExtracted = extracted.toRealPath();
        for (String entry : entries) {
            checkCancelled.run();
            Path target = classRoot.resolve(entry);
            String location = entryPath(extracted, target);
            boolean versioned = hasVersionedClass(extracted.resolve(MULTI_RELEASE_PREFIX), entry, sources.contains(entry));
            if (!classRoot.equals(extracted)) {
                versioned |= hasVersionedClass(classRoot.resolve(MULTI_RELEASE_PREFIX), entry, sources.contains(entry));
            }
            if (versioned) {
                addTarget(matches, entry, new Target(MAIN, location, 0, "", MESSAGE_MULTI_RELEASE));
            } else if (Files.isRegularFile(target)) {
                Path actual = target.toRealPath();
                if (!actual.startsWith(realExtracted) || !entryPath(realExtracted, actual).equals(location)) {
                    addTarget(matches, entry, new Target(MAIN, location, 0, "", "工作区实际路径与目标 class 不一致"));
                } else {
                    addTarget(matches, entry, inspectTarget(MAIN, location, readBounded(target)));
                }
            }
        }
        List<Path> jars;
        try (var paths = Files.walk(extracted)) {
            jars = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JAR_SUFFIX))
                    .sorted().toList();
        }
        for (Path jar : jars) {
            checkCancelled.run();
            scanNestedJar(extracted, jar, entries, sources, matches, checkCancelled);
        }
        return matches;
    }

    /** 检查嵌套 JAR 的重名、版本和签名，收集请求涉及的候选条目。 */
    private void scanNestedJar(Path extracted, Path jar, Set<String> entries, Set<String> sources,
                               Map<String, List<Target>> matches, Runnable checkCancelled) throws IOException {
        String jarPath = entryPath(extracted, jar);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Set<String> versioned = new HashSet<>();
            Set<String> found = new HashSet<>();
            Set<String> duplicates = new HashSet<>();
            boolean signed = false;
            var jarEntries = zip.entries();
            while (jarEntries.hasMoreElements()) {
                var jarEntry = jarEntries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String name = jarEntry.getName();
                if (entries.contains(name) && !found.add(name)) {
                    duplicates.add(name);
                }
                if (name.startsWith(MULTI_RELEASE_PREFIX)) {
                    int separator = name.indexOf('/', MULTI_RELEASE_PREFIX.length());
                    if (separator >= 0) {
                        recordVersionedClass(versioned, name.substring(separator + 1), entries, sources);
                    }
                }
                signed |= isSignatureEntry(name);
            }
            found.addAll(versioned);
            for (String entry : found) {
                checkCancelled.run();
                String location = jarPath + JarPatchConstants.ARCHIVE_ENTRY_SEPARATOR + entry;
                String problem = versioned.contains(entry) ? MESSAGE_MULTI_RELEASE
                        : duplicates.contains(entry) ? "嵌套 JAR 内有重名条目"
                        : signed ? MESSAGE_SIGNED_NESTED
                        : jarPath.contains(JarPatchConstants.ARCHIVE_ENTRY_SEPARATOR) ? "嵌套 JAR 路径包含包内定位分隔符" : null;
                Target target = problem == null
                        ? inspectTarget(jarPath, location, readBounded(zip.getInputStream(zip.getEntry(entry))))
                        : new Target(jarPath, location, 0, "", problem);
                addTarget(matches, entry, target);
            }
        }
    }

    /** 保留候选的原始版本与内容摘要，供应用时核对是否发生变化。 */
    private static Target inspectTarget(String group, String location, byte[] bytes) {
        try {
            return new Target(group, location, originalMajor(bytes), digest(bytes), null);
        } catch (IllegalArgumentException exception) {
            return new Target(group, location, 0, "", exception.getMessage());
        }
    }

    /** 检查版本覆盖条目，避免写入运行时不会使用的基础 class。 */
    private static boolean hasVersionedClass(Path versions, String entry, boolean includeCompanions)
            throws IOException {
        if (!Files.isDirectory(versions)) {
            return false;
        }
        try (var directories = Files.list(versions)) {
            for (Path directory : directories.toList()) {
                Path classFile = directory.resolve(entry);
                if (Files.isRegularFile(classFile)) {
                    return true;
                }
                if (includeCompanions && Files.isDirectory(classFile.getParent())) {
                    String family = stem(classFile.getFileName().toString()) + "$";
                    try (var classes = Files.list(classFile.getParent())) {
                        if (classes.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                                .anyMatch(name -> name.startsWith(family) && name.endsWith(CLASS_SUFFIX))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** 源码可能生成内部类，任一版本覆盖条目都要阻止该源码的替换。 */
    private static void recordVersionedClass(Set<String> versioned, String entry, Set<String> entries,
                                             Set<String> sources) {
        if (entries.contains(entry)) {
            versioned.add(entry);
        }
        if (!entry.endsWith(CLASS_SUFFIX)) {
            return;
        }
        for (int index = entry.indexOf('$', entry.lastIndexOf('/') + 1); index >= 0;
             index = entry.indexOf('$', index + 1)) {
            String owner = entry.substring(0, index) + CLASS_SUFFIX;
            if (sources.contains(owner)) {
                versioned.add(owner);
            }
        }
    }

    /** 识别嵌套 JAR 根 META-INF 下的标准签名条目。 */
    private static boolean isSignatureEntry(String entry) {
        if (!entry.startsWith(META_INF_PREFIX) || entry.indexOf('/', META_INF_PREFIX.length()) >= 0) {
            return false;
        }
        String name = entry.substring(META_INF_PREFIX.length()).toUpperCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return name.startsWith("SIG-") || dot >= 0
                && JarPatchConstants.SIGNATURE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** 记录候选，即使存在多处同名类也保留各自的实际位置。 */
    private static void addTarget(Map<String, List<Target>> matches, String entry, Target target) {
        matches.computeIfAbsent(entry, ignored -> new ArrayList<>()).add(target);
    }

    /** 防止重复输入和源码生成的内部 class 与直接导入的 class 重叠。 */
    private static Set<String> overlappingInputs(List<Input> inputs) {
        Set<String> conflicts = new HashSet<>();
        Set<String> seen = new HashSet<>();
        Set<String> sources = new HashSet<>();
        for (Input input : inputs) {
            String key = input.identity().entry().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                conflicts.add(key);
            }
            if (input.source()) {
                sources.add(key);
            }
        }
        for (String key : seen) {
            for (int index = key.indexOf('$', key.lastIndexOf('/') + 1); index >= 0;
                 index = key.indexOf('$', index + 1)) {
                String owner = key.substring(0, index) + CLASS_SUFFIX;
                if (sources.contains(owner)) {
                    conflicts.add(owner);
                    conflicts.add(key);
                }
            }
        }
        return conflicts;
    }

    /** 检查唯一目标、手工源码编辑冲突以及 class 版本。 */
    private static String matchProblem(Input input, List<Target> candidates, Set<String> edited) {
        if (candidates.isEmpty()) {
            return "当前包中找不到对应类";
        }
        if (candidates.size() != 1) {
            return "同名类有多个目标，请先解决包内重名";
        }
        Target target = candidates.get(0);
        if (target.problem() != null) {
            return target.problem();
        }
        String stem = stem(input.identity().entry());
        String sourcePrefix = MAIN.equals(target.group()) ? JarPatchConstants.TREE_SOURCE_PREFIX
                : JarPatchConstants.TREE_SOURCE_PREFIX + JarPatchConstants.SOURCE_NESTED_JAR_DIR
                + "/" + target.group() + "/";
        if (edited.contains(sourcePrefix + stem + JAVA_SUFFIX)) {
            return MESSAGE_EXISTING_EDIT;
        }
        for (int index = stem.indexOf('$', stem.lastIndexOf('/') + 1); index >= 0; index = stem.indexOf('$', index + 1)) {
            if (edited.contains(sourcePrefix + stem.substring(0, index) + JAVA_SUFFIX)) {
                return MESSAGE_EXISTING_EDIT;
            }
        }
        if (!input.source() && target.major() != input.identity().major()) {
            return "字节码版本不一致：原包 Java " + javaRelease(target.major())
                    + "，补丁 Java " + javaRelease(input.identity().major());
        }
        return null;
    }

    /** 读取 class 头，不加载用户包里的类。 */
    private static int originalMajor(byte[] bytes) {
        if (bytes.length < 8 || ByteBuffer.wrap(bytes).getInt() != CLASS_MAGIC) {
            throw new IllegalArgumentException(MESSAGE_INVALID_CLASS);
        }
        ByteBuffer header = ByteBuffer.wrap(bytes);
        if ((header.getShort(4) & 0xffff) == PREVIEW_MINOR_VERSION) {
            throw new IllegalArgumentException(MESSAGE_PREVIEW_CLASS);
        }
        return header.getShort(6) & 0xffff;
    }

    /** 取得去掉 class 后缀的斜杠类名。 */
    private static String stem(String entry) {
        return entry.substring(0, entry.length() - CLASS_SUFFIX.length());
    }

    /** 仅读取实际文件，符号链接不会作为补丁输入。 */
    private static byte[] readBounded(Path file) throws IOException {
        return readBounded(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS));
    }

    /** 限制单项读取大小并关闭输入流。 */
    private static byte[] readBounded(InputStream stream) throws IOException {
        try (stream) {
            byte[] bytes = stream.readNBytes(MAX_FILE_BYTES + 1);
            if (bytes.length > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("单个补丁文件或目标 class 最大支持 8 MB");
            }
            return bytes;
        }
    }

    /** 创建用于预览确认的 SHA-256 摘要。 */
    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_SHA256_UNAVAILABLE, exception);
        }
    }

    /** 计算单个输入或目标的内容摘要。 */
    private static String digest(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    /** 按字段分隔写入摘要，避免路径拼接歧义。 */
    private static void update(MessageDigest digest, String field) {
        digest.update(field.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
    }

    /** 已验证的 class 条目及主版本。 */
    record ClassIdentity(String entry, int major) {
    }

    /** 外部补丁文件和预览时的内容摘要。 */
    record Input(Path path, String relative, boolean source, ClassIdentity identity, String hash) {
    }

    /** 写入目标及预览时的包内版本与内容。 */
    record Target(String group, String location, int major, String hash, String problem) {
    }

    /** 界面预览及成功匹配的应用计划。 */
    record Plan(PatchDirectoryPreview preview, Map<Input, Target> matches) {
    }
}
