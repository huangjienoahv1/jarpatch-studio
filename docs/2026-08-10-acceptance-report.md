# JarPatch Studio 六阶段交付验收报告

## 验收结论

2026-08-10 已在 Windows x64 完成后端构建、三轮连续真实样本业务验收、正式免安装包构建，并在最终源码和后端包上追加一轮完整验收。全部验收均通过；最终发布包 SHA-256 与清单重新计算结果一致。

macOS/Linux 的稳定启动、原生打包、jlink 运行时和统一清单入口已经交付，但当前主机是 Windows，不能替代对应原生系统的产物构建、签名、公证和启动验收。

## 构建环境

- PowerShell：7.6.4。
- JDK/内置运行时：Java 17.0.12 LTS。
- Maven：3.8.6。
- Node.js：26.4.0。
- npm：11.17.0。
- Electron：43.3.0。
- electron-builder：26.15.3。

## 后端和依赖检查

- Maven 编译 86 个 Java 源文件成功。
- Maven 插件实际版本：clean 3.4.1、resources 3.3.1、compiler 3.11.0、surefire 3.5.2、jar 3.4.2、dependency 3.8.1、Spring Boot 3.3.6。
- 根 npm 审计：0 个漏洞。
- 前端 npm 审计：0 个漏洞。
- JavaScript、PowerShell 脚本语法检查通过。
- 三个 Shell 脚本均为 UTF-8 + LF，`.gitattributes` 固定 `*.sh eol=lf`，并已使用 Git Bash 执行 `bash -n` 通过；实际打包和启动仍留给 macOS/Linux 原生发布主机。

## 连续真实样本验收

最终一轮记录时间：`2026-08-10T23:36:00.3813496+08:00`。为验证 Windows 导入发布状态机，完整验收先连续执行三次；正式发布构建完成后，又使用最终后端包追加执行一次。每次均为 3 个端到端样本和 3 个风险样本通过。

安全与生命周期：

- 无令牌请求返回 HTTP 401。
- 健康检查实例 ID 与启动参数一致。
- 验收结束后后端安全退出，18766 端口释放。

端到端样本：

| 样本 | 目标 Java | 包类型 | 编译产物 | 持久化日志 | 导出校验 |
| --- | ---: | --- | ---: | ---: | --- |
| Java 8 普通 JAR | 8 | `STANDARD_JAR` | 1 | 5 | 通过 |
| Java 17 Spring Boot JAR | 17 | `SPRING_BOOT_JAR` | 1 | 5 | 通过 |
| Java 8 WAR | 8 | `WAR` | 1 | 5 | 通过 |

风险样本：

- 签名包：识别“存在签名文件”。
- 多版本 JAR：识别“存在多版本类目录”。
- 混淆样本：识别“可能存在混淆代码”。
- 错误向导返回 6 类条目。
- 工作区清理预览返回 8 个待清理文件、2390 字节，并确认清理后历史保留。

## Windows 正式发布包

- 文件：`release/windows/JarPatch-Studio-0.1.0-Windows-x64.exe`。
- 大小：151316178 字节。
- SHA-256：`75044a990beb0f7af1f21392ae4ca5e0a5dd4877313d99dbdc7d728019d3eb7e`。
- 发布清单：`release/windows/release-manifest.json`，JSON 回读成功且哈希复算一致。
- 内置后端：存在 `resources/backend/jarpatch-studio-backend.jar`。
- 内置 Java：Java 17.0.12 LTS。
- Authenticode：`NotSigned`。当前产物是未签名内部交付包，不宣称可信发布者签名。

## 已确认的稳定性修复

Windows 对非空目录执行 `ATOMIC_MOVE` 时曾间歇抛出 `AccessDeniedException`。完整日志确认 Java 文件流均已关闭，失败后目录可以立即回滚删除。最终实现不使用重试或非原子复制，而改为固定最终目录加 `.importing`/`.ready` 状态标记：完整文件生成前不写项目记录，只原子发布同目录小标记；启动时清理没有数据库记录的孤儿标记目录。修改后连续三轮完整验收通过。

## 发布边界

- Windows 包已在当前机器实际构建并校验，但未配置组织代码签名证书。
- macOS 必须在原生主机完成 Developer ID 签名和 notarization 后才能公开发布。
- Linux 必须在原生主机完成 AppImage/tar.gz 构建与启动验收；渠道需要签名时再按渠道要求执行。
- 详细门禁以 `docs/release-checklist.md` 为准。
