package com.jarpatch.repository;

import com.jarpatch.common.JarPatchConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.model.AnalysisReport;
import com.jarpatch.model.AnalysisHistoryRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

/**
 * 结构分析报告仓储。
 * <p>
 * 分析服务把完整报告序列化后写入 SQLite，导出前分析与用户主动分析共用同一持久化格式。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class AnalysisReportRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建分析报告仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     * @param objectMapper JSON 序列化器
     */
    public AnalysisReportRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 持久化一份不可变分析快照。
     *
     * @param projectId 项目 ID
     * @param report    分析报告
     * @param createdAt 创建时间
     * @throws JsonProcessingException 报告无法序列化时抛出
     */
    public void insert(String projectId, AnalysisReport report, String createdAt) throws JsonProcessingException {
        jdbcTemplate.update("INSERT INTO analysis_reports (id, project_id, report_json, created_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), projectId, objectMapper.writeValueAsString(report), createdAt);
    }

    /**
     * 查询项目最近结构分析历史。
     *
     * @param projectId 项目 ID
     * @param limit     最大记录数
     * @return 按时间倒序的分析快照
     */
    public List<AnalysisHistoryRecord> findByProjectId(String projectId, int limit) {
        return jdbcTemplate.query("SELECT id, report_json, created_at FROM analysis_reports " +
                        "WHERE project_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
                (resultSet, rowNum) -> {
                    AnalysisHistoryRecord record = new AnalysisHistoryRecord();
                    record.setId(resultSet.getString("id"));
                    try {
                        record.setReport(objectMapper.readValue(resultSet.getString("report_json"), AnalysisReport.class));
                    } catch (JsonProcessingException exception) {
                        throw new IllegalStateException(JarPatchConstants.MESSAGE_ANALYSIS_HISTORY_CORRUPTED, exception);
                    }
                    record.setCreatedAt(resultSet.getString("created_at"));
                    return record;
                }, projectId, limit);
    }
}
