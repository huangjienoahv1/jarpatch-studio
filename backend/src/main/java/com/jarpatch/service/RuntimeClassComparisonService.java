package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.ArchiveLimitsProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 复用 CFR 还原运行时 class 快照，供部署包对照和在线源码编辑使用。
 *
 * @author 黄杰
 */
@Service
public class RuntimeClassComparisonService {

    static final int MAX_CLASS_BYTES = 1024 * 1024;
    static final int MAX_SOURCE_BYTES = 2 * 1024 * 1024;
    private static final long MAX_NESTED_BYTES = 128L * 1024 * 1024;
    private static final long MAX_TOTAL_NESTED_BYTES = 512L * 1024 * 1024;
    private static final int MAX_MATCHES = 24;
    private static final int CLASS_MAGIC = 0xcafebabe;
    private static final String JAR_SUFFIX = ".jar";
    private static final String WAR_SUFFIX = ".war";
    private static final String VERSIONED_ROOT = "META-INF/versions/";
    private static final List<String> CLASS_ROOTS = List.of("", JarPatchConstants.SPRING_BOOT_CLASSES_DIR + "/",
            JarPatchConstants.WAR_CLASSES_DIR + "/");
    private static final List<String> LIB_ROOTS = List.of(JarPatchConstants.SPRING_BOOT_LIB_DIR + "/",
            JarPatchConstants.WAR_LIB_DIR + "/", "WEB-INF/lib-provided/");
    private static final Pattern JAVA_CLASS = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*");
    private static final Pattern VERSION_DIRECTORY = Pattern.compile("[1-9][0-9]*");

    private final CfrBatchDecompiler decompiler;
    private final ArchiveLimitsProperties archiveLimits;

    /**
     * 创建与普通项目反编译共用 CFR 的对照服务。
     *
     * @param decompiler CFR 批量反编译执行器
     * @param archiveLimits 项目通用归档读取限制
     */
    public RuntimeClassComparisonService(CfrBatchDecompiler decompiler, ArchiveLimitsProperties archiveLimits) {
        this.decompiler = decompiler;
        this.archiveLimits = archiveLimits;
    }

    /**
     * 读取本地部署包并计算 class 摘要。单个匹配直接对照，多候选由用户选择。
     *
     * @param archivePath 桌面端选择的 Jar 或 War
     * @param className JVM 类全名
     * @param encoded 运行时 class 的 Base64 内容
     * @param entryIndex 用户选择的候选序号，可省略
     * @param expectedSha256 候选列表呈现时的摘要，用于识别包被替换
     * @return 候选摘要及所选 class 的两侧反编译结果
     * @throws IOException 归档或临时文件读取失败
     */
    public Comparison compare(String archivePath, String className, String encoded, Integer entryIndex,
                              String expectedSha256) throws IOException {
        Path archive = requireArchive(archivePath);
        String name = classEntry(className);
        byte[] runtime = decodeSnapshot(encoded);
        Path working = Files.createTempDirectory("jarpatch-runtime-compare-");
        try {
            List<Candidate> candidates = findCandidates(archive, name, working);
            String runtimeHash = digest(runtime);
            List<Entry> entries = candidates.stream().map(Candidate::entry).toList();
            Integer index = entryIndex;
            if (index == null && candidates.size() == 1) index = 0;
            if (index == null) return new Comparison(archive.getFileName().toString(), runtimeHash, entries, null, null, null, null);
            if (index < 0 || index >= candidates.size()) throw new IllegalArgumentException("请选择包内 class 候选");
            Candidate candidate = candidates.get(index);
            if (entryIndex != null && !candidate.entry().sha256().equals(expectedSha256)) {
                throw new IllegalArgumentException("部署包候选已变化，请重新对照本地包");
            }
            boolean identical = runtimeHash.equals(candidate.entry().sha256());
            String runtimeSource = source(working.resolve("runtime"), name, runtime);
            String localSource = identical ? runtimeSource : source(working.resolve("local"), name, candidate.bytes());
            return new Comparison(archive.getFileName().toString(), runtimeHash, entries, index,
                    identical, runtimeSource, localSource);
        } finally {
            FileSystemUtils.deleteRecursively(working);
        }
    }

