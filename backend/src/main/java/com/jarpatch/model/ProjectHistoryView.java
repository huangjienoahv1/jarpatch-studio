package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目可查询历史聚合视图。
 * <p>
 * 项目历史接口一次返回结构分析快照、导出校验结果和统一操作时间线，前端只做展示，
 * 不修改任何历史数据。
 * </p>
 *
 * @author 黄杰
 */
public class ProjectHistoryView {

    private List<AnalysisHistoryRecord> analyses = new ArrayList<>();
    private List<ExportValidationHistoryRecord> exportValidations = new ArrayList<>();
    private List<OperationJournalRecord> operations = new ArrayList<>();

    public List<AnalysisHistoryRecord> getAnalyses() {
        return analyses;
    }

    public void setAnalyses(List<AnalysisHistoryRecord> analyses) {
        this.analyses = analyses;
    }

    public List<ExportValidationHistoryRecord> getExportValidations() {
        return exportValidations;
    }

    public void setExportValidations(List<ExportValidationHistoryRecord> exportValidations) {
        this.exportValidations = exportValidations;
    }

    public List<OperationJournalRecord> getOperations() {
        return operations;
    }

    public void setOperations(List<OperationJournalRecord> operations) {
        this.operations = operations;
    }
}
