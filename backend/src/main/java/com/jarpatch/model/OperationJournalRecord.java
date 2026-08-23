package com.jarpatch.model;

/**
 * 项目业务操作时间线记录。
 * <p>
 * 导入、分析、编译和导出任务在实际状态流转时写入该模型，operationId 与任务 ID 一致，
 * 便于从项目历史追踪到任务日志和后端诊断日志。
 * </p>
 *
 * @author 黄杰
 */
public class OperationJournalRecord {

    private String id;
    private String projectId;
    private String operationId;
    private String operationType;
    private String targetPath;
    private String status;
    private String details;
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
