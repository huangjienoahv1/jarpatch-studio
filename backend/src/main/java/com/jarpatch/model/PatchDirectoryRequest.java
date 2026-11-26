package com.jarpatch.model;

/**
 * 补丁目录请求。预览只需要目录，应用需要匹配当前内容的确认标识。
 *
 * @author 黄杰
 */
public record PatchDirectoryRequest(String directory, String confirmation) {
}
