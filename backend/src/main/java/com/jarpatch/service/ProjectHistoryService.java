package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectHistoryView;
import com.jarpatch.repository.AnalysisReportRepository;
import com.jarpatch.repository.ExportValidationRepository;
import com.jarpatch.repository.OperationJournalRepository;
import com.jarpatch.repository.ProjectRepository;
import org.springframework.stereotype.Service;

/**
 * 项目历史聚合查询服务。
 * <p>
 * HTTP 入口按项目读取分析报告、导出校验和业务操作日志，实际数据来自 SQLite，
 * 本服务只执行有上限的只读聚合，不改变工作区或历史记录。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ProjectHistoryService {

    private static final int HISTORY_LIMIT = 100;

    private final ProjectRepository projectRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final ExportValidationRepository exportValidationRepository;
    private final OperationJournalRepository operationJournalRepository;

    /**
     * 创建项目历史服务。
     *
     * @param projectRepository 项目仓储
     * @param analysisReportRepository 分析报告仓储
     * @param exportValidationRepository 导出校验仓储
     * @param operationJournalRepository 操作日志仓储
     */
    public ProjectHistoryService(ProjectRepository projectRepository,
                                 AnalysisReportRepository analysisReportRepository,
                                 ExportValidationRepository exportValidationRepository,
                                 OperationJournalRepository operationJournalRepository) {
        this.projectRepository = projectRepository;
        this.analysisReportRepository = analysisReportRepository;
        this.exportValidationRepository = exportValidationRepository;
        this.operationJournalRepository = operationJournalRepository;
    }

    /**
     * 查询项目最近历史数据。
     *
     * @param projectId 项目 ID
     * @return 三类历史聚合视图
     */
    public ProjectHistoryView get(String projectId) {
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_NOT_FOUND);
        }
        ProjectHistoryView view = new ProjectHistoryView();
        view.setAnalyses(analysisReportRepository.findByProjectId(projectId, HISTORY_LIMIT));
        view.setExportValidations(exportValidationRepository.findByProjectId(projectId, HISTORY_LIMIT));
        view.setOperations(operationJournalRepository.findByProjectId(projectId, HISTORY_LIMIT));
        return view;
    }
}
