# 发布验收样本

该目录保存 JarPatch Studio 发布验收所需的最小真实 Java 源码，不包含自动化测试用例。

使用 PowerShell 7 执行 `build-samples.ps1` 后，会在 `output` 目录生成：

- Java 8 普通 Jar；
- Java 17 Spring Boot 标准目录布局 Jar；
- Java 8 War；
- 使用临时随机口令和临时证书签名的 Jar；
- Java 8 基线、Java 17 版本目录的 Multi-Release Jar；
- 包含多个短类名的混淆特征 Jar。

`output` 和中间 `work` 目录均为构建产物，不进入版本库。
