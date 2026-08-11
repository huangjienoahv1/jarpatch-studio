package com.jarpatch.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.model.AnalysisReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

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
}
