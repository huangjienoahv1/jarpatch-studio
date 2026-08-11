package com.jarpatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jar/War 资源安全限制配置。
 * <p>
 * 导入预解析和实际解压共享本配置，统一约束原包大小、条目数量、展开大小、单文件大小、
 * 压缩比和路径深度，避免恶意或异常压缩包耗尽本机磁盘与内存。
 * </p>
 *
 * @author 黄杰
 */
@ConfigurationProperties(prefix = "jarpatch.archive-limits")
public class ArchiveLimitsProperties {

    private long maxArchiveBytes;
    private int maxEntryCount;
    private long maxUncompressedBytes;
    private long maxEntryBytes;
    private double maxCompressionRatio;
    private int maxPathDepth;

    public long getMaxArchiveBytes() {
        return maxArchiveBytes;
    }

    public void setMaxArchiveBytes(long maxArchiveBytes) {
        this.maxArchiveBytes = maxArchiveBytes;
    }

    public int getMaxEntryCount() {
        return maxEntryCount;
    }

    public void setMaxEntryCount(int maxEntryCount) {
        this.maxEntryCount = maxEntryCount;
    }

    public long getMaxUncompressedBytes() {
        return maxUncompressedBytes;
    }

    public void setMaxUncompressedBytes(long maxUncompressedBytes) {
        this.maxUncompressedBytes = maxUncompressedBytes;
    }

    public long getMaxEntryBytes() {
        return maxEntryBytes;
    }

    public void setMaxEntryBytes(long maxEntryBytes) {
        this.maxEntryBytes = maxEntryBytes;
    }

    public double getMaxCompressionRatio() {
        return maxCompressionRatio;
    }

    public void setMaxCompressionRatio(double maxCompressionRatio) {
        this.maxCompressionRatio = maxCompressionRatio;
    }

    public int getMaxPathDepth() {
        return maxPathDepth;
    }

    public void setMaxPathDepth(int maxPathDepth) {
        this.maxPathDepth = maxPathDepth;
    }
}
