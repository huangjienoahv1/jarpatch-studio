package com.jarpatch.model;

/**
 * 文件搜索结果。
 * <p>
 * 搜索接口返回该模型给 Electron 前端，用户点击结果后会用 path 字段打开对应文件，
 * lineNumber 和 preview 用于快速定位命中的内容。
 * </p>
 *
 * @author 黄杰
 */
public class SearchResult {

    private String path;
    private int lineNumber;
    private String preview;

    /**
     * 创建空搜索结果，供 JSON 序列化框架使用。
     */
    public SearchResult() {
    }

    /**
     * 创建完整搜索结果。
     *
     * @param path       文件树路径
     * @param lineNumber 命中行号
     * @param preview    命中行预览
     */
    public SearchResult(String path, int lineNumber, String preview) {
        this.path = path;
        this.lineNumber = lineNumber;
        this.preview = preview;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
