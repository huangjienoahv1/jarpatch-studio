package com.jarpatch.repository;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 应用配置仓储。
 * <p>
 * 当前用于保存 JDK 安装目录等全局配置，后续项目级默认导出目录和界面偏好也复用该表。
 * 控制器不会直接操作数据库，统一由服务层调用该仓储读写 app_settings。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class AppSettingsRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建应用配置仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public AppSettingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据键读取配置值。
     *
     * @param settingKey 配置键
     * @return 配置值
     */
    public Optional<String> findValue(String settingKey) {
        return jdbcTemplate.query("SELECT setting_value FROM app_settings WHERE setting_key = ?",
                resultSet -> resultSet.next() ? Optional.ofNullable(resultSet.getString("setting_value")) : Optional.empty(),
                settingKey);
    }

    /**
     * 保存或更新配置值。
     * <p>
     * 入口来自配置页面保存按钮，实际执行点是 SQLite 的 UPSERT，结果写入 app_settings。
     * </p>
     *
     * @param settingKey   配置键
     * @param settingValue  配置值
     * @param now          当前时间字符串
     */
    public void upsert(String settingKey, String settingValue, String now) {
        jdbcTemplate.update("INSERT INTO app_settings " +
                        "(setting_key, setting_value, creator, updater, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(setting_key) DO UPDATE SET " +
                        "setting_value = excluded.setting_value, " +
                        "updater = excluded.updater, " +
                        "updated_at = excluded.updated_at",
                settingKey, settingValue, JarPatchConstants.DEFAULT_AUDITOR, JarPatchConstants.DEFAULT_AUDITOR, now, now);
    }
}
