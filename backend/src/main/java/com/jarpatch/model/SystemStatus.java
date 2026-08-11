package com.jarpatch.model;

/**
 * 后端实例健康状态。
 * <p>
 * Electron 主进程启动后轮询该模型，只有产品名、实例 ID 和令牌均匹配时才创建工作台
 * 窗口，从而避免把被占用端口上的未知进程误认为 JarPatch Studio 后端。
 * </p>
 *
 * @author 黄杰
 */
public class SystemStatus {

    private String product;
    private String instanceId;
    private String status;

    /**
     * 获取产品名。
     *
     * @return 产品名
     */
    public String getProduct() {
        return product;
    }

    /**
     * 设置产品名。
     *
     * @param product 产品名
     */
    public void setProduct(String product) {
        this.product = product;
    }

    /**
     * 获取当前实例 ID。
     *
     * @return 实例 ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 设置当前实例 ID。
     *
     * @param instanceId 实例 ID
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 获取健康状态。
     *
     * @return 状态码
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置健康状态。
     *
     * @param status 状态码
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
