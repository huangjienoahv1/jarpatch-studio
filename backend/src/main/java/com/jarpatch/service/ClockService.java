package com.jarpatch.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间服务。
 * <p>
 * 仓储和任务服务通过该类生成统一中国时区时间字符串，避免各处直接拼装时间格式。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ClockService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter CHINA_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取当前时间字符串。
     *
     * @return 中国时区时间字符串
     */
    public String now() {
        return LocalDateTime.now(CHINA_ZONE).format(CHINA_TIME_FORMATTER);
    }

    /**
     * 获取当前时间向后偏移指定天数的中国时区时间字符串。
     *
     * @param days 增加天数
     * @return 中国时区时间字符串
     */
    public String plusDays(long days) {
        return LocalDateTime.now(CHINA_ZONE).plusDays(days).format(CHINA_TIME_FORMATTER);
    }

    /**
     * 判断中国时区时间字符串是否已早于当前时刻。
     *
     * @param timestamp 待判断的时间字符串
     * @return 已过期或格式无效时返回 true
     */
    public boolean isExpired(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return true;
        }
        try {
            LocalDateTime target = LocalDateTime.parse(timestamp, CHINA_TIME_FORMATTER);
            return !target.isAfter(LocalDateTime.now(CHINA_ZONE));
        } catch (RuntimeException exception) {
            return true;
        }
    }
}
