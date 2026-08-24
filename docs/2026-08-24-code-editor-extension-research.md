# JarPatch Studio 代码编辑能力扩展研究

## 目标

在不改变现有文件读取、编码保真、并发覆盖保护和保存接口的前提下，为代码编辑区补齐两类能力：

- 传统代码智能：补全、错误提示、悬停、跳转、快速修复、格式化和自动整理 import。
- AI 辅助编辑：按用户明确指令完成补全、解释、修复和重构，任何修改都先预览再应用。

## GitHub 方案比较

| 方案 | 可提供能力 | 结论 |
| --- | --- | --- |
| [Monaco Editor](https://github.com/microsoft/monaco-editor) | VS Code 编辑器核心、模型、诊断标记、补全/悬停/代码操作接口 | 采用，替换原文本框并保留现有读写契约 |
| [Eclipse JDT Language Server](https://github.com/eclipse-jdtls/eclipse.jdt.ls) | Java 语义补全、实时诊断、代码操作、整理 import、跳转和格式化 | 采用，作为传统 Java 智能的权威来源 |
| [monaco-languageclient](https://github.com/TypeFox/monaco-languageclient) | Monaco 与 LSP 的通用客户端及 Java 示例 | 参考；本项目只需要受控的单一 JDT LS 通道，因此实现了边界更小的专用客户端 |
| [Monacopilot](https://github.com/Arshad-Yaseen/monacopilot) | Monaco AI 补全组件 | 参考其显式补全交互，不直接引入服务端和框架依赖 |
| [Tabby](https://github.com/TabbyML/tabby) | 可自托管 AI 代码助手和 OpenAPI 服务 | 保留为可配置兼容端点选择，当前不把模型运行时打进桌面包 |
| [Continue](https://github.com/continuedev/continue) | 聊天、编辑和补全的完整 IDE 助手形态 | 参考交互分层；其 IDE 扩展架构不直接嵌入本应用 |

Monaco 官方说明，VS Code 扩展不能直接在 Monaco 浏览器环境中运行，因此不能只安装一个 VS Code Java 扩展解决问题。JDT LS 当前发行版要求 Java 21+ 运行时；它只用于启动语言服务器，JarPatch Studio 后端与目标源码编译仍按原有 Java 17/项目目标版本规则运行。

## 已实现架构

### 传统能力

1. `editor-bridge.js` 用 Monaco 模型承载文件内容，并继续向原页面暴露 `value`、`disabled`、选区和输入事件等兼容接口。
2. Electron 主进程只允许启动配置目录中的 Eclipse JDT LS，并验证平台配置、唯一 Equinox launcher 和 Java 21+。
3. 渲染进程通过受控 IPC 传输 `Content-Length` JSON-RPC，不获得任意进程启动能力。
4. 项目打开后，JDT LS 以 `sources` 为源码目录、`compiled` 为输出目录、`extracted/**/*.jar` 为引用库建立项目上下文。
5. LSP 的诊断映射为 Monaco 错误标记；补全的 `additionalTextEdits` 会同时应用，因此可补写 import；快速修复和“整理 import”使用 JDT LS 的代码操作。

### AI 能力

1. 仅支持用户显式点击或快捷键触发，不在输入时自动上传源码。
2. 支持 OpenAI Responses API 和兼容 Chat Completions 的端点；使用严格 JSON Schema 返回 `explanation`、`replacement` 或 `completion`。
3. 解释结果只读；修复和重构结果先显示差异范围对应的预览，再由用户点击应用。
4. 请求大小严格限制，不隐式截断代码；范围过大时要求用户选择更小代码段。
5. 远程端点必须使用 HTTPS，本机回环服务可使用 HTTP；远程端点必须配置 API Key。
6. API Key 由 Electron `safeStorage` 加密保存，只在主进程发请求时解密，不暴露给页面。
7. OpenAI 请求关闭服务端存储，并发送不可逆的本机稳定安全标识；模型输出必须通过动作类型和字段契约校验后才能展示或应用。

## 配置与使用

1. 打开右上角“开发能力”。
2. 传统能力：下载并解压 JDT LS，填写“JDT LS 目录”和“JDT LS 运行 JDK（Java 21+）”，启用后保存。
3. AI 能力：选择协议，填写完整 API 地址、模型名称和 API Key，启用后保存。
4. 打开项目和 Java 文件：传统能力会在编辑器底部显示连接状态。
5. 使用“快速修复”“整理 import”“AI 补全”或“AI 助手”。

OpenAI Responses 端点示例为 `https://api.openai.com/v1/responses`。API Key 不应写入 README、脚本、源码或截图。

## 边界

- JDT LS 发行包和 Java 21+ 运行时由用户配置，不随当前安装包捆绑；未配置时 Monaco 基础编辑仍可正常使用。
- Java 语义结果取决于反编译源码完整性和已提取依赖；缺失依赖会作为真实诊断展示，不使用伪补全掩盖。
- AI 是辅助建议来源，不替代 JDT LS 诊断、`javac` 编译和导出校验。
- 当前 AI 通道面向兼容所选协议且支持严格结构化输出的模型端点，不对不兼容响应做代码围栏剥离或猜测性修复。
