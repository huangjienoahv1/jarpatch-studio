package com.jarpatch.model;

import java.nio.file.Path;

/**
 * 已验证的 javac 编译器信息。
 * <p>
 * JdkService 执行 javac -version 后创建该模型，CompileService 使用可执行路径和明确的
 * Java 特性版本决定是否允许编译，并为目标字节码写入严格的 --release 参数。
 * </p>
 *
 * @author 黄杰
 */
public class JdkCompilerInfo {

    private final Path javacPath;
    private final int featureVersion;
    private final String versionText;

    /**
     * 创建已验证的编译器信息。
     *
     * @param javacPath     javac 可执行路径
     * @param featureVersion Java 特性版本
     * @param versionText    javac 原始版本文本
     */
    public JdkCompilerInfo(Path javacPath, int featureVersion, String versionText) {
        this.javacPath = javacPath;
        this.featureVersion = featureVersion;
        this.versionText = versionText;
    }

    public Path getJavacPath() {
        return javacPath;
    }

    public int getFeatureVersion() {
        return featureVersion;
    }

    public String getVersionText() {
        return versionText;
    }
}
