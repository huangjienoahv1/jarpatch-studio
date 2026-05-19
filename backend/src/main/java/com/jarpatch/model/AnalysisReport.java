package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 包结构分析报告。
 * <p>
 * 分析接口返回该模型给前端，报告包含包类型、入口类、Manifest 状态、依赖列表、
 * 修改文件和导出风险。
 * </p>
 *
 * @author 黄杰
 */
public class AnalysisReport {

    private String projectId;
    private String packageType;
    private String entryClass;
    private boolean manifestExists;
    private boolean springBootLayout;
    private boolean warLayout;
    private int classCount;
    private int dependencyCount;
    private List<String> dependencies = new ArrayList<>();
    private List<String> modifiedFiles = new ArrayList<>();
    private List<RiskItem> risks = new ArrayList<>();

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getEntryClass() {
        return entryClass;
    }

    public void setEntryClass(String entryClass) {
        this.entryClass = entryClass;
    }

    public boolean isManifestExists() {
        return manifestExists;
    }

    public void setManifestExists(boolean manifestExists) {
        this.manifestExists = manifestExists;
    }

    public boolean isSpringBootLayout() {
        return springBootLayout;
    }

    public void setSpringBootLayout(boolean springBootLayout) {
        this.springBootLayout = springBootLayout;
    }

    public boolean isWarLayout() {
        return warLayout;
    }

    public void setWarLayout(boolean warLayout) {
        this.warLayout = warLayout;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public int getDependencyCount() {
        return dependencyCount;
    }

    public void setDependencyCount(int dependencyCount) {
        this.dependencyCount = dependencyCount;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getModifiedFiles() {
        return modifiedFiles;
    }

    public void setModifiedFiles(List<String> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    public List<RiskItem> getRisks() {
        return risks;
    }

    public void setRisks(List<RiskItem> risks) {
        this.risks = risks;
    }
}
