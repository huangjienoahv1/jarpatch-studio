package com.jarpatch.config;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 任务日志 WebSocket 本地握手拦截器。
 * <p>
 * WebSocket 建连入口先校验随机令牌，再确认路径中的任务 ID 已写入当前实例数据库；
 * 任一条件不满足时拒绝升级连接，避免任意网页订阅或探测本机任务日志。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class LocalAccessHandshakeInterceptor implements HandshakeInterceptor {

    private static final UriTemplate TASK_URI_TEMPLATE = new UriTemplate("/ws/tasks/{taskId}");
    private static final String TASK_ID_ATTRIBUTE = "taskId";

    private final LocalAccessProperties localAccessProperties;
    private final TaskService taskService;

    /**
     * 创建 WebSocket 本地握手拦截器。
     *
     * @param localAccessProperties 本地实例访问配置
     * @param taskService           任务服务
     */
    public LocalAccessHandshakeInterceptor(LocalAccessProperties localAccessProperties, TaskService taskService) {
        this.localAccessProperties = localAccessProperties;
        this.taskService = taskService;
    }

    /**
     * 在协议升级前校验令牌和任务归属，并把任务 ID 写入会话属性。
     *
     * @param request    握手请求
     * @param response   握手响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 会话属性
     * @return 允许握手时返回 true
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst(LocalAccessProperties.TOKEN_QUERY_PARAMETER);
        String taskId = resolveTaskId(request);
        if (!tokenMatches(token) || taskId == null || taskService.findById(taskId).isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(TASK_ID_ATTRIBUTE, taskId);
        return true;
    }

    /**
     * 握手完成后无需附加处理。
     *
     * @param request   握手请求
     * @param response  握手响应
     * @param wsHandler WebSocket 处理器
     * @param exception 握手异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手阶段只负责验证，连接注册由 TaskWebSocketHandler 统一完成。
    }

    /**
     * 从握手地址解析任务 ID。
     *
     * @param request 握手请求
     * @return 任务 ID；路径不匹配时返回 null
     */
    private String resolveTaskId(ServerHttpRequest request) {
        Map<String, String> values = TASK_URI_TEMPLATE.match(request.getURI().getPath());
        String taskId = values.get(TASK_ID_ATTRIBUTE);
        return taskId == null || taskId.isBlank() ? null : taskId;
    }

    /**
     * 使用常量时间比较校验 WebSocket 查询参数令牌。
     *
     * @param requestToken 查询参数令牌
     * @return 令牌一致时返回 true
     */
    private boolean tokenMatches(String requestToken) {
        if (requestToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                localAccessProperties.getToken().getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8));
    }
}
