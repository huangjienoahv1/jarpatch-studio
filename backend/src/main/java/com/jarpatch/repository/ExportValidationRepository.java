package com.jarpatch.repository;

import com.jarpatch.model.ExportValidationResult;
import com.jarpatch.model.ExportValidationHistoryRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Arrays;
import java.util.List;

/**
 * 导出结构校验结果仓储。
 * <p>
 * 无论校验成功或失败，ExportService 都会在最终文件发布前写入该表；失败记录保留目标路径
 * 和精确错误，而不会把未通过校验的临时包发布为用户目标文件。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class ExportValidationRepository {

    private static final String ITEM_SEPARATOR = "\n";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建导出校验仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public ExportValidationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 保存单次导出校验结果。
     *
     * @param projectId 项目 ID
     * @param outputPath 用户目标路径
     * @param result     校验结果
     * @param createdAt  校验时间
     */
    public void insert(String projectId, String outputPath, ExportValidationResult result, String createdAt) {
        jdbcTemplate.update("INSERT INTO export_validations " +
                        "(id, project_id, output_path, valid, checks, errors, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), projectId, outputPath, result.isValid() ? 1 : 0,
                String.join(ITEM_SEPARATOR, result.getChecks()),
                String.join(ITEM_SEPARATOR, result.getErrors()),
                createdAt);
    }

    /**
     * 查询项目最近导出校验历史。
     *
     * @param projectId 项目 ID
     * @param limit     最大记录数
     * @return 按时间倒序的校验记录
     */
    public List<ExportValidationHistoryRecord> findByProjectId(String projectId, int limit) {
        return jdbcTemplate.query("SELECT id, output_path, valid, checks, errors, created_at " +
                        "FROM export_validations WHERE project_id = ? " +
                        "ORDER BY created_at DESC, id DESC LIMIT ?",
                (resultSet, rowNum) -> {
                    ExportValidationHistoryRecord record = new ExportValidationHistoryRecord();
                    record.setId(resultSet.getString("id"));
                    record.setOutputPath(resultSet.getString("output_path"));
                    record.setValid(resultSet.getInt("valid") == 1);
                    record.setChecks(splitItems(resultSet.getString("checks")));
                    record.setErrors(splitItems(resultSet.getString("errors")));
                    record.setCreatedAt(resultSet.getString("created_at"));
                    return record;
                }, projectId, limit);
    }

    /**
     * 把数据库换行分隔字段恢复为列表。
     *
     * @param value 数据库存储值
     * @return 非空项目列表
     */
    private List<String> splitItems(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(ITEM_SEPARATOR, -1))
                .filter(item -> !item.isBlank())
                .toList();
    }
}
