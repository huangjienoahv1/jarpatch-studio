package com.jarpatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 本地 HTTP 接口令牌过滤器。
 * <p>
 * 所有 /api 请求在进入控制器前校验 Electron 启动时生成的随机令牌；校验失败直接返回
 * 401，不会触达任何读取本地路径、修改工作区或导出文件的业务入口。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class LocalApiSecurityFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String MESSAGE_UNAUTHORIZED = "JarPatch Studio 本地实例令牌无效";

    private final LocalAccessProperties localAccessProperties;
    private final ObjectMapper objectMapper;

    /**
     * 创建本地接口令牌过滤器。
     *
     * @param localAccessProperties 本地实例访问配置
     * @param objectMapper          JSON 序列化器
     */
    public LocalApiSecurityFilter(LocalAccessProperties localAccessProperties, ObjectMapper objectMapper) {
        this.localAccessProperties = localAccessProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 对 API 请求执行常量时间令牌比较，通过后才进入控制器调用链。
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器链执行失败时抛出
     * @throws IOException      响应写入失败时抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requiresAuthentication(request) || tokenMatches(request.getHeader(LocalAccessProperties.TOKEN_HEADER))) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failed(MESSAGE_UNAUTHORIZED));
    }

    /**
     * 判断当前请求是否属于需要令牌保护的业务接口。
     *
     * @param request HTTP 请求
     * @return 需要令牌校验时返回 true
     */
    private boolean requiresAuthentication(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX)
                && !OPTIONS_METHOD.equalsIgnoreCase(request.getMethod());
    }

    /**
     * 使用常量时间比较检查请求令牌。
     *
     * @param requestToken 请求头令牌
     * @return 令牌一致时返回 true
     */
    private boolean tokenMatches(String requestToken) {
        if (requestToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                localAccessProperties.getToken().getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8));
    }
}
