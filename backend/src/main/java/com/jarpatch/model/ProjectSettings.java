package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目级设置。
 * <p>
 * 项目设置接口返回原包检测出的目标 Java 版本，以及该项目独立的默认导出目录、
 * 推荐嵌套 Jar、可编辑文件上限和界面偏好。
 * </p>
 *
 * @author 黄杰
 */
public class ProjectSettings {

    private String projectId;
    private Integer targetJavaVersion;
    private String defaultExportDirectory;
    private List<String> selectedNestedJars = new ArrayList<>();
    private long maxEditableFileBytes;
    private String uiPreferencesJson;
    private String updatedAt;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Integer getTargetJavaVersion() {
        return targetJavaVersion;
    }

    public void setTargetJavaVersion(Integer targetJavaVersion) {
        this.targetJavaVersion = targetJavaVersion;
    }

    public String getDefaultExportDirectory() {
        return defaultExportDirectory;
    }

    public void setDefaultExportDirectory(String defaultExportDirectory) {
        this.defaultExportDirectory = defaultExportDirectory;
    }

    public List<String> getSelectedNestedJars() {
        return selectedNestedJars;
    }

    public void setSelectedNestedJars(List<String> selectedNestedJars) {
        this.selectedNestedJars = selectedNestedJars;
    }

    public long getMaxEditableFileBytes() {
        return maxEditableFileBytes;
    }

    public void setMaxEditableFileBytes(long maxEditableFileBytes) {
        this.maxEditableFileBytes = maxEditableFileBytes;
    }

    public String getUiPreferencesJson() {
        return uiPreferencesJson;
    }

    public void setUiPreferencesJson(String uiPreferencesJson) {
        this.uiPreferencesJson = uiPreferencesJson;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
