package com.jarpatch.model;

/**
 * 保存文件内容请求。
 * <p>
 * 前端编辑 Java 源码或文本资源后，通过该模型把相对路径和新内容传给后端保存接口，
 * 保存成功后后端会写入修改记录。
 * </p>
 *
 * @author 黄杰
 */
public class SaveContentRequest {

    private String path;
    private String content;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
