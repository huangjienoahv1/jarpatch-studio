package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.UserRole;
import com.jarpatch.common.UserStatus;
import com.jarpatch.config.UserContext;
import com.jarpatch.model.CreateUserRequest;
import com.jarpatch.model.UpdateUserRequest;
import com.jarpatch.model.UserRecord;
import com.jarpatch.model.UserView;
import com.jarpatch.repository.UserRepository;
import com.jarpatch.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 本地用户管理业务服务。
 * <p>
 * 仅由管理员入口 {@link com.jarpatch.controller.UserController} 调用，负责用户的查询筛选、
 * 创建、资料与角色编辑、启停控制、管理员重置密码与删除；任何可能导致系统失去可用管理员
 * 或锁死当前管理员自身的操作都会在服务层被严格拦截。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class UserManagementService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{" + JarPatchConstants.USERNAME_MIN_LENGTH + ","
                    + JarPatchConstants.USERNAME_MAX_LENGTH + "}$");

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordHashService passwordHashService;
    private final AuthService authService;
    private final ClockService clockService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建用户管理服务。
     *
     * @param userRepository      用户仓储
     * @param sessionRepository   会话仓储
     * @param passwordHashService 密码哈希服务
     * @param authService         认证服务
     * @param clockService        时间服务
     * @param transactionTemplate 事务模板
     */
    public UserManagementService(UserRepository userRepository,
                                 UserSessionRepository sessionRepository,
                                 PasswordHashService passwordHashService,
                                 AuthService authService,
                                 ClockService clockService,
                                 TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHashService = passwordHashService;
        this.authService = authService;
        this.clockService = clockService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 查询全部用户，并支持按用户名/显示名关键词、角色和状态进行过滤。
     *
     * @param keyword 可选关键词
     * @param role    可选角色过滤
     * @param status  可选状态过滤
     * @return 脱敏用户列表
     */
    public List<UserView> listUsers(String keyword, String role, String status) {
        String normalizedKeyword = keyword == null ? JarPatchConstants.EMPTY_TEXT : keyword.trim().toLowerCase();
        String normalizedRole = role == null ? JarPatchConstants.EMPTY_TEXT : role.trim().toUpperCase();
        String normalizedStatus = status == null ? JarPatchConstants.EMPTY_TEXT : status.trim().toUpperCase();

        return userRepository.findAll().stream()
                .filter(user -> normalizedRole.isEmpty() || normalizedRole.equals(user.getRole()))
                .filter(user -> normalizedStatus.isEmpty() || normalizedStatus.equals(user.getStatus()))
                .filter(user -> normalizedKeyword.isEmpty()
                        || user.getUsername().toLowerCase().contains(normalizedKeyword)
                        || user.getDisplayName().toLowerCase().contains(normalizedKeyword))
                .map(UserRecord::toView)
                .collect(Collectors.toList());
    }

    /**
     * 创建新的本地账号并写入 SQLite。
     *
     * @param request 新增用户请求
     * @return 创建后的脱敏用户视图
     */
    public UserView createUser(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USERNAME_INVALID);
        }
        String username = validateUsername(request.getUsername());
        String displayName = validateDisplayName(request.getDisplayName());
        UserRole role = UserRole.fromCode(request.getRole());
        authService.validatePasswordLength(request.getPassword());

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USERNAME_EXISTS);
        }

        PasswordHashService.PasswordDigest digest = passwordHashService.hashPassword(request.getPassword());
        String now = clockService.now();
        String operator = UserContext.currentUsername();

        UserRecord record = new UserRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUsername(username);
        record.setDisplayName(displayName);
        record.setRole(role.getCode());
        record.setStatus(UserStatus.ENABLED.getCode());
        record.setPasswordHash(digest.hash());
        record.setPasswordSalt(digest.salt());
        record.setMustChangePassword(!Boolean.FALSE.equals(request.getMustChangePassword()));
        record.setCreator(operator);
        record.setUpdater(operator);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        userRepository.insert(record);
        return record.toView();
    }

    /**
     * 更新指定用户的显示名、角色和启停状态；禁用账号时立即吊销其所有活跃会话。
     *
     * @param userId  目标用户 ID
     * @param request 更新请求
     * @return 更新后的脱敏用户视图
     */
    public UserView updateUser(String userId, UpdateUserRequest request) {
        UserRecord target = requireUser(userId);
        if (request == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_DISPLAY_NAME_REQUIRED);
        }
        String displayName = validateDisplayName(request.getDisplayName());
        UserRole nextRole = UserRole.fromCode(request.getRole());
        UserStatus nextStatus = UserStatus.fromCode(request.getStatus());

        UserView currentUser = UserContext.currentUser();
        boolean isSelf = currentUser != null && target.getId().equals(currentUser.getId());
        if (isSelf && nextStatus == UserStatus.DISABLED) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_USER_CANNOT_DISABLE_SELF);
        }
        if (isSelf && nextRole != UserRole.ADMIN) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_USER_CANNOT_DEMOTE_SELF);
        }

        boolean wasEnabledAdmin = isEnabledAdmin(target);
        boolean willRemainEnabledAdmin = nextRole == UserRole.ADMIN && nextStatus == UserStatus.ENABLED;
        if (wasEnabledAdmin && !willRemainEnabledAdmin
                && userRepository.countEnabledAdmins() <= JarPatchConstants.MINIMUM_ACTIVE_ADMIN_COUNT) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_USER_LAST_ADMIN_PROTECTED);
        }

        String now = clockService.now();
        String operator = UserContext.currentUsername();
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.updateProfile(target.getId(), displayName, nextRole.getCode(),
                    nextStatus.getCode(), operator, now);
            if (nextStatus == UserStatus.DISABLED) {
                sessionRepository.deleteByUserId(target.getId());
            }
        });

        target.setDisplayName(displayName);
        target.setRole(nextRole.getCode());
        target.setStatus(nextStatus.getCode());
        target.setUpdatedAt(now);
        return target.toView();
    }

    /**
     * 管理员重置目标用户密码，强制其下次登录修改密码并立即踢出全部现有会话。
     *
     * @param userId      目标用户 ID
     * @param newPassword 新临时密码
     */
    public void resetPassword(String userId, String newPassword) {
        UserRecord target = requireUser(userId);
        authService.validatePasswordLength(newPassword);

        PasswordHashService.PasswordDigest digest = passwordHashService.hashPassword(newPassword);
        String now = clockService.now();
        String operator = UserContext.currentUsername();

        transactionTemplate.executeWithoutResult(status -> {
            userRepository.updatePassword(target.getId(), digest.hash(), digest.salt(), true, operator, now);
            sessionRepository.deleteByUserId(target.getId());
        });
    }

    /**
     * 删除指定用户；禁止删除当前登录账号及最后一个启用中的管理员。
     *
     * @param userId 目标用户 ID
     */
    public void deleteUser(String userId) {
        UserRecord target = requireUser(userId);
        UserView currentUser = UserContext.currentUser();
        if (currentUser != null && target.getId().equals(currentUser.getId())) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_USER_CANNOT_DELETE_SELF);
        }
        if (isEnabledAdmin(target)
                && userRepository.countEnabledAdmins() <= JarPatchConstants.MINIMUM_ACTIVE_ADMIN_COUNT) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_USER_LAST_ADMIN_PROTECTED);
        }
        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteByUserId(target.getId());
            userRepository.deleteById(target.getId());
        });
    }

    /**
     * 查询目标用户，不存在时抛出清晰业务错误。
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    private UserRecord requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_NOT_FOUND));
    }

    /**
     * 校验并返回规整后的用户名。
     *
     * @param rawUsername 原始输入用户名
     * @return 规整后的用户名
     */
    private String validateUsername(String rawUsername) {
        String normalized = rawUsername == null ? JarPatchConstants.EMPTY_TEXT : rawUsername.trim();
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USERNAME_INVALID);
        }
        return normalized;
    }

    /**
     * 校验并返回规整后的显示名称。
     *
     * @param rawDisplayName 原始显示名
     * @return 规整后的显示名
     */
    private String validateDisplayName(String rawDisplayName) {
        String normalized = rawDisplayName == null ? JarPatchConstants.EMPTY_TEXT : rawDisplayName.trim();
        if (normalized.isEmpty() || normalized.length() > JarPatchConstants.DISPLAY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_DISPLAY_NAME_REQUIRED);
        }
        return normalized;
    }

    /**
     * 判断目标账号当前是否属于启用中的管理员。
     *
     * @param user 用户实体
     * @return 启用中管理员返回 true
     */
    private boolean isEnabledAdmin(UserRecord user) {
        return UserRole.ADMIN.getCode().equals(user.getRole())
                && UserStatus.ENABLED.getCode().equals(user.getStatus());
    }
}
