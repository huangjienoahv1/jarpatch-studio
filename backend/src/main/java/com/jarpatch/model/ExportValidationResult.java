package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 导出临时包结构校验结果。
 * <p>
 * ExportValidationService 在最终文件发布前写入检查项和错误项；ExportService 只在 valid
 * 为 true 时原子移动目标文件，失败结果持久化用于错误向导和历史诊断。
 * </p>
 *
 * @author 黄杰
 */
public class ExportValidationResult {

    private boolean valid;
    private List<String> checks = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

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
}
