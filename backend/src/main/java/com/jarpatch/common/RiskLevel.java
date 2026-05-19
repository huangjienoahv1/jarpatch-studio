package com.jarpatch.common;

/**
 * 分析风险等级枚举。
 * <p>
 * 分析服务发现签名文件、嵌套 Jar、多版本目录或混淆迹象时，通过该枚举给前端展示
 * 明确风险等级。
 * </p>
 *
 * @author 黄杰
 */
public enum RiskLevel {

    INFO("INFO", "提示"),
    WARN("WARN", "警告"),
    HIGH("HIGH", "高风险");

    private final String code;
    private final String label;

    /**
     * 创建风险等级枚举项。
     *
     * @param code  数据库存储编码
     * @param label 界面展示名称
     */
    RiskLevel(String code, String label) {
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
