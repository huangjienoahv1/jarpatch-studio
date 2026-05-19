package com.jarpatch.common;

/**
 * 工作区文件类型枚举。
 * <p>
 * 文件内容接口通过该枚举判断用户能否编辑文件，导出前差异统计也用它区分 Java 代码、
 * 文本资源、二进制文件和签名文件。
 * </p>
 *
 * @author 黄杰
 */
public enum FileKind {

    JAVA("JAVA", "Java 源码", true),
    RESOURCE("RESOURCE", "文本资源", true),
    SIGNATURE("SIGNATURE", "签名文件", false),
    BINARY("BINARY", "二进制文件", false),
    DIRECTORY("DIRECTORY", "目录", false);

    private final String code;
    private final String label;
    private final boolean editable;

    /**
     * 创建文件类型枚举项。
     *
     * @param code     数据库存储编码
     * @param label    界面展示名称
     * @param editable 是否允许通过界面编辑
     */
    FileKind(String code, String label, boolean editable) {
        this.code = code;
        this.label = label;
        this.editable = editable;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEditable() {
        return editable;
    }
}
