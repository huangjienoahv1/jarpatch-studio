package com.jarpatch;

import com.jarpatch.config.JarPatchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * JarPatch Studio 后端启动入口。
 * <p>
 * 该类用于启动本地 HTTP 服务和 WebSocket 服务，Electron 前端通过本地接口调用
 * Jar/War 导入、分析、编辑、编译和导出能力。核心调用关系为：前端请求控制器，
 * 控制器调用服务层，服务层把项目、任务和修改记录写入本地 SQLite。
 * </p>
 *
 * @author 黄杰
 */
@SpringBootApplication
@EnableConfigurationProperties(JarPatchProperties.class)
public class JarPatchStudioApplication {

    /**
     * 启动 JarPatch Studio 后端服务。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(JarPatchStudioApplication.class, args);
    }
}
