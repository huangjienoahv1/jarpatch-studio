package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.UserContext;
import com.jarpatch.model.AuthBootstrapStatus;
import com.jarpatch.model.ChangePasswordRequest;
import com.jarpatch.model.LoginRequest;
import com.jarpatch.model.LoginResult;
import com.jarpatch.model.UserView;
import com.jarpatch.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地用户登录与会话管理控制器。
 * <p>
 * 桌面端通过本控制器获取初始管理员提示状态、执行账密登录、恢复当前会话用户信息、
 * 修改本人密码以及退出登录；实际密码校验与会话落库由 {@link AuthService} 完成。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 本地认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 获取登录页引导状态（仅默认管理员未改密时显示初始账号提示）。
     *
     * @return 登录页引导状态
     */
    @GetMapping("/bootstrap")
    public ApiResponse<AuthBootstrapStatus> bootstrap() {
        return ApiResponse.success(authService.getBootstrapStatus());
    }

    /**
     * 使用用户名和密码登录，返回会话令牌和脱敏用户信息。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@RequestBody LoginRequest request) {
        String username = request == null ? null : request.getUsername();
        String password = request == null ? null : request.getPassword();
        return ApiResponse.success(authService.login(username, password));
    }

    /**
     * 查询当前会话对应的已认证用户信息。
     *
     * @return 当前用户脱敏视图
     */
    @GetMapping("/me")
    public ApiResponse<UserView> currentUser() {
        return ApiResponse.success(UserContext.currentUser());
    }

    /**
     * 退出当前会话并销毁后端令牌记录。
     *
     * @return 退出提示文案
     */
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        authService.logout(UserContext.currentSessionToken());
        return ApiResponse.success(JarPatchConstants.MESSAGE_AUTH_LOGOUT_SUCCESS);
    }

    /**
     * 当前登录用户修改本人密码（同时适用于首次登录强制改密）。
     *
     * @param request 改密请求
     * @return 更新后的脱敏用户视图
     */
    @PostMapping("/change-password")
    public ApiResponse<UserView> changePassword(@RequestBody ChangePasswordRequest request) {
        String currentPassword = request == null ? null : request.getCurrentPassword();
        String newPassword = request == null ? null : request.getNewPassword();
        return ApiResponse.success(authService.changeOwnPassword(currentPassword, newPassword));
    }
}
