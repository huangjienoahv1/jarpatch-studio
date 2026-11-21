package com.jarpatch.config;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.UserRole;
import com.jarpatch.model.UserView;

/**
 * 当前请求线程的用户身份上下文。
 * <p>
 * 入口位于 {@link LocalApiSecurityFilter}：请求通过会话鉴权后写入当前登录用户，
 * 请求结束时在 finally 块中强制清理；仓储层和服务层读取当前用户名作为审计人落库，
 * 后台启动恢复等无 HTTP 请求场景回退为 {@link JarPatchConstants#DEFAULT_AUDITOR}。
 * </p>
 *
 * @author 黄杰
 */
public final class UserContext {

    private static final ThreadLocal<UserView> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SESSION_TOKEN = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 绑定当前请求线程的已认证用户与会话令牌。
     *
     * @param user         当前登录用户视图
     * @param sessionToken 当前请求携带的会话令牌
     */
    public static void set(UserView user, String sessionToken) {
        CURRENT_USER.set(user);
        CURRENT_SESSION_TOKEN.set(sessionToken);
    }

    /**
     * 获取当前请求绑定的已认证用户。
     *
     * @return 当前用户视图；无登录上下文时返回 null
     */
    public static UserView currentUser() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前请求携带的会话令牌。
     *
     * @return 会话令牌；无上下文时返回 null
     */
    public static String currentSessionToken() {
        return CURRENT_SESSION_TOKEN.get();
    }

    /**
     * 获取用于审计落库的当前操作用户名。
     *
     * @return 登录用户名；无请求上下文时返回默认审计人 admin
     */
    public static String currentUsername() {
        UserView user = CURRENT_USER.get();
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            return JarPatchConstants.DEFAULT_AUDITOR;
        }
        return user.getUsername();
    }

    /**
     * 判断当前请求线程是否具有管理员角色。
     *
     * @return 管理员返回 true
     */
    public static boolean isAdmin() {
        UserView user = CURRENT_USER.get();
        return user != null && UserRole.ADMIN.getCode().equals(user.getRole());
    }

    /**
     * 清理当前线程的用户身份与会话令牌，防止线程池复用造成上下文串扰。
     */
    public static void clear() {
        CURRENT_USER.remove();
        CURRENT_SESSION_TOKEN.remove();
    }
}
