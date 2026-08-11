package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.WorkspaceCleanupPreview;
import com.jarpatch.repository.ProjectRepository;
import com.jarpatch.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 独立工作区清理服务。
 * <p>
 * 用户先调用预览入口读取项目、路径、文件数、大小和最后使用时间；确认入口校验一次性标识、
 * 有效期、工作区快照和运行中任务后才删除文件，结果只标记工作区已清理，不删除项目历史。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class WorkspaceCleanupService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final ClockService clockService;
    private final Map<String, CleanupAuthorization> authorizations = new ConcurrentHashMap<>();

    /**
     * 创建工作区清理服务。
     *
     * @param projectRepository 项目仓储
     * @param taskRepository    任务仓储
     * @param workspaceService  工作区服务
     * @param clockService      时间服务
     */
    public WorkspaceCleanupService(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   WorkspaceService workspaceService,
                                   ClockService clockService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.clockService = clockService;
    }

    /**
     * 创建当前工作区的清理预览和一次性确认标识。
     *
     * @param projectId 项目 ID
     * @return 清理预览
     * @throws IOException 工作区统计失败时抛出
     */
    public WorkspaceCleanupPreview preview(String projectId) throws IOException {
        ProjectRecord project = requireProject(projectId);
        ensureWorkspaceAvailable(project);
        ensureNoRunningTask(projectId);
        purgeExpiredAuthorizations();
        Path workspacePath = workspaceService.projectRoot(project);
        WorkspaceStats stats = calculateStats(workspacePath);
        String confirmationId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(JarPatchConstants.WORKSPACE_CLEANUP_CONFIRMATION_MINUTES,
                ChronoUnit.MINUTES);
        authorizations.put(confirmationId, new CleanupAuthorization(projectId, workspacePath,
                stats.fileCount(), stats.totalBytes(), expiresAt));

        WorkspaceCleanupPreview preview = new WorkspaceCleanupPreview();
        preview.setProjectId(projectId);
        preview.setProjectName(project.getName());
        preview.setWorkspacePath(workspacePath.toString());
        preview.setFileCount(stats.fileCount());
        preview.setTotalBytes(stats.totalBytes());
        preview.setLastUsedAt(project.getUpdatedAt());
        preview.setConfirmationId(confirmationId);
        preview.setExpiresAt(expiresAt.toString());
        return preview;
    }

    /**
     * 使用一次性确认标识清理未发生变化的工作区。
     *
     * @param projectId      项目 ID
     * @param confirmationId 预览返回的确认标识
     * @throws IOException 删除工作区失败时抛出
     */
    public void clean(String projectId, String confirmationId) throws IOException {
        CleanupAuthorization authorization = authorizations.remove(confirmationId);
        ProjectRecord project = requireProject(projectId);
        ensureWorkspaceAvailable(project);
        ensureNoRunningTask(projectId);
        if (authorization == null || authorization.expiresAt().isBefore(Instant.now())
                || !authorization.projectId().equals(projectId)
                || !authorization.workspacePath().equals(workspaceService.projectRoot(project))) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID);
        }
        WorkspaceStats currentStats = calculateStats(authorization.workspacePath());
        if (currentStats.fileCount() != authorization.fileCount()
                || currentStats.totalBytes() != authorization.totalBytes()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID);
        }
        workspaceService.deleteWorkspace(authorization.workspacePath());
        projectRepository.markWorkspaceCleaned(projectId, clockService.now());
    }

    /**
     * 统计工作区内普通文件数量和总字节数。
     *
     * @param workspacePath 工作区路径
     * @return 工作区统计快照
     * @throws IOException 遍历或读取文件失败时抛出
     */
    private WorkspaceStats calculateStats(Path workspacePath) throws IOException {
        long fileCount = 0L;
        long totalBytes = 0L;
        try (Stream<Path> stream = Files.walk(workspacePath)) {
            var iterator = stream.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                fileCount++;
                totalBytes = Math.addExact(totalBytes, Files.size(path));
            }
        }
        return new WorkspaceStats(fileCount, totalBytes);
    }

    /**
     * 删除已过期的一次性确认标识。
     */
    private void purgeExpiredAuthorizations() {
        Instant now = Instant.now();
        authorizations.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * 校验项目当前不存在运行中任务。
     *
     * @param projectId 项目 ID
     */
    private void ensureNoRunningTask(String projectId) {
        if (taskRepository.countRunningByProject(projectId) > 0) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_TASK_RUNNING);
        }
    }

    /**
     * 校验项目工作区尚未清理且实际存在。
     *
     * @param project 项目记录
     */
    private void ensureWorkspaceAvailable(ProjectRecord project) {
        if (project.getWorkspaceCleanedAt() != null || !Files.isDirectory(workspaceService.projectRoot(project))) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_ALREADY_CLEANED);
        }
    }

    /**
     * 读取项目记录。
     *
     * @param projectId 项目 ID
     * @return 项目记录
     */
    private ProjectRecord requireProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_NOT_FOUND));
    }

    /**
     * 一次性清理授权快照。
     */
    private record CleanupAuthorization(String projectId, Path workspacePath, long fileCount,
                                        long totalBytes, Instant expiresAt) {
    }

    /**
     * 工作区文件数量和字节数快照。
     */
    private record WorkspaceStats(long fileCount, long totalBytes) {
    }
}
