package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
