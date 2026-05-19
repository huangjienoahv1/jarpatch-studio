package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入前预解析结果。
 * <p>
 * 前端选择 Jar/War 后先调用预解析接口，后端读取包类型、pom.xml 模块信息和嵌套 Jar 候选项。
 * 前端展示该结果让用户确认反编译范围，再调用正式导入接口。
 * </p>
 *
 * @author 黄杰
 */
public class ProjectImportInspection {

    private String filePath;
    private String packageType;
    private List<String> pomModules = new ArrayList<>();
    private List<NestedJarCandidate> candidates = new ArrayList<>();
    private int selectedCount;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public List<String> getPomModules() {
        return pomModules;
    }

    public void setPomModules(List<String> pomModules) {
        this.pomModules = pomModules == null ? new ArrayList<>() : pomModules;
    }

    public List<NestedJarCandidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<NestedJarCandidate> candidates) {
        this.candidates = candidates == null ? new ArrayList<>() : candidates;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }
}
