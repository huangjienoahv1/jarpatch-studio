package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.CreateUserRequest;
import com.jarpatch.model.ResetPasswordRequest;
import com.jarpatch.model.UpdateUserRequest;
import com.jarpatch.model.UserView;
import com.jarpatch.service.UserManagementService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 本地用户管理控制器（仅管理员角色可访问）。
 * <p>
 * 入口位于前端右上角用户菜单中的“用户管理”弹窗，权限由 {@link com.jarpatch.config.LocalApiSecurityFilter}
 * 强制校验；实际新增、编辑、启停、重置密码与删除均由 {@link UserManagementService} 落库执行。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserManagementService userManagementService;

    /**
     * 创建用户管理控制器。
     *
     * @param userManagementService 用户管理服务
     */
    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * 查询用户列表，支持按关键词、角色和状态过滤。
     *
     * @param keyword 可选关键词
     * @param role    可选角色过滤
     * @param status  可选状态过滤
     * @return 脱敏用户列表
     */
    @GetMapping
    public ApiResponse<List<UserView>> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(userManagementService.listUsers(keyword, role, status));
    }

    /**
     * 新增本地用户。
     *
     * @param request 新增用户请求
     * @return 新增后的脱敏用户视图
     */
    @PostMapping
    public ApiResponse<UserView> createUser(@RequestBody CreateUserRequest request) {
        return ApiResponse.success(userManagementService.createUser(request));
    }

    /**
     * 修改指定用户的显示名称、角色或启停状态。
     *
     * @param userId  目标用户 ID
     * @param request 更新请求
     * @return 更新后的脱敏用户视图
     */
    @PutMapping("/{userId}")
    public ApiResponse<UserView> updateUser(@PathVariable("userId") String userId,
                                            @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userManagementService.updateUser(userId, request));
    }

    /**
     * 管理员重置目标用户密码并吊销其现有会话。
     *
     * @param userId  目标用户 ID
     * @param request 重置密码请求
     * @return 成功提示文案
     */
    @PostMapping("/{userId}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable("userId") String userId,
                                             @RequestBody ResetPasswordRequest request) {
        String newPassword = request == null ? null : request.getNewPassword();
        userManagementService.resetPassword(userId, newPassword);
        return ApiResponse.success(JarPatchConstants.MESSAGE_USER_PASSWORD_RESET_SUCCESS);
    }

    /**
     * 删除指定用户及其会话。
     *
     * @param userId 目标用户 ID
     * @return 成功提示文案
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        userManagementService.deleteUser(userId);
        return ApiResponse.success(JarPatchConstants.MESSAGE_USER_DELETED);
    }
}
