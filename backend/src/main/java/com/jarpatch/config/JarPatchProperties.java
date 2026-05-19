package com.jarpatch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JarPatch Studio 本地配置属性。
 * <p>
 * 该类读取 application.yml 中的工作区根目录配置，工作区服务会根据该目录创建项目、
 * 原始包、解压结果、反编译源码、编译输出和导出文件。
 * </p>
 *
 * @author 黄杰
 */
@ConfigurationProperties(prefix = "jarpatch")
public class JarPatchProperties {

    private String workspaceRoot;

    /**
     * 获取工作区根目录。
     *
     * @return 工作区根目录路径
     */
    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    /**
     * 设置工作区根目录。
     *
     * @param workspaceRoot 工作区根目录路径
     */
    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }
}
