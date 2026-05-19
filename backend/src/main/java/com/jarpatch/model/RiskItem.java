package com.jarpatch.model;

/**
 * 包结构分析风险项。
 * <p>
 * 分析服务把签名文件、嵌套 Jar、多版本目录和混淆迹象转换为风险项，前端在分析面板
 * 中按 level 字段展示风险等级。
 * </p>
 *
 * @author 黄杰
 */
public class RiskItem {

    private String level;
    private String title;
    private String detail;

    /**
     * 创建空风险项，供 JSON 序列化框架使用。
     */
    public RiskItem() {
    }

    /**
     * 创建完整风险项。
     *
     * @param level  风险等级
     * @param title  风险标题
     * @param detail 风险说明
     */
    public RiskItem(String level, String title, String detail) {
        this.level = level;
        this.title = title;
        this.detail = detail;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
