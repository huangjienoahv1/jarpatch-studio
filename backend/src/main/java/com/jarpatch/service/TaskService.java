package com.jarpatch.service;

import com.jarpatch.common.TaskStatus;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * 后台任务服务。
 * <p>
 * 导入、分析、编译和导出入口都通过该服务创建任务、更新进度、写入 SQLite 并广播
 * WebSocket 日志。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ClockService clockService;
    private final TaskLogBroadcaster broadcaster;

    /**
     * 创建任务服务。
     *
     * @param taskRepository 任务仓储
     * @param clockService   时间服务
     * @param broadcaster    WebSocket 日志广播服务
     */
    public TaskService(TaskRepository taskRepository, ClockService clockService, TaskLogBroadcaster broadcaster) {
        this.taskRepository = taskRepository;
        this.clockService = clockService;
        this.broadcaster = broadcaster;
    }

    /**
     * 创建运行中的任务。
     *
     * @param projectId 项目 ID，可为空
     * @param taskType  任务类型
     * @param message   初始消息
     * @return 任务记录
     */
    public TaskRecord create(String projectId, String taskType, String message) {
        String now = clockService.now();
        TaskRecord record = new TaskRecord();
        record.setId(UUID.randomUUID().toString());
        record.setProjectId(projectId);
        record.setTaskType(taskType);
        record.setStatus(TaskStatus.RUNNING.getCode());
        record.setProgress(0);
        record.setMessage(message);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        taskRepository.insert(record);
        broadcaster.broadcast(record.getId(), message);
        return record;
    }

    /**
     * 更新任务为执行中状态。
     *
     * @param record   任务记录
     * @param progress 进度百分比
     * @param message  当前执行消息
     */
    public void running(TaskRecord record, int progress, String message) {
        update(record, TaskStatus.RUNNING, progress, message);
    }

    /**
     * 更新任务为成功状态。
     *
     * @param record  任务记录
     * @param message 成功消息
     */
    public void success(TaskRecord record, String message) {
        update(record, TaskStatus.SUCCESS, 100, message);
    }

    /**
     * 更新任务为失败状态。
     *
     * @param record  任务记录
     * @param message 失败原因
     */
    public void failed(TaskRecord record, String message) {
        update(record, TaskStatus.FAILED, record.getProgress(), message);
    }

    /**
     * 根据任务 ID 查询任务记录。
     *
     * @param taskId 任务 ID
     * @return 任务记录
     */
    public Optional<TaskRecord> findById(String taskId) {
        return taskRepository.findById(taskId);
    }

    /**
     * 更新任务状态并推送日志。
     *
     * @param record   任务记录
     * @param status   任务状态
     * @param progress 进度百分比
     * @param message  当前消息
     */
    private void update(TaskRecord record, TaskStatus status, int progress, String message) {
        record.setStatus(status.getCode());
        record.setProgress(progress);
        record.setMessage(message);
        record.setUpdatedAt(clockService.now());
        taskRepository.update(record);
        broadcaster.broadcast(record.getId(), "[" + progress + "%] " + message);
    }
}
