package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.model.JdkSettingsRequest;
import com.jarpatch.model.JdkSettingsView;
import com.jarpatch.service.JdkSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用设置控制器。
 * <p>
 * 设置页面通过该控制器读取和保存 JDK 配置；实际的校验和落库由 JdkSettingsService 完成。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final JdkSettingsService jdkSettingsService;

    /**
     * 创建设置控制器。
     *
     * @param jdkSettingsService JDK 配置服务
     */
    public SettingsController(JdkSettingsService jdkSettingsService) {
        this.jdkSettingsService = jdkSettingsService;
    }

    /**
     * 读取 JDK 配置。
     *
     * @return JDK 配置视图
     */
    @GetMapping("/jdk")
    public ApiResponse<JdkSettingsView> getJdkSettings() {
        return ApiResponse.success(jdkSettingsService.getSettings());
    }

    /**
     * 保存 JDK 配置。
     *
     * @param request JDK 配置请求
     * @return 保存后的 JDK 配置视图
     */
    @PutMapping("/jdk")
    public ApiResponse<JdkSettingsView> saveJdkSettings(@RequestBody JdkSettingsRequest request) {
        return ApiResponse.success(jdkSettingsService.saveJavaHome(request.getJavaHome()));
    }
}
