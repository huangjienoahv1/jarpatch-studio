package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import com.jarpatch.model.ProjectRecord;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * 为已有项目准备反编译源码的编译副本。编译失败后，使用原始 class 与归档依赖让 CFR
 * 重新确认报错行的 Object 强转，修复范围由导入基线和用户保存的版本共同限定。
 *
 * @author 黄杰
 */
@Component
final class DecompiledSourceRecovery {

    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern CAST_CALL = Pattern.compile(
            "\\A((?:" + IDENTIFIER + "\\.)*" + IDENTIFIER + "\\(\\h*)"
                    + "\\(\\h*(?:java\\.lang\\.)?Object\\h*\\)\\h*(" + IDENTIFIER + "\\h*\\)\\h*;)\\z");
    private static final Pattern JAVAC_LOCATION = Pattern.compile("(.+\\.java):(\\d{1,9}):.*");
    private static final String SOURCE_LINE_BOUNDARY = "(?<=\\n)|(?<=\\r)(?!\\n)";

    private final WorkspaceService workspaceService;
    private final CfrBatchDecompiler decompiler;

    /**
     * 复用项目的基线路径校验和 CFR 源码输出。
     *
     * @param workspaceService 项目工作区服务
     * @param decompiler CFR 反编译组件
     */
    DecompiledSourceRecovery(WorkspaceService workspaceService, CfrBatchDecompiler decompiler) {
        this.workspaceService = workspaceService;
        this.decompiler = decompiler;
    }

    /**
     * 在本次编译的 staging 中准备修复副本，每个候选语句必须与基线相同，并由带依赖的
     * 重新反编译结果确认。未改动的 Java 输入继续使用工作区文件。
     *
     * @param project 当前项目
     * @param target 主包标识或嵌套 Jar 条目
     * @param mainPrefix 主包类条目目录前缀
     * @param sources 源码树路径与已校验的源码文件
     * @param diagnostics 首次 javac 的原始错误
     * @param classpath 本次编译使用的原包依赖
     * @param staging 本目标修复目录
     * @param ensureActive 任务取消检查
     * @return 以工作区源码为键的修复副本和所改行号
     * @throws IOException 原包、基线或临时源码无法读写
     */
    Map<Path, RecoveredSource> prepare(ProjectRecord project, String target, String mainPrefix,
                                      Map<String, Path> sources, String diagnostics, String classpath,
                                      Path staging, Runnable ensureActive) throws IOException {
        if (!diagnostics.contains("Object")) {
            return Map.of();
        }
        Map<String, Map<Integer, String>> candidatesBySource = new LinkedHashMap<>();
        for (Map.Entry<String, Path> source : sources.entrySet()) {
            ensureActive.run();
            Map<Integer, String> candidates = findCandidates(
                    workspaceService.resolveBaseline(project, source.getKey()), source.getValue(), diagnostics);
            if (!candidates.isEmpty()) {
                candidatesBySource.put(source.getKey(), candidates);
            }
        }
        if (candidatesBySource.isEmpty()) {
            return Map.of();
        }

        Path original = workspaceService.projectRoot(project).resolve(JarPatchConstants.WORKSPACE_ORIGINAL_DIR)
                .resolve(Path.of(project.getOriginalPath()).getFileName());
        if (!Files.isRegularFile(original)) {
            return Map.of();
        }

        Files.createDirectories(staging);
        try (ZipFile outer = new ZipFile(original.toFile())) {
            if (JarPatchConstants.COMPILE_TARGET_MAIN.equals(target)) {
                return recover(outer, mainPrefix, target, sources, candidatesBySource, classpath, staging, ensureActive);
            }
            Path nestedFile = staging.resolve("original.jar");
            if (!copyEntry(outer, target, nestedFile)) {
                return Map.of();
            }
            try (ZipFile nested = new ZipFile(nestedFile.toFile())) {
                return recover(nested, "", target, sources, candidatesBySource,
                        nestedFile + File.pathSeparator + classpath, staging, ensureActive);
            }
        }
    }

