package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JDK 工具定位服务。
 * <p>
 * 编译服务通过该类定位 javac，导出服务通过 Java Zip API 打包，因此不依赖 jar 命令。
 * 定位顺序为当前 Java 运行时、JAVA_HOME、系统 PATH。
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

    /**
     * 定位 javac 可执行文件。
     *
     * @return javac 路径
     */
    public Path findJavac() {
        String executableName = isWindows() ? WINDOWS_JAVAC : UNIX_JAVAC;
        Path javaHomeCandidate = fromJavaHome(System.getProperty("java.home"), executableName);
        if (javaHomeCandidate != null) {
            return javaHomeCandidate;
        }
        Path envCandidate = fromJavaHome(System.getenv(JAVA_HOME_ENV), executableName);
        if (envCandidate != null) {
            return envCandidate;
        }
        Path pathCandidate = fromPath(executableName);
        if (pathCandidate != null) {
            return pathCandidate;
        }
        throw new IllegalStateException(JarPatchConstants.MESSAGE_JDK_NOT_FOUND);
    }

    /**
     * 从 Java Home 解析 javac。
     *
     * @param javaHome       Java Home 路径
     * @param executableName javac 文件名
     * @return javac 路径，找不到时返回 null
     */
    private Path fromJavaHome(String javaHome, String executableName) {
        if (javaHome == null || javaHome.trim().isEmpty()) {
            return null;
        }
        Path home = Paths.get(javaHome).toAbsolutePath().normalize();
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
     * 判断当前系统是否为 Windows。
     *
     * @return Windows 返回 true
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
