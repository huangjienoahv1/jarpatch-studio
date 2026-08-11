package com.jarpatch.model;

/**
 * 持久化任务日志记录。
 * <p>
 * 任务服务每次状态流转时写入该模型，任务日志查询接口据此恢复应用重启前后的完整进度记录。
 * </p>
 *
 * @author 黄杰
 */
public class TaskLogRecord {

    private String id;
    private String taskId;
    private int progress;
    private String status;
    private String message;
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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
