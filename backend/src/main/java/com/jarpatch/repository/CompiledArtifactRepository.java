package com.jarpatch.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 已提交编译产物清单仓储。
 * <p>
 * CompileService 在全部 class 成功写回后按项目替换清单；差异视图和导出校验读取该表，
 * 确认主 classes 与嵌套 Jar 内实际需要验证的 class 条目。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class CompiledArtifactRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建编译产物仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public CompiledArtifactRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 在单个数据库事务内替换项目编译产物清单。
     *
     * @param projectId     项目 ID
     * @param artifactPaths 已提交产物路径
     * @param createdAt     提交时间
     */
    @Transactional
    public void replaceProjectArtifacts(String projectId, List<String> artifactPaths, String createdAt) {
        jdbcTemplate.update("DELETE FROM compiled_artifacts WHERE project_id = ?", projectId);
        for (String artifactPath : artifactPaths) {
            jdbcTemplate.update("INSERT INTO compiled_artifacts (id, project_id, artifact_path, created_at) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), projectId, artifactPath, createdAt);
        }
    }

    /**
     * 查询项目当前已提交编译产物路径。
     *
     * @param projectId 项目 ID
     * @return 按路径排序的产物清单
     */
    public List<String> findPaths(String projectId) {
        return jdbcTemplate.queryForList(
                "SELECT artifact_path FROM compiled_artifacts WHERE project_id = ? ORDER BY artifact_path",
                String.class,
                projectId);
    }

    /**
     * Java 源码再次修改后清空已过期编译产物清单。
     *
     * @param projectId 项目 ID
     */
    public void clear(String projectId) {
        jdbcTemplate.update("DELETE FROM compiled_artifacts WHERE project_id = ?", projectId);
    }
}
