package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 错误排查向导条目。
 * <p>
 * 系统错误向导接口按该模型返回错误类别、典型现象、检查项和处理动作，前端只负责展示。
 * </p>
 *
 * @author 黄杰
 */
public class ErrorGuideItem {

    private String code;
    private String title;
    private String symptom;
    private List<String> checks = new ArrayList<>();
    private List<String> actions = new ArrayList<>();

    public ErrorGuideItem() {
    }

    public ErrorGuideItem(String code, String title, String symptom, List<String> checks, List<String> actions) {
        this.code = code;
        this.title = title;
        this.symptom = symptom;
        this.checks = checks;
        this.actions = actions;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public List<String> getChecks() {
        return checks;
    }

    public void setChecks(List<String> checks) {
        this.checks = checks;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }
}
