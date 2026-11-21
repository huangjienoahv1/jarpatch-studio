package com.jarpatch.model;

/**
 * 用户登录请求。
 * <p>
 * 桌面端登录页通过该请求体提交用户名与密码，由认证服务校验后签发会话令牌。
 * </p>
 *
 * @author 黄杰
 */
public class LoginRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
