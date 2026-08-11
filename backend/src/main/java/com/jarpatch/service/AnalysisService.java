package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.common.RiskLevel;
import com.jarpatch.model.AnalysisReport;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.RiskItem;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.FileChangeRepository;
import com.jarpatch.repository.AnalysisReportRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 包结构分析服务。
 * <p>
 * 分析接口调用该服务扫描 extracted 和 sources 目录，识别入口类、依赖、签名文件、
 * 嵌套 Jar、多版本目录和混淆迹象，并把分析任务进度写入 SQLite 与 WebSocket。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class AnalysisService {

    private static final String TASK_TYPE_ANALYZE = "ANALYZE";
    private static final String RISK_SIGNATURE_TITLE = "存在签名文件";
    private static final String RISK_NESTED_JAR_TITLE = "存在嵌套 Jar";
    private static final String RISK_MULTI_RELEASE_TITLE = "存在多版本类目录";
    private static final String RISK_OBFUSCATION_TITLE = "可能存在混淆代码";
    private static final String MESSAGE_ANALYSIS_START = "开始分析包结构";
    private static final String MESSAGE_ANALYSIS_MANIFEST = "读取 Manifest 和入口类";
    private static final String MESSAGE_ANALYSIS_STATISTICS = "统计 class 和依赖";
    private static final String MESSAGE_ANALYSIS_RISKS = "识别导出风险";
    private static final String MESSAGE_ANALYSIS_SUCCESS = "分析完成，报告已返回前端";
    private static final String MESSAGE_ANALYSIS_FAILED = "分析失败";
    private static final String RISK_SIGNATURE_DETAIL = "修改 class 或资源后，原签名通常会失效，导出包可能无法通过签名校验。";
    private static final String RISK_NESTED_JAR_DETAIL = "包内存在依赖 Jar，导出时会保留结构；Spring Boot 依赖 Jar 会按未压缩方式写入。";
    private static final String RISK_MULTI_RELEASE_DETAIL = "检测到 META-INF/versions，多版本类可能需要按目标 Java 版本单独验证。";
    private static final String RISK_OBFUSCATION_DETAIL = "检测到较多短类名，可能是混淆代码，反编译后的源码不一定能直接重新编译。";
    private static final int PROGRESS_MANIFEST = 20;
    private static final int PROGRESS_STATISTICS = 40;
    private static final int PROGRESS_RISKS = 70;

    private final WorkspaceService workspaceService;
    private final FileChangeRepository fileChangeRepository;
    private final TaskService taskService;
    private final AnalysisReportRepository analysisReportRepository;
    private final ClockService clockService;

    /**
     * 创建分析服务。
     *
     * @param workspaceService     工作区服务
     * @param fileChangeRepository 修改记录仓储
     * @param taskService          任务服务
     * @param analysisReportRepository 分析报告仓储
     * @param clockService         时间服务
     */
    public AnalysisService(WorkspaceService workspaceService,
                           FileChangeRepository fileChangeRepository,
                           TaskService taskService,
                           AnalysisReportRepository analysisReportRepository,
                           ClockService clockService) {
        this.workspaceService = workspaceService;
        this.fileChangeRepository = fileChangeRepository;
        this.taskService = taskService;
        this.analysisReportRepository = analysisReportRepository;
        this.clockService = clockService;
    }

    /**
     * 执行项目结构分析。
     *
     * @param project 项目记录
     * @return 分析报告
     * @throws IOException 读取工作区失败时抛出
     */
    public AnalysisReport analyze(ProjectRecord project) throws IOException {
        return analyze(project, null);
    }

    /**
     * 执行项目结构分析。
     * <p>
     * 前端先创建任务并通过 taskId 连接 WebSocket 后，把 taskId 传入这里；分析过程仍在
     * 当前请求内同步执行，但日志会实时广播到前端任务面板。
     * </p>
     *
     * @param project 项目记录
     * @param taskId  预创建任务 ID，可为空
     * @return 分析报告
     * @throws IOException 读取工作区失败时抛出
     */
    public AnalysisReport analyze(ProjectRecord project, String taskId) throws IOException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE_ANALYZE, MESSAGE_ANALYSIS_START);
        try {
            Path extractedDir = workspaceService.extractedDir(project);
            AnalysisReport report = new AnalysisReport();
            report.setProjectId(project.getId());
            report.setPackageType(project.getPackageType());

            taskService.running(task, PROGRESS_MANIFEST, MESSAGE_ANALYSIS_MANIFEST);
            taskService.ensureNotCancelled(task.getId());
            readManifest(extractedDir, report);

            taskService.running(task, PROGRESS_STATISTICS, MESSAGE_ANALYSIS_STATISTICS);
            taskService.ensureNotCancelled(task.getId());
            report.setSpringBootLayout(Files.exists(extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR)));
            report.setWarLayout(Files.exists(extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR)));
            report.setClassCount(countByExtension(extractedDir, JarPatchConstants.CLASS_EXTENSION));
            report.setDependencies(findDependencies(extractedDir));
            report.setDependencyCount(report.getDependencies().size());
            report.setModifiedFiles(fileChangeRepository.findPaths(project.getId()));

            taskService.running(task, PROGRESS_RISKS, MESSAGE_ANALYSIS_RISKS);
            taskService.ensureNotCancelled(task.getId());
            addRisks(extractedDir, report);

            analysisReportRepository.insert(project.getId(), report, clockService.now());
            taskService.success(task, MESSAGE_ANALYSIS_SUCCESS);
            return report;
        } catch (IllegalStateException e) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(e.getMessage())) {
                throw e;
            }
            taskService.failed(task, MESSAGE_ANALYSIS_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + e.getMessage());
            throw e;
        } catch (RuntimeException | IOException e) {
            taskService.failed(task, MESSAGE_ANALYSIS_FAILED + JarPatchConstants.MESSAGE_DETAIL_SEPARATOR + e.getMessage());
            throw e;
        }
    }

    /**
     * 在导出任务内部执行完整结构与风险分析，不创建第二条分析任务。
     * <p>
     * 导出入口在写临时包前调用本方法，实际读取点仍是 extracted；分析结果用于签名、
     * 多版本和包布局门禁，随后导出服务才允许进入打包与结构校验。
     * </p>
     *
     * @param project 项目记录
     * @return 当前工作区结构分析报告
     * @throws IOException 读取工作区失败时抛出
     */
    public AnalysisReport analyzeForExport(ProjectRecord project) throws IOException {
        Path extractedDir = workspaceService.extractedDir(project);
        AnalysisReport report = new AnalysisReport();
        report.setProjectId(project.getId());
        report.setPackageType(project.getPackageType());
        readManifest(extractedDir, report);
        report.setSpringBootLayout(Files.exists(extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR)));
        report.setWarLayout(Files.exists(extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR)));
        report.setClassCount(countByExtension(extractedDir, JarPatchConstants.CLASS_EXTENSION));
        report.setDependencies(findDependencies(extractedDir));
        report.setDependencyCount(report.getDependencies().size());
        report.setModifiedFiles(fileChangeRepository.findPaths(project.getId()));
        addRisks(extractedDir, report);
        analysisReportRepository.insert(project.getId(), report, clockService.now());
        return report;
    }

    /**
     * 读取 Manifest 并提取入口类。
     *
     * @param extractedDir 解压目录
     * @param report       分析报告
     * @throws IOException Manifest 读取失败时抛出
     */
    private void readManifest(Path extractedDir, AnalysisReport report) throws IOException {
        Path manifestPath = extractedDir.resolve(JarPatchConstants.MANIFEST_PATH);
        report.setManifestExists(Files.exists(manifestPath));
        if (!Files.exists(manifestPath)) {
            return;
        }
        try (Stream<String> lines = Files.lines(manifestPath, StandardCharsets.UTF_8)) {
            String mainClass = lines
                    .filter(line -> line.startsWith("Main-Class:"))
                    .map(line -> line.substring("Main-Class:".length()).trim())
                    .findFirst()
                    .orElse("");
            report.setEntryClass(mainClass);
        } catch (IOException e) {
            Manifest manifest = new Manifest(Files.newInputStream(manifestPath));
            report.setEntryClass(manifest.getMainAttributes().getValue("Main-Class"));
        }
    }

    /**
     * 统计指定后缀文件数量。
     *
     * @param root      根目录
     * @param extension 文件后缀
     * @return 文件数量
     * @throws IOException 读取目录失败时抛出
     */
    private int countByExtension(Path root, String extension) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return (int) stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("." + extension))
                    .count();
        }
    }

    /**
     * 查找依赖 Jar 列表。
     *
     * @param extractedDir 解压目录
     * @return 依赖 Jar 相对路径列表
     * @throws IOException 读取目录失败时抛出
     */
    private List<String> findDependencies(Path extractedDir) throws IOException {
        try (Stream<Path> stream = Files.walk(extractedDir)) {
            return stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("." + JarPatchConstants.JAR_EXTENSION))
                    .map(path -> extractedDir.relativize(path).toString().replace('\\', '/'))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * 识别可能影响导出的风险。
     *
     * @param extractedDir 解压目录
     * @param report       分析报告
     * @throws IOException 读取目录失败时抛出
     */
    private void addRisks(Path extractedDir, AnalysisReport report) throws IOException {
        if (hasSignatureFile(extractedDir)) {
            report.getRisks().add(new RiskItem(RiskLevel.HIGH.getCode(), RISK_SIGNATURE_TITLE,
                    RISK_SIGNATURE_DETAIL));
        }
        if (!report.getDependencies().isEmpty()) {
            report.getRisks().add(new RiskItem(RiskLevel.INFO.getCode(), RISK_NESTED_JAR_TITLE,
                    RISK_NESTED_JAR_DETAIL));
        }
        if (Files.exists(extractedDir.resolve("META-INF/versions"))) {
            report.getRisks().add(new RiskItem(RiskLevel.WARN.getCode(), RISK_MULTI_RELEASE_TITLE,
                    RISK_MULTI_RELEASE_DETAIL));
        }
        if (hasShortClassNames(extractedDir)) {
            report.getRisks().add(new RiskItem(RiskLevel.WARN.getCode(), RISK_OBFUSCATION_TITLE,
                    RISK_OBFUSCATION_DETAIL));
        }
    }

    /**
     * 判断是否存在签名文件。
     *
     * @param extractedDir 解压目录
     * @return 存在签名文件时返回 true
     * @throws IOException 读取目录失败时抛出
     */
    private boolean hasSignatureFile(Path extractedDir) throws IOException {
        try (Stream<Path> stream = Files.walk(extractedDir.resolve("META-INF"))) {
            return stream.filter(path -> Files.isRegularFile(path))
                    .map(path -> extension(path.getFileName().toString()))
                    .anyMatch(JarPatchConstants.SIGNATURE_EXTENSIONS::contains);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 通过短类名比例识别混淆迹象。
     *
     * @param extractedDir 解压目录
     * @return 可能混淆时返回 true
     * @throws IOException 读取目录失败时抛出
     */
    private boolean hasShortClassNames(Path extractedDir) throws IOException {
        try (Stream<Path> stream = Files.walk(extractedDir)) {
            long shortNames = stream.filter(path -> Files.isRegularFile(path))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .filter(name -> name.length() <= 2)
                    .limit(10)
                    .count();
            return shortNames >= 5;
        }
    }

    /**
     * 获取文件小写后缀。
     *
     * @param fileName 文件名
     * @return 小写后缀
     */
    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return JarPatchConstants.EMPTY_TEXT;
        }
        return fileName.substring(index + 1).toLowerCase();
    }
}