    /**
     * 校验本地归档可按 Jar 或 War 读取。
     */
    Path requireArchive(String file) throws IOException {
        if (file == null || file.isBlank()) throw new IllegalArgumentException("请选择本地 Jar 或 War");
        Path path = Path.of(file).toAbsolutePath().normalize();
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!Files.isRegularFile(path) || !(name.endsWith(JAR_SUFFIX) || name.endsWith(WAR_SUFFIX))) {
            throw new IllegalArgumentException("请选择可读取的本地 Jar 或 War");
        }
        if (Files.size(path) > archiveLimits.getMaxArchiveBytes()) throw new IllegalArgumentException("本地部署包超过归档大小限制");
        return path;
    }

    /**
     * 从在线编辑绑定的 class 快照还原源码，完成后清理临时目录。
     *
     * @param className 运行时类全名
     * @param encoded 对应 class 快照的 Base64 内容
     * @return CFR 还原的 Java 源码
     * @throws IOException 快照或源码的临时文件读写失败
     */
    public String decompileSnapshot(String className, String encoded) throws IOException {
        String name = classEntry(className);
        byte[] bytes = decodeSnapshot(encoded);
        Path working = Files.createTempDirectory("jarpatch-runtime-source-");
        try {
            String content = source(working, name, bytes);
            if (content == null) throw new IllegalStateException("该 class 未生成可编辑的反编译源码");
            return content;
        } finally {
            FileSystemUtils.deleteRecursively(working);
        }
    }

    /**
     * 验证类名并返回 class 的包内相对路径。
     */
    static String classEntry(String className) {
        if (className == null || className.length() > 240 || !JAVA_CLASS.matcher(className).matches()) {
            throw new IllegalArgumentException("Java 类全名无效");
        }
        return className.replace('.', '/') + ".class";
    }

    /**
     * 解码并限制运行时 class 快照的大小，验证文件头后返回字节数组。
     */
    static byte[] decodeSnapshot(String encoded) {
        if (encoded == null || encoded.length() > ((MAX_CLASS_BYTES + 2) / 3) * 4) {
            throw new IllegalArgumentException("运行中 class 大小超过限制");
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        requireClassBytes(bytes);
        return bytes;
    }

    /**
     * 搜索主包及标准依赖目录，限制临时读取的嵌套依赖总量。
     */
    private List<Candidate> findCandidates(Path archive, String name, Path working) throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            addMatches(zip, CLASS_ROOTS, "", name, candidates);
            var items = zip.entries();
            long remaining = MAX_TOTAL_NESTED_BYTES;
            while (items.hasMoreElements()) {
                ZipEntry entry = items.nextElement();
                String location = entry.getName();
                if (entry.isDirectory() || !location.endsWith(JAR_SUFFIX) || LIB_ROOTS.stream().noneMatch(location::startsWith)) continue;
                Path nested = Files.createTempFile(working, "dependency-", JAR_SUFFIX);
                try {
                    remaining -= copyNested(zip, entry, nested, Math.min(remaining, MAX_NESTED_BYTES));
                    try (ZipFile dependency = new ZipFile(nested.toFile())) {
                        addMatches(dependency, List.of(""), location + "!/", name, candidates);
                    }
                } finally {
                    Files.deleteIfExists(nested);
                }
            }
        }
        return candidates;
    }

    /**
     * 将一个嵌套 Jar 写入临时路径，校验实际展开大小。
     */
    private long copyNested(ZipFile zip, ZipEntry entry, Path nested, long limit) throws IOException {
        String tooLarge = "嵌套依赖超过对照读取限制，可直接选择该依赖 Jar";
        if (entry.getSize() > limit) throw new IllegalArgumentException(tooLarge);
        try (InputStream input = zip.getInputStream(entry); OutputStream output = Files.newOutputStream(nested)) {
            byte[] buffer = new byte[8192];
            long copied = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                copied += count;
                if (copied > limit) throw new IllegalArgumentException(tooLarge);
                output.write(buffer, 0, count);
            }
            return copied;
        }
    }

    /**
     * 提取精确匹配的普通 class 或其多版本副本，保留来源路径。
     */
    private void addMatches(ZipFile zip, List<String> roots, String prefix, String name, List<Candidate> candidates)
            throws IOException {
        if (zip.size() > archiveLimits.getMaxEntryCount()) throw new IllegalArgumentException("部署包超过归档条目数量限制");
        var items = zip.entries();
        while (items.hasMoreElements()) {
            ZipEntry next = items.nextElement();
            if (next.isDirectory() || roots.stream().noneMatch(root -> matches(next.getName(), root, name))) continue;
            if (candidates.size() >= MAX_MATCHES) throw new IllegalArgumentException("同名 class 候选过多，请选择具体依赖 Jar");
            try (InputStream input = zip.getInputStream(next)) {
                byte[] bytes = input.readNBytes(MAX_CLASS_BYTES + 1);
                requireClassBytes(bytes);
                candidates.add(new Candidate(new Entry(candidates.size(), prefix + next.getName(), digest(bytes), bytes.length), bytes));
            }
        }
    }

    /**
     * 判断归档条目是否属于指定 class 或其 Multi-Release 版本。
     */
    private boolean matches(String entry, String root, String name) {
        if (entry.equals(root + name)) return true;
        String versions = root + VERSIONED_ROOT;
        if (!entry.startsWith(versions)) return false;
        int separator = entry.indexOf('/', versions.length());
        return separator > versions.length() && VERSION_DIRECTORY.matcher(entry.substring(versions.length(), separator)).matches()
                && entry.substring(separator + 1).equals(name);
    }

    /**
     * 确认字节码头部和长度有效。
     */
    private static void requireClassBytes(byte[] bytes) {
        if (bytes.length < 8 || bytes.length > MAX_CLASS_BYTES || ByteBuffer.wrap(bytes).getInt() != CLASS_MAGIC) {
            throw new IllegalArgumentException("class 格式无效或超过 1 MiB");
        }
    }

    /**
     * 计算用于精确对照的 SHA-256 摘要。
     */
    private String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 缺少 SHA-256", exception);
        }
    }

    /**
     * 以同一 CFR 配置生成源码。无源码产出时保留二进制对照结果。
     */
    private String source(Path working, String name, byte[] bytes) throws IOException {
        Path input = working.resolve("classes").resolve(name);
        Files.createDirectories(input.getParent());
        Files.write(input, bytes);
        Path output = working.resolve("source");
        decompiler.decompile(List.of(input), output, null);
        try (var paths = Files.walk(output)) {
            Path file = paths.filter(Files::isRegularFile).findFirst().orElse(null);
            return file == null || Files.size(file) > MAX_SOURCE_BYTES ? null : Files.readString(file);
        }
    }

    /** 包内候选和该候选的实际字节摘要。 */
    public record Entry(int index, String location, String sha256, int size) { }

    /** 候选条目、选中项的二进制结果和两侧可用源码。 */
    public record Comparison(String archive, String runtimeSha256, List<Entry> entries, Integer selected,
                             Boolean identical, String runtimeSource, String localSource) { }

    /** 临时保存有界的候选 class 内容。 */
    private record Candidate(Entry entry, byte[] bytes) { }
}
