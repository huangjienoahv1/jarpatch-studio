package com.jarpatch.controller;

import com.jarpatch.common.ApiResponse;
import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.AnalysisReport;
import com.jarpatch.model.ExportProjectRequest;
import com.jarpatch.model.FileNode;
import com.jarpatch.model.ImportProjectRequest;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.ProjectImportInspection;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.SaveContentRequest;
import com.jarpatch.model.SearchResult;
import com.jarpatch.service.AnalysisService;
import com.jarpatch.service.CompileService;
import com.jarpatch.service.ExportService;
import com.jarpatch.service.FileContentService;
import com.jarpatch.service.FileTreeService;
import com.jarpatch.service.ProjectInspectionService;
import com.jarpatch.service.ProjectService;
import com.jarpatch.service.SearchService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 项目工作台 HTTP 控制器。
 * <p>
 * Electron 前端通过该控制器完成导入、项目列表、文件树、内容读写、分析、编译和导出。
 * 控制器只负责接口编排，实际执行点在对应服务层，结果写入工作区和 SQLite。
 * </p>
 *
 * @author 黄杰
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final FileTreeService fileTreeService;
    private final FileContentService fileContentService;
    private final AnalysisService analysisService;
    private final CompileService compileService;
    private final ExportService exportService;
    private final SearchService searchService;
    private final ProjectInspectionService projectInspectionService;

    /**
     * 创建项目控制器。
     *
     * @param projectService     项目服务
     * @param fileTreeService    文件树服务
     * @param fileContentService 文件内容服务
     * @param analysisService    分析服务
     * @param compileService     编译服务
     * @param exportService      导出服务
     * @param searchService      搜索服务
     * @param projectInspectionService 导入前预解析服务
     */
    public ProjectController(ProjectService projectService,
                             FileTreeService fileTreeService,
                             FileContentService fileContentService,
                             AnalysisService analysisService,
                             CompileService compileService,
                             ExportService exportService,
                             SearchService searchService,
                             ProjectInspectionService projectInspectionService) {
        this.projectService = projectService;
        this.fileTreeService = fileTreeService;
        this.fileContentService = fileContentService;
        this.analysisService = analysisService;
        this.compileService = compileService;
        this.exportService = exportService;
        this.searchService = searchService;
        this.projectInspectionService = projectInspectionService;
    }

    /**
     * 导入前预解析 Jar 或 War。
     * <p>
     * 前端打开文件后先调用该接口，实际执行点是 ProjectInspectionService，只读取原始包内
     * pom.xml 和嵌套 Jar 候选项，不创建工作区、不写 SQLite。
     * </p>
     *
     * @param request 预解析请求
     * @return 导入前预解析结果
     * @throws IOException 读取原始包失败时抛出
     */
    @PostMapping("/inspect")
    public ApiResponse<ProjectImportInspection> inspectProject(@RequestBody ImportProjectRequest request) throws IOException {
        return ApiResponse.success(projectInspectionService.inspect(request.getFilePath()));
    }

    /**
     * 导入 Jar 或 War 项目。
     *
     * @param request 导入请求
     * @return 项目记录
     * @throws IOException 导入失败时抛出
     */
    @PostMapping("/import")
    public ApiResponse<ProjectRecord> importProject(@RequestBody ImportProjectRequest request) throws IOException {
        return ApiResponse.success(projectService.importProject(request.getFilePath(), request.getSelectedNestedJars()));
    }

    /**
     * 查询本地项目历史。
     *
     * @return 项目记录列表
     */
    @GetMapping
    public ApiResponse<List<ProjectRecord>> listProjects() {
        return ApiResponse.success(projectService.findAll());
    }

    /**
     * 删除左侧项目历史记录。
     * <p>
     * 入口是前端项目历史删除按钮，实际执行点在项目服务和仓储层，结果写入 SQLite；
     * 该接口只删除历史记录和关联数据库记录，不删除用户本地工作区文件。
     * </p>
     *
     * @param projectId 项目 ID
     * @return 删除结果文案
     */
    @DeleteMapping("/{projectId}")
    public ApiResponse<String> deleteProjectHistory(@PathVariable("projectId") String projectId) {
        projectService.deleteHistory(projectId);
        return ApiResponse.success(JarPatchConstants.MESSAGE_PROJECT_HISTORY_DELETED);
    }

    /**
     * 查询项目文件树。
     *
     * @param projectId 项目 ID
     * @return 文件树根节点
     * @throws IOException 读取文件树失败时抛出
     */
    @GetMapping("/{projectId}/tree")
    public ApiResponse<FileNode> tree(@PathVariable("projectId") String projectId) throws IOException {
        ProjectRecord project = requireProject(projectId);
        return ApiResponse.success(fileTreeService.buildTree(project));
    }

    /**
     * 读取文件内容。
     *
     * @param projectId 项目 ID
     * @param path      文件树相对路径
     * @return 文件内容
     * @throws IOException 读取失败时抛出
     */
    @GetMapping("/{projectId}/files/content")
    public ApiResponse<String> readContent(@PathVariable("projectId") String projectId, @RequestParam("path") String path) throws IOException {
        ProjectRecord project = requireProject(projectId);
        return ApiResponse.success(fileContentService.read(project, path));
    }

    /**
     * 保存文件内容。
     *
     * @param projectId 项目 ID
     * @param request   保存请求
     * @return 保存结果
     * @throws IOException 写入失败时抛出
     */
    @PutMapping("/{projectId}/files/content")
    public ApiResponse<String> saveContent(@PathVariable("projectId") String projectId, @RequestBody SaveContentRequest request) throws IOException {
        ProjectRecord project = requireProject(projectId);
        fileContentService.save(project, request.getPath(), request.getContent());
        return ApiResponse.success("保存完成");
    }

    /**
     * 搜索项目文件名和内容。
     *
     * @param projectId 项目 ID
     * @param keyword   搜索关键词
     * @return 搜索结果列表
     * @throws IOException 搜索失败时抛出
     */
    @GetMapping("/{projectId}/search")
    public ApiResponse<List<SearchResult>> search(@PathVariable("projectId") String projectId,
                                                  @RequestParam("keyword") String keyword) throws IOException {
        ProjectRecord project = requireProject(projectId);
        return ApiResponse.success(searchService.search(project, keyword));
    }

    /**
     * 执行项目结构分析。
     *
     * @param projectId 项目 ID
     * @return 分析报告
     * @throws IOException 分析失败时抛出
     */
    @PostMapping("/{projectId}/analyze")
    public ApiResponse<AnalysisReport> analyze(@PathVariable("projectId") String projectId) throws IOException {
        ProjectRecord project = requireProject(projectId);
        return ApiResponse.success(analysisService.analyze(project));
    }

    /**
     * 编译已修改 Java 文件。
     *
     * @param projectId 项目 ID
     * @return 编译结果
     * @throws IOException          编译文件读写失败时抛出
     * @throws InterruptedException javac 执行被中断时抛出
     */
    @PostMapping("/{projectId}/compile")
    public ApiResponse<OperationResult> compile(@PathVariable("projectId") String projectId) throws IOException, InterruptedException {
        ProjectRecord project = requireProject(projectId);
        return ApiResponse.success(compileService.compile(project));
    }

    /**
     * 导出修改后的 Jar 或 War。
     *
     * @param projectId 项目 ID
     * @param request   导出请求
     * @return 导出结果
     * @throws IOException 导出失败时抛出
     */
    @PostMapping("/{projectId}/export")
    public ApiResponse<OperationResult> export(@PathVariable("projectId") String projectId, @RequestBody(required = false) ExportProjectRequest request) throws IOException {
        ProjectRecord project = requireProject(projectId);
        String outputPath = request == null ? null : request.getOutputPath();
        return ApiResponse.success(exportService.export(project, outputPath));
    }

    /**
     * 查询项目，不存在时抛出清晰业务错误。
     *
     * @param projectId 项目 ID
     * @return 项目记录
     */
    private ProjectRecord requireProject(String projectId) {
        return projectService.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(JarPatchConstants.MESSAGE_PROJECT_NOT_FOUND));
    }
}
