# JarPatch Studio

JarPatch Studio 是一个完全开源的跨平台桌面软件，面向普通 Java 开发者，用于在没有源码或源码不完整时，对普通 Jar、Spring Boot Jar、War 包进行结构分析、反编译查看、代码/资源编辑、重新编译和导出新包。

## 当前版本能力

- 打开普通 Jar、Spring Boot Jar、War。
- 解压原始包到本地工作区。
- 导入前解析 `pom.xml` 和嵌套 Jar，用户手动选择需要反编译的 Jar。
- 使用 CFR 主 API 分批反编译主 class 和用户选中的嵌套 Jar 到 `sources` 目录。
- 在桌面界面懒加载查看 `sources` 和 `extracted` 两类文件树，展开目录时只读取直接子项。
- 编辑 Java 源码和文本资源文件。
- 代码编辑区已升级为 Monaco Editor，提供语法高亮、括号匹配、代码折叠、缩略图、查找替换、快捷键和命令面板。
- 可选接入 Eclipse JDT LS：提供 Java 代码补全、悬停说明、实时错误/警告、跳转定义、快速修复、格式化和整理 import；补全返回的附加编辑会同步写入 import。
- 可选接入 OpenAI Responses、兼容 Chat Completions 或兼容 Anthropic Messages 的 AI 端点：支持显式 AI 补全、解释、修复和重构；解释只读展示，代码修改必须先预览再由用户应用。
- 编辑器提供“中文预览”按钮，可将 `\uXXXX` 形式的中文 Unicode 转义显示为正常中文；预览使用独立的只读 Monaco Editor（保留语法高亮、行号、折叠、小地图与深浅色主题联动），自动高亮解码区间并支持“上一处 / 下一处”定位及滚动位置双向同步，不替换、不保存原始文本。
- 保存时保留原始编码、BOM 和换行；无 BOM 旧文本通过项目默认编码或文件级编码显式选择读取，不做自动猜测；内容未改变时不写文件，外部内容变化时阻止覆盖。
- 保存期间可继续编辑，新输入会保留为未保存状态；重复保存会提示等待当前保存完成，后续修改需要再次保存。切换文件或项目时，过期的文件读取和保存响应不会覆盖当前文档。
- Java 源码统一按 UTF-8 保存和编译；编辑器工具栏的编码切换仅对资源文件生效，Java 文件选择其他编码保存会被拒绝，避免重新编译后中文字符串乱码。
- 二进制文件和签名文件只读展示，点击会提示当前不能编辑。
- 通过右上角“开发能力”打开配置弹窗；编译 JDK 保存时会校验 `bin/javac` 可用性并写入 SQLite，JDT LS 与 AI 设置由 Electron 主进程单独安全保存。
- 自动读取原始 class major version，严格选择匹配 JDK 并使用 `--release` 编译。
- 补丁编译固定使用 `-proc:none`，不自动运行原包依赖中的注解处理器；需要注解处理生成代码的源码应使用原项目构建流程。
- 记录本地项目历史、任务状态、持久化任务日志、修改基线、编译产物、分析报告和导出校验结果到 SQLite。
- 项目历史界面可查询分析报告、导出校验和带统一操作 ID 的任务时间线。
- 删除历史时可明确选择“仅删除历史”或“预览清理工作区后再删除”；孤立工作区必须单独扫描、预览和确认，不会后台自动删除。
- 分析 Manifest、入口类、Spring Boot 结构、War 结构、依赖 Jar、签名文件、多版本目录和混淆迹象。
- 提供基于固定导入基线的源码/资源差异和已提交 class 清单。
- 编译先写独立 staging，全部成功后才统一写回，提交失败时恢复备份。
- 导出修改后的 Jar 或 War；先写同目录临时文件，结构校验通过后才原子发布。导出路径不能位于项目工作区内部（`exports` 目录除外），避免导出包被后续编译和再次导出收录。
- 导出最终发布与导出记录、任务成功状态在同一短事务内提交；提交异常时恢复原目标文件。最终提交先完成时，取消按钮会保留实际成功结果。进程突然退出时仍需核对目标文件及同目录 `.backup` 备份，不承诺文件系统和 SQLite 跨进程崩溃原子性。
- 签名包修改后默认阻止保留失效签名，只有明确选择后才移除签名并导出未签名包。
- 后端仅监听 `127.0.0.1`，桌面端每次启动生成随机令牌和实例 ID，HTTP 与 WebSocket 都必须完成握手。
- 后端迁移、索引、条件状态更新、崩溃恢复、健康检查和安全退出已接入。
- 提供项目设置、错误排查向导、脱敏诊断导出、单实例启动、Windows 免安装包以及 macOS/Linux 原生构建入口。
- 提供远程 Docker 运行时工作台，可查看容器日志、在线反编译已加载的类、检查 JVM、追踪方法耗时，并从容器内发送业务 HTTP 请求。
- 运行时工作台支持本地部署包对照、请求集合回归、请求日志关联、JFR 和火焰图采集，以及诊断报告导出。
- 运行时工作台支持 Kubernetes、多个目标实例检查、Actuator、Loki、Arthas Tunnel 控制台和 Docker 容器热替换。
- 打开、保存、搜索、分析、编译、导出等操作会在右上角显示即时提示，并同步写入底部执行日志；导入、分析、编译、导出会先创建任务、连接 `/ws/tasks/{taskId}`，在任务状态栏里实时显示进度，执行期间可直接取消。
- 同一项目的保存、分析、编译、导出、历史删除和工作区清理预览/确认共享非阻塞互斥边界；已有操作未结束时，新操作会立即提示项目操作正在执行。存在运行中任务时不能删除历史。
- 工作区清理确认校验路径、大小、修改时间和文件内容摘要，等长修改也会使旧确认失效；预览与确认会读取完整工作区内容，大工作区耗时取决于磁盘与文件体量。
- 项目历史和执行日志使用中国时区时间显示。
- 桌面端已移除默认 `File / Edit / View / Window / Help` 菜单栏，后续需要菜单能力时再按功能补回。
- 左侧项目栏默认展开，可用右上角箭头收起到左侧窄栏，点击箭头再次展开。
- 主工作区采用代码优先布局，“分析与风险”默认收起为右侧窄按钮，点击后才展开风险详情，避免长期占用代码编辑空间。
- 顶部操作栏会随窗口宽度自适应：非全屏时项目摘要与操作按钮分行展示，更窄窗口下按钮按完整文案整行换行，不会被压缩成逐字竖排。
- 任务状态、编辑器工具栏和底部执行日志采用紧凑布局；编辑器工具栏会为文件摘要保留最小宽度，并让操作按钮在自身区域内整组换行，避免长路径逐字竖排后挤走代码正文。
- 中间文件树和代码编辑区使用固定工作区高度，内容过长时在区域内部滚动；文件树横向滚动条固定在文件树可视区域底部，代码区文件名会完整换行展示，文件树和代码区之间支持拖拽调整宽度。

## 不做什么

- 不让 AI 自动、后台或静默修改代码；AI 修改只能由用户明确触发并确认应用。
- 不提供攻击、注入、漏洞利用、内存马等能力。
- 不自动兜底修改业务代码。
- 不承诺所有混淆包、签名包、多版本包都能无损重新编译。

## 技术架构

- 前端：Electron + Monaco Editor。
- Java 代码智能：可选 Eclipse JDT LS（LSP）。
- AI 代码助手：支持 OpenAI Responses API、兼容 Chat Completions 和兼容 Anthropic Messages 的端点。
- 后端：Java 17 + Spring Boot 3。
- 通信：仅本机 HTTP API + 带令牌握手的 WebSocket 实时日志。
- 存储：SQLite。
- 反编译：CFR。
- 工作区：默认位于用户目录下的 `.jarpatch-studio/projects`。

## 项目分析文档

- 本轮五阶段开发、验证证据和正式发布边界见 [2026-08-23 全部阶段开发与验证报告](docs/2026-08-23-all-stages-development-report.md)。
- 当前完成度、剩余进阶内容和优化优先级见 [2026-08-23 当前完成度与进阶优化分析](docs/2026-08-23-jarpatch-studio-current-status-and-optimization.md)。
- 第一阶段发布一致性开发和 Windows 样本复验结果见 [2026-08-23 第一阶段开发与验收报告](docs/2026-08-23-first-stage-development-report.md)。
- 1 万、5 万、20 万条目分级实测见 [2026-08-23 大包性能基准](docs/2026-08-23-large-archive-benchmark.md)。
- 三平台签名、公证、原生验收和回滚步骤见 [发布运行手册](docs/release-runbook.md)。
- 2026-08-10 六阶段发布级改造的历史分析见 [项目现状分析与补充建议](docs/2026-08-10-jarpatch-studio-project-analysis.md)。
- 本次代码编辑能力的 GitHub 方案比较、选型和配置边界见 [代码编辑能力扩展研究](docs/2026-08-24-code-editor-extension-research.md)。

