package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.OrphanWorkspaceEntry;
import com.jarpatch.model.OrphanWorkspacePreview;
import com.jarpatch.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 孤立工作区显式预览与确认清理服务。
 * <p>
 * 项目历史删除后保留的工作区不会被后台推断删除；用户先调用预览入口，实际删除点只接受
 * 未过期且文件数量、大小、路径均未变化的一次性确认标识。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class OrphanWorkspaceService {

    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;
    private final Map<String, OrphanAuthorization> authorizations = new ConcurrentHashMap<>();

    /**
     * 创建孤立工作区服务。
     *
     * @param projectRepository 项目仓储
     * @param workspaceService  工作区服务
     */
    public OrphanWorkspaceService(ProjectRepository projectRepository, WorkspaceService workspaceService) {
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
    }

    /**
     * 扫描未被项目历史登记的就绪工作区并创建一次性预览。
     *
     * @return 孤立工作区预览
     * @throws IOException 目录扫描失败时抛出
     */
    public OrphanWorkspacePreview preview() throws IOException {
        purgeExpiredAuthorizations();
        List<WorkspaceSnapshot> snapshots = scanSnapshots();
        String confirmationId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(JarPatchConstants.WORKSPACE_CLEANUP_CONFIRMATION_MINUTES,
                ChronoUnit.MINUTES);
        authorizations.put(confirmationId, new OrphanAuthorization(snapshots, expiresAt));

        OrphanWorkspacePreview preview = new OrphanWorkspacePreview();
        preview.setEntries(snapshots.stream().map(this::toEntry).toList());
        preview.setConfirmationId(confirmationId);
        preview.setExpiresAt(expiresAt.toString());
        return preview;
    }

    /**
     * 校验预览快照仍完全一致后删除其中全部孤立工作区。
     *
     * @param confirmationId 预览返回的一次性确认标识
     * @return 实际删除工作区数量
     * @throws IOException 重新扫描或删除失败时抛出
     */
    public int clean(String confirmationId) throws IOException {
        OrphanAuthorization authorization = authorizations.remove(confirmationId);
        if (authorization == null || authorization.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID);
        }
        List<WorkspaceSnapshot> current = scanSnapshots();
        if (!authorization.snapshots().equals(current)) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_WORKSPACE_CLEANUP_CONFIRMATION_INVALID);
        }
        for (WorkspaceSnapshot snapshot : current) {
            workspaceService.deleteWorkspace(snapshot.path());
        }
        return current.size();
    }

    /**
     * 扫描工作区根目录的直接子目录，只把带就绪标记且未登记的目录列为孤立项。
     *
     * @return 稳定排序的候选快照
     * @throws IOException 扫描失败时抛出
     */
    private List<WorkspaceSnapshot> scanSnapshots() throws IOException {
        Path root = workspaceService.workspaceRoot();
        Files.createDirectories(root);
        Set<Path> registered = projectRepository.findAll().stream()
                .map(project -> Path.of(project.getWorkspacePath()).toAbsolutePath().normalize())
                .collect(Collectors.toSet());
        List<Path> candidates;
        try (Stream<Path> stream = Files.list(root)) {
            candidates = stream.filter(Files::isDirectory)
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(path -> Files.isRegularFile(path.resolve(JarPatchConstants.WORKSPACE_READY_MARKER)))
                    .filter(path -> !registered.contains(path))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
        List<WorkspaceSnapshot> snapshots = new ArrayList<>();
        for (Path candidate : candidates) {
            snapshots.add(calculateSnapshot(candidate));
        }
        return snapshots;
    }

    /**
     * 统计单个候选工作区的文件数、总大小和最后修改时间。
     *
     * @param path 候选工作区路径
     * @return 不可变统计快照
     * @throws IOException 遍历失败时抛出
     */
    private WorkspaceSnapshot calculateSnapshot(Path path) throws IOException {
        long fileCount = 0L;
        long totalBytes = 0L;
        long lastModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        try (Stream<Path> stream = Files.walk(path)) {
            var iterator = stream.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                fileCount++;
                totalBytes = Math.addExact(totalBytes, Files.size(file));
                lastModifiedMillis = Math.max(lastModifiedMillis, Files.getLastModifiedTime(file).toMillis());
            }
        }
        return new WorkspaceSnapshot(path, fileCount, totalBytes, lastModifiedMillis);
    }

    /**
     * 把内部统计快照转换为接口模型。
     *
     * @param snapshot 内部快照
     * @return 前端预览项
     */
    private OrphanWorkspaceEntry toEntry(WorkspaceSnapshot snapshot) {
        OrphanWorkspaceEntry entry = new OrphanWorkspaceEntry();
        entry.setWorkspacePath(snapshot.path().toString());
        entry.setFileCount(snapshot.fileCount());
        entry.setTotalBytes(snapshot.totalBytes());
        entry.setLastModifiedAt(Instant.ofEpochMilli(snapshot.lastModifiedMillis()).toString());
        return entry;
    }

    /**
     * 清除已过期的一次性确认标识。
     */
    private void purgeExpiredAuthorizations() {
        Instant now = Instant.now();
        authorizations.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * 单个孤立工作区不可变统计快照。
     */
    private record WorkspaceSnapshot(Path path, long fileCount, long totalBytes, long lastModifiedMillis) {
    }

    /**
     * 一次性批量清理授权。
     */
    private record OrphanAuthorization(List<WorkspaceSnapshot> snapshots, Instant expiresAt) {
    }
}
