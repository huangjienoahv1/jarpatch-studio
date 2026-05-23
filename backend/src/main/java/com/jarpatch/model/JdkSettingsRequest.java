package com.jarpatch.model;

/**
 * JDK 配置保存请求。
 * <p>
 * 前端在设置弹窗中输入 JDK 安装目录后，通过该模型把路径提交给后端校验和保存。
 * </p>
 *
 * @author 黄杰
 */
public class JdkSettingsRequest {

    private String javaHome;

    /**
     * 获取 JDK 安装目录。
     *
     * @return JDK 安装目录
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * 设置 JDK 安装目录。
     *
     * @param javaHome JDK 安装目录
     */
    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }
}
