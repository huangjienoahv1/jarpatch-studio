package com.jarpatch.model;

/**
 * 嵌套 Jar 反编译候选项。
 * <p>
 * 预解析接口扫描 Jar/War 后返回该模型给前端，用户在导入前勾选需要反编译的嵌套 Jar。
 * 选中的路径会回传到导入接口，并由反编译服务写入 sources/nested-jars。
 * </p>
 *
 * @author 黄杰
 */
public class NestedJarCandidate {

    private String path;
    private String name;
    private boolean selected;
    private String reason;
    private int classCount;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }
}
