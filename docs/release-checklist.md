# JarPatch Studio 发布检查清单

本清单用于每次正式发布。任何一项失败都应停止发布，不得把未校验产物复制到正式发布目录。

## 1. 固定构建环境

- JDK 17 或更高版本，必须同时包含 `java`、`javac` 和 `jlink`。
- Node.js `26.4.0`，版本来源为根目录 `.node-version`。
- npm `11.17.0`，版本来源为根目录 `package.json` 的 `engines` 和 `packageManager`。
- Electron `43.3.0`、electron-builder `26.15.3`，由 `frontend/package-lock.json` 锁定。
- Maven 插件版本必须由根 `pom.xml` 的 `pluginManagement` 锁定，不接受 Maven 默认隐式版本。
- 应用版本只修改根 `package.json`；执行 `node scripts/sync-version.js --check` 必须通过。

## 2. 构建前检查

- 确认源码、脚本和中文文档均为 UTF-8。
- 确认 `*.sh` 为 LF，`*.ps1`/`*.cmd` 为 CRLF，规则由 `.gitattributes` 固定。
- 执行 `npm.cmd audit --omit=dev` 和 `npm.cmd --prefix frontend audit`，记录风险数量。
- 执行 `mvn dependency:tree`，确认没有意外的重复或未锁定直接依赖。
- 确认本地端口 `127.0.0.1:18765` 没有被其他实例占用。

## 3. 各平台构建入口

Windows 必须使用 PowerShell 7：

```powershell
pwsh.exe -NoLogo -NoProfile -File .\build.ps1
```

macOS 必须在 macOS 原生主机执行：

```bash
./build-macos.sh
```

Linux 必须在 Linux 原生主机执行：

```bash
./build-linux.sh
```

脚本会构建后端、执行 `npm ci`、生成当前平台的 jlink 运行时、打包桌面程序，并输出：

- `release/windows/`
- `release/macos/`
- `release/linux/`

每个目录必须包含 `release-manifest.json`，其中登记版本、平台、架构、JDK、Node.js、npm、Git commit、构建前源码是否干净、构建入口及其 SHA-256、签名状态、文件大小和产物 SHA-256。构建脚本必须先让解包应用通过 `--smoke-check`。

## 4. 真实样本验收

Windows 下执行：

```powershell
pwsh.exe -NoLogo -NoProfile -File .\samples\acceptance\build-samples.ps1
pwsh.exe -NoLogo -NoProfile -File .\samples\acceptance\run-acceptance.ps1
```

必须确认：

- 无令牌访问返回 HTTP 401，健康检查实例 ID 与启动参数一致。
- Java 8 普通 JAR、Java 17 Spring Boot JAR、Java 8 WAR 完成导入、编辑、编译、差异、导出和结构校验。
- 签名包、多版本 JAR、混淆包分别识别对应风险。
- 导出文件没有覆盖原包，结构校验失败时目标文件不存在。
- 清理工作区前展示文件数、大小和确认标识，清理后项目历史仍保留。
- 删除历史可明确选择保留或先清理工作区；孤立工作区只能通过扫描、预览和一次性确认入口删除。
- 安全退出后后端端口释放，任务日志和验收结果可以从磁盘回读。

## 5. 签名和发布边界

- 输入业务包：未修改时可以保留原签名；有修改时默认阻止保留失效签名，只有用户明确选择后才移除签名并导出未签名包。
- Windows 桌面包：公开发布使用 `build.ps1 -RequireSigning`，要求 Authenticode 验证为 `Valid`；内部未签名包清单必须为 `NOT_SIGNED`。
- macOS 桌面包：公开发布设置 `JARPATCH_REQUIRE_SIGNING=true`，必须通过 Developer ID、notarytool 和 staple 验证，清单为 `VALID_NOTARIZED`。
- Linux 桌面包：渠道要求签名时设置 `JARPATCH_REQUIRE_SIGNING=true`，每个产物的 detached signature 必须通过 `gpg --verify`，清单为 `VALID_GPG`。

## 6. 发布门禁

- 后端构建成功，JavaScript 和 PowerShell 语法检查成功。
- 当前平台原生发布脚本成功，发布清单可被 JSON 解析。
- `release-manifest.json` 的 `sourceClean` 必须为 `true`，`gitCommit` 必须与验收报告和待发布提交一致，`buildEntrySha256` 必须与实际构建入口复算结果一致。
- `release-manifest.json` 中每个 SHA-256 与文件重新计算结果一致。
- 真实样本验收结果为成功，且后端日志没有未处理异常。
- Windows/macOS/Linux 必须分别在对应原生系统完成验收；不能把脚本存在等同于原生产物已经验证。
- 签名、公证、原生验收和回滚命令按 `docs/release-runbook.md` 执行并归档。
- 发布说明必须记录已验证平台、未验证平台、签名状态、已知限制和回滚方式。
