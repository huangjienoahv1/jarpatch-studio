package com.jarpatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.ProjectSettings;
import com.jarpatch.repository.ProjectRepository;
import com.jarpatch.repository.ProjectSettingsRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * 项目级设置服务。
 * <p>
 * 项目设置接口通过本服务读取和校验设置；目标 Java 版本始终来自原包 class 检测，
 * 导出服务和文件服务分别消费默认目录与可编辑大小上限。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class ProjectSettingsService {

    private final ProjectRepository projectRepository;
    private final ProjectSettingsRepository projectSettingsRepository;
    private final ClockService clockService;
    private final ObjectMapper objectMapper;

    /**
     * 创建项目设置服务。
     *
     * @param projectRepository         项目仓储
     * @param projectSettingsRepository 项目设置仓储
     * @param clockService              时间服务
     * @param objectMapper              JSON 解析器
     */
    public ProjectSettingsService(ProjectRepository projectRepository,
                                  ProjectSettingsRepository projectSettingsRepository,
                                  ClockService clockService,
                                  ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.projectSettingsRepository = projectSettingsRepository;
        this.clockService = clockService;
        this.objectMapper = objectMapper;
    }

    /**
     * 读取项目设置；尚未保存时返回明确默认值。
     *
     * @param projectId 项目 ID
     * @return 项目设置
     */
    public ProjectSettings get(String projectId) {
        ProjectRecord project = requireProject(projectId);
        ProjectSettings settings = projectSettingsRepository.findByProjectId(projectId)
                .orElseGet(() -> createDefaults(project));
        settings.setTargetJavaVersion(project.getTargetJavaVersion());
        return settings;
    }

    /**
     * 校验并保存项目设置。
     *
     * @param projectId 项目 ID
     * @param requested 用户提交的完整设置
     * @return 保存后的设置
     * @throws JsonProcessingException 设置序列化失败时抛出
     */
    public ProjectSettings save(String projectId, ProjectSettings requested) throws JsonProcessingException {
        ProjectRecord project = requireProject(projectId);
        if (!Objects.equals(project.getTargetJavaVersion(), requested.getTargetJavaVersion())) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_SETTING_JAVA_VERSION_MISMATCH);
        }
        ProjectSettings settings = new ProjectSettings();
        settings.setProjectId(projectId);
        settings.setTargetJavaVersion(project.getTargetJavaVersion());
        settings.setDefaultExportDirectory(validateExportDirectory(requested.getDefaultExportDirectory()));
        settings.setSelectedNestedJars(normalizeNestedJars(requested.getSelectedNestedJars()));
        settings.setMaxEditableFileBytes(validateFileLimit(requested.getMaxEditableFileBytes()));
        settings.setUiPreferencesJson(validateUiPreferences(requested.getUiPreferencesJson()));
        projectSettingsRepository.upsert(settings, clockService.now());
        return get(projectId);
    }

    /**
     * 获取项目允许编辑的单文件字节上限。
     *
     * @param projectId 项目 ID
     * @return 字节上限
     */
    public long maxEditableFileBytes(String projectId) {
        return get(projectId).getMaxEditableFileBytes();
    }

    /**
     * 获取项目默认导出目录。
     *
     * @param projectId 项目 ID
     * @return 未配置时为 null
     */
    public String defaultExportDirectory(String projectId) {
        return get(projectId).getDefaultExportDirectory();
    }

    /**
     * 创建未持久化的项目默认设置。
     *
     * @param project 项目记录
     * @return 默认设置
     */
    private ProjectSettings createDefaults(ProjectRecord project) {
        ProjectSettings settings = new ProjectSettings();
        settings.setProjectId(project.getId());
        settings.setTargetJavaVersion(project.getTargetJavaVersion());
        settings.setSelectedNestedJars(new ArrayList<>());
        settings.setMaxEditableFileBytes(JarPatchConstants.DEFAULT_MAX_EDITABLE_FILE_BYTES);
        settings.setUiPreferencesJson(JarPatchConstants.EMPTY_JSON_OBJECT);
        settings.setUpdatedAt(project.getUpdatedAt());
        return settings;
    }

    /**
     * 校验默认导出目录为绝对目录路径。
     *
     * @param value 用户输入
     * @return 规范化目录字符串，空值返回 null
     */
    private String validateExportDirectory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Path path = Paths.get(value).toAbsolutePath().normalize();
        if (!Paths.get(value).isAbsolute()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_SETTING_EXPORT_DIRECTORY_INVALID);
        }
        return path.toString();
    }

    /**
     * 规范化并去重嵌套 Jar 相对路径。
     *
     * @param values 用户选择
     * @return 稳定有序的相对路径
     */
    private List<String> normalizeNestedJars(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return new ArrayList<>();
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String relative = value.replace('\\', '/');
            if (relative.startsWith(JarPatchConstants.ZIP_SEPARATOR)
                    || relative.contains("../") || relative.equals("..")) {
                throw new IllegalArgumentException(JarPatchConstants.MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH);
            }
            normalized.add(relative);
        }
        return new ArrayList<>(normalized);
    }

    /**
     * 校验项目可编辑文件大小范围。
     *
     * @param value 字节数
     * @return 合法字节数
     */
    private long validateFileLimit(long value) {
        if (value < JarPatchConstants.MIN_MAX_EDITABLE_FILE_BYTES
                || value > JarPatchConstants.MAX_MAX_EDITABLE_FILE_BYTES) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_SETTING_FILE_LIMIT_INVALID);
        }
        return value;
    }

    /**
     * 校验界面偏好是 JSON 对象并输出规范 JSON。
     *
     * @param value JSON 文本
     * @return 规范 JSON 对象文本
     * @throws JsonProcessingException JSON 解析失败时抛出
     */
    private String validateUiPreferences(String value) throws JsonProcessingException {
        String json = value == null || value.isBlank() ? JarPatchConstants.EMPTY_JSON_OBJECT : value;
        JsonNode node = objectMapper.readTree(json);
        if (!node.isObject()) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_SETTING_UI_PREFERENCES_INVALID);
        }
        return objectMapper.writeValueAsString(node);
    }

    /**
     * 读取项目，不存在时抛出业务异常。
     *
     * @param projectId 项目 ID
     * @return 项目记录
     */
    private ProjectRecord requireProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_NOT_FOUND));
    }
}
