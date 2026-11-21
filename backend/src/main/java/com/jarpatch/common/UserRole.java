package com.jarpatch.common;

/**
 * 系统用户角色枚举。
 * <p>
 * 用于区分管理员与普通用户的接口访问权限和前端菜单可见性；控制器、安全过滤器和
 * 用户管理服务统一通过该枚举进行角色判断，避免散落硬编码角色字符串。
 * </p>
 *
 * @author 黄杰
 */
public enum UserRole {

    /**
     * 系统管理员，具备用户管理、开发能力配置及工作区清理等高权限能力。
     */
    ADMIN("ADMIN", "管理员"),

    /**
     * 普通研发用户，具备项目导入、编辑、分析、编译、导出与运行时诊断能力。
     */
    USER("USER", "普通用户");

    private final String code;
    private final String label;

    /**
     * 创建用户角色枚举项。
     *
     * @param code  持久化角色码
     * @param label 中文展示名称
     */
    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 获取持久化角色码。
     *
     * @return 角色码
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
     * 解析并校验外部传入的角色码。
     *
     * @param value 待解析角色码
     * @return 匹配的角色枚举
     */
    public static UserRole fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_ROLE_INVALID);
        }
        String normalized = value.trim().toUpperCase();
        for (UserRole role : values()) {
            if (role.code.equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException(JarPatchConstants.MESSAGE_USER_ROLE_INVALID);
    }
}
