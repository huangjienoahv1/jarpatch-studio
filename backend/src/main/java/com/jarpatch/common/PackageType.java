package com.jarpatch.common;

/**
 * 导入包类型枚举。
 * <p>
 * 分析服务和导出服务通过该枚举区分普通 Jar、Spring Boot Jar 和 War，
 * 防止不同结构的包被同一套打包逻辑误处理。
 * </p>
 *
 * @author 黄杰
 */
public enum PackageType {

    STANDARD_JAR("STANDARD_JAR", "普通 Jar"),
    SPRING_BOOT_JAR("SPRING_BOOT_JAR", "Spring Boot Jar"),
    WAR("WAR", "War 包");

    private final String code;
    private final String label;

    /**
     * 创建包类型枚举项。
     *
     * @param code  数据库存储编码
     * @param label 界面展示名称
     */
    PackageType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
