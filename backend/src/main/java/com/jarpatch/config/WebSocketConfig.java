package com.jarpatch.config;

import com.jarpatch.websocket.TaskWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 注册配置。
 * <p>
 * 该配置把任务日志推送入口挂载到 /ws/tasks/{taskId}，Electron 前端在执行分析、
 * 编译和导出时通过该通道接收实时进度。
 * </p>
 *
 * @author 黄杰
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskWebSocketHandler taskWebSocketHandler;

    /**
     * 创建 WebSocket 配置。
     *
     * @param taskWebSocketHandler 任务日志 WebSocket 处理器
     */
    public WebSocketConfig(TaskWebSocketHandler taskWebSocketHandler) {
        this.taskWebSocketHandler = taskWebSocketHandler;
    }

    /**
     * 注册任务日志 WebSocket 地址。
     *
     * @param registry WebSocket 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskWebSocketHandler, "/ws/tasks/{taskId}")
                .setAllowedOrigins("*");
    }
}
