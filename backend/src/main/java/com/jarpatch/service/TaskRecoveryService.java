package com.jarpatch.service;

import com.jarpatch.model.ProjectRecord;
import com.jarpatch.repository.ProjectRepository;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后端进程中断状态恢复服务。
 * <p>
 * 应用就绪时触发：任务服务使用带状态条件的 SQL 把遗留 RUNNING 任务标记为失败；工作区服务
 * 根据 SQLite 已登记路径清理中断导入留下的内部标记目录。任务结果写入日志表，项目历史不被误删。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class TaskRecoveryService {

    private final TaskService taskService;
    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;

    /**
     * 创建任务恢复服务。
     *
     * @param taskService       任务服务
     * @param projectRepository 项目仓储
     * @param workspaceService  工作区服务
     */
    public TaskRecoveryService(TaskService taskService,
                               ProjectRepository projectRepository,
                               WorkspaceService workspaceService) {
        this.taskService = taskService;
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
    }

    /**
     * 在应用就绪后恢复上次异常退出留下的任务状态。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() throws IOException {
        taskService.recoverInterruptedTasks();
        Set<String> registeredPaths = projectRepository.findAll().stream()
                .map(ProjectRecord::getWorkspacePath)
                .collect(Collectors.toSet());
        workspaceService.recoverIncompleteImports(registeredPaths);
    }
}
