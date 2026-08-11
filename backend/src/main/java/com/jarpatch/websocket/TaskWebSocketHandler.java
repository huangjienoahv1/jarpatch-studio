package com.jarpatch.websocket;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.service.TaskLogBroadcaster;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;


/**
 * 任务日志 WebSocket 处理器。
 * <p>
 * 前端连接 /ws/tasks/{taskId} 后，该处理器把连接注册到日志广播服务；任务执行时，
 * TaskService 会把入口、实际执行点和结果写入位置实时推送给该连接。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private static final String TASK_ID_ATTRIBUTE = "taskId";

    private final TaskLogBroadcaster broadcaster;

    /**
     * 创建任务日志 WebSocket 处理器。
     *
     * @param broadcaster 任务日志广播服务
     */
    public TaskWebSocketHandler(TaskLogBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * 建立连接后注册会话。
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.register(resolveTaskId(session), session);
    }

    /**
     * 连接关闭后移除会话。
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.remove(resolveTaskId(session), session);
    }

    /**
     * 从连接地址解析任务 ID。
     *
     * @param session WebSocket 会话
     * @return 任务 ID
     */
    private String resolveTaskId(WebSocketSession session) {
        Object taskId = session.getAttributes().get(TASK_ID_ATTRIBUTE);
        if (taskId == null) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_NOT_FOUND);
        }
        return taskId.toString();
    }
}
