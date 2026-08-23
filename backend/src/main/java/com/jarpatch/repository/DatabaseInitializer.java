package com.jarpatch.repository;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.service.ClockService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * SQLite 顺序数据库迁移器。
 * <p>
 * 后端启动时先创建 schema_migrations，再按版本在事务内执行可重复校验的结构迁移；
 * 旧版五张表通过列存在性检查原地升级，新安装直接得到外键、索引、任务日志、分析报告、
 * 编译产物、导出校验和崩溃恢复日志所需完整结构。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class DatabaseInitializer {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";
    private static final int MIGRATION_BASE_SCHEMA = 1;
    private static final int MIGRATION_RELEASE_DATA = 2;
    private static final int MIGRATION_RUNTIME_STABILITY = 3;
    private static final int MIGRATION_PRODUCTIZATION = 4;
    private static final int MIGRATION_HISTORY_AND_ENCODING = 5;
    private static final int MIGRATION_FILE_ENCODING_SELECTIONS = 6;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ClockService clockService;
    private final String datasourceUrl;

    /**
     * 创建 SQLite 顺序迁移器。
     *
     * @param jdbcTemplate       Spring JDBC 操作入口
     * @param transactionTemplate 事务模板
     * @param clockService       中国时区时间服务
     * @param datasourceUrl      SQLite 数据源地址
     */
    public DatabaseInitializer(JdbcTemplate jdbcTemplate,
                               TransactionTemplate transactionTemplate,
                               ClockService clockService,
                               @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.clockService = clockService;
        this.datasourceUrl = datasourceUrl;
    }

    /**
     * 在仓储使用前按版本完成数据库迁移和外键检查配置。
     *
     * @throws IOException 数据库父目录创建失败时抛出
     */
    @PostConstruct
    public void init() throws IOException {
        createDatabaseParent();
        jdbcTemplate.execute("PRAGMA foreign_keys = ON");
        jdbcTemplate.execute("PRAGMA busy_timeout = 5000");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS schema_migrations (" +
                "version INTEGER PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "applied_at TEXT NOT NULL)");
        applyMigration(MIGRATION_BASE_SCHEMA, "base_schema", this::createBaseSchema);
        applyMigration(MIGRATION_RELEASE_DATA, "release_data", this::createReleaseDataSchema);
        applyMigration(MIGRATION_RUNTIME_STABILITY, "runtime_stability", this::createRuntimeStabilitySchema);
        applyMigration(MIGRATION_PRODUCTIZATION, "productization", this::createProductizationSchema);
        applyMigration(MIGRATION_HISTORY_AND_ENCODING, "history_and_encoding", this::createHistoryAndEncodingSchema);
        applyMigration(MIGRATION_FILE_ENCODING_SELECTIONS, "file_encoding_selections",
                this::createFileEncodingSelectionSchema);
    }

    /**
     * 创建第一版项目、任务、修改、导出和全局设置基础表。
     */
    private void createBaseSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS projects (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "package_type TEXT NOT NULL, " +
                "original_path TEXT NOT NULL, " +
                "workspace_path TEXT NOT NULL, " +
                "creator TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "updater TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT REFERENCES projects(id) ON DELETE CASCADE, " +
                "task_type TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "progress INTEGER NOT NULL, " +
                "message TEXT, " +
                "creator TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "updater TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS file_changes (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE, " +
                "relative_path TEXT NOT NULL, " +
                "file_kind TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "UNIQUE(project_id, relative_path))");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS export_records (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE, " +
                "output_path TEXT NOT NULL, " +
                "created_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
                "setting_key TEXT PRIMARY KEY, " +
                "setting_value TEXT NOT NULL, " +
                "creator TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "updater TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL)");
    }

    /**
     * 增加 Java 版本、文件哈希、编译产物和导出校验正式数据结构。
     */
    private void createReleaseDataSchema() {
        ensureColumn("projects", "target_java_version", "INTEGER");
        ensureColumn("projects", "class_major_version", "INTEGER");
        ensureColumn("projects", "java_version_evidence", "TEXT");
        ensureColumn("file_changes", "original_hash", "TEXT");
        ensureColumn("file_changes", "current_hash", "TEXT");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS compiled_artifacts (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE, " +
                "artifact_path TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "UNIQUE(project_id, artifact_path))");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS export_validations (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE, " +
                "output_path TEXT NOT NULL, " +
                "valid INTEGER NOT NULL, " +
                "checks TEXT, " +
                "errors TEXT, " +
                "created_at TEXT NOT NULL)");
    }

    /**
     * 增加持久化任务日志、分析报告、崩溃清理日志和常用查询索引。
     */
    private void createRuntimeStabilitySchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS task_logs (" +
                "id TEXT PRIMARY KEY, " +
                "task_id TEXT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE, " +
                "progress INTEGER NOT NULL, " +
                "status TEXT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS analysis_reports (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL REFERENCES projects(id) ON DELETE CASCADE, " +
                "report_json TEXT NOT NULL, " +
                "created_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS operation_journals (" +
                "id TEXT PRIMARY KEY, " +
                "operation_type TEXT NOT NULL, " +
                "target_path TEXT NOT NULL, " +
                "created_at TEXT NOT NULL)");

        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_projects_updated_at ON projects(updated_at DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tasks_status_updated_at ON tasks(status, updated_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_file_changes_project_id ON file_changes(project_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_export_records_project_id ON export_records(project_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compiled_artifacts_project_id ON compiled_artifacts(project_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_export_validations_project_id ON export_validations(project_id, created_at DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_logs_task_id ON task_logs(task_id, created_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_analysis_reports_project_id ON analysis_reports(project_id, created_at DESC)");
    }

    /**
     * 增加项目级设置和独立工作区清理状态。
     */
    private void createProductizationSchema() {
        ensureColumn("projects", "workspace_cleaned_at", "TEXT");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS project_settings (" +
                "project_id TEXT PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE, " +
                "default_export_directory TEXT, " +
                "selected_nested_jars_json TEXT NOT NULL DEFAULT '[]', " +
                "max_editable_file_bytes INTEGER NOT NULL DEFAULT 5242880, " +
                "ui_preferences_json TEXT NOT NULL DEFAULT '{}', " +
                "creator TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "updater TEXT NOT NULL DEFAULT '" + JarPatchConstants.DEFAULT_AUDITOR + "', " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL)");
    }

    /**
     * 增加显式文本编码和可查询操作时间线字段。
     */
    private void createHistoryAndEncodingSchema() {
        ensureColumn("project_settings", "default_encoding", "TEXT NOT NULL DEFAULT 'UTF-8'");
        ensureColumn("operation_journals", "project_id", "TEXT");
        ensureColumn("operation_journals", "operation_id", "TEXT");
        ensureColumn("operation_journals", "status", "TEXT NOT NULL DEFAULT 'SUCCESS'");
        ensureColumn("operation_journals", "details", "TEXT");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_operation_journals_project_id " +
                "ON operation_journals(project_id, created_at DESC)");
    }

    /**
     * 为已修改文件保留用户明确确认的编码，供差异和后续读取复用。
     */
    private void createFileEncodingSelectionSchema() {
        ensureColumn("file_changes", "encoding", "TEXT");
    }

    /**
     * 在事务内执行尚未应用的单个迁移并记录版本。
     *
     * @param version 迁移版本
     * @param name    迁移名称
     * @param action  迁移动作
     */
    private void applyMigration(int version, String name, Runnable action) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schema_migrations WHERE version = ?", Integer.class, version);
        if (count != null && count > 0) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            action.run();
            jdbcTemplate.update("INSERT INTO schema_migrations (version, name, applied_at) VALUES (?, ?, ?)",
                    version, name, clockService.now());
        });
    }

    /**
     * 为旧版表补充缺失列。
     *
     * @param tableName  表名
     * @param columnName 列名
     * @param columnType SQLite 列定义
     */
    private void ensureColumn(String tableName, String columnName, String columnType) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(" + tableName + ")");
        boolean exists = columns.stream().anyMatch(column -> columnName.equalsIgnoreCase(String.valueOf(column.get("name"))));
        if (!exists) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        }
    }

    /**
     * 创建 SQLite 数据库父目录。
     *
     * @throws IOException 目录创建失败时抛出
     */
    private void createDatabaseParent() throws IOException {
        if (!datasourceUrl.startsWith(SQLITE_PREFIX)) {
            return;
        }
        Path databasePath = Paths.get(datasourceUrl.substring(SQLITE_PREFIX.length())).toAbsolutePath().normalize();
        if (databasePath.getParent() != null) {
            Files.createDirectories(databasePath.getParent());
        }
    }
}
