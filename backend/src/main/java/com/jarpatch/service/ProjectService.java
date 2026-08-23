package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.PackageType;
import com.jarpatch.common.TaskStatus;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.JavaVersionInfo;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 项目导入和查询服务。
 * <p>
 * 控制器的导入入口调用该服务，执行顺序为：校验原始包、创建任务、创建工作区、复制原始包、
 * 解压、识别包类型、反编译、项目记录落库。结果写入 SQLite projects 表和工作区目录。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ProjectService {

    private static final String TASK_TYPE_IMPORT = "IMPORT";
    private static final String MESSAGE_IMPORT_START = "开始导入包: ";
    private static final String MESSAGE_IMPORT_CREATE_STAGING = "创建导入临时工作区";
    private static final String MESSAGE_IMPORT_COPY_ORIGINAL = "复制原始包到工作区";
    private static final String MESSAGE_IMPORT_UNZIP = "解压 Jar/War 到工作区";
    private static final String MESSAGE_IMPORT_DETECT = "识别包结构";
    private static final String MESSAGE_IMPORT_DECOMPILE = "执行 CFR 反编译";
    private static final String MESSAGE_IMPORT_CAPTURE_BASELINE = "捕获可编辑文件导入基线";
    private static final String MESSAGE_IMPORT_COMMIT = "提交完整项目工作区";
    private static final String MESSAGE_IMPORT_SUCCESS = "导入完成，项目记录已写入 SQLite";
    private static final String MESSAGE_IMPORT_FAILED = "导入失败";
    private static final String MESSAGE_IMPORT_CLEANUP_FAILED = "导入失败后的工作区清理失败";
    private static final int PROGRESS_CREATE_STAGING = 10;
    private static final int PROGRESS_COPY_ORIGINAL = 20;
    private static final int PROGRESS_UNZIP = 40;
    private static final int PROGRESS_DETECT = 60;
    private static final int PROGRESS_DECOMPILE = 75;
    private static final int PROGRESS_CAPTURE_BASELINE = 85;
    private static final int PROGRESS_COMMIT = 90;

    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;
    private final ArchiveService archiveService;
    private final PackageDetectService packageDetectService;
    private final DecompilerService decompilerService;
    private final TaskService taskService;
    private final ClockService clockService;
    private final JavaVersionService javaVersionService;
    private final BaselineService baselineService;

    /**
     * 创建项目服务。
     *
     * @param projectRepository  项目仓储
     * @param workspaceService   工作区服务
     * @param archiveService     压缩包服务
     * @param packageDetectService 包类型识别服务
     * @param decompilerService  反编译服务
     * @param taskService        任务服务
     * @param clockService       时间服务
     * @param javaVersionService Java class 目标版本识别服务
     * @param baselineService    导入基线服务
     */
    public ProjectService(ProjectRepository projectRepository,
                          WorkspaceService workspaceService,
                          ArchiveService archiveService,
                          PackageDetectService packageDetectService,
                          DecompilerService decompilerService,
                          TaskService taskService,
                          ClockService clockService,
                          JavaVersionService javaVersionService,
                          BaselineService baselineService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
        this.packageDetectService = packageDetectService;
        this.decompilerService = decompilerService;
        this.taskService = taskService;
        this.clockService = clockService;
        this.javaVersionService = javaVersionService;
        this.baselineService = baselineService;
    }

    /**
     * 导入 Jar 或 War 项目。
     * <p>
     * 入口在 /api/projects/import。前端会先通过预解析接口让用户选择需要反编译的嵌套 Jar，
     * 这里解压原始包后只把用户选择结果传给反编译服务，结果写入 sources 和 SQLite。
     * </p>
     *
     * @param filePath           原始包路径
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @return 项目记录
     * @throws IOException 导入失败时抛出
     */
    public ProjectRecord importProject(String filePath, List<String> selectedNestedJars) throws IOException {
        return importProject(filePath, selectedNestedJars, null);
    }

    /**
     * 导入 Jar 或 War 项目。
     * <p>
     * 当前版本支持前端先创建任务再传入 taskId，这样导入阶段、反编译阶段和后续取消
     * 操作都能围绕同一条任务记录和 WebSocket 连接执行。
     * </p>
     *
     * @param filePath           原始包路径
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @param taskId             预创建任务 ID，可为空
     * @return 项目记录
     * @throws IOException 导入失败时抛出
     */
    public ProjectRecord importProject(String filePath, List<String> selectedNestedJars, String taskId) throws IOException {
        Path archiveFile = Paths.get(filePath).toAbsolutePath().normalize();
        validateArchive(archiveFile);

        TaskRecord task = taskService.prepare(taskId, null, TASK_TYPE_IMPORT,
                MESSAGE_IMPORT_START + archiveFile.getFileName());
        String projectId = UUID.randomUUID().toString();
        Path importRoot = null;
        Path readyRoot = null;
        boolean projectInserted = false;
        try {
            taskService.running(task, PROGRESS_CREATE_STAGING, MESSAGE_IMPORT_CREATE_STAGING);
            importRoot = workspaceService.createImportWorkspace(projectId);
            Path originalFile = importRoot.resolve(JarPatchConstants.WORKSPACE_ORIGINAL_DIR)
                    .resolve(archiveFile.getFileName().toString());

            taskService.running(task, PROGRESS_COPY_ORIGINAL, MESSAGE_IMPORT_COPY_ORIGINAL);
            Files.copy(archiveFile, originalFile);

            taskService.running(task, PROGRESS_UNZIP, MESSAGE_IMPORT_UNZIP);
            Path extractedDir = importRoot.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR);
            archiveService.unzip(originalFile, extractedDir, () -> taskService.isCancelled(task.getId()));

            taskService.running(task, PROGRESS_DETECT, MESSAGE_IMPORT_DETECT);
            PackageType packageType = packageDetectService.detect(originalFile, extractedDir);
            JavaVersionInfo javaVersionInfo = javaVersionService.detectProjectVersion(extractedDir, packageType.getCode());

            taskService.running(task, PROGRESS_DECOMPILE, MESSAGE_IMPORT_DECOMPILE);
            decompilerService.decompile(extractedDir, importRoot.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR),
                    packageType.getCode(), normalizeSelectedNestedJars(selectedNestedJars), () -> taskService.isCancelled(task.getId()));

            taskService.running(task, PROGRESS_CAPTURE_BASELINE, MESSAGE_IMPORT_CAPTURE_BASELINE);
            baselineService.capture(importRoot);

            // 所有文件步骤成功后仅原子切换就绪标记，再写入 projects，失败记录不会出现在项目列表。
            taskService.running(task, PROGRESS_COMMIT, MESSAGE_IMPORT_COMMIT);
            readyRoot = workspaceService.markImportReady(importRoot);
            importRoot = null;
            ProjectRecord record = buildRecord(projectId, archiveFile, readyRoot, packageType, javaVersionInfo);
            projectRepository.insert(record);
            projectInserted = true;
            taskService.bindProject(task, projectId);
            taskService.success(task, MESSAGE_IMPORT_SUCCESS);
            return record;
        } catch (IllegalStateException e) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(e.getMessage())) {
                throw e;
            }
            taskService.failed(task, MESSAGE_IMPORT_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + e.getMessage());
            throw e;
        } catch (RuntimeException | IOException e) {
            taskService.failed(task, MESSAGE_IMPORT_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + e.getMessage());
            throw e;
        } finally {
            rollbackIncompleteImport(projectId, importRoot, readyRoot, projectInserted, task);
        }
    }

    /**
     * 回滚未成功完成的导入操作。
     *
     * @param projectId      项目 ID
     * @param importRoot     未就绪工作区，可为空
     * @param readyRoot      已就绪工作区，可为空
     * @param projectInserted 项目记录是否已写入
     * @param task           当前导入任务
     */
    private void rollbackIncompleteImport(String projectId,
                                           Path importRoot,
                                           Path readyRoot,
                                          boolean projectInserted,
                                          TaskRecord task) {
        if (TaskStatus.SUCCESS.getCode().equals(task.getStatus())) {
            return;
        }
        try {
            if (projectInserted) {
                projectRepository.deleteHistory(projectId);
            }
            if (importRoot != null) {
                workspaceService.deleteWorkspace(importRoot);
            }
            if (readyRoot != null) {
                workspaceService.deleteWorkspace(readyRoot);
            }
        } catch (IOException cleanupException) {
            throw new IllegalStateException(MESSAGE_IMPORT_CLEANUP_FAILED
                    + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + cleanupException.getMessage(), cleanupException);
        }
    }

    /**
     * 查询所有项目历史。
     *
     * @return 项目历史列表
     */
    public List<ProjectRecord> findAll() {
        return projectRepository.findAll();
    }

    /**
     * 根据项目 ID 查询项目。
     *
     * @param projectId 项目 ID
     * @return 项目记录
     */
    public Optional<ProjectRecord> findById(String projectId) {
        return projectRepository.findById(projectId);
    }

    /**
     * 删除项目历史记录。
     * <p>
     * 入口在控制器 DELETE 项目接口，实际执行点是项目仓储，删除结果写入 SQLite；
     * 为避免误删用户文件，本方法只删除历史和关联数据库记录，不删除工作区目录。
     * </p>
     *
     * @param projectId 项目 ID
     */
    @Transactional
    public void deleteHistory(String projectId) {
        ProjectRecord project = findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_NOT_FOUND));
        projectRepository.deleteHistory(project.getId());
    }

    /**
     * 规范化前端传入的嵌套 Jar 选择结果。
     *
     * @param selectedNestedJars 用户选择需要反编译的嵌套 Jar 相对路径
     * @return 去空后的嵌套 Jar 路径集合；为空时返回空集合
     */
    private Set<String> normalizeSelectedNestedJars(List<String> selectedNestedJars) {
        if (selectedNestedJars == null) {
            return Set.of();
        }
        return selectedNestedJars.stream()
                .filter(path -> path != null && !path.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * 校验原始包是否存在且类型受支持。
     *
     * @param archiveFile 原始包路径
     */
    private void validateArchive(Path archiveFile) {
        String fileName = archiveFile.getFileName().toString().toLowerCase();
        boolean supported = fileName.endsWith("." + JarPatchConstants.JAR_EXTENSION)
                || fileName.endsWith("." + JarPatchConstants.WAR_EXTENSION);
        if (!Files.exists(archiveFile) || !supported) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_UNSUPPORTED_PACKAGE);
        }
    }

    /**
     * 构建项目记录。
     *
     * @param projectId   项目 ID
     * @param archiveFile 原始包路径
     * @param projectRoot 项目工作区
     * @param packageType 包类型
     * @param javaVersionInfo 原包 Java 版本识别结果
     * @return 项目记录
     */
    private ProjectRecord buildRecord(String projectId,
                                      Path archiveFile,
                                      Path projectRoot,
                                      PackageType packageType,
                                      JavaVersionInfo javaVersionInfo) {
        String now = clockService.now();
        ProjectRecord record = new ProjectRecord();
        record.setId(projectId);
        record.setName(archiveFile.getFileName().toString());
        record.setPackageType(packageType.getCode());
        record.setOriginalPath(archiveFile.toString());
        record.setWorkspacePath(projectRoot.toString());
        record.setTargetJavaVersion(javaVersionInfo.getFeatureVersion());
        record.setClassMajorVersion(javaVersionInfo.getClassMajorVersion());
        record.setJavaVersionEvidence(javaVersionInfo.getEvidence());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }
}
