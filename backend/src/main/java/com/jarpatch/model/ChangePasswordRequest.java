package com.jarpatch.model;

/**
 * 当前用户修改密码请求。
 * <p>
 * 适用于首次登录强制改密以及日常个人中心自主修改密码场景。
 * </p>
 *
 * @author 黄杰
 */
public class ChangePasswordRequest {

    private String currentPassword;
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
