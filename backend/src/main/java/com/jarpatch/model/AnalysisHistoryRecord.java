package com.jarpatch.model;

/**
 * 项目结构分析历史快照。
 * <p>
 * 历史查询接口从 analysis_reports 读取不可变报告，并把完整分析结果与生成时间返回前端。
 * </p>
 *
 * @author 黄杰
 */
public class AnalysisHistoryRecord {

    private String id;
    private AnalysisReport report;
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AnalysisReport getReport() {
        return report;
    }

    public void setReport(AnalysisReport report) {
        this.report = report;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
