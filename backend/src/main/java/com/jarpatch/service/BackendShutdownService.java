package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import jakarta.annotation.PreDestroy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地后端安全退出服务。
 * <p>
 * Electron 退出入口调用本服务；服务在 HTTP 响应发回后关闭 Spring 上下文，最终释放端口、
 * SQLite 连接和后台资源，Electron 仅在限定时间未退出时终止自己启动的子进程。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class BackendShutdownService {

    private final ConfigurableApplicationContext applicationContext;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "jarpatch-backend-shutdown");
        thread.setDaemon(false);
        return thread;
    });

    /**
     * 创建后端退出服务。
     *
     * @param applicationContext Spring 应用上下文
     */
    public BackendShutdownService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 安排在当前 HTTP 响应提交后关闭后端。
     */
    public void requestShutdown() {
        scheduler.schedule(applicationContext::close,
                JarPatchConstants.BACKEND_SHUTDOWN_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Spring 上下文关闭时释放退出调度器。
     */
    @PreDestroy
    public void closeScheduler() {
        scheduler.shutdown();
    }
}
