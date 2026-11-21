package com.jarpatch.model;

/**
 * 登录页启动引导状态。
 * <p>
 * 仅在预置默认管理员尚未修改初始密码时提示首次使用指引，修改密码后自动隐藏，避免
 * 在已完成初始化的环境中暴露默认口令提示。
 * </p>
 *
 * @author 黄杰
 */
public class AuthBootstrapStatus {

    private boolean defaultAdminHintVisible;
    private String defaultAdminUsername;
    private String defaultAdminInitialPassword;

    public boolean isDefaultAdminHintVisible() {
        return defaultAdminHintVisible;
    }

    public void setDefaultAdminHintVisible(boolean defaultAdminHintVisible) {
        this.defaultAdminHintVisible = defaultAdminHintVisible;
    }

    public String getDefaultAdminUsername() {
        return defaultAdminUsername;
    }

    public void setDefaultAdminUsername(String defaultAdminUsername) {
        this.defaultAdminUsername = defaultAdminUsername;
    }

    public String getDefaultAdminInitialPassword() {
        return defaultAdminInitialPassword;
    }

    public void setDefaultAdminInitialPassword(String defaultAdminInitialPassword) {
        this.defaultAdminInitialPassword = defaultAdminInitialPassword;
    }
}
