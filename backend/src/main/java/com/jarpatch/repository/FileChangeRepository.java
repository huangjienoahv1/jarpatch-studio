package com.jarpatch.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 文件修改记录仓储。
 * <p>
 * 文件保存服务在用户修改 Java 或资源文件后调用该仓储记录相对路径，分析和导出服务
 * 通过这些记录生成修改清单。
 * </p>
 *
 * @author 黄杰
 */
@Repository
public class FileChangeRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建文件修改记录仓储。
     *
     * @param jdbcTemplate Spring JDBC 操作入口
     */
    public FileChangeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增或更新修改记录。
     *
     * @param projectId    项目 ID
     * @param relativePath 修改文件相对路径
     * @param fileKind     文件类型
     * @param now          当前时间
     */
    public void upsert(String projectId, String relativePath, String fileKind, String now) {
        int updated = jdbcTemplate.update("UPDATE file_changes SET file_kind = ?, updated_at = ? " +
                        "WHERE project_id = ? AND relative_path = ?",
                fileKind, now, projectId, relativePath);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO file_changes " +
                            "(id, project_id, relative_path, file_kind, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID().toString(), projectId, relativePath, fileKind, now, now);
        }
    }

    /**
     * 查询项目修改文件路径。
     *
     * @param projectId 项目 ID
     * @return 修改文件相对路径列表
     */
    public List<String> findPaths(String projectId) {
        return jdbcTemplate.queryForList("SELECT relative_path FROM file_changes WHERE project_id = ? ORDER BY updated_at DESC",
                String.class, projectId);
    }

    /**
     * 查询项目已修改 Java 文件路径。
     *
     * @param projectId 项目 ID
     * @return Java 文件相对路径列表
     */
    public List<String> findJavaPaths(String projectId) {
        return jdbcTemplate.queryForList("SELECT relative_path FROM file_changes " +
                        "WHERE project_id = ? AND file_kind = 'JAVA' ORDER BY updated_at DESC",
                String.class, projectId);
    }
}
