package com.jarpatch.repository;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQLite 数据库初始化器。
 * <p>
 * 后端启动时由该类创建项目表、任务表、修改记录表和导出记录表。所有默认审计字段
 * 按项目约定写入 admin，避免出现工具身份值。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final String datasourceUrl;

    /**
     * 创建数据库初始化器。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public DatabaseInitializer(JdbcTemplate jdbcTemplate, @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.datasourceUrl = datasourceUrl;
    }

    /**
     * 在 Spring Bean 初始化完成后创建本地 SQLite 表。
     */
    @PostConstruct
    public void init() throws IOException {
        createDatabaseParent();
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
                "project_id TEXT, " +
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
                "project_id TEXT NOT NULL, " +
                "relative_path TEXT NOT NULL, " +
                "file_kind TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL, " +
                "UNIQUE(project_id, relative_path))");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS export_records (" +
                "id TEXT PRIMARY KEY, " +
                "project_id TEXT NOT NULL, " +
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
     * 创建 SQLite 数据库父目录，避免首次启动时数据库文件无法落盘。
     *
     * @throws IOException 目录创建失败时抛出
     */
    private void createDatabaseParent() throws IOException {
        String prefix = "jdbc:sqlite:";
        if (!datasourceUrl.startsWith(prefix)) {
            return;
        }
        Path databasePath = Paths.get(datasourceUrl.substring(prefix.length())).toAbsolutePath().normalize();
        if (databasePath.getParent() != null) {
            Files.createDirectories(databasePath.getParent());
        }
    }
}
