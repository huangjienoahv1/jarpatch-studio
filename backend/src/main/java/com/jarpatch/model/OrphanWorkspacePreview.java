package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 孤立工作区批量清理预览。
 * <p>
 * 预览包含完整候选快照和一次性确认标识；删除入口会重新统计并验证快照未变化。
 * </p>
 *
 * @author 黄杰
 */
public class OrphanWorkspacePreview {

    private List<OrphanWorkspaceEntry> entries = new ArrayList<>();
    private String confirmationId;
    private String expiresAt;

    public List<OrphanWorkspaceEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<OrphanWorkspaceEntry> entries) {
        this.entries = entries;
    }

    public String getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
