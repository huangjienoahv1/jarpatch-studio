package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 项目导出前差异报告。
 * <p>
 * 报告分别列出源码差异、资源差异和已写回 class 清单，前端在导出前展示该报告供用户
 * 明确确认。
 * </p>
 *
 * @author 黄杰
 */
public class DiffReport {

    private List<FileDiff> sourceDiffs = new ArrayList<>();
    private List<FileDiff> resourceDiffs = new ArrayList<>();
    private List<String> compiledArtifacts = new ArrayList<>();

    public List<FileDiff> getSourceDiffs() {
        return sourceDiffs;
    }

    public void setSourceDiffs(List<FileDiff> sourceDiffs) {
        this.sourceDiffs = sourceDiffs;
    }

    public List<FileDiff> getResourceDiffs() {
        return resourceDiffs;
    }

    public void setResourceDiffs(List<FileDiff> resourceDiffs) {
        this.resourceDiffs = resourceDiffs;
    }

    public List<String> getCompiledArtifacts() {
        return compiledArtifacts;
    }

    public void setCompiledArtifacts(List<String> compiledArtifacts) {
        this.compiledArtifacts = compiledArtifacts;
    }
}