    /**
     * 从 javac 报错行选出基线中唯一且原样保留的单参数调用，候选值为去掉 Object 强转后的语句。
     */
    private Map<Integer, String> findCandidates(Path baseline, Path source, String diagnostics) throws IOException {
        String sourceName = source.toString().replace('\\', '/');
        List<Integer> reportedLines = diagnostics.lines().map(JAVAC_LOCATION::matcher)
                .filter(location -> location.matches() && location.group(1).replace('\\', '/').equals(sourceName))
                .map(location -> Integer.parseInt(location.group(2))).distinct().sorted().toList();
        if (reportedLines.isEmpty() || !Files.isRegularFile(baseline)) {
            return Map.of();
        }

        List<String> originalLines = Files.readString(baseline).lines().map(String::strip).toList();
        List<String> editedLines = Files.readString(source).lines().map(String::strip).toList();
        Map<Integer, String> candidates = new LinkedHashMap<>();
        for (int line : reportedLines) {
            if (line < 1 || line > editedLines.size()) {
                continue;
            }
            String current = editedLines.get(line - 1);
            var call = CAST_CALL.matcher(current);
            if (call.matches() && appearsOnce(originalLines, current) && appearsOnce(editedLines, current)) {
                candidates.put(line, call.group(1) + call.group(2));
            }
        }
        return candidates;
    }

    /**
     * 逐个反编译原始 class，仅将 CFR 确认的候选语句写入保留用户编辑和原换行的编译副本。
     */
    private Map<Path, RecoveredSource> recover(ZipFile archive, String prefix, String target,
                                               Map<String, Path> sources, Map<String, Map<Integer, String>> candidates,
                                               String classpath, Path staging, Runnable ensureActive) throws IOException {
        Map<Path, RecoveredSource> recovered = new HashMap<>();
        for (Map.Entry<String, Map<Integer, String>> candidate : candidates.entrySet()) {
            ensureActive.run();
            String javaPath = candidate.getKey();
            String classEntry = JavaVersionService.resolveClassEntry(target, javaPath);
            Path classFile = staging.resolve("classes").resolve(classEntry);
            if (!copyEntry(archive, prefix + classEntry, classFile)) {
                continue;
            }
            Path rereadDir = staging.resolve("decompiled");
            decompiler.decompile(List.of(classFile), rereadDir, classpath, () -> {
                ensureActive.run();
                return false;
            });
            String packagePath = classEntry.substring(0, classEntry.lastIndexOf('/') + 1);
            Path source = sources.get(javaPath);
            Path relativeSource = Path.of(packagePath).resolve(source.getFileName());
            Path reread = rereadDir.resolve(relativeSource);
            if (!Files.isRegularFile(reread)) {
                continue;
            }
            List<String> confirmed = Files.readString(reread).lines().map(String::strip).toList();
            Map<Integer, String> edits = new LinkedHashMap<>(candidate.getValue());
            edits.values().removeIf(line -> !appearsOnce(confirmed, line));
            if (!edits.isEmpty()) {
                Path copy = staging.resolve("sources").resolve(relativeSource);
                writeSourceCopy(source, copy, edits);
                recovered.put(source, new RecoveredSource(copy, List.copyOf(edits.keySet())));
            }
        }
        return recovered;
    }

    /** 将原包中的已定位条目写入 staging，缺少条目时跳过自动修复。 */
    private boolean copyEntry(ZipFile archive, String name, Path destination) throws IOException {
        var entry = archive.getEntry(name);
        if (entry == null || entry.isDirectory()) {
            return false;
        }
        Files.createDirectories(destination.getParent());
        try (var contents = archive.getInputStream(entry)) {
            Files.copy(contents, destination);
        }
        return true;
    }

    /** 确认语句定位唯一，避免把另一处同名调用误当成反编译证据。 */
    private boolean appearsOnce(List<String> lines, String line) {
        int index = lines.indexOf(line);
        return index >= 0 && index == lines.lastIndexOf(line);
    }

    /** 仅替换确认的语句文本，保留缩进、换行和其余用户源码。 */
    private void writeSourceCopy(Path source, Path copy, Map<Integer, String> edits) throws IOException {
        String[] lines = Files.readString(source).split(SOURCE_LINE_BOUNDARY, -1);
        edits.forEach((line, replacement) -> {
            int index = line - 1;
            lines[index] = lines[index].replace(lines[index].strip(), replacement);
        });
        Files.createDirectories(copy.getParent());
        Files.writeString(copy, String.join("", lines));
    }

    /** 供第二次 javac 使用的副本和执行日志中可定位的原始行号。 */
    record RecoveredSource(Path path, List<Integer> lines) {
    }
}
