package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.model.OrphanWorkspacePreview;
import com.jarpatch.service.OrphanWorkspaceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 全局工作区生命周期控制器。
 * <p>
 * 桌面端通过该入口扫描项目历史之外的就绪工作区，并在用户确认后执行批量清理；
 * 预览和删除严格分离，后台启动流程不会调用删除入口。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final OrphanWorkspaceService orphanWorkspaceService;

    /**
     * 创建工作区生命周期控制器。
     *
     * @param orphanWorkspaceService 孤立工作区服务
     */
    public WorkspaceController(OrphanWorkspaceService orphanWorkspaceService) {
        this.orphanWorkspaceService = orphanWorkspaceService;
    }

    /**
     * 预览全部孤立工作区，不删除任何文件。
     *
     * @return 候选项和一次性确认标识
     * @throws IOException 扫描失败时抛出
     */
    @GetMapping("/orphans/cleanup-preview")
    public ApiResponse<OrphanWorkspacePreview> previewOrphans() throws IOException {
        return ApiResponse.success(orphanWorkspaceService.preview());
    }

    /**
     * 使用预览确认标识清理快照未变化的孤立工作区。
     *
     * @param confirmationId 一次性确认标识
     * @return 实际删除数量
     * @throws IOException 删除失败时抛出
     */
    @DeleteMapping("/orphans")
    public ApiResponse<Integer> cleanOrphans(@RequestParam("confirmationId") String confirmationId)
            throws IOException {
        return ApiResponse.success(orphanWorkspaceService.clean(confirmationId));
    }
}
