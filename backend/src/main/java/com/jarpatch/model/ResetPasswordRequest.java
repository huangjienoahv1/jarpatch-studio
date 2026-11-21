package com.jarpatch.model;

/**
 * 管理员重置目标用户密码请求。
 * <p>
 * 重置完成后目标用户会被强制要求下次登录修改密码，且既有活跃会话会立即失效。
 * </p>
 *
 * @author 黄杰
 */
public class ResetPasswordRequest {

    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
