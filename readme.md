# JarPatch Studio

JarPatch Studio 是一个完全开源的跨平台桌面软件，面向普通 Java 开发者，用于在没有源码或源码不完整时，对普通 Jar、Spring Boot Jar、War 包进行结构分析、反编译查看、代码/资源编辑、重新编译和导出新包。

## 当前版本能力

- 打开普通 Jar、Spring Boot Jar、War。
- 解压原始包到本地工作区。
- 导入前解析 `pom.xml` 和嵌套 Jar，用户手动选择需要反编译的 Jar。
- 使用 CFR 反编译主 class 和用户选中的嵌套 Jar 到 `sources` 目录。
- 在桌面界面查看 `sources` 和 `extracted` 两类文件树。
- 编辑 Java 源码和文本资源文件。
- 二进制文件和签名文件只读展示，点击会提示当前不能编辑。
- 通过右上角“配置”打开 JDK 配置弹窗，保存时会校验 `bin/javac` 可用性并写入 SQLite。
- 记录本地项目历史、任务状态、修改文件和导出记录到 SQLite。
- 左侧项目历史支持删除历史记录，删除时只移除 SQLite 中的历史和关联记录，不删除本地工作区文件。
- 分析 Manifest、入口类、Spring Boot 结构、War 结构、依赖 Jar、签名文件、多版本目录和混淆迹象。
- 编译已修改 Java 文件，并把生成的 class 写回解压目录。
- 导出修改后的 Jar 或 War。
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
- 通信：本地 HTTP API，任务日志预留 WebSocket。
- 存储：SQLite。
- 反编译：CFR。
- 工作区：默认位于用户目录下的 `.jarpatch-studio/projects`。

## 目录说明

```text
JavaHot/
  backend/                 Java 17 后端服务
  frontend/                Electron 前端
  docs/                    设计与交付文档
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

### GET `/api/projects/{id}/tree`

读取文件树。

返回：`sources` 和 `extracted` 两个根节点。

### GET `/api/projects/{id}/files/content?path=...`

读取可编辑文件内容。

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
  "content": "文件内容"
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

### POST `/api/tasks/{taskId}/cancel`

取消正在执行的任务。

### WS `/ws/tasks/{taskId}`

任务日志 WebSocket 地址。前端已在导入、分析、编译和导出前先连接这个地址，任务执行中会实时收到进度日志。

## 本地启动

### 一键启动

Windows 下可以直接双击根目录文件：

```text
2026-05-16-start-jarpatch-studio.cmd
```

该脚本会检查 Java、npm、后端 Jar 和 Electron 依赖；后端 Jar 不存在时会先尝试执行 Maven 构建。

脚本不仅检查 `backend/target/jarpatch-studio-backend.jar` 是否存在，还会确认它是包含 `BOOT-INF/lib/cfr-0.152.jar` 的完整 Spring Boot 可执行包。如果之前构建被正在运行的后端进程打断，留下了普通 jar，脚本会重新构建后端，避免运行时反编译器缺少内部类。

### 1. 构建后端

当前工程要求 Java 17 或更高版本编译，目标字节码为 Java 17。

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\jbr'
mvn -DskipTests package
```

如果你的电脑已正确配置 Java 17，可以直接执行：

```powershell
mvn -DskipTests package
```

### 2. 启动桌面端

```powershell
cd D:\code\JavaHot
npm install --prefix frontend
npm start
```

Electron 会尝试启动 `backend/target/jarpatch-studio-backend.jar`。

如果已经安装过前端依赖，后续只需要执行：

```powershell
cd D:\code\JavaHot
npm start
```

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
10. 点击“分析”，确认风险。
   - “分析与风险”区域默认收起；点击右侧“分析与风险”按钮可以展开，查看包结构、依赖数量、修改文件数量和风险项。
   - 查看完成后点击“收起”，编辑器会重新占用主要空间，方便继续改代码。
11. 如修改了 Java 文件，点击“编译”。
    - 编译前会先整理被修改的反编译 Java 源码：删除连续注解序列中完全重复的注解，并删除 `CommonResult.success(...)` 入参最外层多余的 `(Object)` 强转。
    - 该整理只发生在 `sources` 下本次参与编译的 Java 文件上，目的是让反编译产物恢复为 `javac` 可接受的源码形态。
12. 点击“导出”，选择输出路径。
13. 每次操作后先看右上角即时提示；如果任务正在执行，再看任务状态栏和底部“执行日志”，两者会同步显示进度。

## 常见问题

### 为什么修改后签名可能失效？

Jar 或 War 中如果存在 `META-INF/*.SF`、`*.RSA`、`*.DSA`、`*.EC`，修改 class 或资源后，原签名通常不再可信。软件会在分析结果中提示该风险。

### 为什么反编译后的 Java 不一定能编译？

反编译是从 class 还原源码，不等同于原始源码。混淆、泛型擦除、内部类、编译器差异都可能导致还原源码无法直接重新编译。

当前编译入口已经增加反编译源码兼容整理：在执行 `javac` 之前，对本次修改的 Java 文件删除“完全重复的相邻注解”和 `CommonResult.success(...)` 入参最外层多余 `(Object)` 强转。入口仍是 `POST /api/projects/{id}/compile`，实际执行点仍是本机 `javac`，整理结果写回工作区 `sources` 文件后再进入编译。

### 为什么编译时会提示找不到 JDK？

编译服务会优先读取右上角“配置”里保存的 JDK 安装目录；如果没有保存配置，就按当前 Java 运行时、`JAVA_HOME`、系统 `PATH` 的顺序自动检测。

如果保存过的 JDK 路径后来被移动或删除，设置页会直接提示已保存配置不可用，这时需要重新选择 JDK 安装目录再保存。

