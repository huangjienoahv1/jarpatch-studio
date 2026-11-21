package com.jarpatch.repository;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.UserRole;
import com.jarpatch.common.UserStatus;
import com.jarpatch.model.UserRecord;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 本地用户账户仓储。
 * <p>
 * 认证服务和用户管理服务通过本仓储读写 SQLite 中的 users 表；用户名按不区分大小写
 * 唯一匹配，保证本地多账号管理的一致性与可预测性。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<UserRecord> rowMapper = new UserRowMapper();

    /**
     * 创建用户仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 统计当前用户表中的总账号数。
     *
     * @return 总用户数
     */
    public int countAll() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return count == null ? JarPatchConstants.EMPTY_SIZE : count;
    }

    /**
     * 统计当前处于启用状态的管理员数量，用于保护最后一个管理员不被禁用、降级或删除。
     *
     * @return 启用中的管理员人数
     */
    public int countEnabledAdmins() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ? AND status = ?",
                Integer.class, UserRole.ADMIN.getCode(), UserStatus.ENABLED.getCode());
        return count == null ? JarPatchConstants.EMPTY_SIZE : count;
    }

    /**
     * 根据主键 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    public Optional<UserRecord> findById(String id) {
        try {
            UserRecord record = jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", rowMapper, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /**
     * 按用户名（忽略大小写）查询用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    public Optional<UserRecord> findByUsername(String username) {
        try {
            UserRecord record = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE LOWER(username) = LOWER(?)", rowMapper, username);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    /**
     * 查询全部用户列表，按管理员优先、创建时间升序排列。
     *
     * @return 用户实体列表
     */
    public List<UserRecord> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM users ORDER BY CASE WHEN role = 'ADMIN' THEN 0 ELSE 1 END, created_at ASC, username ASC",
                rowMapper);
    }

    /**
     * 新增一条本地用户记录。
     *
     * @param record 用户实体
     */
    public void insert(UserRecord record) {
        jdbcTemplate.update("INSERT INTO users " +
                        "(id, username, display_name, role, status, password_hash, password_salt, " +
                        "must_change_password, last_login_at, creator, updater, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                record.getId(), record.getUsername(), record.getDisplayName(), record.getRole(), record.getStatus(),
                record.getPasswordHash(), record.getPasswordSalt(),
                record.isMustChangePassword() ? JarPatchConstants.SQLITE_FLAG_TRUE : JarPatchConstants.SQLITE_FLAG_FALSE,
                record.getLastLoginAt(), record.getCreator(), record.getUpdater(),
                record.getCreatedAt(), record.getUpdatedAt());
    }

    /**
     * 更新用户显示名称、角色和启停状态。
     *
     * @param id          用户 ID
     * @param displayName 显示名称
     * @param role        角色码
     * @param status      状态码
     * @param updater     操作人
     * @param updatedAt   更新时间
     */
    public void updateProfile(String id,
                              String displayName,
                              String role,
                              String status,
                              String updater,
                              String updatedAt) {
        jdbcTemplate.update("UPDATE users SET display_name = ?, role = ?, status = ?, updater = ?, updated_at = ? " +
                        "WHERE id = ?",
                displayName, role, status, updater, updatedAt, id);
    }

    /**
     * 更新用户密码哈希、盐值及是否强制改密标记。
     *
     * @param id                 用户 ID
     * @param passwordHash       新密码哈希
     * @param passwordSalt       新随机盐
     * @param mustChangePassword 下次是否强制改密
     * @param updater            操作人
     * @param updatedAt          更新时间
     */
    public void updatePassword(String id,
                               String passwordHash,
                               String passwordSalt,
                               boolean mustChangePassword,
                               String updater,
                               String updatedAt) {
        jdbcTemplate.update("UPDATE users SET password_hash = ?, password_salt = ?, must_change_password = ?, " +
                        "updater = ?, updated_at = ? WHERE id = ?",
                passwordHash, passwordSalt,
                mustChangePassword ? JarPatchConstants.SQLITE_FLAG_TRUE : JarPatchConstants.SQLITE_FLAG_FALSE,
                updater, updatedAt, id);
    }

    /**
     * 记录用户最近成功登录时间。
     *
     * @param id          用户 ID
     * @param lastLoginAt 最近登录时间
     */
    public void updateLastLoginAt(String id, String lastLoginAt) {
        jdbcTemplate.update("UPDATE users SET last_login_at = ? WHERE id = ?", lastLoginAt, id);
    }

    /**
     * 删除指定用户（外键级联清理关联会话）。
     *
     * @param id 用户 ID
     */
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    /**
     * 用户记录行映射器。
     */
    private static class UserRowMapper implements RowMapper<UserRecord> {

        /**
         * 将 SQLite 查询行映射为 {@link UserRecord}。
         *
         * @param rs     结果集
         * @param rowNum 行号
         * @return 用户实体
         * @throws SQLException 字段读取失败时抛出
         */
        @Override
        public UserRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRecord record = new UserRecord();
            record.setId(rs.getString("id"));
            record.setUsername(rs.getString("username"));
            record.setDisplayName(rs.getString("display_name"));
            record.setRole(rs.getString("role"));
            record.setStatus(rs.getString("status"));
            record.setPasswordHash(rs.getString("password_hash"));
            record.setPasswordSalt(rs.getString("password_salt"));
            record.setMustChangePassword(rs.getInt("must_change_password") == JarPatchConstants.SQLITE_FLAG_TRUE);
            record.setLastLoginAt(rs.getString("last_login_at"));
            record.setCreator(rs.getString("creator"));
            record.setUpdater(rs.getString("updater"));
            record.setCreatedAt(rs.getString("created_at"));
            record.setUpdatedAt(rs.getString("updated_at"));
            return record;
        }
    }
}
