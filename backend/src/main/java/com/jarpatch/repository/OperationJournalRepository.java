package com.jarpatch.repository;

import com.jarpatch.model.OperationJournalRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 项目业务操作日志仓储。
 * <p>
 * 任务服务在状态入口写入 operation_journals，历史服务按项目倒序读取并展示时间线。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class OperationJournalRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建操作日志仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public OperationJournalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 追加一条不可变操作状态记录。
     *
     * @param projectId    项目 ID
     * @param operationId  操作 ID，任务流程中与任务 ID 一致
     * @param operationType 操作类型
     * @param targetPath   操作目标
     * @param status       操作状态
     * @param details      状态说明
     * @param createdAt    发生时间
     */
    public void insert(String projectId,
                       String operationId,
                       String operationType,
                       String targetPath,
                       String status,
                       String details,
                       String createdAt) {
        jdbcTemplate.update("INSERT INTO operation_journals " +
                        "(id, project_id, operation_id, operation_type, target_path, status, details, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), projectId, operationId, operationType, targetPath,
                status, details, createdAt);
    }

    /**
     * 查询项目最近操作时间线。
     *
     * @param projectId 项目 ID
     * @param limit     最大记录数
     * @return 按时间倒序的操作记录
     */
    public List<OperationJournalRecord> findByProjectId(String projectId, int limit) {
        return jdbcTemplate.query("SELECT id, project_id, operation_id, operation_type, target_path, " +
                        "status, details, created_at FROM operation_journals WHERE project_id = ? " +
                        "ORDER BY created_at DESC, id DESC LIMIT ?",
                (resultSet, rowNum) -> {
                    OperationJournalRecord record = new OperationJournalRecord();
                    record.setId(resultSet.getString("id"));
                    record.setProjectId(resultSet.getString("project_id"));
                    record.setOperationId(resultSet.getString("operation_id"));
                    record.setOperationType(resultSet.getString("operation_type"));
                    record.setTargetPath(resultSet.getString("target_path"));
                    record.setStatus(resultSet.getString("status"));
                    record.setDetails(resultSet.getString("details"));
                    record.setCreatedAt(resultSet.getString("created_at"));
                    return record;
                }, projectId, limit);
    }

    /**
     * 删除项目历史时同步删除其操作时间线。
     *
     * @param projectId 项目 ID
     */
    public void deleteByProjectId(String projectId) {
        jdbcTemplate.update("DELETE FROM operation_journals WHERE project_id = ?", projectId);
    }
}
