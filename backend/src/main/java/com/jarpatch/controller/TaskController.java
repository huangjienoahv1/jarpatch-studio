package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_TASK_NOT_FOUND));
        return ApiResponse.success(record);
    }

    /**
     * 创建一个新的任务记录。
     * <p>
     * 前端会先调用该接口拿到 taskId，再连接 /ws/tasks/{taskId}，最后把 taskId 传给导入、
     * 分析、编译或导出接口，这样长流程中的进度日志可以实时推送到页面。
     * </p>
     *
     * @param request 创建任务的请求参数
     * @return 新建任务记录
     */
    @PostMapping
    public ApiResponse<TaskRecord> createTask(@RequestBody(required = false) Map<String, String> request) {
        if (request == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_TASK_TYPE_EMPTY);
        }
        String taskType = trimToNull(request.get("taskType"));
        if (taskType == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_TASK_TYPE_EMPTY);
        }
        String projectId = trimToNull(request.get("projectId"));
        String message = trimToNull(request.get("message"));
        return ApiResponse.success(taskService.create(projectId, taskType, message == null ? "" : message));
    }

    /**
     * 取消正在执行的任务。
     *
     * @param taskId 任务 ID
     * @return 取消后的任务记录
     */
    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskRecord> cancelTask(@PathVariable("taskId") String taskId) {
        return ApiResponse.success(taskService.cancel(taskId, JarPatchConstants.MESSAGE_TASK_CANCELLED));
    }

    /**
     * 去除空白并把空字符串转换为空值。
     *
     * @param value 原始请求值
     * @return 清理后的字符串，空白时返回 null
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
