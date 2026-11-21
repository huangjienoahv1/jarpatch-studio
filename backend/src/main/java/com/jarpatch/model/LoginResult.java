package com.jarpatch.model;

/**
 * 登录结果视图。
 * <p>
 * 登录成功后返回新签发的一次性会话令牌、过期时间与当前用户脱敏信息，供桌面端在
 * 后续 HTTP 请求头与 WebSocket 握手中携带。
 * </p>
 *
 * @author 黄杰
 */
public class LoginResult {

    private String sessionToken;
    private String expiresAt;
    private UserView user;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UserView getUser() {
        return user;
    }

    public void setUser(UserView user) {
        this.user = user;
    }
}
