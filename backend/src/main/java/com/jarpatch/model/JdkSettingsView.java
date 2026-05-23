package com.jarpatch.model;

/**
 * JDK 配置视图。
 * <p>
 * 该模型同时返回已保存的 JDK 配置和当前实际可用的 JDK 信息，前端设置页面据此显示
 * 当前保存值、实际生效值和校验状态。
 * </p>
 *
 * @author 黄杰
 */
public class JdkSettingsView {

    private String configuredJavaHome;
    private String configuredJavacPath;
    private boolean configuredValid;
    private String effectiveJavaHome;
    private String effectiveJavacPath;
    private boolean effectiveValid;
    private String message;

    /**
     * 获取已保存的 JDK 安装目录。
     *
     * @return 已保存的 JDK 安装目录
     */
    public String getConfiguredJavaHome() {
        return configuredJavaHome;
    }

    /**
     * 设置已保存的 JDK 安装目录。
     *
     * @param configuredJavaHome 已保存的 JDK 安装目录
     */
    public void setConfiguredJavaHome(String configuredJavaHome) {
        this.configuredJavaHome = configuredJavaHome;
    }

    /**
     * 获取已保存配置对应的 javac 路径。
     *
     * @return javac 路径
     */
    public String getConfiguredJavacPath() {
        return configuredJavacPath;
    }

    /**
     * 设置已保存配置对应的 javac 路径。
     *
     * @param configuredJavacPath javac 路径
     */
    public void setConfiguredJavacPath(String configuredJavacPath) {
        this.configuredJavacPath = configuredJavacPath;
    }

    /**
     * 判断已保存配置是否可用。
     *
     * @return 可用时返回 true
     */
    public boolean isConfiguredValid() {
        return configuredValid;
    }

    /**
     * 设置已保存配置是否可用。
     *
     * @param configuredValid 是否可用
     */
    public void setConfiguredValid(boolean configuredValid) {
        this.configuredValid = configuredValid;
    }

    /**
     * 获取实际生效的 JDK 安装目录。
     *
     * @return 实际生效的 JDK 安装目录
     */
    public String getEffectiveJavaHome() {
        return effectiveJavaHome;
    }

    /**
     * 设置实际生效的 JDK 安装目录。
     *
     * @param effectiveJavaHome 实际生效的 JDK 安装目录
     */
    public void setEffectiveJavaHome(String effectiveJavaHome) {
        this.effectiveJavaHome = effectiveJavaHome;
    }

    /**
     * 获取实际生效的 javac 路径。
     *
     * @return 实际生效的 javac 路径
     */
    public String getEffectiveJavacPath() {
        return effectiveJavacPath;
    }

    /**
     * 设置实际生效的 javac 路径。
     *
     * @param effectiveJavacPath 实际生效的 javac 路径
     */
    public void setEffectiveJavacPath(String effectiveJavacPath) {
        this.effectiveJavacPath = effectiveJavacPath;
    }

    /**
     * 判断实际生效的 JDK 是否可用。
     *
     * @return 可用时返回 true
     */
    public boolean isEffectiveValid() {
        return effectiveValid;
    }

    /**
     * 设置实际生效的 JDK 是否可用。
     *
     * @param effectiveValid 是否可用
     */
    public void setEffectiveValid(boolean effectiveValid) {
        this.effectiveValid = effectiveValid;
    }

    /**
     * 获取状态说明。
     *
     * @return 状态说明
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置状态说明。
     *
     * @param message 状态说明
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