## 目录说明

```text
jarpatch-studio/
  backend/                 Java 17 后端服务
  frontend/                Electron 前端
  docs/                    设计与交付文档
  samples/acceptance/      真实验收样本和执行脚本
  scripts/                 跨平台发布清单工具
  build.ps1                Windows 发布入口（PowerShell 7）
  build-macos.sh           macOS 原生发布入口
  build-linux.sh           Linux 原生发布入口
  readme.md                项目说明书
  LICENSE                  Apache-2.0 许可证
```

## 后端接口

> 鉴权说明：除本机健康检查、安全退出、引导状态与登录接口外，所有业务 HTTP 请求均需同时携带 `X-JarPatch-Token`（Electron 本机随机实例令牌）和 `X-JarPatch-Session`（登录签发的会话令牌）；WebSocket `/ws/tasks/{taskId}` 需在查询参数中携带 `token` 与 `session`。

### 本地认证与用户管理接口

- **GET `/api/auth/bootstrap`**：读取登录页引导状态。仅在预置默认管理员 `admin`（初始密码 `Admin@123`）尚未修改密码时返回首次使用提示，改密后自动隐藏。
- **POST `/api/auth/login`**：提交 `{"username","password"}` 执行 PBKDF2-HMAC-SHA256 校验并签发 7 天会话令牌（连续输错 5 次锁定 60 秒）。
- **GET `/api/auth/me`**：返回当前会话对应的脱敏用户信息（`id`、`username`、`displayName`、`role`、`status`、`mustChangePassword`、`lastLoginAt` 等）。
- **POST `/api/auth/change-password`**：当前登录用户提交 `{"currentPassword","newPassword"}` 修改本人密码（适用于首次登录强制改密和个人改密），同步吊销其他会话。
- **POST `/api/auth/logout`**：退出当前会话并销毁后端令牌摘要。
- **GET `/api/users?keyword=&role=&status=`**（仅 `ADMIN`）：按关键词、角色与状态查询本地账号列表。
- **POST `/api/users`**（仅 `ADMIN`）：新增本地账号（支持 `ADMIN` / `USER` 角色与“首次登录强制改密”开关）。
- **PUT `/api/users/{userId}`**（仅 `ADMIN`）：修改显示名称、角色或启用/禁用状态（服务端禁止禁用/降级自己或最后一个启用管理员，禁用时立即吊销活跃会话）。
- **POST `/api/users/{userId}/reset-password`**（仅 `ADMIN`）：重置目标用户密码、标记下次登录强制改密并吊销其所有活跃会话。
- **DELETE `/api/users/{userId}`**（仅 `ADMIN`）：删除目标账号及其会话（禁止删除自己或最后一个启用管理员；既有审计历史中的 `operator` 保留）。

### 角色权限边界

- **管理员（`ADMIN`）**：拥有全部能力，包括用户管理（`/api/users/**`）、全局开发能力/JDK 配置（`PUT /api/settings/jdk`）、清理当前工作区（`/api/projects/{id}/workspace/**`）、扫描清理孤立工作区（`/api/workspaces/orphans/**`）及删除项目历史（`DELETE /api/projects/{id}`）。
- **普通用户（`USER`）**：可执行日常 Jar/War 导入、浏览、编辑、搜索、分析、差异对比、编译、导出、运行时工作台诊断、排错向导、脱敏诊断导出以及修改本人密码；高危管理入口在前端隐藏且在后端返回 HTTP 403 拦截。

### POST `/api/projects/inspect`

导入前预解析 Jar 或 War，只读取包类型、`pom.xml` 模块和嵌套 Jar 候选项，不创建工作区、不写入项目历史。

请求：

```json
{
  "filePath": "D:/example/demo.jar"
}
```

返回：包类型、`pom.xml` 模块列表、嵌套 Jar 候选项、默认推荐勾选数量。

### POST `/api/projects/import`

导入 Jar 或 War，创建本地项目。

请求：

```json
{
  "filePath": "D:/example/demo.jar",
  "taskId": "任务 ID",
  "selectedNestedJars": [
    "BOOT-INF/lib/AIQuality-module-qcs-2025.09-SNAPSHOT.jar"
  ]
}
```

返回：项目 ID、包类型、原始路径、工作区路径。

### POST `/api/tasks`

创建任务记录，前端会先拿到 `taskId` 再连接 WebSocket，然后把 `taskId` 传给导入、分析、编译或导出接口。

请求：

```json
{
  "taskType": "IMPORT",
  "projectId": null,
  "message": "开始导入包: demo.jar"
}
```

返回：任务记录。

### GET `/api/settings/jdk`

读取当前 JDK 配置。

返回：已保存的 JDK 安装目录、对应的 `javac` 路径、当前实际生效路径和校验状态。

### PUT `/api/settings/jdk`

保存 JDK 安装目录并立即校验 `bin/javac` 是否可用。

请求：

```json
{
  "javaHome": "C:/Program Files/Java/jdk-17"
}
```

返回：保存后的 JDK 配置视图。

### GET `/api/projects`

读取本地项目历史。

返回：项目记录列表。

### DELETE `/api/projects/{id}`

删除左侧项目历史。

说明：该接口只删除 SQLite 中的项目历史、任务、修改记录和导出记录，不删除本地工作区文件。

### GET/PUT `/api/projects/{id}/settings`

读取或保存项目设置。目标 Java 版本只读，来源是原包 class；可设置默认导出目录、选择的嵌套 Jar、最大可编辑文件字节数、无 BOM 文本默认编码和界面偏好 JSON。

### GET `/api/projects/{id}/workspace/cleanup-preview`

返回工作区绝对路径、文件数、总大小、最后使用时间和一次性 `confirmationId`，不会删除任何文件。

### DELETE `/api/projects/{id}/workspace?confirmationId=...`

校验预览快照和一次性确认标识后只删除工作区，项目历史仍保留。存在运行中任务或预览后内容变化时拒绝清理。

### GET `/api/projects/{id}/history`

读取最近的结构分析快照、导出校验结果和统一操作时间线。

### GET `/api/workspaces/orphans/cleanup-preview`

扫描未被项目历史登记的 `.ready` 工作区，只返回路径、文件数、大小、最后修改时间和一次性确认标识。

### DELETE `/api/workspaces/orphans?confirmationId=...`

重新校验完整预览快照后清理孤立工作区；预览过期或目录内容变化时拒绝删除。

### GET `/api/projects/{id}/diff`

从固定导入基线计算源码差异、资源差异、原始/当前 SHA-256，并返回本次已提交 class 清单。

### GET `/api/projects/{id}/tree`

读取文件树。

返回：`sources` 和 `extracted` 两个根节点。

### GET `/api/projects/{id}/tree/children?path=...`

按目录展开动作只返回一个目录的直接子节点，并使用稳定的大小写无关顺序。

### GET `/api/projects/{id}/files/content?path=...&encoding=...`

读取可编辑文件内容。`encoding` 可选，只用于用户明确覆盖无 BOM 文件的项目默认编码。

路径示例：

```text
sources/com/example/Demo.java
extracted/application.yml
```

### PUT `/api/projects/{id}/files/content`

保存 Java 或文本资源文件。

请求：

```json
{
  "path": "sources/com/example/Demo.java",
  "content": "文件内容",
  "expectedHash": "打开文件时返回的 SHA-256",
  "encoding": "打开文件时后端返回的编码"
}
```

### POST `/api/projects/{id}/analyze`

执行结构分析。

请求会携带 `X-Task-Id` 头，用来让前端先连任务日志，再把分析过程实时显示出来。

返回：包类型、入口类、依赖数量、修改文件、风险项。

### POST `/api/projects/{id}/compile`

编译已修改 Java 文件。

请求会携带 `X-Task-Id` 头。

返回：任务 ID、修改文件列表、编译消息。

### POST `/api/projects/{id}/export`

导出修改后的 Jar 或 War。

请求体可选携带 `taskId`，用于把导出过程挂到同一条任务日志上。

请求：

```json
{
  "outputPath": "D:/example/demo-patched.jar",
  "taskId": "任务 ID"
}
```

### GET `/api/tasks/{taskId}`

查询任务状态。

### GET `/api/tasks/{taskId}/logs`

按数据库插入顺序读取持久化任务日志，后端重启后仍可查看。

### POST `/api/tasks/{taskId}/cancel`

取消正在执行的任务。

