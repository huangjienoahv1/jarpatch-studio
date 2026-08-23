package com.jarpatch.model;

/**
 * 脱敏后的诊断任务日志。
 * <p>
 * 诊断服务把 SQLite 任务 ID 作为统一操作 ID，并仅输出受长度限制和脱敏处理后的
 * 单行消息；系统诊断接口和桌面端导出功能共同使用该模型。
 * </p>
 *
 * @author 黄杰
 */
public class DiagnosticTaskLog {

    private String operationId;
    private int progress;
    private String status;
    private String message;
    private String createdAt;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
