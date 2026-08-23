package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目导出结构校验历史记录。
 * <p>
 * 每条记录对应一次导出发布前校验，保留目标路径、通过状态、检查项、错误和发生时间。
 * </p>
 *
 * @author 黄杰
 */
public class ExportValidationHistoryRecord {

    private String id;
    private String outputPath;
    private boolean valid;
    private List<String> checks = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getChecks() {
        return checks;
    }

    public void setChecks(List<String> checks) {
        this.checks = checks;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
