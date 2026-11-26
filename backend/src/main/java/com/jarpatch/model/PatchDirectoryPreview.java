package com.jarpatch.model;

import java.util.List;

/**
 * 补丁目录的只读预览及与文件内容绑定的确认标识。
 *
 * @author 黄杰
 */
public record PatchDirectoryPreview(String directory, String confirmation, boolean applicable, List<Entry> entries) {

    /** 输入文件、全限定类名、候选包内位置和阻塞原因。 */
    public record Entry(String input, String className, List<String> targets, String problem) {
    }
}
