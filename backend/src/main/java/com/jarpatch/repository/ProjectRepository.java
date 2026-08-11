package com.jarpatch.repository;

import com.jarpatch.model.ProjectRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 项目记录仓储。
 * <p>
 * 项目导入服务通过该仓储写入项目记录，首页和工作台接口通过该仓储查询项目历史。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class ProjectRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ProjectRecord> rowMapper = new ProjectRowMapper();

    /**
     * 创建项目仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public ProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增项目记录。
     *
     * @param record 项目记录
     */
    public void insert(ProjectRecord record) {
        jdbcTemplate.update("INSERT INTO projects " +
                        "(id, name, package_type, original_path, workspace_path, target_java_version, " +
                        "class_major_version, java_version_evidence, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.getId(), record.getName(), record.getPackageType(), record.getOriginalPath(),
                record.getWorkspacePath(), record.getTargetJavaVersion(), record.getClassMajorVersion(),
                record.getJavaVersionEvidence(), record.getCreatedAt(), record.getUpdatedAt());
    }

    /**
     * 根据项目 ID 查询项目。
     *
     * @param id 项目 ID
     * @return 项目记录
     */
    public Optional<ProjectRecord> findById(String id) {
        try {
            ProjectRecord record = jdbcTemplate.queryForObject("SELECT * FROM projects WHERE id = ?", rowMapper, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查询所有项目，按更新时间倒序返回。
     *
     * @return 项目记录列表
     */
    public List<ProjectRecord> findAll() {
        return jdbcTemplate.query("SELECT * FROM projects ORDER BY updated_at DESC", rowMapper);
    }

    /**
     * 更新项目更新时间。
     *
     * @param id        项目 ID
     * @param updatedAt 更新时间
     */
    public void touch(String id, String updatedAt) {
        jdbcTemplate.update("UPDATE projects SET updated_at = ? WHERE id = ?", updatedAt, id);
    }

    /**
     * 标记项目工作区已独立清理，同时保留项目历史。
     *
     * @param id        项目 ID
     * @param cleanedAt 清理时间
     */
    public void markWorkspaceCleaned(String id, String cleanedAt) {
        jdbcTemplate.update("UPDATE projects SET workspace_cleaned_at = ?, updated_at = ? WHERE id = ?",
                cleanedAt, cleanedAt, id);
    }

    /**
     * 删除项目历史和关联记录。
     * <p>
     * 入口在左侧项目历史删除按钮，实际执行点是 SQLite 删除语句，结果写到
     * projects、tasks、file_changes 和 export_records 表；不会删除本地工作区文件。
     * </p>
     *
     * @param id 项目 ID
     */
    public void deleteHistory(String id) {
        jdbcTemplate.update("DELETE FROM task_logs WHERE task_id IN (SELECT id FROM tasks WHERE project_id = ?)", id);
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM file_changes WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM export_records WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM compiled_artifacts WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM export_validations WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM analysis_reports WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM project_settings WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", id);
    }

    /**
     * 项目记录行映射器。
     */
    private static class ProjectRowMapper implements RowMapper<ProjectRecord> {

        /**
         * 把 SQLite 查询结果转换为项目模型。
         *
         * @param rs     查询结果集
         * @param rowNum 当前行号
         * @return 项目记录
         * @throws SQLException 字段读取失败时抛出
         */
        @Override
        public ProjectRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProjectRecord record = new ProjectRecord();
            record.setId(rs.getString("id"));
            record.setName(rs.getString("name"));
            record.setPackageType(rs.getString("package_type"));
            record.setOriginalPath(rs.getString("original_path"));
            record.setWorkspacePath(rs.getString("workspace_path"));
            Number targetJavaVersion = (Number) rs.getObject("target_java_version");
            Number classMajorVersion = (Number) rs.getObject("class_major_version");
            record.setTargetJavaVersion(targetJavaVersion == null ? null : targetJavaVersion.intValue());
            record.setClassMajorVersion(classMajorVersion == null ? null : classMajorVersion.intValue());
            record.setJavaVersionEvidence(rs.getString("java_version_evidence"));
            record.setWorkspaceCleanedAt(rs.getString("workspace_cleaned_at"));
            record.setCreatedAt(rs.getString("created_at"));
            record.setUpdatedAt(rs.getString("updated_at"));
            return record;
        }
    }
}
