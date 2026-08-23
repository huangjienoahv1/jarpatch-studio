package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.TaskStatus;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.model.TaskLogRecord;
import com.jarpatch.repository.TaskLogRepository;
import com.jarpatch.repository.OperationJournalRepository;
import com.jarpatch.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final ClockService clockService;
    private final TaskLogBroadcaster broadcaster;
    private final OperationJournalRepository operationJournalRepository;

    /**
     * 创建任务服务。
     *
     * @param taskRepository 任务仓储
     * @param taskLogRepository 任务日志仓储
     * @param clockService      时间服务
     * @param broadcaster       WebSocket 日志广播服务
     * @param operationJournalRepository 操作时间线仓储
     */
    public TaskService(TaskRepository taskRepository,
                       TaskLogRepository taskLogRepository,
                       ClockService clockService,
                       TaskLogBroadcaster broadcaster,
                       OperationJournalRepository operationJournalRepository) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.clockService = clockService;
        this.broadcaster = broadcaster;
        this.operationJournalRepository = operationJournalRepository;
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
        record.setProgress(JarPatchConstants.EMPTY_SIZE);
        record.setMessage(message);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        taskRepository.insert(record);
        appendLog(record, message);
        appendOperation(record, message);
        broadcaster.broadcast(record.getId(), message);
        return record;
    }

    /**
     * 复用前端预先创建的任务，或者在未传入 taskId 时现场创建任务。
     * <p>
     * 入口在控制器，实际执行点仍然是任务仓储和广播服务；这让前端可以先拿到 taskId，
     * 再连接 WebSocket 并发起长请求，从而真正看到实时日志。
     * </p>
     *
     * @param taskId   预创建任务 ID，可为空
     * @param projectId 项目 ID，可为空
     * @param taskType  任务类型
     * @param message   初始消息
     * @return 任务记录
     */
    public TaskRecord prepare(String taskId, String projectId, String taskType, String message) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return create(projectId, taskType, message);
        }
        return findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_TASK_NOT_FOUND));
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
        update(record, TaskStatus.SUCCESS, JarPatchConstants.ONE_HUNDRED_PERCENT, message);
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
     * 取消任务。
     * <p>
     * 前端取消按钮调用该方法，实际执行点是 tasks 表更新和 WebSocket 广播；后续长流程
     * 会在下一次进度更新前检测到取消状态并退出。
     * </p>
     *
     * @param taskId 任务 ID
     * @param message 取消提示
     * @return 取消后的任务记录
     */
    public TaskRecord cancel(String taskId, String message) {
        TaskRecord record = require(taskId);
        if (TaskStatus.SUCCESS.getCode().equals(record.getStatus())
                || TaskStatus.FAILED.getCode().equals(record.getStatus())
                || TaskStatus.CANCELED.getCode().equals(record.getStatus())) {
            return record;
        }
        update(record, TaskStatus.CANCELED, record.getProgress(), message);
        return require(taskId);
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
     * 导入完成项目落库后，把预创建任务绑定到项目，使任务日志和操作时间线可按项目追溯。
     *
     * @param record    导入任务
     * @param projectId 已落库项目 ID
     */
    public void bindProject(TaskRecord record, String projectId) {
        taskRepository.bindProject(record.getId(), projectId);
        record.setProjectId(projectId);
    }

    /**
     * 查询任务的持久化日志。
     *
     * @param taskId 任务 ID
     * @return 按产生顺序排列的日志
     */
    public List<TaskLogRecord> findLogs(String taskId) {
        require(taskId);
        return taskLogRepository.findByTaskId(taskId);
    }

    /**
     * 后端启动时把上次进程遗留的运行中任务原子更新为失败，并补写持久化日志。
     * <p>
     * 入口在应用就绪事件，实际状态更新发生在带 RUNNING 条件的 SQL，结果写入 tasks 和 task_logs。
     * </p>
     *
     * @return 实际恢复的任务数量
     */
    public int recoverInterruptedTasks() {
        int recoveredCount = 0;
        for (TaskRecord record : taskRepository.findByStatus(TaskStatus.RUNNING.getCode())) {
            record.setStatus(TaskStatus.FAILED.getCode());
            record.setMessage(JarPatchConstants.MESSAGE_TASK_INTERRUPTED);
            record.setUpdatedAt(clockService.now());
            if (taskRepository.updateIfRunning(record) > 0) {
                appendLog(record, record.getMessage());
                appendOperation(record, record.getMessage());
                recoveredCount++;
            }
        }
        return recoveredCount;
    }

    /**
     * 判断任务是否已取消。
     *
     * @param taskId 任务 ID
     * @return 已取消时返回 true
     */
    public boolean isCancelled(String taskId) {
        return findById(taskId)
                .map(record -> TaskStatus.CANCELED.getCode().equals(record.getStatus()))
                .orElse(false);
    }

    /**
     * 读取任务记录，不存在时抛出业务异常。
     *
     * @param taskId 任务 ID
     * @return 任务记录
     */
    public TaskRecord require(String taskId) {
        return findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_TASK_NOT_FOUND));
    }

    /**
     * 判断任务是否仍然可继续执行。
     *
     * @param taskId 任务 ID
     */
    public void ensureNotCancelled(String taskId) {
        if (isCancelled(taskId)) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
        }
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
        if (taskRepository.updateIfRunning(record) == 0) {
            TaskRecord latest = require(record.getId());
            copyState(record, latest);
            if (TaskStatus.CANCELED.getCode().equals(latest.getStatus()) && status != TaskStatus.CANCELED) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
            }
            return;
        }
        appendLog(record, message);
        appendOperation(record, message);
        broadcaster.broadcast(record.getId(), String.format(JarPatchConstants.TASK_LOG_PROGRESS_FORMAT,
                progress, message));
    }

    /**
     * 追加与任务状态同事务提交的日志。
     *
     * @param record 当前任务状态
     * @param message 日志消息
     */
    private void appendLog(TaskRecord record, String message) {
        TaskLogRecord log = new TaskLogRecord();
        log.setId(UUID.randomUUID().toString());
        log.setTaskId(record.getId());
        log.setProgress(record.getProgress());
        log.setStatus(record.getStatus());
        log.setMessage(message == null ? JarPatchConstants.EMPTY_TEXT : message);
        log.setCreatedAt(clockService.now());
        taskLogRepository.insert(log);
    }

    /**
     * 把任务状态同步追加到项目操作时间线；尚未绑定项目的导入任务在落库成功前不写入。
     *
     * @param record  当前任务状态
     * @param message 状态说明
     */
    private void appendOperation(TaskRecord record, String message) {
        if (record.getProjectId() == null || record.getProjectId().isBlank()) {
            return;
        }
        operationJournalRepository.insert(record.getProjectId(), record.getId(), record.getTaskType(),
                record.getProjectId(), record.getStatus(),
                message == null ? JarPatchConstants.EMPTY_TEXT : message, clockService.now());
    }

    /**
     * 当条件更新未命中时，把调用方持有的任务对象同步为数据库最新状态。
     *
     * @param target 调用方任务对象
     * @param source 数据库最新任务对象
     */
    private void copyState(TaskRecord target, TaskRecord source) {
        target.setStatus(source.getStatus());
        target.setProgress(source.getProgress());
        target.setMessage(source.getMessage());
        target.setUpdatedAt(source.getUpdatedAt());
    }
}
