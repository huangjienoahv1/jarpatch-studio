package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.OperationResult;
import com.jarpatch.model.ProjectRecord;
import com.jarpatch.model.TaskRecord;
import com.jarpatch.repository.FileChangeRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Java 修改文件编译服务。
 * <p>
 * 编译入口来自 /api/projects/{id}/compile，实际执行点是本机 javac，编译结果先写入
 * compiled 目录，再复制到 extracted 的 class 根目录，导出服务随后基于 extracted 打包。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class CompileService {

    private static final String TASK_TYPE_COMPILE = "COMPILE";
    private static final String JAVAC_ARGUMENT_FILE_NAME = "javac-arguments.txt";
    private static final String JAVAC_ARGUMENT_FILE_PREFIX = "@";
    private static final String MAIN_CLASS_COMPILE_TARGET = "main-classes";
    private static final String NESTED_JAR_SOURCE_PREFIX = JarPatchConstants.SOURCE_NESTED_JAR_DIR + JarPatchConstants.ZIP_SEPARATOR;
    private static final String NESTED_JAR_SOURCE_MARKER = "." + JarPatchConstants.JAR_EXTENSION + JarPatchConstants.ZIP_SEPARATOR;
    private static final String COMMON_RESULT_SUCCESS_CALL = "CommonResult.success(";
    private static final String OBJECT_CAST_TOKEN = "(Object)";
    private static final char WINDOWS_PATH_SEPARATOR = '\\';
    private static final char JAVAC_ARGUMENT_PATH_SEPARATOR = '/';
    private static final String JAVAC_ARGUMENT_QUOTE = "\"";
    private static final String JAVAC_ARGUMENT_ESCAPED_QUOTE = "\\\"";
    private static final char JAVA_ANNOTATION_PREFIX = '@';
    private static final char JAVA_DOT = '.';
    private static final char JAVA_OPEN_PARENTHESIS = '(';
    private static final char JAVA_CLOSE_PARENTHESIS = ')';
    private static final char JAVA_DOUBLE_QUOTE = '"';
    private static final char JAVA_SINGLE_QUOTE = '\'';
    private static final char JAVA_ESCAPE_CHARACTER = '\\';
    private static final char JAVA_SLASH = '/';
    private static final char JAVA_ASTERISK = '*';
    private final WorkspaceService workspaceService;
    private final ArchiveService archiveService;
    private final FileChangeRepository fileChangeRepository;
    private final TaskService taskService;
    private final JdkService jdkService;

    /**
     * 创建编译服务。
     *
     * @param workspaceService     工作区服务
     * @param archiveService       压缩包服务
     * @param fileChangeRepository 修改记录仓储
     * @param taskService          任务服务
     * @param jdkService           JDK 工具定位服务
     */
    public CompileService(WorkspaceService workspaceService,
                          ArchiveService archiveService,
                          FileChangeRepository fileChangeRepository,
                          TaskService taskService,
                          JdkService jdkService) {
        this.workspaceService = workspaceService;
        this.archiveService = archiveService;
        this.fileChangeRepository = fileChangeRepository;
        this.taskService = taskService;
        this.jdkService = jdkService;
    }

    /**
     * 编译已修改 Java 文件并替换 class。
     *
     * @param project 项目记录
     * @return 编译结果
     * @throws IOException          文件读写失败时抛出
     * @throws InterruptedException javac 被中断时抛出
     */
    public OperationResult compile(ProjectRecord project) throws IOException, InterruptedException {
        return compile(project, null);
    }

    /**
     * 编译已修改 Java 文件并替换 class。
     * <p>
     * 前端先创建任务并连接 WebSocket 后，把 taskId 传入这里；编译仍由本机 javac 执行，
     * 但进度日志会实时推送，取消请求也能在任务执行过程中生效。
     * </p>
     *
     * @param project 项目记录
     * @param taskId  预创建任务 ID，可为空
     * @return 编译结果
     * @throws IOException          文件读写失败时抛出
     * @throws InterruptedException javac 被中断时抛出
     */
    public OperationResult compile(ProjectRecord project, String taskId) throws IOException, InterruptedException {
        TaskRecord task = taskService.prepare(taskId, project.getId(), TASK_TYPE_COMPILE, "开始编译修改过的 Java 文件");
        try {
            List<String> javaPaths = fileChangeRepository.findJavaPaths(project.getId());
            if (javaPaths.isEmpty()) {
                throw new IllegalStateException(JarPatchConstants.MESSAGE_NO_MODIFIED_JAVA);
            }

            taskService.running(task, 20, "定位本机 javac");
            taskService.ensureNotCancelled(task.getId());
            Path javac = jdkService.findJavac();
            Path compiledDir = workspaceService.compiledDir(project);
            Files.createDirectories(compiledDir);

            taskService.running(task, 40, "执行 javac 编译");
            StringBuilder output = new StringBuilder();
            Map<String, List<String>> javaPathsByTarget = groupJavaPathsByTarget(javaPaths);
            for (Map.Entry<String, List<String>> entry : javaPathsByTarget.entrySet()) {
                taskService.ensureNotCancelled(task.getId());
                Path targetCompiledDir = compiledDir.resolve(safeCompileTargetName(entry.getKey()));
                resetCompiledDir(targetCompiledDir);
                normalizeDecompiledSources(project, entry.getValue());
                List<String> command = buildCommand(project, javac, targetCompiledDir, entry.getValue());
                output.append(runProcess(command, workspaceService.sourceDir(project), () -> taskService.isCancelled(task.getId())));
                writeCompiledClasses(project, entry.getKey(), targetCompiledDir, () -> taskService.isCancelled(task.getId()));
            }

            OperationResult result = new OperationResult();
            result.setTaskId(task.getId());
            result.setChangedFiles(javaPaths);
            result.setMessage(output.isEmpty() ? "编译完成" : output.toString());
            taskService.success(task, "编译完成，class 文件已写入 extracted 目录");
            return result;
        } catch (IllegalStateException e) {
            if (JarPatchConstants.MESSAGE_TASK_CANCELLED.equals(e.getMessage())) {
                throw e;
            }
            taskService.failed(task, "编译失败: " + e.getMessage());
            throw e;
        } catch (RuntimeException | IOException | InterruptedException e) {
            taskService.failed(task, "编译失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 构建 javac 命令。
     * <p>
     * Windows 直接把大量依赖 Jar 拼进命令行时会触发 CreateProcess error=206。
     * 这里把 classpath、输出目录和源码文件写入 javac 参数文件，进程启动阶段只传入
     * @参数文件路径，实际编译参数仍由 javac 按标准机制读取。
     * </p>
     *
     * @param project     项目记录
     * @param javac       javac 路径
     * @param compiledDir 编译输出目录
     * @param javaPaths   已修改 Java 文件路径
     * @return javac 命令参数
     * @throws IOException 读取依赖失败时抛出
     */
    private List<String> buildCommand(ProjectRecord project, Path javac, Path compiledDir, List<String> javaPaths) throws IOException {
        Path argumentFile = compiledDir.resolve(JAVAC_ARGUMENT_FILE_NAME);
        writeArgumentFile(project, compiledDir, javaPaths, argumentFile);

        List<String> command = new ArrayList<>();
        command.add(javac.toString());
        command.add(JAVAC_ARGUMENT_FILE_PREFIX + argumentFile.toString());
        return command;
    }

    /**
     * 写入 javac 参数文件。
     * <p>
     * 参数文件是编译入口到实际 javac 执行点之间的长参数承载文件，内容只影响本次编译；
     * javac 读取后把 class 输出到 compiled 目录，后续复制流程再写回 extracted。
     * </p>
     *
     * @param project      项目记录
     * @param compiledDir  编译输出目录
     * @param javaPaths    已修改 Java 文件路径
     * @param argumentFile javac 参数文件
     * @throws IOException 写入参数文件失败时抛出
     */
    private void writeArgumentFile(ProjectRecord project, Path compiledDir, List<String> javaPaths, Path argumentFile) throws IOException {
        List<String> arguments = new ArrayList<>();
        arguments.add("-encoding");
        arguments.add(JarPatchConstants.UTF_8);
        arguments.add("-classpath");
        arguments.add(formatArgumentFileValue(buildClasspath(project)));
        arguments.add("-d");
        arguments.add(formatArgumentFileValue(compiledDir.toString()));
        for (String javaPath : javaPaths) {
            if (javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
                // 只把 sources 下被修改过的 Java 文件写入参数文件，避免误编译用户未改动的反编译源码。
                arguments.add(formatArgumentFileValue(workspaceService.resolveSource(project, javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length())).toString()));
            }
        }
        Files.write(argumentFile, arguments, StandardCharsets.UTF_8);
    }

    /**
     * 编译前整理反编译源码中的确定性兼容问题。
     * <p>
     * 入口来自编译接口，实际执行点在生成 javac 参数文件之前；结果写回 sources 下被修改的
     * Java 文件。这里只处理不改变业务语义的反编译冗余形态，避免 javac 在真正业务代码
     * 编译前就因为重复注解或泛型强转失败。
     * </p>
     *
     * @param project   项目记录
     * @param javaPaths 本次编译目标中的 Java 文件树路径
     * @throws IOException 读取或写回源码失败时抛出
     */
    private void normalizeDecompiledSources(ProjectRecord project, List<String> javaPaths) throws IOException {
        for (String javaPath : javaPaths) {
            if (javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
                Path sourceFile = workspaceService.resolveSource(project, javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length()));
                normalizeDecompiledSource(sourceFile);
            }
        }
    }

    /**
     * 整理单个 Java 文件的反编译冗余源码。
     *
     * @param sourceFile Java 源码文件
     * @throws IOException 读取或写回源码失败时抛出
     */
    private void normalizeDecompiledSource(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile, StandardCharsets.UTF_8);
        String normalizedSource = removeCommonResultObjectCasts(removeDuplicateAnnotationSequence(source));
        if (!source.equals(normalizedSource)) {
            Files.writeString(sourceFile, normalizedSource, StandardCharsets.UTF_8);
        }
    }

    /**
     * 删除同一个连续注解序列中的完全重复注解。
     * <p>
     * CFR 在参数同时存在声明注解和类型注解时，可能还原成
     * {@code @Valid @NotNull(...) @Valid @NotNull(...)}。Java 不允许非可重复注解重复出现；
     * 该方法只删除文本完全一致的重复注解，保留第一次出现的位置和后续类型声明。
     * </p>
     *
     * @param source 原始 Java 源码
     * @return 去重后的 Java 源码
     */
    private String removeDuplicateAnnotationSequence(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            if (isLineCommentStart(source, index)) {
                index = appendUntilLineEnd(source, index, result);
            } else if (isBlockCommentStart(source, index)) {
                index = appendUntilBlockCommentEnd(source, index, result);
            } else if (isStringOrCharStart(source, index)) {
                index = appendUntilStringOrCharEnd(source, index, result);
            } else if (isAnnotationStart(source, index)) {
                index = appendAnnotationSequence(source, index, result);
            } else {
                result.append(source.charAt(index));
                index++;
            }
        }
        return result.toString();
    }

    /**
     * 追加一个连续注解序列，并删除其中完全重复的注解。
     *
     * @param source 原始 Java 源码
     * @param start  注解序列开始下标
     * @param result 输出缓冲区
     * @return 注解序列结束后的下标
     */
    private int appendAnnotationSequence(String source, int start, StringBuilder result) {
        Set<String> annotations = new HashSet<>();
        String pendingWhitespace = "";
        int index = start;
        boolean appended = false;
        while (isAnnotationStart(source, index)) {
            int annotationEnd = findAnnotationEnd(source, index);
            if (annotationEnd <= index) {
                result.append(source.charAt(index));
                return index + 1;
            }
            String annotation = source.substring(index, annotationEnd);
            if (annotations.add(annotation)) {
                if (appended) {
                    result.append(pendingWhitespace);
                }
                result.append(annotation);
                appended = true;
            }
            int next = skipWhitespace(source, annotationEnd);
            pendingWhitespace = source.substring(annotationEnd, next);
            if (!isAnnotationStart(source, next)) {
                result.append(pendingWhitespace);
                return next;
            }
            index = next;
        }
        return index;
    }

    /**
     * 查找单个注解的结束下标。
     *
     * @param source 原始 Java 源码
     * @param start  注解开始下标
     * @return 注解结束下标
     */
    private int findAnnotationEnd(String source, int start) {
        int index = start + 1;
        while (index < source.length()) {
            char value = source.charAt(index);
            if (Character.isJavaIdentifierPart(value) || value == JAVA_DOT) {
                index++;
                continue;
            }
            break;
        }
        int next = skipWhitespace(source, index);
        if (next < source.length() && source.charAt(next) == JAVA_OPEN_PARENTHESIS) {
            int close = findMatchingParenthesis(source, next);
            return close < 0 ? index : close + 1;
        }
        return index;
    }

    /**
     * 删除 {@code CommonResult.success(...)} 入参外层多余的 {@code (Object)} 强转。
     * <p>
     * 该强转来自反编译结果，会让 {@code CommonResult<T>} 的泛型返回值被推断成
     * {@code Object}，从而和控制器方法签名冲突。方法只处理 CommonResult.success 的
     * 最外层入参，不影响日志里的 {@code new Object[]{...}} 或其他业务表达式。
     * </p>
     *
     * @param source 原始 Java 源码
     * @return 删除冗余强转后的 Java 源码
     */
    private String removeCommonResultObjectCasts(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            int callIndex = source.indexOf(COMMON_RESULT_SUCCESS_CALL, index);
            if (callIndex < 0) {
                result.append(source.substring(index));
                break;
            }
            result.append(source, index, callIndex);
            int argumentStart = callIndex + COMMON_RESULT_SUCCESS_CALL.length();
            int close = findMatchingParenthesis(source, argumentStart - 1);
            if (close < 0) {
                result.append(source.substring(callIndex));
                break;
            }
            String argument = source.substring(argumentStart, close);
            result.append(COMMON_RESULT_SUCCESS_CALL)
                    .append(removeLeadingObjectCasts(argument))
                    .append(JAVA_CLOSE_PARENTHESIS);
            index = close + 1;
        }
        return result.toString();
    }

    /**
     * 删除表达式最外层连续的 {@code (Object)} 强转和仅用于包裹强转的括号。
     *
     * @param argument CommonResult.success 的入参表达式
     * @return 去掉外层 Object 强转后的表达式
     */
    private String removeLeadingObjectCasts(String argument) {
        String expression = argument;
        boolean changed = true;
        while (changed) {
            changed = false;
            int leadingWhitespaceEnd = skipWhitespace(expression, 0);
            String prefix = expression.substring(0, leadingWhitespaceEnd);
            String body = expression.substring(leadingWhitespaceEnd);
            if (body.startsWith(OBJECT_CAST_TOKEN)) {
                expression = prefix + body.substring(OBJECT_CAST_TOKEN.length());
                changed = true;
                continue;
            }
            int close = body.startsWith(String.valueOf(JAVA_OPEN_PARENTHESIS)) ? findMatchingParenthesis(body, 0) : -1;
            if (close > 0 && skipWhitespace(body, close + 1) == body.length()) {
                expression = prefix + body.substring(1, close);
                changed = true;
            }
        }
        return expression;
    }

    /**
     * 判断当前位置是否是 Java 注解开始。
     *
     * @param source 原始 Java 源码
     * @param index  当前下标
     * @return 是注解开始时返回 true
     */
    private boolean isAnnotationStart(String source, int index) {
        return index >= 0
                && index + 1 < source.length()
                && source.charAt(index) == JAVA_ANNOTATION_PREFIX
                && Character.isJavaIdentifierStart(source.charAt(index + 1));
    }

    /**
     * 跳过连续空白字符。
     *
     * @param source 原始文本
     * @param start  开始下标
     * @return 第一个非空白字符下标
     */
    private int skipWhitespace(String source, int start) {
        int index = start;
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * 查找与指定左括号匹配的右括号。
     *
     * @param source    原始 Java 源码或表达式
     * @param openIndex 左括号下标
     * @return 匹配右括号下标，未找到时返回 -1
     */
    private int findMatchingParenthesis(String source, int openIndex) {
        int depth = 1;
        int index = openIndex + 1;
        while (index < source.length()) {
            if (isLineCommentStart(source, index)) {
                index = skipUntilLineEnd(source, index);
            } else if (isBlockCommentStart(source, index)) {
                index = skipUntilBlockCommentEnd(source, index);
            } else if (isStringOrCharStart(source, index)) {
                index = skipUntilStringOrCharEnd(source, index);
            } else {
                char value = source.charAt(index);
                if (value == JAVA_OPEN_PARENTHESIS) {
                    depth++;
                } else if (value == JAVA_CLOSE_PARENTHESIS) {
                    depth--;
                    if (depth == 0) {
                        return index;
                    }
                }
                index++;
            }
        }
        return -1;
    }

    /**
     * 判断当前位置是否是行注释开始。
     *
     * @param source 原始 Java 源码
     * @param index  当前下标
     * @return 是行注释开始时返回 true
     */
    private boolean isLineCommentStart(String source, int index) {
        return index + 1 < source.length()
                && source.charAt(index) == JAVA_SLASH
                && source.charAt(index + 1) == JAVA_SLASH;
    }

    /**
     * 判断当前位置是否是块注释开始。
     *
     * @param source 原始 Java 源码
     * @param index  当前下标
     * @return 是块注释开始时返回 true
     */
    private boolean isBlockCommentStart(String source, int index) {
        return index + 1 < source.length()
                && source.charAt(index) == JAVA_SLASH
                && source.charAt(index + 1) == JAVA_ASTERISK;
    }

    /**
     * 判断当前位置是否是字符串或字符字面量开始。
     *
     * @param source 原始 Java 源码
     * @param index  当前下标
     * @return 是字符串或字符字面量开始时返回 true
     */
    private boolean isStringOrCharStart(String source, int index) {
        char value = source.charAt(index);
        return value == JAVA_DOUBLE_QUOTE || value == JAVA_SINGLE_QUOTE;
    }

    /**
     * 复制行注释直到行尾。
     *
     * @param source 原始 Java 源码
     * @param start  注释开始下标
     * @param result 输出缓冲区
     * @return 行注释结束后的下标
     */
    private int appendUntilLineEnd(String source, int start, StringBuilder result) {
        int end = skipUntilLineEnd(source, start);
        result.append(source, start, end);
        return end;
    }

    /**
     * 复制块注释直到注释结束。
     *
     * @param source 原始 Java 源码
     * @param start  注释开始下标
     * @param result 输出缓冲区
     * @return 块注释结束后的下标
     */
    private int appendUntilBlockCommentEnd(String source, int start, StringBuilder result) {
        int end = skipUntilBlockCommentEnd(source, start);
        result.append(source, start, end);
        return end;
    }

    /**
     * 复制字符串或字符字面量直到字面量结束。
     *
     * @param source 原始 Java 源码
     * @param start  字面量开始下标
     * @param result 输出缓冲区
     * @return 字面量结束后的下标
     */
    private int appendUntilStringOrCharEnd(String source, int start, StringBuilder result) {
        int end = skipUntilStringOrCharEnd(source, start);
        result.append(source, start, end);
        return end;
    }

    /**
     * 跳过行注释直到行尾。
     *
     * @param source 原始 Java 源码
     * @param start  注释开始下标
     * @return 行注释结束后的下标
     */
    private int skipUntilLineEnd(String source, int start) {
        int index = start;
        while (index < source.length()) {
            char value = source.charAt(index);
            if (value == '\n' || value == '\r') {
                return index;
            }
            index++;
        }
        return index;
    }

    /**
     * 跳过块注释直到注释结束。
     *
     * @param source 原始 Java 源码
     * @param start  注释开始下标
     * @return 块注释结束后的下标
     */
    private int skipUntilBlockCommentEnd(String source, int start) {
        int index = start + 2;
        while (index + 1 < source.length()) {
            if (source.charAt(index) == JAVA_ASTERISK && source.charAt(index + 1) == JAVA_SLASH) {
                return index + 2;
            }
            index++;
        }
        return source.length();
    }

    /**
     * 跳过字符串或字符字面量直到字面量结束。
     *
     * @param source 原始 Java 源码
     * @param start  字面量开始下标
     * @return 字面量结束后的下标
     */
    private int skipUntilStringOrCharEnd(String source, int start) {
        char quote = source.charAt(start);
        int index = start + 1;
        while (index < source.length()) {
            char value = source.charAt(index);
            if (value == JAVA_ESCAPE_CHARACTER) {
                index += 2;
                continue;
            }
            index++;
            if (value == quote) {
                return index;
            }
        }
        return index;
    }

    /**
     * 格式化 javac 参数文件中的参数值。
     * <p>
     * javac 参数文件内的反斜杠会参与转义解析，因此 Windows 路径统一转成 javac 可识别的
     * 正斜杠路径，再用引号包裹，保证 JDK 安装目录、工作区或 Jar 文件名包含空格时仍可解析。
     * </p>
     *
     * @param value 原始参数值
     * @return 参数文件可读取的安全参数值
     */
    private String formatArgumentFileValue(String value) {
        return JAVAC_ARGUMENT_QUOTE
                + value.replace(WINDOWS_PATH_SEPARATOR, JAVAC_ARGUMENT_PATH_SEPARATOR)
                .replace(JAVAC_ARGUMENT_QUOTE, JAVAC_ARGUMENT_ESCAPED_QUOTE)
                + JAVAC_ARGUMENT_QUOTE;
    }

    /**
     * 构建编译 classpath。
     *
     * @param project 项目记录
     * @return classpath 字符串
     * @throws IOException 读取依赖失败时抛出
     */
    private String buildClasspath(ProjectRecord project) throws IOException {
        List<String> entries = new ArrayList<>();
        Path extractedDir = workspaceService.extractedDir(project);
        entries.add(extractedDir.toString());
        entries.add(classRoot(project).toString());
        try (Stream<Path> stream = Files.walk(extractedDir)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("." + JarPatchConstants.JAR_EXTENSION))
                    .forEach(path -> entries.add(path.toString()));
        }
        return String.join(File.pathSeparator, entries);
    }

    /**
     * 按 class 写回目标拆分 Java 修改文件。
     * <p>
     * 普通源码写回主 classes 根目录；sources/nested-jars 下的源码按照原嵌套 Jar 相对路径分组，
     * 编译完成后写回对应 Jar，避免多模块 Spring Boot 包的业务模块被误写到 BOOT-INF/classes。
     * </p>
     *
     * @param javaPaths 修改过的 Java 文件路径
     * @return 按写回目标分组后的 Java 文件路径
     */
    private Map<String, List<String>> groupJavaPathsByTarget(List<String> javaPaths) {
        Map<String, List<String>> groupedPaths = new LinkedHashMap<>();
        for (String javaPath : javaPaths) {
            String target = resolveCompileTarget(javaPath);
            groupedPaths.computeIfAbsent(target, key -> new ArrayList<>()).add(javaPath);
        }
        return groupedPaths;
    }

    /**
     * 解析单个 Java 文件的 class 写回目标。
     *
     * @param javaPath 修改记录中的 Java 文件树路径
     * @return 主 classes 标识或嵌套 Jar 相对路径
     */
    private String resolveCompileTarget(String javaPath) {
        if (javaPath == null || !javaPath.startsWith(JarPatchConstants.TREE_SOURCE_PREFIX)) {
            return MAIN_CLASS_COMPILE_TARGET;
        }
        String sourceRelativePath = javaPath.substring(JarPatchConstants.TREE_SOURCE_PREFIX.length());
        if (!sourceRelativePath.startsWith(NESTED_JAR_SOURCE_PREFIX)) {
            return MAIN_CLASS_COMPILE_TARGET;
        }
        String nestedRelativePath = sourceRelativePath.substring(NESTED_JAR_SOURCE_PREFIX.length());
        int markerIndex = nestedRelativePath.indexOf(NESTED_JAR_SOURCE_MARKER);
        if (markerIndex < 0) {
            throw new IllegalArgumentException(JarPatchConstants.MESSAGE_INVALID_NESTED_JAR_SOURCE_PATH);
        }
        return nestedRelativePath.substring(0, markerIndex + ("." + JarPatchConstants.JAR_EXTENSION).length());
    }

    /**
     * 清空当前目标的编译输出目录，避免历史 class 干扰本次写回。
     *
     * @param targetCompiledDir 当前目标编译输出目录
     * @throws IOException 删除或创建目录失败时抛出
     */
    private void resetCompiledDir(Path targetCompiledDir) throws IOException {
        if (Files.exists(targetCompiledDir)) {
            try (Stream<Path> stream = Files.walk(targetCompiledDir)) {
                stream.sorted((left, right) -> right.compareTo(left))
                        .forEach(path -> deleteCompiledPath(path));
            }
        }
        Files.createDirectories(targetCompiledDir);
    }

    /**
     * 删除编译输出目录中的单个路径。
     *
     * @param path 待删除路径
     */
    private void deleteCompiledPath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("清理编译输出失败: " + path, e);
        }
    }

    /**
     * 执行外部 javac 进程。
     *
     * @param command 命令参数
     * @param workDir 工作目录
     * @param cancelRequested 取消检查回调
     * @return javac 输出
     * @throws IOException          进程启动失败时抛出
     * @throws InterruptedException 进程等待被中断时抛出
     */
    private String runProcess(List<String> command, Path workDir, BooleanSupplier cancelRequested) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        Thread readerThread = new Thread(() -> readProcessOutput(process, output), "jarpatch-javac-output");
        readerThread.setDaemon(true);
        readerThread.start();
        try {
            while (process.isAlive()) {
                if (cancelRequested.getAsBoolean()) {
                    process.destroyForcibly();
                    readerThread.join();
                    throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
                }
                process.waitFor(200, TimeUnit.MILLISECONDS);
            }
            readerThread.join();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(output.toString());
        }
        return output.toString();
    }

    /**
     * 读取 javac 进程输出。
     *
     * @param process 外部进程
     * @param output  输出缓冲
     */
    private void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (IOException ignored) {
            // 进程被取消时输入流会关闭，读取线程直接退出即可。
        }
    }

    /**
     * 按编译目标把 class 写回 extracted。
     * <p>
     * 主工程源码写回 BOOT-INF/classes、WEB-INF/classes 或普通 Jar 根目录；
     * 嵌套 Jar 源码则重建并覆盖 extracted 中对应的原 Jar 文件。
     * </p>
     *
     * @param project     项目记录
     * @param compileTarget 主 classes 标识或嵌套 Jar 相对路径
     * @param compiledDir 编译输出目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 复制失败时抛出
     */
    private void writeCompiledClasses(ProjectRecord project, String compileTarget, Path compiledDir, BooleanSupplier cancelRequested) throws IOException {
        if (MAIN_CLASS_COMPILE_TARGET.equals(compileTarget)) {
            copyCompiledClassesToDirectory(compiledDir, classRoot(project), cancelRequested);
            return;
        }
        Path nestedJar = workspaceService.resolveExtracted(project, compileTarget);
        archiveService.replaceClassesInJar(nestedJar, compiledDir, cancelRequested);
    }

    /**
     * 把编译输出复制到普通 class 根目录。
     *
     * @param compiledDir 编译输出目录
     * @param targetRoot  class 目标根目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 复制失败时抛出
     */
    private void copyCompiledClassesToDirectory(Path compiledDir, Path targetRoot, BooleanSupplier cancelRequested) throws IOException {
        try (Stream<Path> stream = Files.walk(compiledDir)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().endsWith("." + JarPatchConstants.CLASS_EXTENSION))
                    .forEach(path -> {
                        if (cancelRequested.getAsBoolean()) {
                            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
                        }
                        copyCompiledClass(compiledDir, targetRoot, path);
                    });
        }
    }

    /**
     * 复制单个 class 文件。
     *
     * @param compiledDir 编译输出目录
     * @param targetRoot  class 目标根目录
     * @param classFile   class 文件
     */
    private void copyCompiledClass(Path compiledDir, Path targetRoot, Path classFile) {
        try {
            Path target = targetRoot.resolve(compiledDir.relativize(classFile)).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(classFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("复制 class 文件失败: " + classFile, e);
        }
    }

    /**
     * 把嵌套 Jar 相对路径转换为可作为编译输出目录名的文本。
     *
     * @param compileTarget 主 classes 标识或嵌套 Jar 相对路径
     * @return 安全目录名
     */
    private String safeCompileTargetName(String compileTarget) {
        return compileTarget.replace(JarPatchConstants.ZIP_SEPARATOR, "_")
                .replace(WINDOWS_PATH_SEPARATOR, '_');
    }

    /**
     * 按包类型解析 class 替换目标目录。
     *
     * @param project 项目记录
     * @return class 根目录
     */
    private Path classRoot(ProjectRecord project) {
        Path extractedDir = workspaceService.extractedDir(project);
        if ("SPRING_BOOT_JAR".equals(project.getPackageType())) {
            return extractedDir.resolve(JarPatchConstants.SPRING_BOOT_CLASSES_DIR);
        }
        if ("WAR".equals(project.getPackageType())) {
            return extractedDir.resolve(JarPatchConstants.WAR_CLASSES_DIR);
        }
        return extractedDir;
    }
}
