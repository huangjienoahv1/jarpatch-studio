package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.service.RuntimeClassComparisonService;
import com.jarpatch.service.RuntimeClassComparisonService.Comparison;
import com.jarpatch.service.RuntimeSourceCompileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 本地令牌保护的运行时 class 对照、反编译和单类编译入口，供桌面工作台使用。
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/runtime")
public class RuntimeClassController {

    private final RuntimeClassComparisonService comparisonService;
    private final RuntimeSourceCompileService compileService;

    /**
     * 注入运行时 class 读取和单类补丁编译服务。
     */
    public RuntimeClassController(RuntimeClassComparisonService comparisonService, RuntimeSourceCompileService compileService) {
        this.comparisonService = comparisonService;
        this.compileService = compileService;
    }

    /**
     * 比较所选部署包和运行中 class 快照。
     *
     * @param request 本地包路径、类名、运行时字节码和可选的候选序号
     * @return 摘要、包内候选和可用源码
     */
    @PostMapping("/class-comparison")
    public ApiResponse<Comparison> compare(@RequestBody ComparisonRequest request) throws IOException {
        return ApiResponse.success(comparisonService.compare(
                request.archive(), request.className(), request.contents(), request.entryIndex(), request.expectedSha256()));
    }

    /**
     * 为当前 JVM 的快照生成可编辑的 Java 源码。
     * @param request 当前加载类的全名和快照内容
     * @return 快照对应的反编译源码
     */
    @PostMapping("/class-source")
    public ApiResponse<String> decompile(@RequestBody SourceRequest request) throws IOException {
        return ApiResponse.success(comparisonService.decompileSnapshot(request.className(), request.contents()));
    }

    /**
     * 按快照中的 class 版本和本地部署包编译编辑稿。
     * @param request 本地部署包、原 class 快照及修改后的 Java 源码
     * @return 可供桌面主进程校验的 Base64 class
     */
    @PostMapping("/class-compile")
    public ApiResponse<String> compile(@RequestBody CompileRequest request) throws IOException, InterruptedException {
        return ApiResponse.success(compileService.compile(
                request.archive(), request.className(), request.contents(), request.source()));
    }

    /** 桌面端提交的单类对照请求。 */
    public record ComparisonRequest(String archive, String className, String contents, Integer entryIndex,
                                    String expectedSha256) { }

    /** 在线源码编辑使用的单类快照。 */
    public record SourceRequest(String className, String contents) { }

    /** 编译在线编辑稿所需的依赖包、快照和源码。 */
    public record CompileRequest(String archive, String className, String contents, String source) { }
}
