package com.jarpatch.model;

/**
 * 工作区清理预览。
 * <p>
 * 清理服务先返回项目、路径、文件数、大小、最后使用时间和一次性确认标识；只有工作区内容
 * 未变化且确认标识仍有效时，后续清理请求才会删除该工作区。
 * </p>
 *
 * @author 黄杰
 */
public class WorkspaceCleanupPreview {

    private String projectId;
    private String projectName;
    private String workspacePath;
    private long fileCount;
    private long totalBytes;
    private String lastUsedAt;
    private String confirmationId;
    private String expiresAt;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

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

    public String getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(String lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