### 为什么导入时出现 CFR 的 NoClassDefFoundError？

如果后端运行期间又执行 `mvn package`，Windows 可能允许 Maven 把 `backend/target/jarpatch-studio-backend.jar` 覆盖成普通 jar，但因为旧后端进程占用文件，Spring Boot 重新打可执行包时无法把 jar 改名为 `.original`，最终留下一个不完整 jar。正在运行的后端后续调用 CFR 时，就可能报 `org/benf/cfr/...` 内部类找不到。

处理方式是先关闭旧后端进程，再重新执行 Maven 构建并启动。当前一键启动脚本已增加完整性检查：如果 jar 里没有 `BOOT-INF/lib/cfr-0.152.jar`，会自动重新构建。

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

## 2026-05-23 后续路线图

这部分是已经确认的第二阶段目标，不属于当前第一版已完成能力。
其中实时任务日志、任务取消和二进制/签名文件只读提示已经落地，后续重点转到配置、差异和交付能力。

### 运行态补齐

- 前端正式接入 `/ws/tasks/{taskId}`，把任务日志从 HTTP 同步返回升级为实时展示。
- 增加任务中断和长任务取消，让导入、反编译、编译、导出都能主动停止。
- 对二进制文件、签名文件、超大文件补充更明确的只读提示和风险说明。

### 配置与排查

- JDK 路径配置页面已经完成，保存时会校验 `bin/javac` 可用性并写入 SQLite。
- 增加项目级设置，包括默认导出目录、默认是否勾选推荐嵌套 Jar、文件树宽度以外的界面偏好。
- 增加完整错误排查向导，把 JDK 缺失、CFR 缺类、编译失败、包签名失效、路径过长等常见问题整理成界面可读说明。
- 增加导出前差异对比视图，展示源码差异、资源差异和即将写回的 class 清单。
- 增加导出后自动结构校验，确认 Manifest、Spring Boot 未压缩嵌套 Jar、War 目录和修改文件是否进入目标包。

### 清理与交付

- 增加“只清理本地工作区”的独立安全入口，和删除历史分开处理。
- 补充 Mac/Linux 的一键启动和打包脚本。
- 补充 Windows 安装包或免安装包产物。
- 补充更多真实样本手工验收记录，覆盖普通 Jar、复杂 Spring Boot Jar、War、签名包、多版本 Jar、混淆包。

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

## 2026-05-16 反编译源码编译兼容修复记录

- 已在编译入口增加源码兼容整理，处理 CFR 反编译后可能出现的重复注解和 `CommonResult.success((Object)...)` 泛型推断失败问题。
- 整理范围限定为本次参与编译的 `sources` Java 文件，结果写回源码文件后再生成 `javac @参数文件`。
- 该处理不改业务调用链：入口仍是编译接口，实际执行点仍是本机 `javac`，编译成功后 class 仍写回 `extracted` 或对应嵌套 Jar。

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
- 已完成反编译源码编译前整理，处理重复注解和 `CommonResult.success(...)` 外层多余 `(Object)` 强转。
- 已完成主工程 class 写回和嵌套 Jar class 写回，避免多模块 Spring Boot 包的业务模块写错位置。
- 已完成导出 Jar 或 War，Spring Boot Jar 导出时保留可启动包结构，并写入 SQLite 导出记录。
- 已完成项目历史列表和历史删除，删除范围只包含 SQLite 历史、任务、修改记录和导出记录，不删除本地工作区文件。
- 已完成右上角即时提示和底部执行日志，打开、导入、保存、搜索、分析、编译、导出、取消和失败都有可见反馈。
- 已完成任务状态栏、`/ws/tasks/{taskId}` 实时日志和任务取消，导入、分析、编译、导出都会先创建任务再执行。
- 已完成二进制文件和签名文件只读提示，点击后会告诉用户当前不能编辑。
- 已完成中国时区时间展示，项目历史和执行日志按 `Asia/Shanghai` 口径显示。
- 已完成移除 Electron 默认菜单栏，避免展示暂未接入的 `File / Edit / View / Window / Help`。
- 已完成 Windows 一键启动脚本，启动前会检查后端 Spring Boot 可执行 Jar 是否包含 CFR 依赖，不完整时重新构建。
- 已完成 SQLite 初始化，当前落库表包括 `projects`、`tasks`、`file_changes`、`export_records`，默认审计人字段使用 `admin`。
- 已完成 WebSocket 后端注册和任务日志广播服务，任务服务已经在导入、分析、编译、导出节点广播日志。

### 还需要完善的功能

- 还没有导出前差异对比视图，目前只记录修改文件列表，后续应展示源码差异、资源差异和即将写回的 class 清单。
- 还没有导出后自动结构校验，后续应在导出完成后检查 Manifest、Spring Boot 未压缩嵌套 Jar、War 目录和修改文件是否进入目标包。
- 还没有清理工作区的管理能力，删除历史不会删除工作区文件，后续应增加“只清理本地工作区”的独立安全入口。
- 还没有项目级设置，例如默认导出目录、默认是否勾选推荐嵌套 Jar、文件树宽度以外的界面偏好。
- 还没有完整的错误排查向导，后续可把 JDK 缺失、CFR 缺类、编译失败、包签名失效、路径过长等常见问题做成界面可读说明。
- 还没有 Mac/Linux 的一键启动和打包脚本，当前主要按 Windows 进行启动和验收。
- 还没有桌面安装包产物，当前以源码目录和 Electron 开发方式启动，后续应补充 Windows 安装包或免安装包。
- 还缺少更多真实样本手工验收记录，尤其是普通 Jar、复杂 Spring Boot Jar、War、签名包、多版本 Jar、混淆包。

## Git Commit 信息

```text
feat: 增加 JDK 配置弹窗和校验保存
```
