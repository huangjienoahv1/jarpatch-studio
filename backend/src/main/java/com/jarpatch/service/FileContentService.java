package com.jarpatch.service;

import com.jarpatch.common.FileKind;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.FileContentView;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.repository.FileChangeRepository;
import com.jarpatch.repository.ProjectRepository;
import com.jarpatch.repository.CompiledArtifactRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 保真文本文件读写服务。
 * <p>
 * 文件读取入口返回原始文本及字节哈希，不再解码 Java Unicode 转义；保存入口先校验哈希，
 * 再保留原编码、BOM 和换行格式生成新字节。新旧字节相同时不写文件、不记录 file_changes；
 * 发生真实修改时通过同目录临时文件原子替换，并更新 SQLite 修改记录。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class FileContentService {

    private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF_16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF_16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String LINE_ENDING_CRLF = "CRLF";
    private static final String LINE_ENDING_LF = "LF";
    private static final String LINE_ENDING_CR = "CR";
    private static final String LINE_ENDING_MIXED = "MIXED";
    private static final String LINE_ENDING_NONE = "NONE";

    private final WorkspaceService workspaceService;
    private final FileKindService fileKindService;
    private final FileChangeRepository fileChangeRepository;
    private final ProjectRepository projectRepository;
    private final ClockService clockService;
    private final CompiledArtifactRepository compiledArtifactRepository;
    private final ProjectSettingsService projectSettingsService;

    /**
     * 创建保真文件内容服务。
     *
     * @param workspaceService     工作区服务
     * @param fileKindService      文件类型服务
     * @param fileChangeRepository 修改记录仓储
     * @param projectRepository    项目仓储
     * @param clockService         时间服务
     * @param compiledArtifactRepository 编译产物仓储
     * @param projectSettingsService 项目设置服务
     */
    public FileContentService(WorkspaceService workspaceService,
                              FileKindService fileKindService,
                              FileChangeRepository fileChangeRepository,
                              ProjectRepository projectRepository,
                              ClockService clockService,
                              CompiledArtifactRepository compiledArtifactRepository,
                              ProjectSettingsService projectSettingsService) {
        this.workspaceService = workspaceService;
        this.fileKindService = fileKindService;
        this.fileChangeRepository = fileChangeRepository;
        this.projectRepository = projectRepository;
        this.clockService = clockService;
        this.compiledArtifactRepository = compiledArtifactRepository;
        this.projectSettingsService = projectSettingsService;
    }

    /**
     * 按原始编码读取可编辑文件，不转换 Unicode 转义或换行符。
     *
     * @param project      项目记录
     * @param relativePath 文件树相对路径
     * @return 内容、哈希、编码和换行格式
     * @throws IOException 读取失败时抛出
     */
    public FileContentView read(ProjectRecord project, String relativePath) throws IOException {
        Path path = resolveEditablePath(project, relativePath);
        ensureWithinEditableLimit(project, Files.size(path));
        return toView(readSnapshot(path), false);
    }

    /**
     * 读取服务内部已确认安全的基线或工作区文本路径。
     *
     * @param path 文本文件路径
     * @return 内容视图
     * @throws IOException 读取失败时抛出
     */
    public FileContentView readPath(Path path) throws IOException {
        return toView(readSnapshot(path), false);
    }

    /**
     * 校验原始哈希并按原编码和换行格式原子保存真实修改。
     *
     * @param project      项目记录
     * @param relativePath 文件树相对路径
     * @param content      编辑器内容
     * @param expectedHash 打开文件时的原始哈希
     * @return 保存后的内容视图
     * @throws IOException 写入失败时抛出
     */
    public FileContentView save(ProjectRecord project,
                                String relativePath,
                                String content,
                                String expectedHash) throws IOException {
        Path path = resolveEditablePath(project, relativePath);
        ensureWithinEditableLimit(project, Files.size(path));
        TextSnapshot original = readSnapshot(path);
        if (expectedHash == null || !expectedHash.equals(original.hash)) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_FILE_CHANGED_EXTERNALLY);
        }
        byte[] updatedBytes = encode(content == null ? JarPatchConstants.EMPTY_TEXT : content, original);
        ensureWithinEditableLimit(project, updatedBytes.length);
        if (Arrays.equals(original.bytes, updatedBytes)) {
            return toView(original, false);
        }

        atomicWrite(path, updatedBytes);
        FileKind kind = fileKindService.detect(path);
        if (kind == FileKind.JAVA) {
            compiledArtifactRepository.clear(project.getId());
        }
        String now = clockService.now();
        Path baselinePath = workspaceService.resolveBaseline(project, relativePath);
        if (Files.isRegularFile(baselinePath)) {
            TextSnapshot baseline = readSnapshot(baselinePath);
            TextSnapshot current = readSnapshot(path);
            if (baseline.hash.equals(current.hash)) {
                fileChangeRepository.delete(project.getId(), relativePath);
            } else {
                fileChangeRepository.upsert(project.getId(), relativePath, kind.getCode(),
                        baseline.hash, current.hash, now);
            }
        } else {
            TextSnapshot current = readSnapshot(path);
            fileChangeRepository.upsert(project.getId(), relativePath, kind.getCode(),
                    null, current.hash, now);
        }
        projectRepository.touch(project.getId(), now);
        return toView(readSnapshot(path), true);
    }

    /**
     * 校验文件大小不超过当前项目设置的单文件编辑上限。
     *
     * @param project 项目记录
     * @param size    文件字节数
     */
    private void ensureWithinEditableLimit(ProjectRecord project, long size) {
        if (size > projectSettingsService.maxEditableFileBytes(project.getId())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_TOO_LARGE_TO_EDIT);
        }
    }

    /**
     * 读取文件字节并严格识别 UTF 编码、BOM 和换行格式。
     *
     * @param path 文件路径
     * @return 原始文本快照
     * @throws IOException 读取失败时抛出
     */
    private TextSnapshot readSnapshot(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        Charset charset = StandardCharsets.UTF_8;
        byte[] bom = new byte[0];
        int offset = 0;
        if (startsWith(bytes, UTF_8_BOM)) {
            bom = UTF_8_BOM;
            offset = UTF_8_BOM.length;
        } else if (startsWith(bytes, UTF_16_LE_BOM)) {
            charset = StandardCharsets.UTF_16LE;
            bom = UTF_16_LE_BOM;
            offset = UTF_16_LE_BOM.length;
        } else if (startsWith(bytes, UTF_16_BE_BOM)) {
            charset = StandardCharsets.UTF_16BE;
            bom = UTF_16_BE_BOM;
            offset = UTF_16_BE_BOM.length;
        }
        String content = decodeStrict(bytes, offset, charset);
        return new TextSnapshot(bytes, content, sha256(bytes), charset, bom, detectLineEnding(content));
    }

    /**
     * 使用严格解码器读取文本，非法字节序列直接阻止编辑。
     *
     * @param bytes   原始字节
     * @param offset  BOM 后起始位置
     * @param charset 字符集
     * @return 解码文本
     */
    private String decodeStrict(byte[] bytes, int offset, Charset charset) {
        try {
            CharBuffer decoded = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_ENCODING_UNSUPPORTED, exception);
        }
    }

    /**
     * 按原文件编码、BOM 和单一换行格式生成待写字节。
     *
     * @param content  新内容
     * @param original 原始快照
     * @return 保真编码后的字节
     */
    private byte[] encode(String content, TextSnapshot original) {
        if (LINE_ENDING_MIXED.equals(original.lineEnding) && !content.equals(original.content)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_MIXED_LINE_ENDINGS);
        }
        String normalizedContent = normalizeLineEndings(content, original.lineEnding);
        byte[] body = normalizedContent.getBytes(original.charset);
        byte[] result = new byte[original.bom.length + body.length];
        System.arraycopy(original.bom, 0, result, 0, original.bom.length);
        System.arraycopy(body, 0, result, original.bom.length, body.length);
        return result;
    }

    /**
     * 把编辑器换行统一还原为原文件单一换行格式。
     *
     * @param content    编辑器内容
     * @param lineEnding 原文件换行格式
     * @return 规范化内容
     */
    private String normalizeLineEndings(String content, String lineEnding) {
        if (LINE_ENDING_MIXED.equals(lineEnding)) {
            return content;
        }
        String lfContent = content.replace("\r\n", "\n").replace('\r', '\n');
        if (LINE_ENDING_CRLF.equals(lineEnding)) {
            return lfContent.replace("\n", "\r\n");
        }
        if (LINE_ENDING_CR.equals(lineEnding)) {
            return lfContent.replace('\n', '\r');
        }
        return lfContent;
    }

    /**
     * 识别文本的单一或混合换行格式。
     *
     * @param content 文本内容
     * @return 换行格式码
     */
    private String detectLineEnding(String content) {
        boolean hasCrLf = content.contains("\r\n");
        String withoutCrLf = content.replace("\r\n", "");
        boolean hasLf = withoutCrLf.indexOf('\n') >= 0;
        boolean hasCr = withoutCrLf.indexOf('\r') >= 0;
        int styles = (hasCrLf ? 1 : 0) + (hasLf ? 1 : 0) + (hasCr ? 1 : 0);
        if (styles > 1) {
            return LINE_ENDING_MIXED;
        }
        if (hasCrLf) {
            return LINE_ENDING_CRLF;
        }
        if (hasLf) {
            return LINE_ENDING_LF;
        }
        if (hasCr) {
            return LINE_ENDING_CR;
        }
        return LINE_ENDING_NONE;
    }

    /**
     * 通过同目录临时文件原子替换文本文件。
     *
     * @param target 最终文件
     * @param bytes  完整新字节
     * @throws IOException 写入或原子移动失败时抛出
     */
    private void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path temporaryFile = Files.createTempFile(target.getParent(), "." + target.getFileName() + ".", ".tmp");
        try {
            Files.write(temporaryFile, bytes);
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException(JarPatchConstants.MESSAGE_EXPORT_ATOMIC_MOVE_REQUIRED, exception);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * 计算原始字节 SHA-256。
     *
     * @param bytes 原始字节
     * @return 小写十六进制哈希
     */
    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance(HASH_ALGORITHM).digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_SHA256_UNAVAILABLE, exception);
        }
    }

    /**
     * 判断字节数组是否包含指定 BOM 前缀。
     *
     * @param bytes  文件字节
     * @param prefix BOM 前缀
     * @return 匹配时返回 true
     */
    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把内部快照转换为接口视图。
     *
     * @param snapshot 文件快照
     * @param changed  本次是否真实写入
     * @return 文件内容视图
     */
    private FileContentView toView(TextSnapshot snapshot, boolean changed) {
        FileContentView view = new FileContentView();
        view.setContent(snapshot.content);
        view.setContentHash(snapshot.hash);
        view.setEncoding(snapshot.charset.name());
        view.setLineEnding(snapshot.lineEnding);
        view.setChanged(changed);
        return view;
    }

    /**
     * 解析并校验可编辑文件路径。
     *
     * @param project      项目记录
     * @param relativePath 文件树相对路径
     * @return 可编辑文件绝对路径
     */
    private Path resolveEditablePath(ProjectRecord project, String relativePath) {
        Path path = resolveWorkspacePath(project, relativePath);
        if (!fileKindService.detect(path).isEditable()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_NOT_EDITABLE);
        }
        return path;
    }

    /**
     * 根据文件树前缀解析工作区真实路径。
     *
     * @param project      项目记录
     * @param relativePath 文件树路径
     * @return 工作区文件路径
     */
    private Path resolveWorkspacePath(ProjectRecord project, String relativePath) {
        if (relativePath != null && relativePath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            return workspaceService.resolveSource(project,
                    relativePath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length()));
        }
        if (relativePath != null && relativePath.startsWith(JarPatchConstants.TREE_EXTRACTED_PREFIX)) {
            return workspaceService.resolveExtracted(project,
                    relativePath.substring(JarPatchConstants.TREE_EXTRACTED_PREFIX.length()));
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_FILE_OUT_OF_WORKSPACE);
    }

    /**
     * 单次文件读取的原始字节与文本元数据快照。
     */
    private static final class TextSnapshot {

        private final byte[] bytes;
        private final String content;
        private final String hash;
        private final Charset charset;
        private final byte[] bom;
        private final String lineEnding;

        /**
         * 创建不可变文本快照。
         *
         * @param bytes      原始字节
         * @param content    解码文本
         * @param hash       SHA-256
         * @param charset    原字符集
         * @param bom        原 BOM
         * @param lineEnding 原换行格式
         */
        private TextSnapshot(byte[] bytes,
                             String content,
                             String hash,
                             Charset charset,
                             byte[] bom,
                             String lineEnding) {
            this.bytes = bytes;
            this.content = content;
            this.hash = hash;
            this.charset = charset;
            this.bom = bom;
            this.lineEnding = lineEnding;
        }
    }
}
