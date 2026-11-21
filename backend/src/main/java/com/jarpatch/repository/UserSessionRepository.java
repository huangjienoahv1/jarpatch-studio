package com.jarpatch.repository;

import com.jarpatch.model.UserSessionRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * 用户持久化会话仓储。
 * <p>
 * 认证服务在登录、会话校验、登出及账号状态变更时通过本仓储管理 SQLite 中的
 * user_sessions 表；所有会话按 SHA-256 摘要索引，不保存明文令牌。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class UserSessionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserSessionRecord> rowMapper = new UserSessionRowMapper();

    /**
     * 创建用户会话仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public UserSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 持久化一条新签发的用户会话。
     *
     * @param record 会话记录
     */
    public void insert(UserSessionRecord record) {
        jdbcTemplate.update("INSERT INTO user_sessions (token_hash, user_id, expires_at, created_at, last_active_at) " +
                        "VALUES (?, ?, ?, ?, ?)",
                record.getTokenHash(), record.getUserId(), record.getExpiresAt(),
                record.getCreatedAt(), record.getLastActiveAt());
    }

    /**
     * 按令牌摘要查询会话记录。
     *
     * @param tokenHash 会话令牌 SHA-256 摘要
     * @return 会话记录
     */
    public Optional<UserSessionRecord> findByTokenHash(String tokenHash) {
        try {
            UserSessionRecord record = jdbcTemplate.queryForObject(
                    "SELECT token_hash, user_id, expires_at, created_at, last_active_at " +
                            "FROM user_sessions WHERE token_hash = ?",
                    rowMapper, tokenHash);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /**
     * 更新会话最近活跃时间。
     *
     * @param tokenHash    会话令牌摘要
     * @param lastActiveAt 最近活跃时间
     */
    public void touch(String tokenHash, String lastActiveAt) {
        jdbcTemplate.update("UPDATE user_sessions SET last_active_at = ? WHERE token_hash = ?",
                lastActiveAt, tokenHash);
    }

    /**
     * 删除指定会话（退出登录或单会话失效）。
     *
     * @param tokenHash 会话令牌摘要
     */
    public void deleteByTokenHash(String tokenHash) {
        jdbcTemplate.update("DELETE FROM user_sessions WHERE token_hash = ?", tokenHash);
    }

    /**
     * 吊销指定用户的全部会话（禁用账号、重置密码或删除账号时触发）。
     *
     * @param userId 用户 ID
     */
    public void deleteByUserId(String userId) {
        jdbcTemplate.update("DELETE FROM user_sessions WHERE user_id = ?", userId);
    }

    /**
     * 吊销指定用户除当前会话以外的其他全部会话（主动改密后执行）。
     *
     * @param userId           用户 ID
     * @param keepTokenHash    保留的当前会话摘要
     */
    public void deleteOtherSessions(String userId, String keepTokenHash) {
        jdbcTemplate.update("DELETE FROM user_sessions WHERE user_id = ? AND token_hash <> ?",
                userId, keepTokenHash);
    }

    /**
     * 清理所有已过期会话记录。
     *
     * @param now 当前中国时区时间字符串
     */
    public void deleteExpired(String now) {
        jdbcTemplate.update("DELETE FROM user_sessions WHERE expires_at <= ?", now);
    }

    /**
     * 会话记录行映射器。
     */
    private static class UserSessionRowMapper implements RowMapper<UserSessionRecord> {

        /**
         * 将查询结果行转换为 {@link UserSessionRecord}。
         *
         * @param rs     结果集
         * @param rowNum 行号
         * @return 会话记录
         * @throws SQLException 字段读取失败时抛出
         */
        @Override
        public UserSessionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserSessionRecord record = new UserSessionRecord();
            record.setTokenHash(rs.getString("token_hash"));
            record.setUserId(rs.getString("user_id"));
            record.setExpiresAt(rs.getString("expires_at"));
            record.setCreatedAt(rs.getString("created_at"));
            record.setLastActiveAt(rs.getString("last_active_at"));
            return record;
        }
    }
}
