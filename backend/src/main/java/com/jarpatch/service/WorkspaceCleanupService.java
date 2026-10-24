package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.WorkspaceCleanupPreview;
import com.jarpatch.repository.ProjectRepository;
import com.jarpatch.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 独立工作区清理服务。
 * <p>
 * 用户先调用预览入口读取项目、路径、文件数、大小和最后使用时间；确认入口校验一次性标识、
 * 有效期、文件路径与内容摘要快照和运行中任务后才删除文件，预览与清理均持有项目锁；
 * 结果只标记工作区已清理，不删除项目历史。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class WorkspaceCleanupService {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String MESSAGE_WORKSPACE_CHANGED = "工作区内容在统计期间发生变化，请重新预览";
    private static final String MESSAGE_UNSUPPORTED_WORKSPACE_ENTRY = "工作区包含非普通文件，不能确认清理";

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final ClockService clockService;
    private final ProjectOperationLockService projectOperationLockService;
    private final Map<String, CleanupAuthorization> authorizations = new ConcurrentHashMap<>();

    /**
     * 创建工作区清理服务。
     *
     * @param projectRepository 项目仓储
     * @param taskRepository    任务仓储
     * @param workspaceService  工作区服务
     * @param clockService      时间服务
     * @param projectOperationLockService 项目操作互斥服务
     */
    public WorkspaceCleanupService(ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   WorkspaceService workspaceService,
                                   ClockService clockService,
                                   ProjectOperationLockService projectOperationLockService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.clockService = clockService;
        this.projectOperationLockService = projectOperationLockService;
    }

    /**
     * 创建当前工作区的清理预览和一次性确认标识。
     *
     * @param projectId 项目 ID
     * @return 清理预览
     * @throws IOException 工作区统计失败时抛出
     */
    public WorkspaceCleanupPreview preview(String projectId) throws IOException {
        return projectOperationLockService.runExclusiveIo(projectId, () -> previewWithinLock(projectId));
    }

    /**
     * 在项目锁内生成路径、大小、修改时间和内容摘要快照，避免应用自身保存与预览交错。
     *
     * @param projectId 项目 ID
     * @return 带一次性确认标识的清理预览
     * @throws IOException 工作区读取失败或统计期间内容改变时抛出
     */
    private WorkspaceCleanupPreview previewWithinLock(String projectId) throws IOException {
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
                stats.fileCount(), stats.totalBytes(), stats.fingerprint(), expiresAt));

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
        projectOperationLockService.runExclusiveIo(projectId, () -> {
            cleanWithinLock(projectId, confirmationId);
            return null;
        });
    }

    /**
     * 在项目互斥锁内重新校验任务、授权和工作区快照后执行删除。
     * <p>
     * 校验与删除必须处于同一互斥边界，防止检查完成后又启动保存、分析、编译或导出，
     * 导致正在使用的工作区被删除。
     * </p>
     *
     * @param projectId      项目 ID
     * @param confirmationId 预览返回的确认标识
     * @throws IOException 删除工作区失败时抛出
     */
    private void cleanWithinLock(String projectId, String confirmationId) throws IOException {
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
                || currentStats.totalBytes() != authorization.totalBytes()
                || !currentStats.fingerprint().equals(authorization.fingerprint())) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID);
        }
        workspaceService.deleteWorkspace(authorization.workspacePath());
        projectRepository.markWorkspaceCleaned(projectId, clockService.now());
    }

    /**
     * 按稳定路径顺序统计文件数量、大小，并流式计算路径、修改时间和内容的 SHA-256。
     * 等长修改、文件重命名和空目录变化也会使确认快照失效；不跟随符号链接。
     *
     * @param workspacePath 工作区路径
     * @return 工作区统计快照
     * @throws IOException 遍历或读取文件失败时抛出
     */
    private WorkspaceStats calculateStats(Path workspacePath) throws IOException {
        long fileCount = 0L;
        long totalBytes = 0L;
        MessageDigest fingerprint = createDigest();
        byte[] buffer = new byte[JarPatchConstants.BUFFER_SIZE];
        try (Stream<Path> stream = Files.walk(workspacePath)) {
            var iterator = stream.filter(path -> !path.equals(workspacePath))
                    .sorted(Comparator.comparing(path -> workspacePath.relativize(path).toString())).iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                fingerprint.update(workspacePath.relativize(path).toString().replace('\\', '/')
                        .getBytes(StandardCharsets.UTF_8));
                fingerprint.update((byte) 0);
                fingerprint.update((byte) (attributes.isDirectory() ? 1 : 0));
                if (attributes.isDirectory()) {
                    continue;
                }
                if (!attributes.isRegularFile()) {
                    throw new IOException(MESSAGE_UNSUPPORTED_WORKSPACE_ENTRY
                            + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + path);
                }
                fingerprint.update(ByteBuffer.allocate(Long.BYTES * 2).putLong(attributes.size())
                        .putLong(attributes.lastModifiedTime().toMillis()).array());
                long bytesRead = 0L;
                try (InputStream input = Files.newInputStream(path)) {
                    int length;
                    while ((length = input.read(buffer)) != -1) {
                        fingerprint.update(buffer, 0, length);
                        bytesRead = Math.addExact(bytesRead, length);
                    }
                }
                BasicFileAttributes current = Files.readAttributes(path, BasicFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (bytesRead != attributes.size() || current.size() != attributes.size()
                        || !current.lastModifiedTime().equals(attributes.lastModifiedTime())) {
                    throw new IOException(MESSAGE_WORKSPACE_CHANGED);
                }
                fileCount++;
                totalBytes = Math.addExact(totalBytes, bytesRead);
            }
        }
        return new WorkspaceStats(fileCount, totalBytes, HexFormat.of().formatHex(fingerprint.digest()));
    }

    /**
     * 创建清理快照所用摘要算法；缺少 SHA-256 时拒绝生成确认标识。
     *
     * @return 新的 SHA-256 摘要实例
     */
    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_SHA256_UNAVAILABLE, exception);
        }
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
                                        long totalBytes, String fingerprint, Instant expiresAt) {
    }

    /**
     * 工作区文件数量和字节数快照。
     */
    private record WorkspaceStats(long fileCount, long totalBytes, String fingerprint) {
    }
}
