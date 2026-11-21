package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.UserRole;
import com.jarpatch.common.UserStatus;
import com.jarpatch.config.UserContext;
import com.jarpatch.model.AuthBootstrapStatus;
import com.jarpatch.model.LoginResult;
import com.jarpatch.model.UserRecord;
import com.jarpatch.model.UserSessionRecord;
import com.jarpatch.model.UserView;
import com.jarpatch.repository.DatabaseInitializer;
import com.jarpatch.repository.UserRepository;
import com.jarpatch.repository.UserSessionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地用户认证与会话生命周期服务。
 * <p>
 * 入口包括 {@link com.jarpatch.controller.AuthController}、{@link com.jarpatch.config.LocalApiSecurityFilter}
 * 与 {@link com.jarpatch.config.LocalAccessHandshakeInterceptor}；启动时在数据库迁移完成后自动预置
 * 默认管理员账号（要求首次登录改密），登录成功后将令牌摘要落库到 user_sessions。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordHashService passwordHashService;
    private final ClockService clockService;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, LoginFailureTracker> failureTrackers = new ConcurrentHashMap<>();

    /**
     * 创建认证服务；通过显式注入 {@link DatabaseInitializer} 确保表结构迁移先于默认管理员预置完成。
     *
     * @param databaseInitializer 数据库顺序迁移器
     * @param userRepository      用户仓储
     * @param sessionRepository   用户会话仓储
     * @param passwordHashService 密码哈希服务
     * @param clockService        中国时区时间服务
     * @param transactionTemplate 事务模板
     */
    public AuthService(DatabaseInitializer databaseInitializer,
                       UserRepository userRepository,
                       UserSessionRepository sessionRepository,
                       PasswordHashService passwordHashService,
                       ClockService clockService,
                       TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHashService = passwordHashService;
        this.clockService = clockService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 启动时确保至少存在预置默认管理员，并顺带清理历史过期会话。
     */
    @PostConstruct
    public void initializeDefaultAdmin() {
        String now = clockService.now();
        sessionRepository.deleteExpired(now);
        if (userRepository.countAll() > JarPatchConstants.EMPTY_SIZE) {
            return;
        }
        // 首次安装或旧库升级后无任何账号时，预置默认管理员 admin 并强制首次登录修改密码。
        PasswordHashService.PasswordDigest digest = passwordHashService.hashPassword(
                JarPatchConstants.DEFAULT_ADMIN_INITIAL_PASSWORD);
        UserRecord admin = new UserRecord();
        admin.setId(UUID.randomUUID().toString());
        admin.setUsername(JarPatchConstants.DEFAULT_ADMIN_USERNAME);
        admin.setDisplayName(JarPatchConstants.DEFAULT_ADMIN_DISPLAY_NAME);
        admin.setRole(UserRole.ADMIN.getCode());
        admin.setStatus(UserStatus.ENABLED.getCode());
        admin.setPasswordHash(digest.hash());
        admin.setPasswordSalt(digest.salt());
        admin.setMustChangePassword(true);
        admin.setCreator(JarPatchConstants.DEFAULT_AUDITOR);
        admin.setUpdater(JarPatchConstants.DEFAULT_AUDITOR);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userRepository.insert(admin);
    }

    /**
     * 返回登录页启动引导信息；仅在默认 admin 尚未修改初始密码时展示首次登录指引。
     *
     * @return 登录页引导状态
     */
    public AuthBootstrapStatus getBootstrapStatus() {
        AuthBootstrapStatus status = new AuthBootstrapStatus();
        Optional<UserRecord> admin = userRepository.findByUsername(JarPatchConstants.DEFAULT_ADMIN_USERNAME);
        boolean showHint = admin.isPresent()
                && admin.get().isMustChangePassword()
                && admin.get().getLastLoginAt() == null
                && passwordHashService.verifyPassword(
                        JarPatchConstants.DEFAULT_ADMIN_INITIAL_PASSWORD,
                        admin.get().getPasswordHash(),
                        admin.get().getPasswordSalt());
        status.setDefaultAdminHintVisible(showHint);
        if (showHint) {
            status.setDefaultAdminUsername(JarPatchConstants.DEFAULT_ADMIN_USERNAME);
            status.setDefaultAdminInitialPassword(JarPatchConstants.DEFAULT_ADMIN_INITIAL_PASSWORD);
        }
        return status;
    }

    /**
     * 校验本地用户名与密码，并在通过后签发持久化会话令牌。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 会话令牌与当前用户脱敏视图
     */
    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_AUTH_CREDENTIALS_REQUIRED);
        }
        String normalizedUsername = username.trim().toLowerCase();
        checkLoginLock(normalizedUsername);

        Optional<UserRecord> found = userRepository.findByUsername(normalizedUsername);
        if (found.isEmpty()) {
            recordLoginFailure(normalizedUsername);
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_AUTH_INVALID_CREDENTIALS);
        }
        UserRecord user = found.get();
        if (!UserStatus.ENABLED.getCode().equals(user.getStatus())) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_AUTH_USER_DISABLED);
        }
        if (!passwordHashService.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt())) {
            recordLoginFailure(normalizedUsername);
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_AUTH_INVALID_CREDENTIALS);
        }

        // 登录成功后清除错误计数，在事务内写入会话摘要并刷新最近登录时间。
        failureTrackers.remove(normalizedUsername);
        String rawToken = passwordHashService.generateSessionToken();
        String tokenHash = passwordHashService.hashSessionToken(rawToken);
        String now = clockService.now();
        String expiresAt = clockService.plusDays(JarPatchConstants.SESSION_TTL_DAYS);

        UserSessionRecord session = new UserSessionRecord();
        session.setTokenHash(tokenHash);
        session.setUserId(user.getId());
        session.setExpiresAt(expiresAt);
        session.setCreatedAt(now);
        session.setLastActiveAt(now);

        transactionTemplate.executeWithoutResult(status -> {
            sessionRepository.deleteExpired(now);
            sessionRepository.insert(session);
            userRepository.updateLastLoginAt(user.getId(), now);
        });

        user.setLastLoginAt(now);
        LoginResult result = new LoginResult();
        result.setSessionToken(rawToken);
        result.setExpiresAt(expiresAt);
        result.setUser(user.toView());
        return result;
    }

    /**
     * 校验请求携带的会话令牌并返回对应启用中用户；过期或已禁用账号会清理会话并返回空。
     *
     * @param rawSessionToken 请求头或查询参数中的明文会话令牌
     * @return 已认证用户视图；令牌无效时返回空
     */
    public Optional<UserView> authenticateSession(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = passwordHashService.hashSessionToken(rawSessionToken.trim());
        Optional<UserSessionRecord> sessionOpt = sessionRepository.findByTokenHash(tokenHash);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        UserSessionRecord session = sessionOpt.get();
        if (clockService.isExpired(session.getExpiresAt())) {
            sessionRepository.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }
        Optional<UserRecord> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty() || !UserStatus.ENABLED.getCode().equals(userOpt.get().getStatus())) {
            sessionRepository.deleteByTokenHash(tokenHash);
            return Optional.empty();
        }
        sessionRepository.touch(tokenHash, clockService.now());
        return Optional.of(userOpt.get().toView());
    }

    /**
     * 退出当前登录会话，从 SQLite 删除对应令牌摘要。
     *
     * @param rawSessionToken 当前会话令牌
     */
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }
        String tokenHash = passwordHashService.hashSessionToken(rawSessionToken.trim());
        sessionRepository.deleteByTokenHash(tokenHash);
    }

    /**
     * 修改当前已登录用户的密码，清除强制改密状态并吊销除当前外的其他活跃会话。
     *
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     * @return 更新后的用户脱敏视图
     */
    public UserView changeOwnPassword(String currentPassword, String newPassword) {
        UserView currentUser = UserContext.currentUser();
        if (currentUser == null) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_AUTH_SESSION_REQUIRED);
        }
        validatePasswordLength(newPassword);
        UserRecord record = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_NOT_FOUND));
        if (currentPassword == null || !passwordHashService.verifyPassword(
                currentPassword, record.getPasswordHash(), record.getPasswordSalt())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_AUTH_OLD_PASSWORD_INVALID);
        }
        if (passwordHashService.verifyPassword(newPassword, record.getPasswordHash(), record.getPasswordSalt())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_AUTH_NEW_PASSWORD_SAME);
        }

        PasswordHashService.PasswordDigest nextDigest = passwordHashService.hashPassword(newPassword);
        String now = clockService.now();
        String currentToken = UserContext.currentSessionToken();
        String currentTokenHash = currentToken == null ? null : passwordHashService.hashSessionToken(currentToken);

        transactionTemplate.executeWithoutResult(status -> {
            userRepository.updatePassword(record.getId(), nextDigest.hash(), nextDigest.salt(),
                    false, record.getUsername(), now);
            if (currentTokenHash != null) {
                sessionRepository.deleteOtherSessions(record.getId(), currentTokenHash);
            }
        });

        record.setMustChangePassword(false);
        record.setUpdatedAt(now);
        return record.toView();
    }

    /**
     * 校验密码长度是否满足安全下限与上限。
     *
     * @param password 待校验密码
     */
    public void validatePasswordLength(String password) {
        if (password == null
                || password.length() < JarPatchConstants.PASSWORD_MIN_LENGTH
                || password.length() > JarPatchConstants.PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PASSWORD_LENGTH_INVALID);
        }
    }

    /**
     * 检查目标用户名是否正处于连续输错密码后的临时锁定窗口内。
     *
     * @param normalizedUsername 归一化小写用户名
     */
    private void checkLoginLock(String normalizedUsername) {
        LoginFailureTracker tracker = failureTrackers.get(normalizedUsername);
        if (tracker == null) {
            return;
        }
        Instant now = Instant.now();
        if (tracker.lockedUntil != null) {
            if (now.isBefore(tracker.lockedUntil)) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_AUTH_LOGIN_LOCKED);
            }
            failureTrackers.remove(normalizedUsername);
        }
    }

    /**
     * 累加失败尝试次数并在达到阈值后设置临时锁定。
     *
     * @param normalizedUsername 归一化小写用户名
     */
    private void recordLoginFailure(String normalizedUsername) {
        failureTrackers.compute(normalizedUsername, (key, existing) -> {
            LoginFailureTracker next = existing == null ? new LoginFailureTracker() : existing;
            next.failureCount++;
            if (next.failureCount >= JarPatchConstants.LOGIN_MAX_FAILURES) {
                next.lockedUntil = Instant.now().plusSeconds(JarPatchConstants.LOGIN_LOCK_SECONDS);
            }
            return next;
        });
    }

    /**
     * 单个用户名的内存失败计数与临时锁定状态。
     */
    private static class LoginFailureTracker {
        private int failureCount;
        private Instant lockedUntil;
    }
}
