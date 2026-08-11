package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * <p>
 * 后端所有控制器异常统一在这里转换为 ApiResponse，前端无需解析 Spring 默认错误页，
 * 可以直接展示 message 字段。
 * </p>
 *
 * @author 黄杰
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务参数异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.failed(exception.getMessage());
    }

    /**
     * 处理非法状态异常。
     *
     * @param exception 非法状态异常
     * @return 统一失败响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalState(IllegalStateException exception) {
        return ApiResponse.failed(exception.getMessage());
    }

    /**
     * 处理未预期异常。
     *
     * @param exception 未预期异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        LOGGER.error(JarPatchConstants.LOG_UNEXPECTED_EXCEPTION, exception);
        return ApiResponse.failed(JarPatchConstants.MESSAGE_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR
                + exception.getMessage());
    }
}
