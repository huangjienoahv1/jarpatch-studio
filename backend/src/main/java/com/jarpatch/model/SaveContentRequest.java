package com.jarpatch.model;

/**
 * 保存文件内容请求。
 * <p>
 * 前端编辑 Java 源码或文本资源后，通过该模型把相对路径和新内容传给后端保存接口，
 * 保存成功后后端会写入修改记录。
 * </p>
 *
 * @author 黄杰
 */
public class SaveContentRequest {

    private String path;
    private String content;
    private String expectedHash;
    private String encoding;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取打开文件时的原始字节哈希。
     *
     * @return SHA-256 哈希
     */
    public String getExpectedHash() {
        return expectedHash;
    }

    /**
     * 设置打开文件时的原始字节哈希。
     *
     * @param expectedHash SHA-256 哈希
     */
    public void setExpectedHash(String expectedHash) {
        this.expectedHash = expectedHash;
    }

    /**
     * 获取打开文件时由用户明确确认的编码。
     *
     * @return 标准编码名
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * 设置打开文件时由用户明确确认的编码。
     *
     * @param encoding 标准编码名
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
