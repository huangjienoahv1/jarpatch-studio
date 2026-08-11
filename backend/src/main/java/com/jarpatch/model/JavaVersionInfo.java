package com.jarpatch.model;

/**
 * 原包 Java 字节码版本识别结果。
 * <p>
 * 导入阶段从主业务 class 文件头读取 major version 并转换为 Java 特性版本；识别结果
 * 写入 projects，编译阶段再按具体目标 class 复核，禁止凭当前 JDK 猜测输出版本。
 * </p>
 *
 * @author 黄杰
 */
public class JavaVersionInfo {

    private final int featureVersion;
    private final int classMajorVersion;
    private final String evidence;

    /**
     * 创建 Java 字节码版本识别结果。
     *
     * @param featureVersion    Java 特性版本
     * @param classMajorVersion class major version
     * @param evidence          检测依据文件
     */
    public JavaVersionInfo(int featureVersion, int classMajorVersion, String evidence) {
        this.featureVersion = featureVersion;
        this.classMajorVersion = classMajorVersion;
        this.evidence = evidence;
    }

    public int getFeatureVersion() {
        return featureVersion;
    }

    public int getClassMajorVersion() {
        return classMajorVersion;
    }

    public String getEvidence() {
        return evidence;
    }
}
