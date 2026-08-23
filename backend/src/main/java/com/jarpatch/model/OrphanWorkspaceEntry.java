package com.jarpatch.model;

/**
 * 未登记工作区清理候选项。
 * <p>
 * 候选项仅由显式预览接口产生，记录路径、文件数、大小和最后修改时间，不能触发自动删除。
 * </p>
 *
 * @author 黄杰
 */
public class OrphanWorkspaceEntry {

    private String workspacePath;
    private long fileCount;
    private long totalBytes;
    private String lastModifiedAt;

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public long getFileCount() {
        return fileCount;
    }

    public void setFileCount(long fileCount) {
        this.fileCount = fileCount;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public String getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(String lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
