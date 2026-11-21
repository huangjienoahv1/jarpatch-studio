package com.jarpatch.model;

/**
 * 管理员新增用户请求。
 * <p>
 * 用户管理弹窗通过该请求创建新的本地账号，并指定角色和初始密码。
 * </p>
 *
 * @author 黄杰
 */
public class CreateUserRequest {

    private String username;
    private String displayName;
    private String role;
    private String password;
    private Boolean mustChangePassword;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
