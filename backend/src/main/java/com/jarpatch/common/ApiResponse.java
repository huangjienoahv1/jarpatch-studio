package com.jarpatch.common;

/**
 * HTTP 接口统一响应体。
 * <p>
 * 控制器统一通过该类返回成功或失败结果，Electron 前端只需要判断 success 字段即可
 * 决定展示正常数据还是错误提示。
 * </p>
 *
 * @param <T> 响应数据类型
 * @author 黄杰
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    /**
     * 创建空响应对象，供 JSON 序列化框架使用。
     */
    public ApiResponse() {
    }

    /**
     * 创建完整响应对象。
     *
     * @param success 是否成功
     * @param message 展示给用户的消息
     * @param data    响应数据
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, JarPatchConstants.MESSAGE_SUCCESS, data);
    }

    /**
     * 创建失败响应。
     *
     * @param message 失败原因
     * @param <T>     响应数据类型
     * @return 失败响应对象
     */
    public static <T> ApiResponse<T> failed(String message) {
        return new ApiResponse<T>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
