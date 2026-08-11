package com.jarpatch.model;

/**
 * 导出项目请求。
 * <p>
 * 前端可选传入导出路径；如果未传入，导出服务会写到项目工作区的 exports 目录。
 * </p>
 *
 * @author 黄杰
 */
public class ExportProjectRequest {

    private String outputPath;
    private String taskId;
    private String signaturePolicy;

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * 获取用户明确选择的签名处理策略。
     *
     * @return 签名策略码
     */
    public String getSignaturePolicy() {
        return signaturePolicy;
    }

    /**
     * 设置用户明确选择的签名处理策略。
     *
     * @param signaturePolicy 签名策略码
     */
    public void setSignaturePolicy(String signaturePolicy) {
        this.signaturePolicy = signaturePolicy;
    }
}
