package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * CFR 批量反编译执行器。
 * <p>
 * {@link DecompilerService} 按稳定顺序收集 class 后调用本组件；本组件以固定批次调用
 * CFR {@link CfrDriver#analyse(List)}，在批次之间检查取消状态，并通过输出接收器把源码
 * 写入目标目录、把单文件异常记录到后端滚动日志。
 * </p>
 *
 * @author 黄杰
 */
@Component
public class CfrBatchDecompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CfrBatchDecompiler.class);
    private static final int CFR_BATCH_SIZE = 200;
    private static final String CFR_OPTION_SILENT = "silent";
    private static final String CFR_OPTION_VALUE_TRUE = "true";
    private static final String JAVA_FILE_SUFFIX = ".java";
    private static final String MESSAGE_OUTPUT_OUT_OF_RANGE = "CFR 输出路径超出源码目录";
    private static final String LOG_DECOMPILE_FILE_FAILED = "CFR 反编译单文件失败，path={}, message={}";

    /**
     * 批量反编译 class 文件，并在批次边界响应任务取消。
     *
     * @param classFiles 已按稳定顺序排列的 class 文件
     * @param sourceDir 源码输出目录
     * @param cancelRequested 取消检查回调
     * @throws IOException 创建或写入源码文件失败时抛出
     */
    public void decompile(List<Path> classFiles, Path sourceDir, BooleanSupplier cancelRequested) throws IOException {
        if (classFiles.isEmpty()) {
            return;
        }
        Files.createDirectories(sourceDir);
        CfrOutputSink outputSink = new CfrOutputSink(sourceDir);
        try {
            for (int start = 0; start < classFiles.size(); start += CFR_BATCH_SIZE) {
                ensureNotCancelled(cancelRequested);
                int end = Math.min(start + CFR_BATCH_SIZE, classFiles.size());
                List<String> batch = classFiles.subList(start, end).stream()
                        .map(path -> path.toAbsolutePath().normalize().toString())
                        .toList();
                CfrDriver driver = new CfrDriver.Builder()
                        .withOptions(Map.of(CFR_OPTION_SILENT, CFR_OPTION_VALUE_TRUE))
                        .withOutputSink(outputSink)
                        .build();
                driver.analyse(batch);
            }
        }
        catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    /**
     * 检查任务取消状态。
     *
     * @param cancelRequested 取消检查回调
     */
    private void ensureNotCancelled(BooleanSupplier cancelRequested) {
        if (cancelRequested != null && cancelRequested.getAsBoolean()) {
            throw new IllegalStateException(JarPatchConstants.MESSAGE_TASK_CANCELLED);
        }
    }

    /**
     * CFR 输出接收器，把反编译源码安全写入目标目录并记录单文件异常。
     */
    private static final class CfrOutputSink implements OutputSinkFactory {

        private final Path sourceDir;

        /**
         * 创建限定输出目录的 CFR 接收器。
         *
         * @param sourceDir 源码输出目录
         */
        private CfrOutputSink(Path sourceDir) {
            this.sourceDir = sourceDir.toAbsolutePath().normalize();
        }

        /**
         * 声明源码和异常两类结构化输出格式。
         *
         * @param sinkType 输出类型
         * @param available CFR 可用格式
         * @return 本接收器支持的首选格式
         */
        @Override
        public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
            if (sinkType == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                return List.of(SinkClass.DECOMPILED);
            }
            if (sinkType == SinkType.EXCEPTION && available.contains(SinkClass.EXCEPTION_MESSAGE)) {
                return List.of(SinkClass.EXCEPTION_MESSAGE);
            }
            return List.of();
        }

        /**
         * 返回源码写入或异常记录函数。
         *
         * @param sinkType 输出类型
         * @param sinkClass 结构化输出格式
         * @return CFR 调用的输出函数
         */
        @Override
        public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
            if (sinkType == SinkType.JAVA && sinkClass == SinkClass.DECOMPILED) {
                return value -> writeJava((SinkReturns.Decompiled) value);
            }
            if (sinkType == SinkType.EXCEPTION && sinkClass == SinkClass.EXCEPTION_MESSAGE) {
                return value -> logException((SinkReturns.ExceptionMessage) value);
            }
            return value -> {
            };
        }

        /**
         * 按包名和类名把 CFR 源码写入安全目标路径。
         *
         * @param decompiled CFR 反编译源码
         */
        private void writeJava(SinkReturns.Decompiled decompiled) {
            String packageName = decompiled.getPackageName();
            String packagePath = packageName == null
                    ? JarPatchConstants.EMPTY_TEXT
                    : packageName.replace('.', '/');
            Path relativePath = packagePath.isBlank()
                    ? Path.of(decompiled.getClassName() + JAVA_FILE_SUFFIX)
                    : Path.of(packagePath, decompiled.getClassName() + JAVA_FILE_SUFFIX);
            Path outputPath = sourceDir.resolve(relativePath).normalize();
            if (!outputPath.startsWith(sourceDir)) {
                throw new IllegalStateException(MESSAGE_OUTPUT_OUT_OF_RANGE);
            }
            try {
                Files.createDirectories(outputPath.getParent());
                Files.writeString(outputPath, decompiled.getJava(), StandardCharsets.UTF_8);
            }
            catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        /**
         * 把 CFR 提供的原始输入路径和异常信息写入滚动日志，保留单文件定位能力。
         *
         * @param exceptionMessage CFR 单文件异常
         */
        private void logException(SinkReturns.ExceptionMessage exceptionMessage) {
            LOGGER.warn(LOG_DECOMPILE_FILE_FAILED, exceptionMessage.getPath(), exceptionMessage.getMessage(),
                    exceptionMessage.getThrownException());
        }
    }
}
