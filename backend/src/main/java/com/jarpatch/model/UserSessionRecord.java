package com.jarpatch.model;

/**
 * 用户持久化会话实体。
 * <p>
 * 对应 SQLite 的 user_sessions 表，仅持久化会话令牌的 SHA-256 摘要，不落盘明文令牌。
 * </p>
 *
 * @author 黄杰
 */
public class UserSessionRecord {

    private String tokenHash;
    private String userId;
    private String expiresAt;
    private String createdAt;
    private String lastActiveAt;

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(String lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
