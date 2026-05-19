package com.jarpatch.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务日志广播服务。
 * <p>
 * WebSocket 处理器在连接建立时注册会话，任务服务在关键流程节点调用该服务推送进度
 * 和日志，前端日志面板即可看到实际执行点。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class TaskLogBroadcaster {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 注册任务 WebSocket 会话。
     *
     * @param taskId  任务 ID
     * @param session WebSocket 会话
     */
    public void register(String taskId, WebSocketSession session) {
        sessions.computeIfAbsent(taskId, key -> Collections.newSetFromMap(new ConcurrentHashMap<WebSocketSession, Boolean>()))
                .add(session);
    }

    /**
     * 移除任务 WebSocket 会话。
     *
     * @param taskId  任务 ID
     * @param session WebSocket 会话
     */
    public void remove(String taskId, WebSocketSession session) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions != null) {
            taskSessions.remove(session);
        }
    }

    /**
     * 向指定任务的所有前端连接推送日志。
     *
     * @param taskId  任务 ID
     * @param message 日志消息
     */
    public void broadcast(String taskId, String message) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions == null) {
            return;
        }
        for (WebSocketSession session : taskSessions) {
            send(session, message);
        }
    }

    /**
     * 向单个 WebSocket 会话发送消息。
     *
     * @param session WebSocket 会话
     * @param message 日志消息
     */
    private void send(WebSocketSession session, String message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException ignored) {
                // 关闭失败不影响任务执行，后续日志仍会发给其他会话。
            }
        }
    }
}
