package com.jarpatch.common;

/**
 * 后台任务状态枚举。
 * <p>
 * 导入、分析、编译和导出都会生成任务记录，前端通过任务状态接口和 WebSocket 日志
 * 追踪实际执行进度。
 * </p>
 *
 * @author 黄杰
 */
public enum TaskStatus {

    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败");

    private final String code;
    private final String label;

    /**
     * 创建任务状态枚举项。
     *
     * @param code  数据库存储编码
     * @param label 界面展示名称
     */
    TaskStatus(String code, String label) {
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
