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
