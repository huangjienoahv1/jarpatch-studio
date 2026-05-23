package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.ExportRecordRepository;
import com.jarpatch.repository.FileChangeRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 项目导出服务。
 * <p>
 * 导出入口来自 /api/projects/{id}/export，实际执行点是 ArchiveService.zipDirectory，结果写入
 * exports 目录或用户指定路径，同时写入 SQLite export_records 表。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ExportService {

    private static final String TASK_TYPE_EXPORT = "EXPORT";
    private static final String PATCHED_SUFFIX = "-patched";

    private final WorkspaceService workspaceService;
    private final ArchiveService archiveService;
    private final FileChangeRepository fileChangeRepository;
    private final ExportRecordRepository exportRecordRepository;
    private final TaskService taskService;
    private final ClockService clockService;

    /**
     * 创建导出服务。
     *
     * @param workspaceService       工作区服务
     * @param archiveService         压缩包服务
     * @param fileChangeRepository   修改记录仓储
     * @param exportRecordRepository 导出记录仓储
     * @param taskService            任务服务
     * @param clockService           时间服务
     */
    public ExportService(WorkspaceService workspaceService,
                         ArchiveService archiveService,
                         FileChangeRepository fileChangeRepository,
                         ExportRecordRepository exportRecordRepository,
                         TaskService taskService,
                         ClockService clockService) {
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
        this.fileChangeRepository = fileChangeRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.taskService = taskService;
        this.clockService = clockService;
    }

    /**
     * 导出修改后的 Jar 或 War。
     *
     * @param project    项目记录
     * @param outputPath 用户指定输出路径，可为空
     * @return 导出结果
     * @throws IOException 打包失败时抛出
     */
    public OperationResult export(ProjectRecord project, String outputPath) throws IOException {
        return export(project, outputPath, null);
    }

    /**
     * 导出修改后的 Jar 或 War。
     * <p>
     * 前端先创建任务并连接 WebSocket 后，把 taskId 传入这里；导出仍然在本机同步执行，
     * 但进度日志和取消状态会实时反馈给前端。
     * </p>
     *
     * @param project    项目记录
     * @param outputPath 用户指定输出路径，可为空
     * @param taskId     预创建任务 ID，可为空
     * @return 导出结果
     * @throws IOException 打包失败时抛出
     */
    public OperationResult export(ProjectRecord project, String outputPath, String taskId) throws IOException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE_EXPORT, "开始导出修改后的包");
        try {
            List<String> changedFiles = fileChangeRepository.findPaths(project.getId());
            Path outputFile = resolveOutputPath(project, outputPath);

            taskService.running(task, 30, "准备导出文件: " + outputFile);
            taskService.ensureNotCancelled(task.getId());
            boolean springBootLayout = "SPRING_BOOT_JAR".equals(project.getPackageType());

            taskService.running(task, 70, "重新打包 extracted 目录");
            archiveService.zipDirectory(workspaceService.extractedDir(project), outputFile, springBootLayout, () -> taskService.isCancelled(task.getId()));

            exportRecordRepository.insert(project.getId(), outputFile.toString(), clockService.now());
            OperationResult result = new OperationResult();
            result.setTaskId(task.getId());
            result.setOutputPath(outputFile.toString());
            result.setChangedFiles(changedFiles);
            result.setMessage("导出完成");
            taskService.success(task, "导出完成，结果已写入: " + outputFile);
            return result;
        } catch (IllegalStateException e) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(e.getMessage())) {
                throw e;
            }
            taskService.failed(task, "导出失败: " + e.getMessage());
            throw e;
        } catch (RuntimeException | IOException e) {
            taskService.failed(task, "导出失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 解析导出路径。
     *
     * @param project    项目记录
     * @param outputPath 用户指定路径
     * @return 输出文件路径
     */
    private Path resolveOutputPath(ProjectRecord project, String outputPath) {
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            return Paths.get(outputPath).toAbsolutePath().normalize();
        }
        String name = project.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String extension = dotIndex > 0 ? name.substring(dotIndex) : "." + JarPatchConstants.JAR_EXTENSION;
        return workspaceService.exportDir(project).resolve(baseName + PATCHED_SUFFIX + extension).normalize();
    }
}
