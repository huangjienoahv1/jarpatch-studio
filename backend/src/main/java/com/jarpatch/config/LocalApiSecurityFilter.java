package com.jarpatch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.UserRole;
import com.jarpatch.model.UserView;
import com.jarpatch.service.AuthService;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 本地 HTTP 接口双令牌与角色鉴权过滤器。
 * <p>
 * 入口为全部 /api HTTP 请求；首先校验 Electron 启动时生成的本机实例令牌（X-JarPatch-Token），
 * 随后对非白名单接口校验本地用户会话令牌（X-JarPatch-Session）、首次强制改密状态及管理员
 * 角色边界，并在通过后把当前用户绑定到 {@link UserContext}，请求结束时在 finally 中清理。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class LocalApiSecurityFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/";
    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String PUT_METHOD = "PUT";
    private static final String DELETE_METHOD = "DELETE";
    private static final String MESSAGE_UNAUTHORIZED = "JarPatch Studio 本地实例令牌无效";

    private static final Set<String> SESSION_EXEMPT_PATHS = Set.of(
            "/api/system/health",
            "/api/system/shutdown",
            "/api/auth/bootstrap",
            "/api/auth/login"
    );

    private static final Set<String> PASSWORD_CHANGE_ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/change-password",
            "/api/auth/logout"
    );

    private static final String PATH_USERS_ROOT = "/api/users";
    private static final String PATH_USERS_PREFIX = "/api/users/";
    private static final String PATH_JDK_SETTINGS = "/api/settings/jdk";
    private static final String PATH_ORPHANS_ROOT = "/api/workspaces/orphans";
    private static final String PATH_ORPHANS_PREFIX = "/api/workspaces/orphans/";
    private static final Pattern PROJECT_ITEM_PATTERN = Pattern.compile("^/api/projects/[^/]+$");
    private static final Pattern PROJECT_WORKSPACE_PATTERN = Pattern.compile("^/api/projects/[^/]+/workspace(?:/.*)?$");

    private final LocalAccessProperties localAccessProperties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    /**
     * 创建本地接口双令牌与角色鉴权过滤器。
     *
     * @param localAccessProperties 本地实例访问配置
     * @param authService           本地用户认证服务
     * @param objectMapper          JSON 序列化器
     */
    public LocalApiSecurityFilter(LocalAccessProperties localAccessProperties,
                                  AuthService authService,
                                  ObjectMapper objectMapper) {
        this.localAccessProperties = localAccessProperties;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    /**
     * 依次执行实例令牌校验、用户会话校验、强制改密门禁和管理员权限门禁。
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
        if (!requiresAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 第一层：校验 Electron 启动后端时注入的本机实例随机令牌。
        if (!tokenMatches(request.getHeader(LocalAccessProperties.TOKEN_HEADER))) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
            return;
        }
        String path = request.getRequestURI();
        if (SESSION_EXEMPT_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        // 第二层：校验本地用户登录会话令牌。
        String sessionToken = request.getHeader(LocalAccessProperties.SESSION_HEADER);
        Optional<UserView> userOpt = authService.authenticateSession(sessionToken);
        if (userOpt.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    JarPatchConstants.MESSAGE_AUTH_SESSION_REQUIRED);
            return;
        }
        UserView user = userOpt.get();
        // 第三层：首次登录或密码被重置后，强制改密前阻止一切业务操作。
        if (user.isMustChangePassword() && !PASSWORD_CHANGE_ALLOWED_PATHS.contains(path)) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    JarPatchConstants.MESSAGE_AUTH_PASSWORD_CHANGE_REQUIRED);
            return;
        }
        // 第四层：高权限接口校验管理员角色。
        if (requiresAdminRole(request.getMethod(), path) && !UserRole.ADMIN.getCode().equals(user.getRole())) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    JarPatchConstants.MESSAGE_AUTH_ADMIN_REQUIRED);
            return;
        }

        UserContext.set(user, sessionToken);
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 判断当前请求是否属于需要鉴权保护的业务接口。
     *
     * @param request HTTP 请求
     * @return 需要鉴权时返回 true
     */
    private boolean requiresAuthentication(HttpServletRequest request) {
        return request.getRequestURI().startsWith(API_PREFIX)
                && !OPTIONS_METHOD.equalsIgnoreCase(request.getMethod());
    }

    /**
     * 判断请求方法与路径是否属于仅限管理员访问的高权限边界。
     *
     * @param method HTTP 方法
     * @param path   请求路径
     * @return 仅限管理员时返回 true
     */
    private boolean requiresAdminRole(String method, String path) {
        if (PATH_USERS_ROOT.equals(path) || path.startsWith(PATH_USERS_PREFIX)) {
            return true;
        }
        if (PUT_METHOD.equalsIgnoreCase(method) && PATH_JDK_SETTINGS.equals(path)) {
            return true;
        }
        if (PATH_ORPHANS_ROOT.equals(path) || path.startsWith(PATH_ORPHANS_PREFIX)) {
            return true;
        }
        if (DELETE_METHOD.equalsIgnoreCase(method) && PROJECT_ITEM_PATTERN.matcher(path).matches()) {
            return true;
        }
        return PROJECT_WORKSPACE_PATTERN.matcher(path).matches();
    }

    /**
     * 使用常量时间比较检查本机实例令牌。
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

    /**
     * 写出统一 UTF-8 JSON 格式的鉴权失败响应。
     *
     * @param response HTTP 响应
     * @param status   HTTP 状态码
     * @param message  失败提示文案
     * @throws IOException 写入响应失败时抛出
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failed(message));
    }
}
