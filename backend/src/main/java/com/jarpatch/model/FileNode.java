package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 前端文件树节点。
 * <p>
 * 文件树接口用该模型描述解压工作区中的目录和文件，前端根据 editable 字段决定是否
 * 允许用户打开编辑器修改内容。
 * </p>
 *
 * @author 黄杰
 */
public class FileNode {

    private String name;
    private String path;
    private String kind;
    private boolean editable;
    private boolean hasChildren;
    private boolean childrenLoaded;
    private List<FileNode> children = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public boolean isChildrenLoaded() {
        return childrenLoaded;
    }

    public void setChildrenLoaded(boolean childrenLoaded) {
        this.childrenLoaded = childrenLoaded;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }
}
