# JarPatch Studio

JarPatch Studio 是一个完全开源的跨平台桌面软件，面向普通 Java 开发者，用于在没有源码或源码不完整时，对普通 Jar、Spring Boot Jar、War 包进行结构分析、反编译查看、代码/资源编辑、重新编译和导出新包。

## 当前版本能力

- 打开普通 Jar、Spring Boot Jar、War。
- 解压原始包到本地工作区。
- 导入前解析 `pom.xml` 和嵌套 Jar，用户手动选择需要反编译的 Jar。
- 使用 CFR 主 API 分批反编译主 class 和用户选中的嵌套 Jar 到 `sources` 目录。
- 在桌面界面懒加载查看 `sources` 和 `extracted` 两类文件树，展开目录时只读取直接子项。
- 编辑 Java 源码和文本资源文件。
- 编辑器提供“中文预览”按钮，可将 `\uXXXX` 形式的中文 Unicode 转义显示为正常中文；预览使用独立只读区域，不替换、不保存原始文本。
- 保存时保留原始编码、BOM 和换行；无 BOM 旧文本通过项目默认编码或文件级编码显式选择读取，不做自动猜测；内容未改变时不写文件，外部内容变化时阻止覆盖。
- 二进制文件和签名文件只读展示，点击会提示当前不能编辑。
- 通过右上角“配置”打开 JDK 配置弹窗，保存时会校验 `bin/javac` 可用性并写入 SQLite。
- 自动读取原始 class major version，严格选择匹配 JDK 并使用 `--release` 编译。
- 记录本地项目历史、任务状态、持久化任务日志、修改基线、编译产物、分析报告和导出校验结果到 SQLite。
- 项目历史界面可查询分析报告、导出校验和带统一操作 ID 的任务时间线。
- 删除历史时可明确选择“仅删除历史”或“预览清理工作区后再删除”；孤立工作区必须单独扫描、预览和确认，不会后台自动删除。
- 分析 Manifest、入口类、Spring Boot 结构、War 结构、依赖 Jar、签名文件、多版本目录和混淆迹象。
- 提供基于固定导入基线的源码/资源差异和已提交 class 清单。
- 编译先写独立 staging，全部成功后才统一写回，提交失败时恢复备份。
- 导出修改后的 Jar 或 War；先写同目录临时文件，结构校验通过后才原子发布。
- 签名包修改后默认阻止保留失效签名，只有明确选择后才移除签名并导出未签名包。
- 后端仅监听 `127.0.0.1`，桌面端每次启动生成随机令牌和实例 ID，HTTP 与 WebSocket 都必须完成握手。
- 后端迁移、索引、条件状态更新、崩溃恢复、健康检查和安全退出已接入。
- 提供项目设置、错误排查向导、脱敏诊断导出、单实例启动、Windows 免安装包以及 macOS/Linux 原生构建入口。
- 打开、保存、搜索、分析、编译、导出等操作会在右上角显示即时提示，并同步写入底部执行日志；导入、分析、编译、导出会先创建任务、连接 `/ws/tasks/{taskId}`，在任务状态栏里实时显示进度，执行期间可直接取消。
- 项目历史和执行日志使用中国时区时间显示。
- 桌面端已移除默认 `File / Edit / View / Window / Help` 菜单栏，后续需要菜单能力时再按功能补回。
- 主工作区采用代码优先布局，“分析与风险”默认收起为右侧窄按钮，点击后才展开风险详情，避免长期占用代码编辑空间。
- 中间文件树和代码编辑区使用固定工作区高度，内容过长时在区域内部滚动；文件树横向滚动条固定在文件树可视区域底部，代码区文件名会完整换行展示，文件树和代码区之间支持拖拽调整宽度。

## 不做什么

- 不加入 AI 自动改代码。
- 不提供攻击、注入、漏洞利用、内存马等能力。
- 不自动兜底修改业务代码。
- 不承诺所有混淆包、签名包、多版本包都能无损重新编译。

## 技术架构

- 前端：Electron。
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

## 本地启动

### 一键启动

Windows 开发启动可直接双击根目录稳定入口：

```text
start-jarpatch-studio.cmd
```

该脚本强制使用 PowerShell 7，检查 npm、后端 Jar 和 Electron 依赖。`scripts/ensure-backend-package.ps1` 会读取后端 Jar 目录，确认应用入口 class 和 CFR 依赖同时存在；文件缺失、损坏或结构不完整时，只停止命令行中精确引用该 Jar 路径的旧 Java 进程，再执行 Maven 构建并复验。

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

### 构建 Windows 免安装包

```powershell
pwsh.exe -NoLogo -NoProfile -File .\build.ps1
```

产物写入 `release/windows/`，并生成包含环境、Git commit、构建前源码是否干净、构建入口及其 SHA-256、签名状态、产物大小和 SHA-256 的 `release-manifest.json`。三个入口都会对解包应用执行 `--smoke-check`，确认内置 Java 和后端健康检查可用。正式签名所需环境变量、macOS 公证、Linux GPG 和回滚步骤见 `docs/release-runbook.md`；macOS/Linux 必须在对应原生系统执行 `./build-macos.sh` 或 `./build-linux.sh`。

## 使用流程

1. 打开 JarPatch Studio。
2. 如需固定编译环境，先点击右上角“配置”，填写 JDK 安装目录并保存。
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
