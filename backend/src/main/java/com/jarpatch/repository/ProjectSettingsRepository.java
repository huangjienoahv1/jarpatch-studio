package com.jarpatch.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.model.ProjectSettings;
import com.jarpatch.common.JarPatchConstants;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 项目级设置仓储。
 * <p>
 * 设置服务通过该仓储按项目读取和覆盖设置，嵌套 Jar 清单以 JSON 数组保存，其他字段保持可查询列。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class ProjectSettingsRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建项目设置仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     * @param objectMapper JSON 序列化器
     */
    public ProjectSettingsRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取已保存的项目设置。
     *
     * @param projectId 项目 ID
     * @return 设置记录
     */
    public Optional<ProjectSettings> findByProjectId(String projectId) {
        try {
            ProjectSettings settings = jdbcTemplate.queryForObject(
                    "SELECT project_id, default_export_directory, selected_nested_jars_json, " +
                            "max_editable_file_bytes, ui_preferences_json, updated_at " +
                            "FROM project_settings WHERE project_id = ?",
                    (resultSet, rowNum) -> {
                        ProjectSettings record = new ProjectSettings();
                        record.setProjectId(resultSet.getString("project_id"));
                        record.setDefaultExportDirectory(resultSet.getString("default_export_directory"));
                        try {
                            record.setSelectedNestedJars(objectMapper.readValue(
                                    resultSet.getString("selected_nested_jars_json"), STRING_LIST_TYPE));
                        } catch (JsonProcessingException exception) {
                            throw new IllegalStateException(JarPatchConstants.MESSAGE_PROJECT_SETTING_NESTED_JARS_CORRUPTED,
                                    exception);
                        }
                        record.setMaxEditableFileBytes(resultSet.getLong("max_editable_file_bytes"));
                        record.setUiPreferencesJson(resultSet.getString("ui_preferences_json"));
                        record.setUpdatedAt(resultSet.getString("updated_at"));
                        return record;
                    }, projectId);
            return Optional.ofNullable(settings);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /**
     * 新增或完整覆盖项目设置。
     *
     * @param settings 设置值
     * @param now      保存时间
     * @throws JsonProcessingException 嵌套 Jar 清单无法序列化时抛出
     */
    public void upsert(ProjectSettings settings, String now) throws JsonProcessingException {
        jdbcTemplate.update("INSERT INTO project_settings " +
                        "(project_id, default_export_directory, selected_nested_jars_json, max_editable_file_bytes, " +
                        "ui_preferences_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(project_id) DO UPDATE SET default_export_directory = excluded.default_export_directory, " +
                        "selected_nested_jars_json = excluded.selected_nested_jars_json, " +
                        "max_editable_file_bytes = excluded.max_editable_file_bytes, " +
                        "ui_preferences_json = excluded.ui_preferences_json, updated_at = excluded.updated_at",
                settings.getProjectId(), settings.getDefaultExportDirectory(),
                objectMapper.writeValueAsString(settings.getSelectedNestedJars()), settings.getMaxEditableFileBytes(),
                settings.getUiPreferencesJson(), now, now);
    }
}
