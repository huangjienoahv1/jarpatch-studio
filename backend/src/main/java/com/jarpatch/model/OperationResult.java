package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 编译和导出操作结果。
 * <p>
 * 编译接口和导出接口用该模型返回任务 ID、输出路径、修改清单和执行日志摘要。
 * </p>
 *
 * @author 黄杰
 */
public class OperationResult {

    private String taskId;
    private String outputPath;
    private List<String> changedFiles = new ArrayList<>();
    private String message;
    private ExportValidationResult validation;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取导出结构校验结果。
     *
     * @return 校验结果
     */
    public ExportValidationResult getValidation() {
        return validation;
    }

    /**
     * 设置导出结构校验结果。
     *
     * @param validation 校验结果
     */
    public void setValidation(ExportValidationResult validation) {
        this.validation = validation;
    }
}
