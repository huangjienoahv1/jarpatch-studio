package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.JdkSettingsView;
import com.jarpatch.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * JDK 工具定位服务。
 * <p>
 * 编译服务通过该类定位 javac，导出服务通过 Java Zip API 打包，因此不依赖 jar 命令。
 * 定位顺序为已保存的 JDK 路径、当前 Java 运行时、JAVA_HOME、系统 PATH。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class JdkService {

    private static final String JAVA_HOME_ENV = "JAVA_HOME";
    private static final String PATH_ENV = "PATH";
    private static final String BIN_DIR = "bin";
    private static final String WINDOWS_JAVAC = "javac.exe";
    private static final String UNIX_JAVAC = "javac";
    private final AppSettingsRepository appSettingsRepository;

    /**
     * 创建 JDK 工具定位服务。
     *
     * @param appSettingsRepository 应用配置仓储
     */
    public JdkService(AppSettingsRepository appSettingsRepository) {
        this.appSettingsRepository = appSettingsRepository;
    }

    /**
     * 定位 javac 可执行文件。
     * <p>
     * 优先使用已保存的 JDK 路径；如果没有保存配置，再按当前 Java 运行时、JAVA_HOME、
     * 系统 PATH 的顺序自动检测。这样编译服务和设置页面读取的是同一份配置。
     * </p>
     *
     * @return javac 路径
     */
    public Path findJavac() {
        String executableName = isWindows() ? WINDOWS_JAVAC : UNIX_JAVAC;
        Optional<Path> configuredCandidate = findConfiguredJavac(executableName);
        if (configuredCandidate.isPresent()) {
            return configuredCandidate.get();
        }
        Path detectedCandidate = detectJavac(executableName);
        if (detectedCandidate != null) {
            return detectedCandidate;
        }
        throw new IllegalStateException(JarPatchConstants.MESSAGE_JDK_NOT_FOUND);
    }

    /**
     * 读取当前 JDK 配置视图。
     * <p>
     * 入口来自设置页面加载，实际执行点是读取 SQLite 中保存的 JDK 配置并计算当前可用的
     * javac；结果用于前端展示保存值和当前生效值。
     * </p>
     *
     * @return JDK 配置视图
     */
    public JdkSettingsView inspectCurrentSettings() {
        String executableName = isWindows() ? WINDOWS_JAVAC : UNIX_JAVAC;
        String configuredJavaHome = loadConfiguredJavaHome();
        Path configuredJavac = configuredJavaHome == null ? null : fromJavaHome(configuredJavaHome, executableName);
        Path effectiveJavac = configuredJavaHome == null ? detectJavac(executableName) : configuredJavac;
        JdkSettingsView view = new JdkSettingsView();
        view.setConfiguredJavaHome(configuredJavaHome);
        view.setConfiguredJavacPath(configuredJavac == null ? null : configuredJavac.toString());
        view.setConfiguredValid(configuredJavac != null);
        view.setEffectiveJavaHome(effectiveJavac == null ? null : resolveJavaHome(effectiveJavac));
        view.setEffectiveJavacPath(effectiveJavac == null ? null : effectiveJavac.toString());
        view.setEffectiveValid(effectiveJavac != null);
        if (configuredJavac != null) {
            view.setMessage("已使用已保存的 JDK 配置");
        } else if (configuredJavaHome != null) {
            view.setMessage(JarPatchConstants.MESSAGE_JDK_CONFIG_INVALID);
        } else if (effectiveJavac != null) {
            view.setMessage("当前使用自动检测到的 JDK");
        } else {
            view.setMessage(JarPatchConstants.MESSAGE_JDK_NOT_FOUND);
        }
        return view;
    }

    /**
     * 校验 JDK 安装目录并返回检测结果。
     * <p>
     * 入口来自设置页面保存按钮，实际执行点是对输入路径寻找 javac；只有通过校验的路径
     * 才会写入 app_settings。
     * </p>
     *
     * @param javaHome JDK 安装目录
     * @return 校验结果视图
     */
    public JdkSettingsView inspectJavaHome(String javaHome) {
        String executableName = isWindows() ? WINDOWS_JAVAC : UNIX_JAVAC;
        String normalizedJavaHome = normalizeJavaHome(javaHome);
        if (normalizedJavaHome == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_JDK_HOME_REQUIRED);
        }
        Path javac = fromJavaHome(normalizedJavaHome, executableName);
        if (javac == null) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_JDK_HOME_INVALID);
        }
        JdkSettingsView view = new JdkSettingsView();
        view.setConfiguredJavaHome(normalizedJavaHome);
        view.setConfiguredJavacPath(javac.toString());
        view.setConfiguredValid(true);
        view.setEffectiveJavaHome(resolveJavaHome(javac));
        view.setEffectiveJavacPath(javac.toString());
        view.setEffectiveValid(true);
        view.setMessage("JDK 校验通过");
        return view;
    }

    /**
     * 从 Java Home 解析 javac。
     *
     * @param javaHome       Java Home 路径
     * @param executableName javac 文件名
     * @return javac 路径，找不到时返回 null
     */
    private Path fromJavaHome(String javaHome, String executableName) {
        String normalizedJavaHome = normalizeJavaHome(javaHome);
        if (normalizedJavaHome == null) {
            return null;
        }
        Path home = Paths.get(normalizedJavaHome).toAbsolutePath().normalize();
        Path candidate = home.resolve(BIN_DIR).resolve(executableName);
        if (Files.exists(candidate)) {
            return candidate;
        }
        Path parentCandidate = home.getParent() == null ? null : home.getParent().resolve(BIN_DIR).resolve(executableName);
        if (parentCandidate != null && Files.exists(parentCandidate)) {
            return parentCandidate;
        }
        return null;
    }

    /**
     * 从系统 PATH 中查找 javac。
     *
     * @param executableName javac 文件名
     * @return javac 路径，找不到时返回 null
     */
    private Path fromPath(String executableName) {
        String pathValue = System.getenv(PATH_ENV);
        if (pathValue == null || pathValue.trim().isEmpty()) {
            return null;
        }
        String[] parts = pathValue.split(File.pathSeparator);
        for (String part : parts) {
            Path candidate = Paths.get(part).resolve(executableName).toAbsolutePath().normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 读取已保存的 JDK 配置并解析 javac。
     *
     * @param executableName javac 文件名
     * @return 已保存配置对应的 javac，未保存时返回空
     */
    private Optional<Path> findConfiguredJavac(String executableName) {
        String configuredJavaHome = loadConfiguredJavaHome();
        if (configuredJavaHome == null) {
            return Optional.empty();
        }
        Path configuredJavac = fromJavaHome(configuredJavaHome, executableName);
        if (configuredJavac == null) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_JDK_CONFIG_INVALID);
        }
        return Optional.of(configuredJavac);
    }

    /**
     * 按当前运行环境自动检测 javac。
     *
     * @param executableName javac 文件名
     * @return javac 路径，找不到时返回 null
     */
    private Path detectJavac(String executableName) {
        Path javaHomeCandidate = fromJavaHome(System.getProperty("java.home"), executableName);
        if (javaHomeCandidate != null) {
            return javaHomeCandidate;
        }
        Path envCandidate = fromJavaHome(System.getenv(JAVA_HOME_ENV), executableName);
        if (envCandidate != null) {
            return envCandidate;
        }
        return fromPath(executableName);
    }

    /**
     * 读取已保存的 JDK 安装目录。
     *
     * @return 已保存的 JDK 安装目录，未配置时返回 null
     */
    private String loadConfiguredJavaHome() {
        return appSettingsRepository.findValue(JarPatchConstants.SETTING_KEY_JDK_HOME).orElse(null);
    }

    /**
     * 标准化 JDK 安装目录。
     *
     * @param javaHome 原始输入路径
     * @return 去除首尾空白后的路径，空值时返回 null
     */
    private String normalizeJavaHome(String javaHome) {
        if (javaHome == null) {
            return null;
        }
        String trimmed = javaHome.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 从 javac 路径反推 JDK 安装目录。
     *
     * @param javacPath javac 路径
     * @return JDK 安装目录
     */
    private String resolveJavaHome(Path javacPath) {
        Path binDir = javacPath.getParent();
        if (binDir == null) {
            return javacPath.toString();
        }
        Path homeDir = binDir.getParent();
        return homeDir == null ? binDir.toString() : homeDir.toString();
    }

    /**
     * 判断当前系统是否为 Windows。
     *
     * @return Windows 返回 true
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
