package com.jarpatch.common;

/**
 * 系统用户账号状态枚举。
 * <p>
 * 用于控制本地用户是否允许登录和持有有效会话；禁用状态账号会由服务层立即吊销
 * 全部活跃会话并拒绝后续访问。
 * </p>
 *
 * @author 黄杰
 */
public enum UserStatus {

    /**
     * 账号启用中，允许正常登录和访问受权限控制的接口。
     */
    ENABLED("ENABLED", "已启用"),

    /**
     * 账号已禁用，拒绝登录并立即吊销全部已有会话。
     */
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String label;

    /**
     * 创建用户状态枚举项。
     *
     * @param code  持久化状态码
     * @param label 中文展示名称
     */
    UserStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 获取持久化状态码。
     *
     * @return 状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取中文展示名称。
     *
     * @return 中文名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 解析并校验外部传入的状态码。
     *
     * @param value 待解析状态码
     * @return 匹配的状态枚举
     */
    public static UserStatus fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_STATUS_INVALID);
        }
        String normalized = value.trim().toUpperCase();
        for (UserStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_STATUS_INVALID);
    }
}
