package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.config.DiagnosticProperties;
import com.jarpatch.config.LocalAccessProperties;
import com.jarpatch.model.DiagnosticSnapshot;
import com.jarpatch.model.DiagnosticTaskLog;
import com.jarpatch.model.TaskLogRecord;
import com.jarpatch.repository.TaskLogRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统诊断快照服务。
 * <p>
 * 系统控制器是入口，本服务读取运行环境、SQLite 任务日志和后端滚动日志，在服务端完成
 * 单行限制、路径与敏感值脱敏，结果由 Electron 保存为 JSON 文件。结果不写入数据库，
 * 也不读取项目源码或访问令牌。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class DiagnosticService {

    private static final Pattern WINDOWS_ABSOLUTE_PATH_PATTERN =
            Pattern.compile("(?i)(?:[a-z]:\\\\|\\\\\\\\)[^\\s,;]+");
    private static final Pattern UNIX_USER_PATH_PATTERN =
            Pattern.compile("/(?:home|Users)/[^/\\s]+(?:/[^\\s,;]+)?");
    private static final Pattern SENSITIVE_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)(token|password|secret|private[_-]?key)\\s*[=:]\\s*[^\\s,;]+"
    );
    private static final String REDACTED_PATH = "[路径已脱敏]";
    private static final String REDACTED_SENSITIVE_VALUE = "[敏感值已脱敏]";
    private static final String MESSAGE_TRUNCATED_SUFFIX = "...（已截断）";
    private static final String USER_HOME_PLACEHOLDER = "${USER_HOME}";
    private static final String USER_NAME_PLACEHOLDER = "${USER_NAME}";

    private final DiagnosticProperties diagnosticProperties;
    private final LocalAccessProperties localAccessProperties;
    private final TaskLogRepository taskLogRepository;
    private final ClockService clockService;

    /**
     * 创建系统诊断快照服务。
     *
     * @param diagnosticProperties 诊断导出配置
     * @param localAccessProperties 当前本地实例身份
     * @param taskLogRepository 任务日志仓储
     * @param clockService 中国时区时间服务
     */
    public DiagnosticService(DiagnosticProperties diagnosticProperties,
                             LocalAccessProperties localAccessProperties,
                             TaskLogRepository taskLogRepository,
                             ClockService clockService) {
        this.diagnosticProperties = diagnosticProperties;
        this.localAccessProperties = localAccessProperties;
        this.taskLogRepository = taskLogRepository;
        this.clockService = clockService;
    }

    /**
     * 生成可由用户导出的脱敏诊断快照。
     *
     * @return 版本、环境、实例 ID、近期任务日志和后端日志
     */
    public DiagnosticSnapshot createSnapshot() {
        DiagnosticSnapshot snapshot = new DiagnosticSnapshot();
        snapshot.setProduct(JarPatchConstants.PRODUCT_NAME);
        snapshot.setVersion(diagnosticProperties.getApplicationVersion());
        snapshot.setInstanceId(localAccessProperties.getInstanceId());
        snapshot.setGeneratedAt(clockService.now());
        snapshot.setJavaVersion(System.getProperty("java.version"));
        snapshot.setJavaVendor(System.getProperty("java.vendor"));
        snapshot.setOsName(System.getProperty("os.name"));
        snapshot.setOsVersion(System.getProperty("os.version"));
        snapshot.setOsArchitecture(System.getProperty("os.arch"));
        snapshot.setTimeZone(TimeZone.getDefault().getID());
        snapshot.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
        snapshot.setMaximumMemoryBytes(Runtime.getRuntime().maxMemory());
        snapshot.setBackendLogPath(sanitizePath(diagnosticProperties.getLogFile()));

        // 任务 ID 是导入、分析、编译和导出的统一操作 ID，诊断记录据此串联状态流转。
        List<DiagnosticTaskLog> taskLogs = taskLogRepository
                .findRecent(diagnosticProperties.getRecentTaskLogLimit())
                .stream()
                .map(this::toDiagnosticTaskLog)
                .toList();
        snapshot.setRecentTaskLogs(taskLogs);
        snapshot.setRecentBackendLogs(readRecentBackendLogs());
        return snapshot;
    }

    /**
     * 把数据库任务日志转换为脱敏诊断日志。
     *
     * @param record 数据库任务日志
     * @return 使用任务 ID 作为操作 ID 的诊断记录
     */
    private DiagnosticTaskLog toDiagnosticTaskLog(TaskLogRecord record) {
        DiagnosticTaskLog log = new DiagnosticTaskLog();
        log.setOperationId(record.getTaskId());
        log.setProgress(record.getProgress());
        log.setStatus(record.getStatus());
        log.setMessage(sanitizeMessage(record.getMessage()));
        log.setCreatedAt(record.getCreatedAt());
        return log;
    }

    /**
     * 从滚动日志主文件读取末尾指定数量的脱敏日志行。
     *
     * @return 按原顺序排列的近期后端日志；文件尚未生成时返回空列表
     */
    private List<String> readRecentBackendLogs() {
        Path logPath = Paths.get(diagnosticProperties.getLogFile()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(logPath)) {
            return List.of();
        }
        Deque<String> recentLines = new ArrayDeque<>(diagnosticProperties.getRecentBackendLogLimit());
        try (BufferedReader reader = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (recentLines.size() == diagnosticProperties.getRecentBackendLogLimit()) {
                    recentLines.removeFirst();
                }
                recentLines.addLast(sanitizeMessage(line));
            }
        }
        catch (IOException exception) {
            recentLines.clear();
            recentLines.add("日志读取失败：" + sanitizeMessage(exception.getMessage()));
        }
        return new ArrayList<>(recentLines);
    }

    /**
     * 按固定隐私规则清理日志文本，只保留首行并限制长度。
     *
     * @param message 原始日志消息
     * @return 不含敏感赋值和绝对用户路径的单行消息
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return JarPatchConstants.EMPTY_TEXT;
        }
        String firstLine = message.split("\\R", 2)[0];
        String sanitized = SENSITIVE_ASSIGNMENT_PATTERN.matcher(firstLine)
                .replaceAll("$1=" + REDACTED_SENSITIVE_VALUE);
        String userName = System.getProperty("user.name");
        if (userName != null && !userName.isBlank()) {
            sanitized = Pattern.compile(Pattern.quote(userName), Pattern.CASE_INSENSITIVE)
                    .matcher(sanitized)
                    .replaceAll(Matcher.quoteReplacement(USER_NAME_PLACEHOLDER));
        }
        sanitized = sanitizePath(sanitized);
        if (sanitized.length() <= diagnosticProperties.getMaxMessageLength()) {
            return sanitized;
        }
        return sanitized.substring(0, diagnosticProperties.getMaxMessageLength()) + MESSAGE_TRUNCATED_SUFFIX;
    }

    /**
     * 隐藏用户主目录和其他绝对用户路径。
     *
     * @param value 可能包含路径的文本
     * @return 脱敏后的文本
     */
    private String sanitizePath(String value) {
        String userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize().toString();
        String sanitized = value.replace(userHome, USER_HOME_PLACEHOLDER);
        sanitized = WINDOWS_ABSOLUTE_PATH_PATTERN.matcher(sanitized).replaceAll(REDACTED_PATH);
        return UNIX_USER_PATH_PATTERN.matcher(sanitized).replaceAll(REDACTED_PATH);
    }
}
