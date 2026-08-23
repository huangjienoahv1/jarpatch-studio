# JarPatch Studio 发布运行手册

## 1. 发布原则

根 `package.json` 的 `version` 是版本唯一修改入口。执行 `node scripts/sync-version.js` 后，脚本只把该版本同步到前端 `package.json` 和 Maven `revision`；三个平台构建入口会先执行 `node scripts/sync-version.js --check`，版本不一致时立即停止。

正式发布必须满足：

1. 当前提交已确定，`git status --porcelain --untracked-files=all` 为空。
2. 在目标平台原生主机执行对应构建入口。
3. 解包应用的 `--smoke-check` 返回 `{"product":"JarPatch Studio","status":"READY",...}`。
4. 发布清单中的 `sourceClean` 为 `true`，`gitCommit`、`buildEntrySha256`、`signingStatus` 与实际状态一致。
5. 产物签名、哈希、真实样本验收和回滚材料一起归档。

私钥、证书密码、Apple 凭据和 GPG 口令不得写入源码、脚本、README、发布清单或 SQLite。

## 2. Windows Authenticode

组织证书应安装到构建账号可访问的 Windows 证书存储。只把证书指纹和组织批准的 RFC 3161 时间戳地址通过当前进程环境变量传入：

```powershell
pwsh.exe -NoLogo -NoProfile
$env:JARPATCH_WINDOWS_CERT_THUMBPRINT='组织证书指纹'
$env:JARPATCH_WINDOWS_TIMESTAMP_URL='组织批准的 RFC 3161 时间戳地址'
pwsh.exe -NoLogo -NoProfile -File .\build.ps1 -RequireSigning
```

`build.ps1` 使用 Windows SDK `signtool.exe` 以 SHA-256 签名，然后通过 `Get-AuthenticodeSignature` 要求状态为 `Valid`。没有证书时可省略 `-RequireSigning` 生成内部验证包，此时清单必须记录 `NOT_SIGNED`，不能作为公开可信发布者版本。

归档材料：Windows 版本、架构、EXE、SHA-256、证书主题与指纹、时间戳、清单、验收报告。

## 3. macOS Developer ID 与公证

先在登录钥匙串安装 Developer ID Application 证书，再通过 `notarytool store-credentials` 把 Apple 公证凭据保存为钥匙串 profile。构建时只传证书身份名称和 profile 名称：

```bash
export JARPATCH_REQUIRE_SIGNING=true
export JARPATCH_MAC_SIGNING_IDENTITY='Developer ID Application: 组织名称 (TEAMID)'
export JARPATCH_MAC_NOTARY_PROFILE='jarpatch-notary'
./build-macos.sh
```

脚本依次执行 Electron 原生构建、解包应用启动验收、`codesign --verify --deep --strict`、DMG 公证、staple 和 staple validate。全部通过后清单记录 `VALID_NOTARIZED`。任何一步失败都不得复制为正式发布材料。

归档材料：macOS 版本、CPU 架构、DMG/ZIP、SHA-256、Developer ID 身份、notary submission 结果、staple 验证、清单、验收报告。

## 4. Linux 原生验收与 GPG

在目标发行版和目标 CPU 架构上配置发布专用 GPG 私钥。正式渠道要求签名时执行：

```bash
export JARPATCH_REQUIRE_SIGNING=true
export JARPATCH_LINUX_GPG_KEY_ID='发布密钥 ID'
./build-linux.sh
```

脚本先运行解包应用 `--smoke-check`，再生成 AppImage 和 tar.gz，对复制到 `release/linux/` 的每个产物生成 ASCII detached signature，并立即执行 `gpg --verify`。成功清单记录 `VALID_GPG`。内部未签名构建记录 `NOT_SIGNED`。

归档材料：发行版和版本、内核、CPU 架构、AppImage/tar.gz、SHA-256、`.asc`、GPG 指纹、清单、验收报告。

## 5. 发布验收

每个平台都必须保留以下结果，不能用其他平台结果替代：

- 解包应用 `--smoke-check` 成功，内置后端启动、健康检查和安全退出均完成。
- Java 8 普通 JAR、Java 17 Spring Boot JAR、Java 8 WAR 的导入、编辑、编译、差异、导出通过。
- 签名包、多版本 JAR、混淆包的风险识别通过。
- 无令牌 HTTP 401、工作区清理边界、发布清单哈希复算通过。
- 平台签名状态与 `release-manifest.json.signingStatus` 一致。

## 6. 回滚

每次发布保留上一正式版本的完整平台产物、清单、签名文件和验收报告。发现问题时：

1. 停止分发有问题版本，不覆盖或删除其归档证据。
2. 恢复上一版本原始产物及其原始清单和签名；不得重新打包后沿用旧签名或旧哈希。
3. 在目标平台重新复算 SHA-256、验证签名并执行 `--smoke-check`。
4. 发布回滚说明，明确问题版本、恢复版本、平台、哈希、签名状态和已知影响。
5. 用户项目工作区和 SQLite 数据默认保留；只有用户明确执行预览确认清理时才删除工作区。

如果数据库迁移已由新版本执行，回滚前必须先确认旧版本能读取当前 schema。当前迁移只追加列和索引，不删除旧字段；仍应使用用户数据库副本完成一次回滚读取验证后再通知用户降级。
