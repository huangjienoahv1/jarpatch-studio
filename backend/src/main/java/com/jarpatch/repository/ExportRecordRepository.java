package com.jarpatch.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * 导出记录仓储。
 * <p>
 * 导出服务成功写出 Jar 或 War 后调用该仓储记录导出文件路径，用于首页和项目历史追踪。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class ExportRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建导出记录仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public ExportRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 写入导出记录。
     *
     * @param projectId  项目 ID
     * @param outputPath 导出文件路径
     * @param createdAt  创建时间
     */
    public void insert(String projectId, String outputPath, String createdAt) {
        jdbcTemplate.update("INSERT INTO export_records (id, project_id, output_path, created_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString(), projectId, outputPath, createdAt);
    }
}
