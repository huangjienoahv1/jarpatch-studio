package com.jarpatch.model;

/**
 * 可编辑文本文件内容视图。
 * <p>
 * 文件读取接口返回原始文本、原始字节哈希、编码和换行格式；保存接口使用哈希做并发校验，
 * 并按原编码与换行格式写回，确保未修改保存不会改变磁盘字节。
 * </p>
 *
 * @author 黄杰
 */
public class FileContentView {

    private String content;
    private String contentHash;
    private String encoding;
    private String lineEnding;
    private boolean changed;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getLineEnding() {
        return lineEnding;
    }

    public void setLineEnding(String lineEnding) {
        this.lineEnding = lineEnding;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
