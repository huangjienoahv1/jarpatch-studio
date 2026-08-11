package com.jarpatch.repository;

import com.jarpatch.model.TaskRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.List;

/**
 * 任务记录仓储。
 * <p>
 * 任务服务通过该仓储创建和更新任务状态，任务查询接口通过它读取当前进度和错误消息。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class TaskRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<TaskRecord> rowMapper = new TaskRowMapper();

    /**
     * 创建任务仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增任务记录。
     *
     * @param record 任务记录
     */
    public void insert(TaskRecord record) {
        jdbcTemplate.update("INSERT INTO tasks " +
                        "(id, project_id, task_type, status, progress, message, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                record.getId(), record.getProjectId(), record.getTaskType(), record.getStatus(),
                record.getProgress(), record.getMessage(), record.getCreatedAt(), record.getUpdatedAt());
    }

    /**
     * 更新任务进度和状态。
     *
     * @param record 任务记录
     */
    public int updateIfRunning(TaskRecord record) {
        return jdbcTemplate.update("UPDATE tasks SET status = ?, progress = ?, message = ?, updated_at = ? " +
                        "WHERE id = ? AND status = ?",
                record.getStatus(), record.getProgress(), record.getMessage(), record.getUpdatedAt(), record.getId(),
                com.jarpatch.common.TaskStatus.RUNNING.getCode());
    }

    /**
     * 查询指定状态的全部任务，用于启动时恢复中断任务。
     *
     * @param status 任务状态
     * @return 匹配的任务记录
     */
    public List<TaskRecord> findByStatus(String status) {
        return jdbcTemplate.query("SELECT * FROM tasks WHERE status = ? ORDER BY created_at", rowMapper, status);
    }

    /**
     * 统计项目仍在运行的任务数。
     *
     * @param projectId 项目 ID
     * @return 运行中任务数
     */
    public int countRunningByProject(String projectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status = ?", Integer.class,
                projectId, com.jarpatch.common.TaskStatus.RUNNING.getCode());
        return count == null ? 0 : count;
    }

    /**
     * 根据任务 ID 查询任务。
     *
     * @param id 任务 ID
     * @return 任务记录
     */
    public Optional<TaskRecord> findById(String id) {
        try {
            TaskRecord record = jdbcTemplate.queryForObject("SELECT * FROM tasks WHERE id = ?", rowMapper, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 任务记录行映射器。
     */
    private static class TaskRowMapper implements RowMapper<TaskRecord> {

        /**
         * 把 SQLite 查询结果转换为任务模型。
         *
         * @param rs     查询结果集
         * @param rowNum 当前行号
         * @return 任务记录
         * @throws SQLException 字段读取失败时抛出
         */
        @Override
        public TaskRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            TaskRecord record = new TaskRecord();
            record.setId(rs.getString("id"));
            record.setProjectId(rs.getString("project_id"));
            record.setTaskType(rs.getString("task_type"));
            record.setStatus(rs.getString("status"));
            record.setProgress(rs.getInt("progress"));
            record.setMessage(rs.getString("message"));
            record.setCreatedAt(rs.getString("created_at"));
            record.setUpdatedAt(rs.getString("updated_at"));
            return record;
        }
    }
}
