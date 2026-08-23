package com.jarpatch.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Map;
import java.util.LinkedHashMap;

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
     * @param originalHash 导入基线哈希
     * @param currentHash  当前文件哈希
     * @param encoding     用户保存时明确确认的编码
     * @param now          当前时间
     */
    public void upsert(String projectId,
                       String relativePath,
                       String fileKind,
                       String originalHash,
                       String currentHash,
                       String encoding,
                       String now) {
        int updated = jdbcTemplate.update("UPDATE file_changes SET file_kind = ?, current_hash = ?, encoding = ?, updated_at = ? " +
                        "WHERE project_id = ? AND relative_path = ?",
                fileKind, currentHash, encoding, now, projectId, relativePath);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO file_changes " +
                            "(id, project_id, relative_path, file_kind, original_hash, current_hash, encoding, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID().toString(), projectId, relativePath, fileKind, originalHash, currentHash,
                    encoding, now, now);
        }
    }

    /**
     * 查询文件保存时明确确认的编码。
     *
     * @param projectId    项目 ID
     * @param relativePath 文件树相对路径
     * @return 尚未修改或未记录时为空
     */
    public Optional<String> findEncoding(String projectId, String relativePath) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT encoding FROM file_changes WHERE project_id = ? AND relative_path = ? AND encoding IS NOT NULL",
                String.class, projectId, relativePath);
        return values.stream().findFirst();
    }

    /**
     * 查询项目内全部已修改文件的明确编码，避免搜索时逐文件访问数据库。
     *
     * @param projectId 项目 ID
     * @return 文件树路径到编码名的映射
     */
    public Map<String, String> findEncodings(String projectId) {
        Map<String, String> encodings = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT relative_path, encoding FROM file_changes " +
                        "WHERE project_id = ? AND encoding IS NOT NULL",
                resultSet -> {
                    encodings.put(resultSet.getString("relative_path"), resultSet.getString("encoding"));
                }, projectId);
        return encodings;
    }

    /**
     * 文件恢复到导入基线后删除修改记录。
     *
     * @param projectId    项目 ID
     * @param relativePath 文件树相对路径
     */
    public void delete(String projectId, String relativePath) {
        jdbcTemplate.update("DELETE FROM file_changes WHERE project_id = ? AND relative_path = ?",
                projectId, relativePath);
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
