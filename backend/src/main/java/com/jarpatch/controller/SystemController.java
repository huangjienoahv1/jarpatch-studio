package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.LocalAccessProperties;
import com.jarpatch.model.DiagnosticSnapshot;
import com.jarpatch.model.SystemStatus;
import com.jarpatch.service.BackendShutdownService;
import com.jarpatch.service.DiagnosticService;
import com.jarpatch.service.ErrorGuideService;
import com.jarpatch.model.ErrorGuideItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 本地后端实例状态控制器。
 * <p>
 * Electron 主进程使用受令牌保护的健康接口完成实例握手；该入口只报告当前实例身份和
 * 就绪状态，不暴露工作区、数据库或系统环境信息。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private static final String STATUS_UP = "UP";

    private final LocalAccessProperties localAccessProperties;
    private final BackendShutdownService backendShutdownService;
    private final ErrorGuideService errorGuideService;
    private final DiagnosticService diagnosticService;

    /**
     * 创建系统状态控制器。
     *
     * @param localAccessProperties 本地实例访问配置
     * @param backendShutdownService 后端退出服务
     * @param errorGuideService 错误排查向导服务
     * @param diagnosticService 脱敏诊断快照服务
     */
    public SystemController(LocalAccessProperties localAccessProperties,
                            BackendShutdownService backendShutdownService,
                            ErrorGuideService errorGuideService,
                            DiagnosticService diagnosticService) {
        this.localAccessProperties = localAccessProperties;
        this.backendShutdownService = backendShutdownService;
        this.errorGuideService = errorGuideService;
        this.diagnosticService = diagnosticService;
    }

    /**
     * 返回当前后端实例的受保护健康状态。
     *
     * @return 产品名、实例 ID 和就绪状态
     */
    @GetMapping("/health")
    public ApiResponse<SystemStatus> health() {
        SystemStatus status = new SystemStatus();
        status.setProduct(JarPatchConstants.PRODUCT_NAME);
        status.setInstanceId(localAccessProperties.getInstanceId());
        status.setStatus(STATUS_UP);
        return ApiResponse.success(status);
    }

    /**
     * 接受当前 Electron 实例的受保护安全退出请求。
     *
     * @return 已接受退出请求的确认消息
     */
    @PostMapping("/shutdown")
    public ApiResponse<String> shutdown() {
        backendShutdownService.requestShutdown();
        return ApiResponse.success(JarPatchConstants.MESSAGE_BACKEND_SHUTDOWN_ACCEPTED);
    }

    /**
     * 返回产品内置的错误排查向导。
     *
     * @return JDK、CFR、编译、签名、路径和端口排查清单
     */
    @GetMapping("/error-guide")
    public ApiResponse<List<ErrorGuideItem>> errorGuide() {
        return ApiResponse.success(errorGuideService.list());
    }

    /**
     * 生成不包含源码、令牌和私钥的系统诊断快照。
     *
     * @return 版本、环境、操作 ID 和脱敏日志
     */
    @GetMapping("/diagnostics")
    public ApiResponse<DiagnosticSnapshot> diagnostics() {
        return ApiResponse.success(diagnosticService.createSnapshot());
    }
}
