package com.jarpatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地跨域访问配置。
 * <p>
 * Electron 前端和 Java 后端运行在同一台机器但端口不同，该配置允许前端通过本地
 * HTTP 调用后端接口。
 * </p>
 *
 * @author 黄杰
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String LOCAL_HTTP_ORIGIN = "http://127.0.0.1:18765";

    private final LocalAccessProperties localAccessProperties;

    /**
     * 创建仅允许桌面页面来源的跨域配置。
     *
     * @param localAccessProperties 本地实例访问配置
     */
    public CorsConfig(LocalAccessProperties localAccessProperties) {
        this.localAccessProperties = localAccessProperties;
    }

    /**
     * 仅开放 Electron 本地文件页面和后端自身来源所需的跨域接口。
     *
     * @param registry 跨域配置注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(localAccessProperties.getAllowedOrigin(), LOCAL_HTTP_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", LocalAccessProperties.TOKEN_HEADER)
                .maxAge(3600L);
    }
}
