package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 密码哈希与会话令牌摘要服务。
 * <p>
 * 使用 JDK 17 标准密码学库（PBKDF2WithHmacSHA256 + SecureRandom 随机盐）实现用户口令单向存储与
 * 常量时间校验，同时生成高熵会话令牌并计算 SHA-256 摘要供数据库持久化。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class PasswordHashService {

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String SHA_256_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 密码加盐哈希结果。
     *
     * @param hash Base64 编码的派生密钥摘要
     * @param salt Base64 编码的随机盐值
     */
    public record PasswordDigest(String hash, String salt) {
    }

    /**
     * 为明文密码生成随机盐与 PBKDF2 摘要。
     *
     * @param rawPassword 明文密码
     * @return 加盐哈希结果
     */
    public PasswordDigest hashPassword(String rawPassword) {
        byte[] salt = new byte[JarPatchConstants.PASSWORD_SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = deriveKey(rawPassword, salt);
        return new PasswordDigest(
                Base64.getEncoder().encodeToString(derived),
                Base64.getEncoder().encodeToString(salt));
    }

    /**
     * 使用常量时间比较校验明文密码与已存储加盐哈希是否匹配。
     *
     * @param rawPassword  待校验明文密码
     * @param expectedHash 已存储 Base64 哈希
     * @param saltBase64   已存储 Base64 盐值
     * @return 密码一致时返回 true
     */
    public boolean verifyPassword(String rawPassword, String expectedHash, String saltBase64) {
        if (rawPassword == null || expectedHash == null || saltBase64 == null) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] expectedBytes = Base64.getDecoder().decode(expectedHash);
        byte[] actualBytes = deriveKey(rawPassword, salt);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    /**
     * 生成高熵 URL 安全随机用户会话令牌。
     *
     * @return 随机明文会话令牌
     */
    public String generateSessionToken() {
        byte[] bytes = new byte[JarPatchConstants.SESSION_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算会话令牌的十六进制 SHA-256 摘要，避免数据库持久化明文会话令牌。
     *
     * @param rawToken 明文会话令牌
     * @return 小写十六进制 SHA-256 摘要
     */
    public String hashSessionToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_SHA256_UNAVAILABLE, exception);
        }
    }

    /**
     * 执行 PBKDF2WithHmacSHA256 密钥派生，并在结束后清空临时字符数组。
     *
     * @param rawPassword 明文密码
     * @param salt        随机盐字节
     * @return 派生密钥字节数组
     */
    private byte[] deriveKey(String rawPassword, byte[] salt) {
        char[] passwordChars = rawPassword.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt,
                JarPatchConstants.PASSWORD_HASH_ITERATIONS,
                JarPatchConstants.PASSWORD_HASH_KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_PASSWORD_ALGORITHM_UNAVAILABLE, exception);
        } finally {
            spec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }
}
