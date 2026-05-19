# JarPatch Studio 第一版设计说明

## 产品定位

JarPatch Studio 面向普通 Java 开发者，用于在缺少源码时对 Jar、Spring Boot Jar、War 做临时修复、结构分析、重新编译和导出。

## 第一版边界

- 支持 Java 17 后端。
- 支持 Electron 桌面界面。
- 支持普通 Jar、Spring Boot Jar、War。
- 支持 Java 源码和文本资源编辑。
- 支持导入前解析 `pom.xml` 和嵌套 Jar 候选项，由用户手动选择需要反编译的 Jar。
- 支持反编译 Spring Boot、War 内部被用户选中的嵌套 Jar，并按原 Jar 路径展示在 `sources/nested-jars`。
- 支持删除左侧项目历史，删除范围限定为 SQLite 历史和关联记录，不删除工作区文件。
- 使用中国时区生成和展示项目历史、任务日志时间。
- 默认隐藏 Electron 系统菜单栏，避免展示暂未接入功能的 `File / Edit / View / Window / Help`。
- 不支持二进制文件直接编辑。
- 不提供攻击、注入、漏洞利用能力。

## 核心调用链

1. 前端选择文件。
2. 后端 `ProjectController.inspectProject` 接收路径，`ProjectInspectionService` 读取 `pom.xml`、包类型和嵌套 Jar 候选项，不写工作区和 SQLite。
3. 前端弹窗展示候选 Jar，用户确认需要反编译的 Jar。
4. 后端 `ProjectController.importProject` 接收路径和 `selectedNestedJars`。
5. `ProjectService` 创建工作区、复制原始包、解压、识别包类型、调用 CFR 反编译主 classes 和用户选择的嵌套 Jar。
6. `ProjectRepository` 写入项目记录。
7. 前端读取文件树和内容。
8. 用户点击左侧项目历史删除时，前端调用 `DELETE /api/projects/{projectId}`，`ProjectService` 通过 `ProjectRepository` 删除 SQLite 历史和关联记录，工作区文件保留。
9. 用户保存文件后，`FileContentService` 写入工作区文件，并通过 `FileChangeRepository` 记录修改。
10. 分析入口调用 `AnalysisService` 生成结构报告和风险项。
11. 编译入口调用 `CompileService`，先整理本次参与编译的反编译源码，删除完全重复的相邻注解和 `CommonResult.success(...)` 入参最外层多余 `(Object)` 强转，再执行本机 `javac`，主源码结果复制回 classes 根目录，嵌套 Jar 源码结果通过 `ArchiveService` 写回原 Jar。
12. 导出入口调用 `ExportService`，由 `ArchiveService` 重新打包并写入导出记录。
13. 前端 `notify` 统一显示打开、保存、搜索、分析、编译、导出等操作的成功、失败和取消提示，同时追加到底部执行日志。

## 工作区结构

```text
.jarpatch-studio/projects/{projectId}/
  original/
  extracted/
  sources/
    nested-jars/
  compiled/
  exports/
```

## 数据库存储

SQLite 表：

- `projects`：项目历史。
- `tasks`：导入、分析、编译、导出任务。
- `file_changes`：修改文件记录。
- `export_records`：导出记录。

审计字段默认值统一使用 `admin`。

## 风险提示

分析服务会提示：

- 签名文件风险。
- 嵌套 Jar 风险。
- 多版本目录风险。
- 混淆代码风险。

## 反编译特殊文件处理

- `module-info.class` 是 Java 模块描述文件，不是普通业务类。
- 导入流程会在主 classes 和嵌套 Jar 解压后的 class 扫描中跳过 `module-info.class`。
- 该文件仍保留在 `extracted` 原始结构中，导出时不会因为跳过反编译而丢失。

## 验收方式

不新增自动化测试用例。使用 Maven 构建、Electron 启动、本地 Jar/War 手工流程验证。

## 后端启动包完整性检查

- 入口：用户双击 `2026-05-16-start-jarpatch-studio.cmd`。
- 实际检查点：脚本打开 `backend/target/jarpatch-studio-backend.jar`，确认包内存在 `BOOT-INF/lib/cfr-0.152.jar`。
- 触发条件：后端 jar 不存在，或者只剩普通 jar、损坏 jar、不含 CFR 依赖的 jar。
- 结果写入：脚本重新执行 `mvn -DskipTests package`，生成完整 Spring Boot 可执行包，避免导入时 CFR 内部类 `NoClassDefFoundError`。

## 2026-05-16 编译兼容补充

- 入口：前端点击“编译”后调用 `POST /api/projects/{id}/compile`。
- 实际执行点：`CompileService` 在生成 `javac @参数文件` 前整理本次修改的 Java 源码，然后调用本机 `javac`。
- 结果写入：整理后的源码写回 `sources`，编译产物写入 `compiled` 后再回写到 `extracted` 或对应嵌套 Jar。
- 处理范围：仅删除同一连续注解序列内文本完全一致的重复注解，以及 `CommonResult.success(...)` 入参最外层多余 `(Object)` 强转。
