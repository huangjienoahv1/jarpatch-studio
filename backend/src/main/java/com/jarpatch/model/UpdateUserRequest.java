package com.jarpatch.model;

/**
 * 管理员编辑用户资料、角色与启停状态请求。
 * <p>
 * 由用户管理界面提交，服务层会校验不能禁用或降级自己及最后一个启用管理员。
 * </p>
 *
 * @author 黄杰
 */
public class UpdateUserRequest {

    private String displayName;
    private String role;
    private String status;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