### WS `/ws/tasks/{taskId}`

任务日志 WebSocket 地址。前端已在导入、分析、编译和导出前先连接这个地址，任务执行中会实时收到进度日志。

### GET `/api/system/health`

返回产品名、`UP` 状态和本次启动实例 ID。必须携带桌面端生成的 `X-JarPatch-Token`。

### GET `/api/system/error-guide`

返回 JDK、CFR、编译、签名、路径和端口问题的检查步骤与处理步骤。

### POST `/api/system/shutdown`

接受安全退出请求，当前实例完成响应后关闭 Spring 上下文；仅桌面端持有的本机令牌可调用。

## 运行时工作台

1. 打开右上角「运行时工作台」：顶部采用极简单行快速工作条（`服务器 → 加载容器 → 容器 → Java 进程 → 连接/释放 Arthas`）。本机已有已保存主机时，打开后默认选中首台常用服务器并收起详细配置面板，直接点击「加载容器」即可连接；首次使用或需要新增主机、修改密码、切换 `OpenSSH 配置` / `Kubernetes` 或选择离线包时，点击右上角「连接配置」（或在服务器下拉框底部选「＋ 新增 / 临时主机…」）展开配置面板。账号密码模式填写服务器地址、账号、端口和密码，加载容器时核对 SHA-256 主机指纹，临时输入的密码在发起连接后清空；使用「OpenSSH 配置」时，先在桌面端配置主机指纹和密钥，再填写 SSH 别名或 user@host。SSH 账号需有 Docker 操作权限。
2. 选择运行中的 Linux 容器。可以先查看日志或开始实时追踪。选择 Java 进程后，也可以在「请求测试」里直接选择该 JVM 的监听端口，填写方法、路径和可选的请求头、请求体。HTTPS 证书使用域名时，填入对应 TLS 主机名，工作台仍从容器内连接 127.0.0.1，并校验证书。
3. 点击「连接 Arthas」。容器需有兼容 JDK、sh、curl、timeout、可写的 /tmp，并允许使用 JVM 所在用户的身份执行 Attach。工作台固定使用 [Arthas 4.3.5 官方完整包](https://github.com/alibaba/arthas/releases/tag/arthas-all-4.3.5)，桌面端下载或手动选择同版 arthas-bin.zip，校验 SHA-256 后上传。容器没有 unzip 时，自动使用 JDK 自带的 jar 解包。选择离线包时，目标容器和客户端都不需要访问下载站点。
4. 在「类与方法」查已加载的类。简单类名自动按包含关系模糊搜索，完整类名精确搜索，也支持手动填写 `*` 通配模式。点击搜索结果会填入完整类名和 ClassLoader，再查看方法或在线反编译。源码打开后，工作台自动滚到源码区。拖动弹窗最右侧的滚动条，可在源码和顶部操作区之间移动。「查看方法」支持组合筛选访问级别和方法类型，列表按参数签名展示重载，并标注 Java 接口声明、Spring 标准 HTTP 映射注解和代理类方法。点击方法会填入方法名，耗时采集仍按方法名匹配。待界面提示采集启动后，切到「请求测试」触发调用。每次采集最多 20 秒或 3 次。停止采集后尝试释放本次诊断对该类的增强。
5. 用「释放 Arthas」结束本次代理会话并清理其临时目录。正常关闭工作台也会执行清理。连接中断或应用被强制结束时，界面只能报告实际确认结果，重新连通后可在仍打开的工作台重试释放。诊断代理只监听远端回环接口，并验证工作台随会话生成的凭据。

请求和诊断输出仅在工作台内展示。日志追踪每次最多 5 分钟。接口测试发送真实业务请求，HTTP 状态和响应内容以目标服务返回为准。账号密码模式使用内置 SSH 客户端。Win7 使用 OpenSSH 配置模式时，需另备兼容 OpenSSH 并加入 PATH。

在连接配置面板的「SSH 服务器与认证」中填写主机、账号、端口和密码后，可点「保存主机」。下次打开工作台可直接在顶部「服务器」下拉框切换已保存主机并点击「加载容器」，无需重新输入密码。密码使用操作系统的凭据加密保存在本机。密码更换后，展开「连接配置」输入新密码并再次保存。同一主机可以按账号和端口保存多条记录。选中记录后可在配置面板中点「移除主机」删除本地保存的信息。应用重启后的首次连接仍需核对 SSH 主机指纹。

### 部署包对照与回归验证

- 在「类与方法」输入准确的类名及可选的 ClassLoader 哈希，点击「对照本地包」。工作台从指定 JVM 采集一个 class，再读取本地 Jar 或 War。候选包含主包、标准目录下的嵌套依赖和多版本条目。选择候选后可以查看 SHA-256 及两侧 CFR 源码差异。Java Agent 和 JVM 的字节码转换也可能产生摘要差异，结合源码和类加载位置判断。
- 在「请求测试」录入请求并添加到集合，在「回归验证」编辑、保存或打开集合。先选择补丁前的目标和监听端口运行基线，再切换到补丁后的目标运行同一集合。结果展示断言、HTTP 状态、响应体变化及耗时差。可精确解析的 JSON 响应按结构比较，超出数值精度范围的响应按原始字节比较。
- 每个用例可设置 HTTP 状态码、最大耗时、文本包含及 JSON 指针等值断言。打开和保存集合时会清理常见鉴权请求头。本次需要的授权头可填在「本次共用请求头」，集合中的请求体会随集合文件保存。批跑会向选定端口逐条发送实际请求。
- 单次请求完成后，可按 Request ID 或 Trace ID 查看关联日志。没有匹配的标识时按请求时间窗口展示候选日志，关联状态会在界面显示。

### 性能采集与报告

- 在「性能采集」选择 JFR 或火焰图，采集时长为 5 至 30 秒。JFR 依赖目标 JDK 支持。火焰图可选择 CPU 定时器、CPU perf 或 Wall clock，能否启动取决于目标 JVM 和容器环境。含已有活动采样的 JVM 会提示状态，等待该采样结束后可以再启动。
- 采集完成后可保存 JFR 或 HTML 火焰图。取消已启动的采集时，工作台等待 JVM 内的采样时限到达并清理临时文件。JFR、火焰图可能含运行环境和业务方法信息，保存位置由使用者选择。
- 「导出诊断报告」生成 JSON，包含 class 摘要、HTTP 指标、日志匹配方式、回归断言和性能采集信息。请求头、请求体、响应正文、日志原文、源码和性能文件正文都留在各自的查看或保存入口。

### Kubernetes 与服务集成

- 连接方式选 Kubernetes。通过已授权的 Linux SSH 主机运行 kubectl，填写 Context、命名空间、可选标签选择器和该主机上的 kubeconfig 路径。选择 Pod 的应用容器后，可查看 JVM、日志和业务端口。工作台在执行前核对 Pod UID、容器实例和 JVM 启动计数，避免把旧操作发到重建后的实例。
- 应用容器缺少 shell 或 JDK 时，可填写同一 Pod 中已运行的调试容器名。该容器需要能看到目标 JVM，提供完整 JDK、sh、curl 和 timeout，并与目标 JVM 使用相同 UID/GID。共享进程命名空间的 sidecar 和目标正确的临时调试容器均可作为执行端。Arthas 完整包从桌面端上传，目标 JVM 需要可写的临时目录。 [Arthas 容器接入](https://arthas.aliyun.com/doc/docker.html)
- 「环境与集成」可以批量检查至多 20 个已加载目标的镜像、运行状态、JVM 和监听端口。Actuator 读取 health、info、metrics、mappings 和 httpexchanges，实际可用端点由应用的暴露配置和权限决定。
- Loki 使用已有服务的 LogQL 查询接口，支持租户与本次 Bearer 凭据。Arthas Tunnel 使用已有控制台的完整地址和已注册 Agent ID，从系统浏览器打开控制台。WebSocket 端口可按反向代理配置填写，缺省使用官方控制台的 7777。 [Loki HTTP API](https://grafana.com/docs/loki/latest/reference/loki-http-api/) · [Arthas Tunnel](https://arthas.aliyun.com/doc/tunnel.html)

### Docker 容器热替换

工作台支持对已连接 Arthas 的 Docker 容器热替换，由操作者选择容器、Java 进程、类和 ClassLoader。选择同名 .class 后，工作台先备份运行中的字节码，再使用 JVM 支持的类重定义。可手动恢复原 class，释放当前会话时也会尝试恢复。更改字段、方法签名等超出目标 JVM 重定义能力的补丁会由 JVM 拒绝。 [Arthas redefine](https://arthas.aliyun.com/doc/redefine.html)

也可以在「类与方法」选择已加载的应用类，点击「反编译并编辑」，在当前页面修改 Java 源码。点击「编译并热替换」后，选择与目标服务对应的本地 Jar 或 War，工具将用部署包中的类和依赖解析类型，并使用「开发能力」配置的本地 JDK 按运行中 class 的版本编译。编译验证通过后，会再次核对目标 class 的字节码，再备份并替换。成功后可在源码区域点击「恢复原 class」。

「反编译并编辑」显示在「反编译」旁，完成 Docker 容器的 Arthas 连接后即可编辑当前类。

在线源码编译支持单 class 补丁，适合修改方法体。若源码无法通过 javac、生成了多个 class，或目标字节码已变化，工作台会显示原因并保留当前编辑内容。线上部署包文件不会被改写。

### 命令行回归

把回归集合和目标配置提交到可访问的 CI 工作区。目标配置中的请求集合路径相对配置文件。

~~~json
{
  "host": "dev-ssh",
  "target": "my-service",
  "port": 8080,
  "suite": "requests.json"
}
~~~

~~~powershell
node frontend/scripts/run-runtime-suite.js runtime-target.json junit > runtime-results.xml
~~~

输出支持 json 和 junit。全部通过时退出码为 0，用例未通过返回 1，配置、连接或批次错误返回 2。运行前可用 JARPATCH_RUNTIME_HEADERS 注入本次请求头。存在多个 JVM 时，在配置中提供 pid。Kubernetes 目标使用页面中的「Pod / 容器」名称，另提供 cluster 对象，其字段为 context、namespace、selector、debug、kubeconfig。

## 本地启动

### 一键启动

Windows 开发启动可直接双击根目录稳定入口：

```text
start-jarpatch-studio.cmd
```

该脚本强制使用 PowerShell 7，检查 npm、后端 Jar 和 Electron 依赖。`scripts/ensure-backend-package.ps1` 会确认应用入口 class 和 CFR 依赖，并通过同目录 `.source.json` 构建记录比较 Maven 文件、后端源码/资源和 Jar 的内容摘要。首次缺少记录、源码变化或包损坏时，只停止命令行中精确引用该 Jar 路径的旧 Java 进程，再执行 Maven 构建并复验；构建期间源码变化时会拒绝复用该包。

### 1. 构建后端

当前工程要求 Java 17 或更高版本编译，目标字节码为 Java 17。

```powershell
pwsh.exe -NoLogo -NoProfile
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
node scripts/sync-version.js --check
mvn.cmd "-Dmaven.test.skip=true" package
```

应用版本唯一修改入口是根 `package.json` 的 `version`。修改后执行 `node scripts/sync-version.js` 同步 Maven `revision` 和前端版本；所有发布入口都会先执行只读一致性检查。

### 2. 启动桌面端

```powershell
pwsh.exe -NoLogo -NoProfile
npm.cmd --prefix frontend ci
npm.cmd start
```

Electron 会尝试启动 `backend/target/jarpatch-studio-backend.jar`。

如果已经安装过前端依赖，后续只需要执行：

```powershell
npm.cmd start
```

### 构建 Windows 安装包与免安装包

```powershell
pwsh.exe -NoLogo -NoProfile -File .\build.ps1
```

内网使用时，传入 `-BundleJdk17 -JdkHome '完整 JDK 17 目录'` 构建。该模式将完整 JDK 17（含 `javac`、`jar` 和许可证）内置到免安装 EXE，双击即可使用内置 Java 启动和编译 Java 17 原包，无需在目标电脑安装 JDK 或设置 `JAVA_HOME`。软件中已保存的编译 JDK 配置仍优先，迁移后应确认原配置路径有效。其他 Java 目标版本仍遵循已有编译器版本校验；JDT LS 和 AI 为可选能力，不包含在该离线包中。

Windows 构建同时生成以 `-Setup.exe` 结尾的安装包、单文件便携 EXE 和以 `-FastStart.zip` 结尾的快速启动包。安装包提供中文安装向导、自选安装目录、桌面和开始菜单快捷方式及系统卸载入口；安装后直接运行本地文件，无需每次解压。卸载保留用户数据和项目工作区。单文件 EXE 每次启动解压到独立的临时目录，退出后清理。经常使用时，可将快速启动包完整解压到独立目录，之后双击解压目录中的 `jarpatch-studio.exe`，可以为它创建快捷方式。后两种包都免安装，三种形式使用相同的用户数据和应用配置，重复启动会激活已有窗口。

已有完整应用目录时，可以只追加安装包，无需重新编译后端（Win7 原生包示例）：

```powershell
pwsh.exe -NoProfile -File .\scripts\build-windows-installer.ps1 `
  -AppDirectory .\release\windows7-native\FastStart `
  -OutputDirectory .\frontend\dist-win7-native `
  -ArtifactName JarPatch-Studio-0.1.0-Windows7-Native-x64-Setup.exe -Windows7
```

该命令仅生成安装包；单独追加交付时应将成品归档到对应 release 目录并记录大小、SHA-256 和签名状态。安装包沿用输入目录的应用版本和后端，输入目录必须来自同一版本的完整成品。

### 构建 Windows 原生后端验证包

在配置了 MSVC x64 和 Windows SDK 的 Visual Studio 2022 开发者命令行中，用 PowerShell 7 运行以下命令。`-NativeImageHome` 指向带有 `native-image` 的 GraalVM JDK 或 Liberica Native Image Kit，`-JdkHome` 指向用于补丁编译的完整 Windows x64 JDK 17。

```powershell
pwsh.exe -NoLogo -NoProfile -File .\build.ps1 -NativeBackend -NativeImageHome 'C:\Tools\GraalVM' -JdkHome 'C:\Tools\JDK17'
```

产物写入 `release/windows-native/`，同时提供安装包（`-Setup.exe`）、单文件便携 EXE 和快速启动 ZIP。桌面端启动包内的 `jarpatch-studio-backend.exe`，所需的 VC 运行库随应用提供，发布包不包含后端 JAR。编译用户的 Java 改动仍使用随包提供的 JDK 17，已保存的编译 JDK 配置优先。标准 Windows x64 原生包与 Win7 SP1 x64 原生包使用各自的入口和输出目录。原生机器码仍可被逆向分析，Electron 前端 JavaScript 仍随包交付。

### 构建 Win7 SP1 64 位兼容包

在开发电脑上双击仓库根目录的 `build-win7.cmd` 即可打包。脚本优先使用命令参数指定的 JDK，其次读取 `JARPATCH_WIN7_JDK_HOME`，未配置时优先使用 `%SystemDrive%\Tools\LibericaJDK21` 中的 Liberica JDK，并兼容已有的 `%TEMP%\jarpatch-win7-dependencies\jdk-21`。构建工具链应保存在固定目录，避免临时目录清理导致构建失败。也可以从 PowerShell 7 指定 JDK 目录：

```powershell
.\build-win7.cmd 'C:\Tools\LibericaJDK21'
```

底层打包入口也可直接执行：

```powershell
pwsh.exe -NoLogo -NoProfile -File .\build.ps1 -Windows7 -JdkHome 'C:\Tools\LibericaJDK21'
```

兼容模式使用 BellSoft Liberica 完整 JDK 21（Windows x64）构建并运行后端，发布包包含同一套 JDK、编译器和许可证。后端字节码目标仍为 Java 17，补丁编译按原始 class 版本指定 `--release`，可处理 Java 8、11、17 原包。桌面端使用最后支持 Win7 的 Electron 22.3.27，编辑器构建目标为 Chromium 108；Node 16 缺少原生 `fetch` 时使用固定版本的 `node-fetch` 2。标准构建继续使用原有 Electron 版本。

产物独立写入 `release/windows7/`，文件名含 `Windows7`，不覆盖 `release/windows/`。构建需要在支持当前 Node.js/npm 工具链的较新 Windows 上执行，内网目标电脑无需安装 Node.js、Maven 或 JDK。目标最低为 **Windows 7 SP1 x64**，不支持未安装 SP1 的 Win7 或 32 位系统。

[Electron 22 已停止维护](https://releases.electronjs.org/release/v22.3.27)，该包用于确有旧系统约束的环境；[Liberica 平台要求](https://bell-sw.com/pages/supported-configurations/)列出 Win7 SP1。构建主机上的启动、编译验收不能代替真实 Win7 验收，发布清单明确标记 `targetOsVerified: false`。发布包附带 [Eclipse JDT LS 1.55.0](https://github.com/eclipse-jdtls/eclipse.jdt.ls/tree/v1.55.0) 并共用内置 JDK 21。构建机首次打包会下载 Eclipse 官方发布包并校验 SHA-256，后续可复用缓存。基础编辑、补丁编译和 Java 代码智能可离线使用。

### 构建 Win7 SP1 x64 原生后端包

在较新的 Windows 构建机上双击 `build-win7-native.cmd`。脚本会探测 Visual Studio 2022 的 MSVC x64 和 Windows SDK。默认从 `%SystemDrive%\Tools\LibericaJDK21` 和 `%SystemDrive%\Tools\LibericaNIK17` 读取工具链，并兼容已有的 `%TEMP%\jarpatch-win7-dependencies\jdk-21` 和 `nik-17`。这两套工具链是构建必需依赖，不应作为临时垃圾清理。也可分别通过 `JARPATCH_WIN7_JDK_HOME` 和 `NIK_HOME`（或 `GRAALVM_HOME`）指定工具链，或直接传参：

```powershell
.\build-win7-native.cmd 'C:\Tools\LibericaJDK21' 'C:\Tools\LibericaNIK17'
```

`release/windows7-native/` 下的 EXE 与快启 ZIP 使用 Electron 22.3.27 和内置 JDK 21、JDT LS。后端由基于 JDK 17 的 Liberica NIK 23.0 编译，复用 Win7 版 Liberica JDK 21 随附的 VC 和 Universal CRT 运行库。该包面向 Win7 SP1 x64，构建清单中的 `targetOsVerified` 反映是否已完成目标系统验收。原 `build-win7.cmd` 继续构建 JVM 兼容包。

### 启动失败诊断

Windows 发布目录同时提供小型原生 `JarPatch-Startup-Diagnostics.exe`。将它和同一发布目录的应用 EXE 放在一起，双击诊断器；应用退出或启动失败后，会自动打开诊断报告和日志目录。诊断器不依赖目标机器的 Java、Node.js、.NET 或 PowerShell，不安装软件、不修改系统运行库。

诊断器先检查目标系统与应用文件，再记录原始进程退出码及常见 DLL 缺失、入口点错误、位数错误等说明；启动报告与 Chromium 日志保存在 `%LOCALAPPDATA%\JarPatch Studio\startup-diagnostics\本次诊断目录`。新版应用另写 `desktop-startup.log`，包含内置 Java 路径、编译器是否存在、启动步骤和后端失败输出；敏感鉴权令牌不会写入启动日志。普通启动的桌面日志位于用户数据目录的 `logs/startup.log`，超过 1 MiB 后保留一个轮换备份。

旧包也可由同版本文件名对应的诊断器收集系统退出码和 Chromium 日志，但不会生成新版桌面日志。`/SMOKE /NOOPEN` 参数用于自动检查启动且不打开主窗口或报告；原始进程返回 0 不能单独证明界面启动成功，需结合 `READY` 或 `STARTUP_FAILED` 记录判断。系统在诊断器自身启动前拦截程序时，仍需查看 Windows 系统提示或应用程序事件记录。

标准 Windows 包写入 `release/windows/`，并生成包含环境、Git commit、构建前源码是否干净、构建入口及其 SHA-256、签名状态、产物大小和 SHA-256 的 `release-manifest.json`。三个平台入口都会对解包应用执行 `--smoke-check`，确认内置 Java 和后端健康检查可用。正式签名所需环境变量、macOS 公证、Linux GPG 和回滚步骤见 `docs/release-runbook.md`；macOS/Linux 必须在对应原生系统执行 `./build-macos.sh` 或 `./build-linux.sh`。

## 使用流程

顶部工具栏的「帮助」可以离线打开图文手册。手册按「入门与账号 → 本地改包流程 → 运行时工作台 → 配置、维护与排错」四组场景组织，内置工作台示意图、快捷键与角色权限速查表、目录关键词检索和滚动阅读高亮。

1. 打开 JarPatch Studio。
2. 点击右上角“开发能力”：
   - 编译 JDK 沿用原有配置。
   - 如需 Java 代码智能，在 Win7 免安装包里勾选启用并保存，两项路径留空即使用内置 JDT LS 和 JDK 21。需要覆盖内置组件时可手动填写对应路径。其他版本请自行配置 JDT LS 和运行它的 JDK。
   - 如需 AI 能力，选择协议并填写端点和模型。HTTP 支持本机回环地址和私有 IPv4 地址（10/8、172.16/12、192.168/16），其他地址须使用 HTTPS 并配置 API Key。HTTP 请求明文传输，保存的 API Key 使用系统安全存储加密。
   - 所有协议均可填写完整接口地址，或填写以 `/v1` 或 `/v1/` 结尾的基础地址。选择“Anthropic 兼容 Messages API”时，例如填写 `http://192.168.1.10:8000/v1/`，实际请求地址为 `http://192.168.1.10:8000/v1/messages`。如网关有鉴权要求，按需选择 `x-api-key` 或 `Authorization Bearer` 并填入密钥。本机或私有 IPv4 无鉴权服务可留空密钥。如已保存旧密钥，勾选“清除已保存的 API Key”。
   - Messages 协议使用基础请求字段，通过提示要求模型返回 JSON，客户端在展示结果前校验 JSON 字段和操作类型。
3. 点击“打开 Jar/War”。
4. 选择本地 Jar 或 War。
5. 等待后端预解析 `pom.xml` 和嵌套 Jar 候选项。
6. 在弹窗中勾选本次需要反编译的嵌套 Jar。
7. 等待后端解压和反编译。
   - 导入、分析、编译和导出都会先创建任务，再连接任务日志 WebSocket；执行过程中右侧任务状态栏会显示进度，点击“取消任务”可以停止当前长任务。
8. 在左侧文件树中打开 `sources` 下的 Java 文件，或 `extracted` 下的资源文件。
   - Spring Boot 多模块包中，`BOOT-INF/classes` 的源码仍直接显示在 `sources/com/...`。
   - 被用户勾选的 `BOOT-INF/lib/*.jar` 或 `WEB-INF/lib/*.jar` 会显示在 `sources/nested-jars/原Jar相对路径/...`，例如 `sources/nested-jars/BOOT-INF/lib/AIQuality-module-qcs.jar/com/...`。
9. 修改并保存。
   - Java 智能启用后，可使用补全、错误波浪线、悬停、定义跳转和灯泡快速修复；“整理 import”会调用 JDT LS 的 Java 语义操作。
   - “AI 补全”只在点击按钮或按 `Alt+\\` 时请求一次；“AI 助手”可解释、修复或重构当前选区/文件，代码结果必须在预览后点击应用。
   - 无 BOM 的旧编码文件先在“项目设置”选择默认编码，也可在编辑器工具栏为当前文件明确切换编码；系统不会自动猜测后直接覆盖。
   - 遇到 `log.info("\u7535\u5b50...")` 这类内容时，可点击编辑器右上角“中文预览”；查看后点击“返回原文”继续编辑，预览不会修改源文件。
   - `Ctrl+S`/`Cmd+S` 可保存；切换文件、切换项目或关闭窗口时，未保存内容必须先确认。
10. 点击“分析”，确认风险。
   - “分析与风险”区域默认收起；点击右侧“分析与风险”按钮可以展开，查看包结构、依赖数量、修改文件数量和风险项。
   - 查看完成后点击“收起”，编辑器会重新占用主要空间，方便继续改代码。
11. 如修改了 Java 文件，点击“编译”。
    - 系统不会按业务特征改写源码；编译错误会原样显示，由用户修正明确源码或依赖选择。
    - 系统从对应原始 class 识别目标版本，严格使用匹配的 javac/`--release`。
12. 打开“差异”，确认源码、资源和 class 清单。
13. 点击“导出”，选择输出路径和明确签名策略。
14. 导出完成后查看结构校验结果；校验失败时目标文件不会发布。
15. 每次操作后先看右上角即时提示；任务详情和持久化日志用于追溯完整顺序。

## 常见问题

### 为什么修改后签名可能失效？

Jar 或 War 中如果存在 `META-INF/*.SF`、`*.RSA`、`*.DSA`、`*.EC`，修改 class 或资源后，原签名通常不再可信。未修改包可以保留签名；已修改包默认阻止导出，用户只能取消或明确移除失效签名后导出未签名包，再按组织流程重新签名。

### 为什么反编译后的 Java 不一定能编译？

反编译是从 class 还原源码，不等同于原始源码。混淆、泛型擦除、内部类、编译器差异都可能导致还原源码无法直接重新编译。

系统不会根据注解、方法名或业务调用模式改写反编译源码。入口仍是 `POST /api/projects/{id}/compile`，实际执行点是经过版本校验的本机 `javac`；编译产物先进入独立 staging，全部目标成功后才统一写回。

### 为什么编译时会提示找不到 JDK？

编译服务会优先读取右上角“配置”里保存的 JDK 安装目录；如果没有保存配置，就按当前 Java 运行时、`JAVA_HOME`、系统 `PATH` 的顺序自动检测。

如果保存过的 JDK 路径后来被移动或删除，设置页会直接提示已保存配置不可用，这时需要重新选择 JDK 安装目录再保存。

### 为什么导入时出现 CFR 的 NoClassDefFoundError？

如果后端运行期间又执行 `mvn package`，Windows 可能允许 Maven 把 `backend/target/jarpatch-studio-backend.jar` 覆盖成普通 jar，但因为旧后端进程占用文件，Spring Boot 重新打可执行包时无法把 jar 改名为 `.original`，最终留下一个不完整 jar。正在运行的后端后续调用 CFR 时，就可能报 `org/benf/cfr/...` 内部类找不到。

当前一键启动脚本会在 Electron 启动前检查 `BOOT-INF/classes/com/jarpatch/JarPatchStudioApplication.class` 和 `BOOT-INF/lib/cfr-0.152.jar`。检查失败时会精确停止引用当前后端 Jar 的旧 Java 进程，重新执行 Maven 构建并再次校验；复验仍失败时停止启动并显示错误。

### Windows 编译时为什么可能出现 CreateProcess error=206？

Windows 对进程启动命令长度有限制。Spring Boot Jar 的 `BOOT-INF/lib` 依赖较多时，如果把所有依赖 Jar 都直接拼到 `javac -classpath` 命令行里，就可能在启动 `javac` 前失败，并提示“文件名或扩展名太长”。

当前编译服务已经改为使用 `javac @参数文件` 机制：入口仍然是 `POST /api/projects/{id}/compile`，实际执行点仍然是本机 `javac`，只是把超长 classpath、输出目录和修改过的 Java 文件路径写入工作区 `compiled/javac-arguments.txt`，再由 `javac` 读取该参数文件完成编译。编译成功后，class 文件仍会从 `compiled` 复制回 `extracted`，导出流程不变。

### 为什么 Spring Boot Jar 要特殊处理？

Spring Boot 可执行 Jar 中的 `BOOT-INF/lib/*.jar` 通常需要保持未压缩存储方式，否则启动器可能无法正确读取嵌套依赖。

### 为什么多模块 Spring Boot Jar 以前只看到一点源码？

多模块项目最终打包成 Spring Boot Jar 后，入口模块 class 通常在 `BOOT-INF/classes`，其他业务模块会以独立 Jar 放在 `BOOT-INF/lib`。如果只反编译 `BOOT-INF/classes`，界面就只能看到启动类、少量 Controller 或 Runner，看不到 `AIQuality-module-system`、`AIQuality-module-qcs` 这类模块。

当前流程会先读取包内 `pom.xml` 和嵌套 Jar，弹窗列出候选 Jar，并基于 `pom.xml` 模块和应用包名前缀给出推荐勾选。用户确认后，只有被勾选的嵌套 Jar 会被反编译到 `sources/nested-jars/原Jar相对路径/...`。如果修改这些源码后点击“编译”，编译结果会写回对应的原嵌套 Jar；主工程源码仍写回 `BOOT-INF/classes`。

### 为什么导入时会提示 module-info 不是普通 class？

Java 9 之后部分依赖 Jar 会包含 `module-info.class`，它是模块描述文件，不是业务 Java 类。当前导入流程会在主 class 和嵌套 Jar 反编译时跳过 `module-info.class`，只把普通业务 class 交给 CFR 反编译，避免出现 `ACC_MODULE` 异常。

### SQLite 数据在哪里？

默认路径：

```text
用户目录/.jarpatch-studio/jarpatch-studio.db
```

### 为什么按钮点击后还要保留底部日志？

右上角提示用于告诉用户当前这一次操作是否完成、失败或取消；底部执行日志用于追溯完整操作顺序，方便后续排查“哪一步开始、哪一步完成、哪一步失败”。

## 2026-08-10 六阶段完成记录

项目已按照现状分析文档的六个阶段完成发布级收口，完整分析见 [JarPatch Studio 项目现状分析与补充建议](docs/2026-08-10-jarpatch-studio-project-analysis.md)，持续发布门禁见 [发布检查清单](docs/release-checklist.md)。

- 第一阶段：仅本机监听、令牌/实例握手、禁止覆盖原包、导入状态标记、编译回滚和导出原子发布完成。
- 第二阶段：class 目标版本识别、javac 版本验证、严格 `--release`、移除业务源码改写、字节保真保存完成。
- 第三阶段：固定基线、差异视图、签名门禁、导出结构校验和失败不发布完成。
- 第四阶段：顺序迁移、外键索引、任务条件更新、任务/工作区崩溃恢复、持久化日志和健康检查完成。
- 第五阶段：工作区预览确认清理、项目设置、错误向导、Windows 免安装包和六类真实样本验收完成。
- 第六阶段：macOS/Linux 原生构建入口、流式搜索与归档、依赖锁定和统一发布清单完成。

### 运行态补齐

- 前端已接入带令牌握手的 `/ws/tasks/{taskId}`，实时展示与 SQLite 持久化日志并存。
- 导入、分析、编译和导出已支持任务状态、取消和重启后遗留任务恢复。
- 二进制、签名和超过项目限制的文件均只读并显示明确原因。

### 配置与排查

- JDK 路径配置页面已经完成，保存时会校验 `bin/javac` 可用性并写入 SQLite。
- 项目级设置已覆盖目标 Java 版本、默认导出目录、嵌套 Jar、可编辑文件大小和界面偏好。
- 错误向导已覆盖 JDK、CFR、编译、签名、路径和端口问题。
- 差异视图已展示源码、资源、哈希和编译 class 清单。
- 导出后已自动校验 Manifest、布局、资源、class、Spring Boot 嵌套 Jar 和签名策略。

### 清理与交付

- “只清理本地工作区”已采用预览、一次性确认和运行任务门禁，与删除历史完全分离。
- Windows、macOS、Linux 均有稳定启动/构建入口；各平台发布脚本生成统一 SHA-256 清单。
- Windows 免安装包已实际构建；macOS/Linux 产物必须在对应原生系统执行脚本后才能标记为已验证。
- 真实样本覆盖 Java 8 普通 JAR、Java 17 Spring Boot JAR、WAR、签名包、多版本 JAR 和混淆包。

## 2026-05-16 页面交互优化记录

- 已将页面视觉调整为更接近桌面 IDE 的浅色工作台风格，保留左侧项目历史、中间文件树和代码编辑器、底部执行日志。
- 已将“分析与风险”改为默认收起，只有用户点击右侧入口或完成分析后才展开，优先保证代码编辑区域宽度。
- 已为左侧项目历史增加删除入口，删除后会刷新历史列表；如果删除的是当前打开项目，主工作区会回到未打开状态。
- 已将项目历史和执行日志时间统一为中国时区展示。
- 已移除 Electron 默认菜单栏中的 `File / Edit / View / Window / Help`。
- 已将中间工作区固定在窗口剩余高度内滚动，文件树宽度从窄栏调整为更适合查看多层 Jar 路径的宽度。
- 已将文件树内容改为独立固定高度滚动区，横向滚动条保持在文件树可视区域底部，避免查看深层目录时反复滚到整棵树底部。
- 已修复代码编辑器顶部文件路径过长时挤压“保存”按钮，导致“保存”文字换行的问题；文件路径改为在标题区完整换行展示。
- 已为编译和导出按钮增加执行中状态，执行期间禁用顶部操作按钮，完成或失败后仍通过右上角提示和执行日志反馈结果。
- 已优化文件树渲染方式，折叠的目录不再提前递归生成子节点 DOM，减少大 Jar、多嵌套目录首次加载和展开时的卡顿。
- 已为文件树和代码区之间增加拖拽分隔条，用户可以按当前 Jar 的目录深度调整文件树宽度，宽度会保存到本地浏览器存储。
- 已修复目录展开/收起时重新请求文件树导致闪烁的问题；现在只在切换项目和刷新树时请求后端，普通目录点击使用前端缓存树数据本地重绘。
- 已压缩文件树节点行距、按钮内边距和层级缩进，让深层包路径一屏能展示更多内容。
- 后续如果继续增加风险详情，不建议直接固定展示在主界面，应继续放在可展开侧栏内，避免影响代码查看和编辑。

## 2026-05-16 多模块 Spring Boot 反编译修复记录

- 已修复 Spring Boot 多模块 Jar 只反编译 `BOOT-INF/classes` 的问题。
- 新增 `sources/nested-jars/` 源码目录，用于展示 `BOOT-INF/lib`、`WEB-INF/lib` 等嵌套 Jar 中属于当前应用包名前缀的反编译源码。
- 已调整编译写回规则：主源码写回主 classes 目录，嵌套 Jar 源码写回对应的原 Jar 文件，避免模块 class 被写错位置。
- 已新增导入前预解析流程：先解析 `pom.xml` 和嵌套 Jar 候选项，再由用户手动选择需要反编译的 Jar。

## 2026-08-10 源码保真调整记录

- 已移除重复注解删除、特定方法 `(Object)` 强转删除等业务特定源码改写。
- 保存与编译入口不会自动修改用户未提交的源码；编码、BOM、换行和原始哈希作为明确合同处理。
- 反编译源码本身不能编译时，系统展示原始 javac 错误，不通过局部规则猜测修复。

## 2026-05-23 当前功能梳理

### 已经完成的功能

- 已完成 Electron 桌面壳和 Java 17 Spring Boot 后端基础结构，前端通过本地 HTTP API 访问后端。
- 已完成普通 Jar、Spring Boot Jar、War 的导入前预解析，可以读取包类型、`pom.xml` 模块和嵌套 Jar 候选项。
- 已完成导入流程：复制原始包、解压到工作区、识别包类型、反编译主 classes 和用户勾选的嵌套 Jar，并写入 SQLite 项目历史。
- 已完成 CFR 反编译接入，并跳过 `module-info.class`，避免 Java 模块描述文件导致导入失败。
- 已完成 `sources` 和 `extracted` 两类文件树展示，支持目录展开、前端缓存树结构和文件树宽度拖拽。
- 已完成 Java 源码和文本资源文件读取、编辑、保存，保存后会记录到 SQLite 的 `file_changes` 表。
- 已完成项目内搜索，可以按关键词搜索文件名和文件内容，并定位到编辑器行号。
- 已完成 JDK 配置弹窗，支持校验 `bin/javac` 并将 JDK 安装目录保存到 SQLite。
- 已完成结构分析，可以读取 Manifest、入口类、class 数量、依赖 Jar、修改文件、签名风险、多版本目录风险和混淆迹象。
- 已完成 Java 编译入口，使用本机 `javac @参数文件` 执行编译，避免 Windows 超长 classpath 触发 `CreateProcess error=206`。
- 已移除反编译源码的业务特定编译前整理，严格把用户保存的源码交给匹配版本的 javac。
- 已完成主工程 class 写回和嵌套 Jar class 写回，避免多模块 Spring Boot 包的业务模块写错位置。
- 已完成导出 Jar 或 War，Spring Boot Jar 导出时保留可启动包结构，并写入 SQLite 导出记录。
- 已完成项目历史列表和历史删除，删除范围只包含 SQLite 历史、任务、修改记录和导出记录，不删除本地工作区文件。
- 已完成右上角即时提示和底部执行日志，打开、导入、保存、搜索、分析、编译、导出、取消和失败都有可见反馈。
- 已完成任务状态栏、`/ws/tasks/{taskId}` 实时日志和任务取消，导入、分析、编译、导出都会先创建任务再执行。
- 已完成二进制文件和签名文件只读提示，点击后会告诉用户当前不能编辑。
- 已完成中国时区时间展示，项目历史和执行日志按 `Asia/Shanghai` 口径显示。
- 已完成移除 Electron 默认菜单栏，避免展示暂未接入的 `File / Edit / View / Window / Help`。
- 已完成 Windows 一键启动脚本的后端包完整性门禁，应用入口或 CFR 依赖缺失时会停止旧后端、重新构建并复验。
- 已完成 SQLite 初始化和顺序迁移，当前包含 11 张业务表：`projects`、`tasks`、`file_changes`、`export_records`、`app_settings`、`compiled_artifacts`、`export_validations`、`task_logs`、`analysis_reports`、`operation_journals`、`project_settings`，另有 `schema_migrations` 迁移记录表；默认审计人字段使用 `admin`。
- 已完成 WebSocket 后端注册和任务日志广播服务，任务服务已经在导入、分析、编译、导出节点广播日志。

### 交付与验证边界

- 当前 Windows 发布包没有组织 Authenticode 证书，应明确标记为未签名；不能宣称可信发布者签名。
- `release/windows` 中 0.1.0 包已在本轮开发工作区重新构建并通过样本复验，但清单明确记录 `sourceClean: false`；它只能作为开发验证包，必须从干净提交再次构建后才能标记为正式当前版本。
- macOS/Linux 构建脚本和配置已交付，但当前 Windows 主机不能替代对应原生系统的构建、签名和启动验收。
- 混淆、损坏或反编译信息不足的 class 仍可能无法恢复为可编译源码，这是反编译边界，不使用业务特定改写绕过。
- 公开发布前应在 Windows、macOS、Linux 原生主机分别执行发布清单，并记录签名/公证状态。

## 2026-11-20 桌面 IDE 视觉与交互体验全面优化

### 已完成优化

- **统一双主题 IDE 设计体系**：默认采用高对比、低噪音的深色 IDE 工作台配色，并支持右上角一键切换深色/浅色主题（偏好持久化至本地存储）；自定义注册了 `jarpatch-dark` 与 `jarpatch-light` 两套 Monaco 编辑器配色，主编辑器、差异对比编辑器与运行时在线反编译编辑器全部随工作台主题实时联动，彻底消除原先浅色外壳与漆黑编辑器及亮白按钮混搭的视觉割裂。
- **顶部操作栏认知分层**：将原先平铺的 13 个同等样式按钮重构为「核心工作流（分析 / 编译 / 差异 / 导出）」「扩展能力（运行时工作台 / 开发能力）」「更多下拉菜单（当前项目设置、历史、工作区清理；孤立工作区、排错向导、导出诊断）」和「主题切换 / 帮助」四个分组，全部配备统一线性 SVG 图标，彻底移除 emoji 图标并显著减少窄屏换行占地。
- **文件树与搜索交互升级**：新增按节点类型与扩展名区分的 SVG 图标（目录、Java、配置、标记语言、压缩包、签名、二进制）、当前打开文件高亮选中态、一键折叠全部目录按钮，以及搜索栏一键清空按钮。
- **代码编辑器区与欢迎空状态**：重构编辑器顶栏按钮为紧凑 IDE 工具栏风格，新增未保存圆点指示器与醒目保存高亮态；未打开项目或未选择文件时展示清晰的欢迎引导页与快捷键提示；智能状态栏增加状态指示圆点。
- **任务进度与底部执行日志升级**：任务栏升级为带状态灯与百分比进度条的紧凑视图；底部执行日志支持顶部拖拽条自由调整高度、一键折叠/展开、复制日志和清空日志。
- **全弹窗与运行时工作台统一视觉**：彻底清除差异弹窗残留的旧米黄色样式，统一所有弹窗与运行时工作台的表单控件、卡片分组、分段式标签页、右上角关闭按钮、自定义细滚动条及 `Esc` 快捷键关闭能力，同时保持 Chromium 108（Windows 7 兼容包）完全兼容。

### 反思与后续可继续改进方向

- **命令面板与快捷跳转**：后续可为项目文件定位增加 `Ctrl+P` 快速打开文件弹窗，在大型多模块 Spring Boot Jar 中显著提升定位速度。
- **真实 Windows 7 SP1 实机复验**：虽然本次新增代码严格遵守 `Electron 22.3.27` / `Chromium 108` / `Node 16` 兼容边界且未升级任何前端框架依赖，公开发布 Win7 兼容包前仍应在真实 Windows 7 SP1 x64 环境走一遍登录、强制改密、用户管理、编辑与编译全流程。
- **多账号审计增强**：当前 SQLite 迁移 v7 已将操作人落库至 `operation_journals.operator` 及各核心表的 `creator` / `updater`，后续可按需增加登录审计日志表与独立“系统审计”查询视图。

## 2026-11-20 本地登录与用户管理 + 全界面 7 项深度交互优化

设计文档见 [本地登录与用户管理及全界面深度 UI 优化设计方案](docs/2026-11-20-auth-user-management-and-ui-polish-design.md)。

### 已完成内容

1. **本地离线账号体系与两级权限（SQLite 迁移 v7）**：
   - 新增 `users` 与 `user_sessions` 表，为 `operation_journals` 补齐 `operator` 审计列；启动自动预置默认管理员 `admin` / `Admin@123`（PBKDF2-HMAC-SHA256 120,000 次迭代 + 随机盐），首次登录强制改密，改密后自动隐藏默认口令提示。
   - 后端接入 `LocalApiSecurityFilter` 双令牌鉴权（`X-JarPatch-Token` + `X-JarPatch-Session`）、强制改密门禁与 `ADMIN` / `USER` 角色边界校验；WebSocket 同步校验 `session` 参数；Electron 主进程同步会话令牌，保障运行时在线热替换接口畅通。
   - 前端新增全屏登录遮罩、首次强制改密卡片、右上角用户身份菜单、个人改密弹窗以及管理员专属「本地用户管理」弹窗（支持搜索筛选、新增、编辑、重置密码、启用/禁用与删除）。
2. **全界面 7 项深度 UI / 交互优化（100% 兼容 Windows 7 / Chromium 108）**：
   - **内置主题化确认弹窗**：全面替换全部 10 处浏览器原生 `window.confirm`，支持信息/警告/危险三态、结构化详情卡片、孤立目录滚动清单及三语义按钮（如导出签名策略、删除历史同步清理工作区）。
   - **编辑器 IDE 级升级**：新增多文件标签栏（支持未保存橙点与标签关闭确认）、分段路径面包屑与一键复制路径按钮，以及全宽底部 IDE 状态栏（实时展示 JDT LS 状态、光标行列号、语言标识、文件编码与当前登录身份）。
   - **文件树与侧边栏提效**：新增 Java 单子目录链自动折叠（Compact Folders，如 `com.example.service` 开关）、左侧项目历史实时筛选框、项目栏收起态快捷导入按钮，以及搜索结果条数汇总与关键词安全高亮。
   - **开发能力配置分段标签化**：将原先长滚动弹窗重构为「编译 JDK」「Java 代码智能（JDT LS）」「AI 代码助手」三组带状态圆点的分段标签页。
   - **嵌套 Jar 筛选与差异对比提效**：反编译选择弹窗新增实时搜索框与已选计数徽标；差异确认弹窗新增「全部展开 / 全部折叠」及中文彩色变更状态徽标。
   - **项目历史与结构分析升级**：项目历史弹窗升级为三组标签页并展示 `@操作人` 与彩色状态徽标；右侧结构分析升级为结构化风险徽标与可点击直达编辑器的「已修改文件」清单。
   - **运行时工作台连接区可折叠**：顶部新增收起/展开连接配置按钮与单行连接摘要条，连接 Arthas 后自动折叠连接配置区以释放更多诊断与源码空间。

## 2026-11-21 运行时工作台顶部连接区重构（单行工作条 + 可展开配置面板）

设计文档见 [运行时工作台顶部连接区重构设计文档](docs/2026-11-21-runtime-connection-bar-design.md)。

### 已完成优化

- **单行快速工作条**：将原先割裂成 4 行、呈 Z 字形填表的连接区重构为常驻单行工作条，严格按自然动线从左到右排列：`服务器 → 加载容器/加载 Pod → 容器/Pod → Java 进程 → 连接/释放 Arthas`，大幅压缩顶部占用高度并释放日志与反编译源码区视野。
- **高频场景免打扰**：已保存常用 SSH 主机时，打开工作台默认选中首台服务器并收起低频配置面板；切换下拉框仅回填配置、不自动发起远程连接；下拉框底部内置「＋ 新增 / 临时主机…」快捷入口，无保存主机时自动展开配置面板引导填写。
- **配置面板语义重构**：点击右上角「连接配置」展开的配置面板采用 `<fieldset>` 分成「SSH 服务器与认证」（聚合认证方式、主机、端口、账号、密码与保存/移除按钮）与「运行环境与 Arthas 包」（整合 Docker/Kubernetes 切换、离线包与条件展示的 K8s 5 项参数）两块；收起态下标题栏自动以微徽标提示 `K8s`、`OpenSSH`、`离线包` 等非默认配置。
- **Arthas 操作按钮互斥联动**：合并原并排占位的「连接 Arthas / 释放 Arthas」位置，未连接显示主按钮「连接 Arthas」，连接后切换为带绿色状态灯（清理未确认时为警示灯）的「释放 Arthas」。

### 反思与后续可继续改进方向

- **常用主机最近使用排序与备注别名**：当前已保存主机按保存顺序展示为 `user@host:port`，若常用服务器增多（超过 5 台），后续可为每台主机补充可选中文别名（如“测试环境-核心服务”）并记住上次成功连接的主机自动置顶。
- **容器/进程列表一键刷新**：在单行工作条上目前通过重新点击「加载容器」刷新容器清单，后续可结合轻量刷新图标与目标状态提示进一步减少排查时的点击成本。

## 2026-11-21 编辑器「中文预览」只读视图升级与定位修复

### 已完成修复

- **从纯绿 `<textarea>` 升级为独立只读 Monaco Editor**：将原先遗留的原生 `<textarea id="unicodePreview">`（无行号、无语法高亮且全屏纯绿）替换为独立只读 Monaco 实例（`window.jarPatchEditor.createUnicodePreviewView`），完整保留语法高亮、行号、折叠、小地图、`Ctrl+F` 查找及 `jarpatch-dark` / `jarpatch-light` 双主题联动；同时为 JDT LS 语义 Provider 补齐主文档模型校验，避免只读内存模型触发无关 LSP 请求。
- **修复进入预览跳回第 1 行的滚动丢失问题**：改为先显示预览容器、完成 Monaco 布局后再恢复原滚动位置；若当前视口内已包含解码中文则保持原位不动，若当前视口内没有解码内容（如文件顶部全是注释与 `import`），则自动居中定位到首处解码中文，点「返回原文」时同步预览滚动位置回主编辑器。
- **新增解码高亮与上下处导航栏**：在 `decodeChineseUnicodeEscapes` 单次扫描中同步计算连续解码中文的精确 1-based 行列区间，在预览区对解码片段施加柔和绿色下划线高亮及滚动条/小地图标尺标记，并在顶部紧凑状态条展示解码统计（处数、字数、行数）与「上一处 / 下一处」快速跳转按钮；预览期间自动禁用源码修复与 AI 改写按钮，确保只读隔离清晰。

### 反思与后续可继续改进方向

- **历史遗留控件随主框架同步盘点**：本次问题的根源是主编辑器升级到 Monaco 且后续切双主题后，辅助视图 `#unicodePreview` 仍停留在早期 `<textarea>` 时代。后续每次做主编辑器或主题升级时，应同步核查差异视图、AI 预览、运行时反编译和中文预览等全部代码展示承载区，避免出现体验断层。
- **运行时工作台在线反编译同样支持中文预览**：当前「中文预览」位于主工作区编辑器，后续若远程容器在线反编译出的字节码中也频繁出现 `\uXXXX` 中文日志，可复用同一套 `decodeChineseUnicodeEscapes` 与只读预览能力提供一键切换。

## 2026-11-21 离线帮助手册全面重构与阅读体验优化

### 已完成内容

- **按使用场景重排四组 14 个章节**：将原先按早期顶部按钮平铺的帮助手册重构为「入门与账号」「本地改包流程」「运行时工作台」「配置、维护与排错」四个分组，完整补齐此前缺失的 **运行时工作台**（单行工作条、连接配置、Docker/Kubernetes、容器日志、类与方法、在线反编译与热替换、JVM 检查、请求测试、部署包对照、回归验证、性能采集、环境与集成、导出诊断报告）及 **本地登录与角色权限**（默认管理员与首次改密、管理员/普通用户权限对比、用户管理）。
- **与当前界面入口和交互细节 100% 对齐**：同步修正已收纳进右上角「更多」下拉菜单的项目设置、项目历史与校验、工作区清理、孤立工作区、错误排查向导与脱敏诊断入口；补全工作台 8 区域示意图（含底部 IDE 状态栏与深浅色主题切换）、常用快捷键速查表、文件树包路径合并、多文件标签栏、状态栏编码切换、升级版只读 Monaco「中文预览」以及三标签页「开发能力」（含 Win7 内置 JDT LS 与内网免密 AI 接入）。
- **轻量阅读体验增强（100% 兼容 Chromium 108 / Win7）**：在左侧目录栏顶部新增多关键词实时筛选（同时匹配章节正文与别名关键词，自动隐藏空分组并展示匹配数量与空态重置），新增正文滚动与左侧目录激活态联动高亮（`requestAnimationFrame` 节流并处理目录点击与底部短章节边界），并支持点击遮罩关闭弹窗。

### 反思与后续可继续改进方向

- **手册内容随功能迭代同步检查**：本次帮助手册滞后源于多轮大功能（运行时工作台、认证权限、顶部栏重构、编辑器升级）上线时未将 `#helpDialog` 纳入发布检查清单；后续新增一级入口或权限变更时，应同步核对帮助手册对应章节与关键词索引。
- **支持上下文直达定位**：后续可在「开发能力」「运行时工作台」「排错向导」等复杂弹窗标题栏补充轻量 `?` 入口，点击直接打开帮助手册并定位到对应章节。
