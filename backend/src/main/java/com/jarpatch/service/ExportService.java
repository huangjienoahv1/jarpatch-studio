package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import com.jarpatch.common.SignaturePolicy;
import com.jarpatch.model.DiffReport;
import com.jarpatch.model.ExportValidationResult;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.ExportRecordRepository;
import com.jarpatch.repository.FileChangeRepository;
import com.jarpatch.repository.CompiledArtifactRepository;
import com.jarpatch.repository.ExportValidationRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
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
    private static final String TEMP_FILE_PREFIX = ".";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final String MESSAGE_EXPORT_START = "开始导出修改后的包";
    private static final String MESSAGE_EXPORT_PRECHECK = "执行导出前结构分析和基线差异确认";
    private static final String MESSAGE_EXPORT_PREPARE = "准备导出文件: ";
    private static final String MESSAGE_EXPORT_WRITE_TEMPORARY = "写入同目录临时导出文件";
    private static final String MESSAGE_EXPORT_VALIDATE = "校验 Manifest、布局、资源、class、嵌套 Jar 和签名";
    private static final String MESSAGE_EXPORT_PUBLISH = "结构校验通过，原子发布导出文件";
    private static final String MESSAGE_EXPORT_COMPLETE = "导出完成";
    private static final String MESSAGE_EXPORT_SUCCESS = "导出完成，结果已写入: ";
    private static final String MESSAGE_EXPORT_FAILED = "导出失败";
    private static final int PROGRESS_PRECHECK = 15;
    private static final int PROGRESS_PREPARE = 30;
    private static final int PROGRESS_WRITE_TEMPORARY = 70;
    private static final int PROGRESS_VALIDATE = 85;
    private static final int PROGRESS_PUBLISH = 95;

    private final WorkspaceService workspaceService;
    private final ArchiveService archiveService;
    private final FileChangeRepository fileChangeRepository;
    private final ExportRecordRepository exportRecordRepository;
    private final TaskService taskService;
    private final ClockService clockService;
    private final AnalysisService analysisService;
    private final DiffService diffService;
    private final SignatureService signatureService;
    private final CompiledArtifactRepository compiledArtifactRepository;
    private final ExportValidationService exportValidationService;
    private final ExportValidationRepository exportValidationRepository;
    private final ProjectSettingsService projectSettingsService;

    /**
     * 创建导出服务。
     *
     * @param workspaceService       工作区服务
     * @param archiveService         压缩包服务
     * @param fileChangeRepository   修改记录仓储
     * @param exportRecordRepository 导出记录仓储
     * @param taskService            任务服务
     * @param clockService           时间服务
     * @param analysisService        结构分析服务
     * @param diffService            基线差异服务
     * @param signatureService       签名识别服务
     * @param compiledArtifactRepository 编译产物仓储
     * @param exportValidationService 导出结构校验服务
     * @param exportValidationRepository 导出校验仓储
     * @param projectSettingsService 项目设置服务
     */
    public ExportService(WorkspaceService workspaceService,
                         ArchiveService archiveService,
                         FileChangeRepository fileChangeRepository,
                         ExportRecordRepository exportRecordRepository,
                         TaskService taskService,
                         ClockService clockService,
                         AnalysisService analysisService,
                         DiffService diffService,
                         SignatureService signatureService,
                         CompiledArtifactRepository compiledArtifactRepository,
                         ExportValidationService exportValidationService,
                         ExportValidationRepository exportValidationRepository,
                         ProjectSettingsService projectSettingsService) {
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
        this.fileChangeRepository = fileChangeRepository;
        this.exportRecordRepository = exportRecordRepository;
        this.taskService = taskService;
        this.clockService = clockService;
        this.analysisService = analysisService;
        this.diffService = diffService;
        this.signatureService = signatureService;
        this.compiledArtifactRepository = compiledArtifactRepository;
        this.exportValidationService = exportValidationService;
        this.exportValidationRepository = exportValidationRepository;
        this.projectSettingsService = projectSettingsService;
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
        return export(project, outputPath, taskId, null);
    }

    /**
     * 按明确签名策略导出修改后的 Jar 或 War。
     *
     * @param project         项目记录
     * @param outputPath      用户指定输出路径，可为空
     * @param taskId          预创建任务 ID，可为空
     * @param signaturePolicy 签名策略码
     * @return 导出结果
     * @throws IOException 打包或分析失败时抛出
     */
    public OperationResult export(ProjectRecord project,
                                  String outputPath,
                                  String taskId,
                                  String signaturePolicy) throws IOException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE_EXPORT, MESSAGE_EXPORT_START);
        Path temporaryFile = null;
        try {
            taskService.running(task, PROGRESS_PRECHECK, MESSAGE_EXPORT_PRECHECK);
            analysisService.analyzeForExport(project);
            DiffReport diffReport = diffService.compare(project);
            SignaturePolicy policy = SignaturePolicy.from(signaturePolicy);
            boolean modified = !diffReport.getSourceDiffs().isEmpty()
                    || !diffReport.getResourceDiffs().isEmpty()
                    || !diffReport.getCompiledArtifacts().isEmpty();
            if (!diffReport.getSourceDiffs().isEmpty() && diffReport.getCompiledArtifacts().isEmpty()) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_SOURCE_NOT_COMPILED);
            }
            boolean signed = !signatureService.findSignatureFiles(project).isEmpty();
            if (signed && modified && policy == SignaturePolicy.PRESERVE_ONLY_UNMODIFIED) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_SIGNED_ARCHIVE_MODIFIED);
            }
            boolean removeSignatures = signed && policy == SignaturePolicy.REMOVE_INVALID_SIGNATURES;

            List<String> changedFiles = fileChangeRepository.findPaths(project.getId());
            Path outputFile = resolveOutputPath(project, outputPath);
            validateOutputPath(project, outputFile);
            Files.createDirectories(outputFile.getParent());
            temporaryFile = Files.createTempFile(outputFile.getParent(),
                    TEMP_FILE_PREFIX + outputFile.getFileName() + TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);

            taskService.running(task, PROGRESS_PREPARE, MESSAGE_EXPORT_PREPARE + outputFile);
            taskService.ensureNotCancelled(task.getId());
            boolean springBootLayout = PackageType.SPRING_BOOT_JAR.getCode().equals(project.getPackageType());

            taskService.running(task, PROGRESS_WRITE_TEMPORARY, MESSAGE_EXPORT_WRITE_TEMPORARY);
            archiveService.zipDirectory(workspaceService.extractedDir(project), temporaryFile, springBootLayout,
                    removeSignatures,
                    () -> taskService.isCancelled(task.getId()));
            taskService.ensureNotCancelled(task.getId());

            taskService.running(task, PROGRESS_VALIDATE, MESSAGE_EXPORT_VALIDATE);
            List<String> compiledArtifacts = compiledArtifactRepository.findPaths(project.getId());
            ExportValidationResult validation = exportValidationService.validate(project, temporaryFile,
                    changedFiles, compiledArtifacts, removeSignatures);
            exportValidationRepository.insert(project.getId(), outputFile.toString(), validation, clockService.now());
            if (!validation.isValid()) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_EXPORT_VALIDATION_FAILED
                        + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                        + String.join(JarPatchConstants.MESSAGE_LIST_SEPARATOR, validation.getErrors()));
            }

            // 导出包完整写入后才以原子移动发布目标文件，失败或取消只删除本次临时文件。
            taskService.running(task, PROGRESS_PUBLISH, MESSAGE_EXPORT_PUBLISH);
            try {
                Files.move(temporaryFile, outputFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException(JarPatchConstants.MESSAGE_EXPORT_ATOMIC_MOVE_REQUIRED, exception);
            }
            temporaryFile = null;

            exportRecordRepository.insert(project.getId(), outputFile.toString(), clockService.now());
            OperationResult result = new OperationResult();
            result.setTaskId(task.getId());
            result.setOutputPath(outputFile.toString());
            result.setChangedFiles(changedFiles);
            result.setValidation(validation);
            result.setMessage(MESSAGE_EXPORT_COMPLETE);
            taskService.success(task, MESSAGE_EXPORT_SUCCESS + outputFile);
            return result;
        } catch (IllegalStateException exception) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(exception.getMessage())) {
                throw exception;
            }
            taskService.failed(task, MESSAGE_EXPORT_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                    + exception.getMessage());
            throw exception;
        } catch (RuntimeException | IOException exception) {
            taskService.failed(task, MESSAGE_EXPORT_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                    + exception.getMessage());
            throw exception;
        } finally {
            if (temporaryFile != null) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    /**
     * 校验导出目标不会覆盖输入原包或工作区原包。
     *
     * @param project    项目记录
     * @param outputFile 规范化导出路径
     * @throws IOException 真实路径比较失败时抛出
     */
    private void validateOutputPath(ProjectRecord project, Path outputFile) throws IOException {
        Path normalizedOutput = outputFile.toAbsolutePath().normalize();
        Path inputArchive = Paths.get(project.getOriginalPath()).toAbsolutePath().normalize();
        Path workspaceOriginalDir = workspaceService.projectRoot(project)
                .resolve(JarPatchConstants.WORKSPACE_ORIGINAL_DIR).toAbsolutePath().normalize();
        if (sameFile(normalizedOutput, inputArchive) || normalizedOutput.startsWith(workspaceOriginalDir)) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_EXPORT_OVERWRITE_ORIGINAL);
        }
    }

    /**
     * 比较两个可能尚不存在的文件路径是否指向同一文件。
     *
     * @param left  左侧路径
     * @param right 右侧路径
     * @return 指向同一文件时返回 true
     * @throws IOException 文件系统比较失败时抛出
     */
    private boolean sameFile(Path left, Path right) throws IOException {
        if (Files.exists(left) && Files.exists(right)) {
            return Files.isSameFile(left, right);
        }
        return left.equals(right);
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
        String defaultDirectory = projectSettingsService.defaultExportDirectory(project.getId());
        Path exportDirectory = defaultDirectory == null
                ? workspaceService.exportDir(project)
                : Paths.get(defaultDirectory).toAbsolutePath().normalize();
        return exportDirectory.resolve(baseName + PATCHED_SUFFIX + extension).normalize();
    }
}
