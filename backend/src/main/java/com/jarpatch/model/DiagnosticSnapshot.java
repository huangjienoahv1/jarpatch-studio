package com.jarpatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 可安全导出的系统诊断快照。
 * <p>
 * 系统控制器通过诊断服务生成本模型，内容限定为版本、运行环境、实例标识、脱敏日志
 * 和统一操作 ID，不包含访问令牌、用户源码或私钥。
 * </p>
 *
 * @author 黄杰
 */
public class DiagnosticSnapshot {

    private String product;
    private String version;
    private String instanceId;
    private String generatedAt;
    private String javaVersion;
    private String javaVendor;
    private String osName;
    private String osVersion;
    private String osArchitecture;
    private String timeZone;
    private int availableProcessors;
    private long maximumMemoryBytes;
    private String backendLogPath;
    private List<DiagnosticTaskLog> recentTaskLogs = new ArrayList<>();
    private List<String> recentBackendLogs = new ArrayList<>();

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public void setJavaVendor(String javaVendor) {
        this.javaVendor = javaVendor;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getOsArchitecture() {
        return osArchitecture;
    }

    public void setOsArchitecture(String osArchitecture) {
        this.osArchitecture = osArchitecture;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public long getMaximumMemoryBytes() {
        return maximumMemoryBytes;
    }

    public void setMaximumMemoryBytes(long maximumMemoryBytes) {
        this.maximumMemoryBytes = maximumMemoryBytes;
    }

    public String getBackendLogPath() {
        return backendLogPath;
    }

    public void setBackendLogPath(String backendLogPath) {
        this.backendLogPath = backendLogPath;
    }

    public List<DiagnosticTaskLog> getRecentTaskLogs() {
        return recentTaskLogs;
    }

    public void setRecentTaskLogs(List<DiagnosticTaskLog> recentTaskLogs) {
        this.recentTaskLogs = recentTaskLogs;
    }

    public List<String> getRecentBackendLogs() {
        return recentBackendLogs;
    }

    public void setRecentBackendLogs(List<String> recentBackendLogs) {
        this.recentBackendLogs = recentBackendLogs;
    }
}
