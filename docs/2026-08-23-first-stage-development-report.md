# JarPatch Studio 第一阶段开发与验收报告

## 1. 本轮结论

本轮已按《JarPatch Studio 当前完成度与进阶优化分析》的第一阶段开始开发，并完成以下可在 Windows 主机闭环的内容：

- 稳定启动入口增加后端 Spring Boot JAR 完整性门禁。
- Windows、macOS、Linux 发布清单增加源码和构建入口溯源字段。
- 使用 JDK 17 重新构建后端和 Windows x64 免安装包。
- 三类端到端样本和三类风险样本重新验收通过。

当前构建发生在未提交的开发工作区，发布清单已如实记录 `sourceClean: false`。因此该产物是本轮开发验证包，不是“干净提交对应的正式当前版本”。

## 2. 源码与构建入口

- Git HEAD：`f188ff855f9666c007d4051fd6cba90fa432f77a`。
- 构建前源码状态：非干净，`sourceClean: false`。
- Windows 构建入口：`build.ps1`。
- 构建入口 SHA-256：`0e531efcef04488dbe84db413a336f94607ce84acd0e5b9fd0a2c4daa135c544`。
- PowerShell：7.6.4。
- JDK：Java 17.0.12 LTS。
- Node.js：26.4.0。
- npm：11.17.0。

构建入口在开始生成构建产物之前读取 Git HEAD 和工作区状态，再将这些值连同入口脚本哈希传给统一发布清单生成器，避免后续产物写入反过来污染“构建前是否干净”的判断。

## 3. 后端包完整性门禁

入口仍为稳定文件 `start-jarpatch-studio.cmd`，实际检查由 `scripts/ensure-backend-package.ps1` 执行。

必须同时存在以下条目：

- `BOOT-INF/classes/com/jarpatch/JarPatchStudioApplication.class`
- `BOOT-INF/lib/cfr-0.152.jar`

文件缺失、JAR 损坏或条目不完整时，脚本只停止命令行中精确引用当前后端 JAR 绝对路径的 `java.exe`/`javaw.exe` 进程，再执行 `mvn.cmd "-Dmaven.test.skip=true" package`。新包通过同一结构复验后才允许继续启动。

验证结果：

- 当前完整后端包检查通过。
- 模拟空 JAR 同时报告缺少应用入口和 CFR，退出码为 1。
- PowerShell 语法解析通过。

## 4. Windows 开发验证包

- 文件：`release/windows/JarPatch-Studio-0.1.0-Windows-x64.exe`。
- 大小：151364729 字节。
- SHA-256：`62989beced89d657e870114b30b1c79c904b3c2b0aa3441aaa52604037f89130`。
- Authenticode：`NotSigned`。
- 发布清单：`release/windows/release-manifest.json`。

清单已完成 JSON 回读，`gitCommit`、`sourceClean`、`buildEntry`、`buildEntrySha256` 均与本轮构建输入一致；产物 SHA-256 复算一致。构建入口强制执行解包应用 `--smoke-check`，本次已成功启动内置后端、匹配实例并安全退出。

## 5. 真实样本验收

验收时间：`2026-08-23T22:54:06.9792222+08:00`。

端到端样本：

| 样本 | 目标 Java | 包类型 | 编译产物 | 任务日志 | 导出校验 |
| --- | ---: | --- | ---: | ---: | --- |
| Java 8 普通 JAR | 8 | `STANDARD_JAR` | 1 | 5 | 通过 |
| Java 17 Spring Boot JAR | 17 | `SPRING_BOOT_JAR` | 1 | 5 | 通过 |
| Java 8 WAR | 8 | `WAR` | 1 | 5 | 通过 |

风险和运行边界：

- 签名包识别“存在签名文件”。
- 多版本 JAR 识别“存在多版本类目录”。
- 混淆样本识别“可能存在混淆代码”。
- 无令牌请求返回 HTTP 401，健康检查实例 ID 匹配。
- 错误向导返回 6 类条目。
- 工作区清理预览返回 8 个文件、2390 字节，清理后历史保留。

## 6. 未完成门禁

- 当前改动尚未形成干净 Git 提交，不能把开发验证包标记为正式当前版本。
- 本轮未执行 Electron 页面手工测试，按项目约定由用户自行验证前端界面。
- Windows 没有组织 Authenticode 证书，当前包仅能作为未签名内部开发验证包。
- macOS/Linux 仍需在对应原生主机完成构建、签名、公证和启动验收。

正式交付时应从干净提交再次执行构建与样本验收，使验收报告中的 commit、`sourceClean: true`、构建入口 SHA-256、发布包 SHA-256 和签名状态完全一致。
