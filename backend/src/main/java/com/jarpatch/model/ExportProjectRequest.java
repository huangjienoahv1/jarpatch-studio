package com.jarpatch.model;

/**
 * 导出项目请求。
 * <p>
 * 前端可选传入导出路径；如果未传入，导出服务会写到项目工作区的 exports 目录。
 * </p>
 *
 * @author 黄杰
 */
public class ExportProjectRequest {

    private String outputPath;

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
