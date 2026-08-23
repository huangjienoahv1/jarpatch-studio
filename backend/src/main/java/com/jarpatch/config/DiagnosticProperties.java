package com.jarpatch.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 诊断导出配置。
 * <p>
 * 系统诊断服务读取本配置确定应用版本、后端滚动日志位置以及导出记录上限；
 * {@code application.yml} 与 Electron 启动环境共同提供实际值。
 * </p>
 *
 * @author 黄杰
 */
@ConfigurationProperties(prefix = "jarpatch.diagnostics")
public class DiagnosticProperties {

    private static final String MESSAGE_VERSION_REQUIRED = "诊断信息中的应用版本不能为空";
    private static final String MESSAGE_LOG_FILE_REQUIRED = "诊断信息中的后端日志路径不能为空";
    private static final String MESSAGE_LIMIT_INVALID = "诊断信息记录上限必须大于零";

    private String applicationVersion;
    private String logFile;
    private int recentTaskLogLimit;
    private int recentBackendLogLimit;
    private int maxMessageLength;

    /**
     * 校验诊断配置，避免运行时导出不完整或无界数据。
     */
    @PostConstruct
    public void validate() {
        if (applicationVersion == null || applicationVersion.isBlank()) {
            throw new IllegalStateException(MESSAGE_VERSION_REQUIRED);
        }
        if (logFile == null || logFile.isBlank()) {
            throw new IllegalStateException(MESSAGE_LOG_FILE_REQUIRED);
        }
        if (recentTaskLogLimit <= 0 || recentBackendLogLimit <= 0 || maxMessageLength <= 0) {
            throw new IllegalStateException(MESSAGE_LIMIT_INVALID);
        }
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public int getRecentTaskLogLimit() {
        return recentTaskLogLimit;
    }

    public void setRecentTaskLogLimit(int recentTaskLogLimit) {
        this.recentTaskLogLimit = recentTaskLogLimit;
    }

    public int getRecentBackendLogLimit() {
        return recentBackendLogLimit;
    }

    public void setRecentBackendLogLimit(int recentBackendLogLimit) {
        this.recentBackendLogLimit = recentBackendLogLimit;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }
}
