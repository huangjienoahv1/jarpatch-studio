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

    /**
     * 开放本地开发和 Electron 页面所需的跨域接口。
     *
     * @param registry 跨域配置注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
