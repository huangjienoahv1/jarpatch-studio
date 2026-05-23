package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入项目请求。
 * <p>
 * Electron 前端选择 Jar 或 War 后，把本机文件路径和用户确认需要反编译的嵌套 Jar
 * 通过该模型传给后端导入接口。
 * </p>
 *
 * @author 黄杰
 */
public class ImportProjectRequest {

    private String filePath;
    private List<String> selectedNestedJars = new ArrayList<>();
    private String taskId;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<String> getSelectedNestedJars() {
        return selectedNestedJars;
    }

    public void setSelectedNestedJars(List<String> selectedNestedJars) {
        this.selectedNestedJars = selectedNestedJars == null ? new ArrayList<>() : selectedNestedJars;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
