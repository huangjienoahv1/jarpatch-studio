package com.jarpatch.model;

/**
 * 本地用户完整存储实体。
 * <p>
 * 仅在仓储层与认证/用户管理服务内部流转，包含加盐密码哈希和盐值；对外 HTTP 响应
 * 必须通过 {@link UserView} 脱敏输出，禁止直接序列化该实体返回给前端。
 * </p>
 *
 * @author 黄杰
 */
public class UserRecord {

    private String id;
    private String username;
    private String displayName;
    private String role;
    private String status;
    private String passwordHash;
    private String passwordSalt;
    private boolean mustChangePassword;
    private String lastLoginAt;
    private String creator;
    private String updater;
    private String createdAt;
    private String updatedAt;

    /**
     * 转换为不包含密码摘要与盐值的安全展示视图。
     *
     * @return 脱敏用户视图
     */
    public UserView toView() {
        UserView view = new UserView();
        view.setId(id);
        view.setUsername(username);
        view.setDisplayName(displayName);
        view.setRole(role);
        view.setStatus(status);
        view.setMustChangePassword(mustChangePassword);
        view.setLastLoginAt(lastLoginAt);
        view.setCreatedAt(createdAt);
        view.setUpdatedAt(updatedAt);
        return view;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getUpdater() {
        return updater;
    }

    public void setUpdater(String updater) {
        this.updater = updater;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
