package com.jarpatch.repository;

import com.jarpatch.model.TaskLogRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
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
                this::mapTaskLog, taskId);
    }

    /**
     * 读取全局最新的任务日志，用于生成受数量限制的诊断快照。
     *
     * @param limit 最大日志条数
     * @return 按产生顺序排列的近期任务日志
     */
    public List<TaskLogRecord> findRecent(int limit) {
        List<TaskLogRecord> records = jdbcTemplate.query(
                "SELECT id, task_id, progress, status, message, created_at " +
                        "FROM task_logs ORDER BY rowid DESC LIMIT ?",
                this::mapTaskLog, limit
        );
        Collections.reverse(records);
        return records;
    }

    /**
     * 把任务日志查询结果映射为领域模型。
     *
     * @param resultSet 当前查询行
     * @param rowNum 行号
     * @return 任务日志记录
     * @throws SQLException 数据库字段读取失败时抛出
     */
    private TaskLogRecord mapTaskLog(ResultSet resultSet, int rowNum) throws SQLException {
        TaskLogRecord record = new TaskLogRecord();
        record.setId(resultSet.getString("id"));
        record.setTaskId(resultSet.getString("task_id"));
        record.setProgress(resultSet.getInt("progress"));
        record.setStatus(resultSet.getString("status"));
        record.setMessage(resultSet.getString("message"));
        record.setCreatedAt(resultSet.getString("created_at"));
        return record;
    }
}
