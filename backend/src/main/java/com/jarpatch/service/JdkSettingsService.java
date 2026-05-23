package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.JdkSettingsView;
import com.jarpatch.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

/**
 * JDK 配置服务。
 * <p>
 * 该服务负责把设置页面输入的 JDK 路径写入 SQLite，并在读取时返回当前保存值和实际
 * 生效值。控制器只负责把请求转给这里，编译服务仍通过 JdkService 读取同一份配置。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class JdkSettingsService {

    private final AppSettingsRepository appSettingsRepository;
    private final ClockService clockService;
    private final JdkService jdkService;

    /**
     * 创建 JDK 配置服务。
     *
     * @param appSettingsRepository 应用配置仓储
     * @param clockService          时间服务
     * @param jdkService            JDK 工具定位服务
     */
    public JdkSettingsService(AppSettingsRepository appSettingsRepository,
                              ClockService clockService,
                              JdkService jdkService) {
        this.appSettingsRepository = appSettingsRepository;
        this.clockService = clockService;
        this.jdkService = jdkService;
    }

    /**
     * 读取当前 JDK 配置视图。
     *
     * @return JDK 配置视图
     */
    public JdkSettingsView getSettings() {
        return jdkService.inspectCurrentSettings();
    }

    /**
     * 校验并保存 JDK 安装目录。
     * <p>
     * 入口来自设置页面保存按钮，实际执行点是 JDK 校验和 SQLite upsert，结果写入
     * app_settings 表，编译服务随后会读取这条配置作为优先来源。
     * </p>
     *
     * @param javaHome JDK 安装目录
     * @return 保存后的 JDK 视图
     */
    public JdkSettingsView saveJavaHome(String javaHome) {
        JdkSettingsView view = jdkService.inspectJavaHome(javaHome);
        appSettingsRepository.upsert(JarPatchConstants.SETTING_KEY_JDK_HOME,
                view.getConfiguredJavaHome(),
                clockService.now());
        return getSettings();
    }
}
