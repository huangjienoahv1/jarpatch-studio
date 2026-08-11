package com.jarpatch.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地后端实例访问配置。
 * <p>
 * Electron 主进程启动后端时生成随机令牌和实例 ID，并通过环境变量传入本类；HTTP
 * 过滤器、WebSocket 握手拦截器和健康检查共同使用这组值识别当前桌面实例。
 * </p>
 *
 * @author 黄杰
 */
@ConfigurationProperties(prefix = "jarpatch.local-access")
public class LocalAccessProperties {

    public static final String TOKEN_HEADER = "X-JarPatch-Token";
    public static final String TOKEN_QUERY_PARAMETER = "token";
    private static final String MESSAGE_TOKEN_REQUIRED = "本地访问令牌不能为空，必须由桌面端启动后端";
    private static final String MESSAGE_INSTANCE_REQUIRED = "本地实例 ID 不能为空，必须由桌面端启动后端";

    private String token;
    private String instanceId;
    private String allowedOrigin;

    /**
     * 校验启动握手参数，禁止在没有实例身份的情况下开放本地接口。
     */
    @PostConstruct
    public void validate() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(MESSAGE_TOKEN_REQUIRED);
        }
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalStateException(MESSAGE_INSTANCE_REQUIRED);
        }
    }

    /**
     * 获取本实例令牌。
     *
     * @return 随机访问令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置本实例令牌。
     *
     * @param token 随机访问令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 获取本实例 ID。
     *
     * @return 实例 ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 设置本实例 ID。
     *
     * @param instanceId 实例 ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 获取允许访问的 Electron 页面来源。
     *
     * @return Origin 值
     */
    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    /**
     * 设置允许访问的 Electron 页面来源。
     *
     * @param allowedOrigin Origin 值
     */
    public void setAllowedOrigin(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }
}
