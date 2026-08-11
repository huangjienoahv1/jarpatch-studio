package com.jarpatch.model;

/**
 * 单个可编辑文件的可靠基线差异。
 * <p>
 * 差异服务直接比较 baseline 与当前工作区的字节哈希，并同时返回两侧原始文本用于桌面端
 * 差异确认，不依赖历史修改标记推测内容。
 * </p>
 *
 * @author 黄杰
 */
public class FileDiff {

    private String path;
    private String category;
    private String status;
    private String originalHash;
    private String currentHash;
    private String originalContent;
    private String currentContent;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalHash() {
        return originalHash;
    }

    public void setOriginalHash(String originalHash) {
        this.originalHash = originalHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String currentContent) {
        this.currentContent = currentContent;
    }
}
