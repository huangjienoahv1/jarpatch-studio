package com.jarpatch.service;

import com.jarpatch.common.JarPatchConstants;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 项目可编辑文件导入基线服务。
 * <p>
 * 导入流程在临时工作区完成反编译后调用本服务，把 sources 和 extracted 中所有可编辑
 * 文本按原始字节复制到 baseline；差异视图和恢复判断只依赖这份固定基线，不重新反编译
 * 或猜测原内容。
 * </p>
 *
 * @author 黄杰
 */
@Service
public class BaselineService {

    private final FileKindService fileKindService;

    /**
     * 创建项目基线服务。
     *
     * @param fileKindService 文件类型服务
     */
    public BaselineService(FileKindService fileKindService) {
        this.fileKindService = fileKindService;
    }

    /**
     * 捕获完整导入临时工作区的可编辑文件基线。
     *
     * @param projectRoot 导入临时工作区根目录
     * @throws IOException 复制失败时抛出
     */
    public void capture(Path projectRoot) throws IOException {
        Path baselineRoot = projectRoot.resolve(JarPatchConstants.WORKSPACE_BASELINE_DIR);
        captureRoot(projectRoot.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR),
                baselineRoot.resolve(JarPatchConstants.WORKSPACE_SOURCE_DIR));
        captureRoot(projectRoot.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR),
                baselineRoot.resolve(JarPatchConstants.WORKSPACE_EXTRACTED_DIR));
    }

    /**
     * 按相对路径复制单个目录内所有可编辑文件。
     *
     * @param sourceRoot   当前文件根目录
     * @param baselineRoot 对应基线根目录
     * @throws IOException 遍历或复制失败时抛出
     */
    private void captureRoot(Path sourceRoot, Path baselineRoot) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }
        try (var stream = Files.walk(sourceRoot)) {
            var iterator = stream.filter(Files::isRegularFile)
                    .filter(path -> fileKindService.detect(path).isEditable())
                    .iterator();
            while (iterator.hasNext()) {
                Path source = iterator.next();
                Path target = baselineRoot.resolve(sourceRoot.relativize(source)).normalize();
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
