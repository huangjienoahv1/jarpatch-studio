package com.jarpatch.common;

/**
 * 修改后导出的签名处理策略。
 * <p>
 * 默认策略只允许未修改包保留原签名；发生源码、资源或 class 变化时必须由用户明确选择
 * 移除失效签名，当前版本不在没有密钥配置的情况下猜测或自动重新签名。
 * </p>
 *
 * @author 黄杰
 */
public enum SignaturePolicy {

    PRESERVE_ONLY_UNMODIFIED,
    REMOVE_INVALID_SIGNATURES;

    /**
     * 解析接口策略码，空值使用安全的保留且阻止修改默认策略。
     *
     * @param value 策略码
     * @return 签名策略
     */
    public static SignaturePolicy from(String value) {
        if (value == null || value.isBlank()) {
            return PRESERVE_ONLY_UNMODIFIED;
        }
        try {
            return SignaturePolicy.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_SIGNATURE_POLICY_UNSUPPORTED
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + value, exception);
        }
    }
}
