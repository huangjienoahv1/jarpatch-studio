package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务查询控制器。
 * <p>
 * 前端在执行长流程时通过该控制器查询任务最新状态，实时日志则通过 WebSocket 获取。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    /**
     * 创建任务控制器。
     *
     * @param taskService 任务服务
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 根据任务 ID 查询任务状态。
     *
     * @param taskId 任务 ID
     * @return 任务记录
     */
    @GetMapping("/{taskId}")
    public ApiResponse<TaskRecord> getTask(@PathVariable("taskId") String taskId) {
        TaskRecord record = taskService.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_FAILED));
        return ApiResponse.success(record);
    }
}
