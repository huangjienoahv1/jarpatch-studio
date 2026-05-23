# JarPatch Studio 第一版开发计划

## Summary
开发一个完全开源的跨平台桌面软件 `JarPatch Studio`，面向普通 Java 开发者，用于在没有源码或源码不完整时，对普通 Jar、Spring Boot Jar、War 包进行结构分析、反编译查看、代码/资源编辑、重新编译和导出新包。

技术路线固定为：`Electron 前端 + Java 后端 + 本地 HTTP API + WebSocket 进度日志 + SQLite 本地历史库`。项目从 `D:\code\JavaHot` 从零开始。许可证使用 `Apache-2.0`。

## Key Changes
- 初始化项目结构：
  - 根目录创建 `readme.md`，作为用户说明书和项目规划文档。
  - 前端使用 Electron，负责文件选择、目录树、代码编辑器、分析面板、差异面板、导出向导。
  - 后端使用 Java，负责 Jar/War 识别、解压、CFR 反编译、JDK 检测、编译、替换 class、重新打包。
  - 本地数据使用 SQLite，保存项目历史、任务记录、修改文件、导出记录、分析结果。
- 第一版支持范围：
  - 支持普通 Jar、Spring Boot Jar、War。
  - 支持编辑反编译后的 Java 文件。
  - 支持编辑文本资源文件：`properties`、`yml`、`yaml`、`xml`、`json`、`txt`、`html`、`css`、`js`。
  - 二进制文件、签名文件默认只展示，不允许直接编辑。
  - 不加入 AI 功能，不做攻击、注入、漏洞利用、内存马等能力。
- 增强分析能力：
  - 识别包类型、入口类、Manifest、Spring Boot 结构、War 结构、依赖 Jar 列表。
  - 标记签名文件、嵌套 Jar、多版本 Jar、混淆迹象、可能影响重打包的风险。
  - 导出前展示修改清单、资源变更、重新编译结果和结构校验结果。
- 本地接口约定：
  - `POST /api/projects/import`：导入 Jar/War，创建本地项目。
  - `GET /api/projects`：读取项目历史。
  - `GET /api/projects/{id}/tree`：读取解压后的文件树。
  - `GET /api/projects/{id}/files/content`：读取文本文件内容。
  - `PUT /api/projects/{id}/files/content`：保存 Java 或资源文件修改。
  - `POST /api/projects/{id}/analyze`：执行结构分析。
  - `POST /api/projects/{id}/compile`：编译已修改 Java 文件。
  - `POST /api/projects/{id}/export`：导出新的 Jar/War。
  - `GET /api/tasks/{taskId}`：查询任务状态。
  - `WS /ws/tasks/{taskId}`：推送进度、日志、错误信息。
- 代码规范：
  - 所有新增 Java 类写类级 Javadoc，包含职责、使用场景、核心调用关系和 `@author 黄杰`。
  - 所有关键方法和 private 方法写方法注释。
  - 核心流程必须写行内注释，说明入口、实际执行点、结果写入位置。
  - 展示文案、默认提示语、状态值、类型码、阈值全部抽常量或枚举。
  - 所有中文文件按 UTF-8 编码处理。

## Implementation Plan
- 第一步搭项目骨架：
  - 创建 Electron 前端、Java 后端、SQLite 初始化、Apache-2.0 许可证和 `readme.md`。
  - Windows 作为第一验证平台，但路径、临时目录、JDK 检测必须按跨平台方式设计。
- 第二步做后端核心闭环：
  - 包识别：普通 Jar、Spring Boot Jar、War 分开处理。
  - 工作区：每次导入生成独立项目目录，保存原始包、解压目录、源码目录、编译输出、导出目录。
  - 反编译：内置 CFR，用户 JDK 自动检测加手动配置。
  - 编译：只编译用户修改过的 Java 文件，使用原包依赖构建 classpath。
  - 打包：普通 Jar、Spring Boot Jar、War 使用各自打包策略，Spring Boot 嵌套 Jar 保持可启动结构。
- 第三步做前端产品界面：
  - 首页展示项目历史和“打开 Jar/War”入口。
  - 工作台包含文件树、代码编辑器、分析面板、日志面板、导出按钮。
  - 导出前必须展示修改清单和风险提示。
- 第四步补产品化能力：
  - SQLite 保存历史项目、任务状态、修改记录、导出记录。
  - README 写清楚功能用途、使用步骤、参数说明、限制边界和常见错误。
  - 新建文档文件统一使用日期开头，例如 `2026-05-16-jarpatch-studio-design.md`。

## Verification
不新增自动化测试用例，按手工验收和构建检查验证：

- 使用一个普通 Jar 验证：导入、反编译、修改 Java、编译、导出、重新运行。
- 使用一个 Spring Boot Jar 验证：导入、识别 `BOOT-INF`、修改 Controller 或配置、导出后能启动。
- 使用一个 War 包验证：导入、识别 `WEB-INF`、修改资源或 Java 类、导出后结构正确。
- 验证资源文件修改：`application.yml`、`properties`、`xml` 保存后导出包中内容正确。
- 验证失败场景：JDK 未配置、CFR 不存在、Java 编译失败、包结构异常时，界面能显示清楚错误。
- 验证历史记录：关闭软件后重新打开，能看到项目历史、修改清单和导出记录。

## Assumptions
- 产品名固定为 `JarPatch Studio`，当前开发目录仍使用 `D:\code\JavaHot`。
- 第一版完全开源，许可证为 `Apache-2.0`。
- 第一轮以 Windows 验证为主，但架构必须支持后续 Mac/Linux。

## 2026-05-23 确认的下一阶段路线图

这部分是当前确认要继续补齐的产品能力，建议按下面顺序推进。
其中实时任务日志、任务取消和二进制/签名文件只读提示已经落地，后续主要补 JDK 配置、差异视图、自动校验和交付能力。

### 第一组：运行态补齐

- 前端接入 `/ws/tasks/{taskId}`，把任务日志从同步返回切换为实时任务日志。
- 增加任务中断和长任务取消，覆盖导入、反编译、编译和导出。
- 增加二进制文件、签名文件、超大文件的只读提示和风险说明。

### 第二组：配置与排查

- 增加 JDK 路径配置页面，并在保存时校验 `javac` 可用性。
- 增加项目级设置，至少包含默认导出目录、默认是否勾选推荐嵌套 Jar、文件树宽度以外的界面偏好。
- 增加导出前差异对比视图，展示源码差异、资源差异和即将写回的 class 清单。
- 增加导出后自动结构校验，确认 Manifest、Spring Boot 未压缩嵌套 Jar、War 目录和修改文件是否进入目标包。
- 增加完整错误排查向导，把 JDK 缺失、CFR 缺类、编译失败、包签名失效、路径过长等问题整理成界面可读说明。

### 第三组：清理与交付

- 增加“只清理本地工作区”的独立安全入口，和删除历史分开处理。
- 补充 Mac/Linux 的一键启动和打包脚本。
- 补充 Windows 安装包或免安装包产物。
- 补充更多真实样本手工验收记录，覆盖普通 Jar、复杂 Spring Boot Jar、War、签名包、多版本 Jar、混淆包。
