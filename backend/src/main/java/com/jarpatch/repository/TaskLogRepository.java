package com.jarpatch.repository;

import com.jarpatch.model.TaskLogRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务日志仓储。
 * <p>
 * 任务服务在状态成功写入后追加日志，查询接口按创建顺序读取，避免日志只存在于 WebSocket 内存通道。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class TaskLogRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建任务日志仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public TaskLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 追加一条任务日志。
     *
     * @param record 任务日志
     */
    public void insert(TaskLogRecord record) {
        jdbcTemplate.update("INSERT INTO task_logs (id, task_id, progress, status, message, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                record.getId(), record.getTaskId(), record.getProgress(), record.getStatus(),
                record.getMessage(), record.getCreatedAt());
    }

    /**
     * 按数据库插入顺序读取任务日志。
     *
     * @param taskId 任务 ID
     * @return 持久化日志列表
     */
    public List<TaskLogRecord> findByTaskId(String taskId) {
        return jdbcTemplate.query("SELECT id, task_id, progress, status, message, created_at " +
                        "FROM task_logs WHERE task_id = ? ORDER BY rowid",
                (resultSet, rowNum) -> {
                    TaskLogRecord record = new TaskLogRecord();
                    record.setId(resultSet.getString("id"));
                    record.setTaskId(resultSet.getString("task_id"));
                    record.setProgress(resultSet.getInt("progress"));
                    record.setStatus(resultSet.getString("status"));
                    record.setMessage(resultSet.getString("message"));
                    record.setCreatedAt(resultSet.getString("created_at"));
                    return record;
                }, taskId);
    }
}
