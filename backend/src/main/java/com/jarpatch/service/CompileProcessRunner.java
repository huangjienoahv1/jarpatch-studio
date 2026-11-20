package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * javac 子进程执行器。
 * <p>
 * 编译服务把已构造的命令交给本执行器；实际执行点在 ProcessBuilder，输出由独立线程持续读取，
 * 取消时强制终止子进程并把任务取消状态返回上层。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class CompileProcessRunner {

    private static final long PROCESS_POLL_INTERVAL_MILLIS = 200L;
    private static final String OUTPUT_READER_THREAD_NAME = "jarpatch-javac-output";
    private static final String JAVAC_ARGUMENT_FILE_NAME = "javac-arguments.txt";
    private static final String JAVAC_FILE_ENCODING_ARGUMENT = "-J-Dfile.encoding=UTF-8";

    /**
     * 为包编译和运行时单类编译生成 javac 参数文件，避免长 classpath 触及系统命令行限制。
     *
     * @param javac 已选定的 JDK 编译器
     * @param output 编译产物和临时参数文件目录
     * @param release 原 class 对应的 Java 版本
     * @param classpath 仅用于解析依赖类型的编译路径
     * @param sources 明确参与编译的源码文件
     * @return 可交给子进程执行的命令
     * @throws IOException 参数文件写入失败
     */
    public List<String> prepareCommand(Path javac, Path output, int release, String classpath, List<Path> sources)
            throws IOException {
        // 参数文件与诊断都按 UTF-8 处理，且不运行原包依赖中发现的注解处理器。
        List<String> arguments = new ArrayList<>(List.of("-proc:none", "--release", String.valueOf(release),
                "-encoding", JarPatchConstants.UTF_8, "-classpath", quoteArgument(classpath),
                "-d", quoteArgument(output.toString())));
        sources.forEach(source -> arguments.add(quoteArgument(source.toString())));
        Path argumentFile = output.resolve(JAVAC_ARGUMENT_FILE_NAME);
        Files.write(argumentFile, arguments, StandardCharsets.UTF_8);
        return List.of(javac.toString(), JAVAC_FILE_ENCODING_ARGUMENT, "@" + argumentFile);
    }

    /**
     * 对参数文件中的路径或 classpath 加引号，保留 Windows 路径含空格时的含义。
     */
    private String quoteArgument(String value) {
        return "\"" + value.replace('\\', '/').replace("\"", "\\\"") + "\"";
    }

    /**
     * 执行 javac 命令并支持取消。
     *
     * @param command         命令参数
     * @param workDir        工作目录
     * @param cancelRequested 取消检查回调
     * @return javac 合并输出
     * @throws IOException          进程启动或输出读取失败时抛出
     * @throws InterruptedException 等待进程被中断时抛出
     */
    public String run(List<String> command,
                      Path workDir,
                      BooleanSupplier cancelRequested) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        Thread readerThread = new Thread(() -> readOutput(process, output), OUTPUT_READER_THREAD_NAME);
        readerThread.setDaemon(true);
        readerThread.start();
        try {
            while (process.isAlive()) {
                if (cancelRequested.getAsBoolean()) {
                    process.destroyForcibly();
                    readerThread.join();
                    throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
                }
                process.waitFor(PROCESS_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
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
     * 持续读取 javac 合并输出流。
     *
     * @param process javac 进程
     * @param output  输出缓冲
     */
    private void readOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        } catch (IOException ignored) {
            // 取消会关闭进程流，主线程负责记录最终任务状态。
        }
    }
}
