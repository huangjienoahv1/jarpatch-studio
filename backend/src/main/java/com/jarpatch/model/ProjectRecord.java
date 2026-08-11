package com.jarpatch.model;

/**
 * 本地项目记录。
 * <p>
 * 每次导入 Jar 或 War 都会创建一条项目记录，记录原始包路径、工作区路径、包类型
 * 和创建时间，项目列表接口直接返回该模型给前端首页。
 * </p>
 *
 * @author 黄杰
 */
public class ProjectRecord {

    private String id;
    private String name;
    private String packageType;
    private String originalPath;
    private String workspacePath;
    private Integer targetJavaVersion;
    private Integer classMajorVersion;
    private String javaVersionEvidence;
    private String workspaceCleanedAt;
    private String createdAt;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    /**
     * 获取原包目标 Java 版本。
     *
     * @return Java 特性版本
     */
    public Integer getTargetJavaVersion() {
        return targetJavaVersion;
    }

    /**
     * 设置原包目标 Java 版本。
     *
     * @param targetJavaVersion Java 特性版本
     */
    public void setTargetJavaVersion(Integer targetJavaVersion) {
        this.targetJavaVersion = targetJavaVersion;
    }

    /**
     * 获取检测到的 class major version。
     *
     * @return class major version
     */
    public Integer getClassMajorVersion() {
        return classMajorVersion;
    }

    /**
     * 设置检测到的 class major version。
     *
     * @param classMajorVersion class major version
     */
    public void setClassMajorVersion(Integer classMajorVersion) {
        this.classMajorVersion = classMajorVersion;
    }

    /**
     * 获取 Java 版本检测依据。
     *
     * @return 检测依据路径
     */
    public String getJavaVersionEvidence() {
        return javaVersionEvidence;
    }

    /**
     * 设置 Java 版本检测依据。
     *
     * @param javaVersionEvidence 检测依据路径
     */
    public void setJavaVersionEvidence(String javaVersionEvidence) {
        this.javaVersionEvidence = javaVersionEvidence;
    }

    /**
     * 获取独立工作区清理时间。
     *
     * @return 未清理时为 null
     */
    public String getWorkspaceCleanedAt() {
        return workspaceCleanedAt;
    }

    /**
     * 设置独立工作区清理时间。
     *
     * @param workspaceCleanedAt 清理时间
     */
    public void setWorkspaceCleanedAt(String workspaceCleanedAt) {
        this.workspaceCleanedAt = workspaceCleanedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
