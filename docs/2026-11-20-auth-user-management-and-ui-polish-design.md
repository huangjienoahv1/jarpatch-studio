# JarPatch Studio 登录与用户管理及全界面 UI 深度优化设计文档

- 日期：2026-11-20
- 作者：黄杰
- 兼容目标：标准 Electron（Chromium 138）与 Windows 7 SP1 x64 兼容包（Electron 22.3.27 / Chromium 108 / Node 16），不引入新前端框架或高版本不兼容依赖。

## 一、本机登录与用户管理架构

### 1.1 数据模型与 SQLite 顺序迁移（Version 7：`user_auth_management`）
- `users` 表：
  - `id TEXT PRIMARY KEY`
  - `username TEXT NOT NULL UNIQUE`（3-32 位字母/数字/点/下划线/连字符）
  - `display_name TEXT NOT NULL`
  - `role TEXT NOT NULL`（`ADMIN` / `USER`）
  - `status TEXT NOT NULL`（`ENABLED` / `DISABLED`）
  - `password_hash TEXT NOT NULL`（PBKDF2WithHmacSHA256，120000 次迭代，256 位输出）
  - `password_salt TEXT NOT NULL`（16 字节随机盐 Base64）
  - `must_change_password INTEGER NOT NULL DEFAULT 0`
  - `last_login_at TEXT`
  - `creator TEXT NOT NULL DEFAULT 'admin'`、`updater TEXT NOT NULL DEFAULT 'admin'`、`created_at TEXT NOT NULL`、`updated_at TEXT NOT NULL`
- `user_sessions` 表：
  - `token_hash TEXT PRIMARY KEY`（随机 32 字节会话令牌的 SHA-256 摘要，库中绝不存明文令牌）
  - `user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - `expires_at TEXT NOT NULL`、`created_at TEXT NOT NULL`、`last_active_at TEXT NOT NULL`
- `operation_journals` 表增列：`operator TEXT NOT NULL DEFAULT 'admin'`。
- 默认管理员预置：若 `users` 表为空，初始化默认管理员 `admin`（显示名 `系统管理员`，初始密码 `Admin@123`，`must_change_password = 1`）。

### 1.2 双令牌鉴权与两级角色权限
- 保留 `X-JarPatch-Token`（Electron 本机实例令牌）校验，并叠加 `X-JarPatch-Session`（用户会话令牌）：
  - 免会话白名单（仍校验 `X-JarPatch-Token`）：`GET /api/system/health`、`POST /api/system/shutdown`、`GET /api/auth/bootstrap`、`POST /api/auth/login`。
  - 强制改密门禁：`mustChangePassword = true` 时，仅允许 `/api/auth/me`、`/api/auth/change-password`、`/api/auth/logout`。
  - 仅管理员（`ADMIN`）可访问：`/api/users/**`、`PUT /api/settings/jdk`、`DELETE /api/projects/{id}`、`GET/DELETE /api/projects/{id}/workspace*`、`GET/DELETE /api/workspaces/orphans*`。
  - 保护边界：禁止禁用/降级/删除自己及最后一个启用管理员；禁用账号或重置密码立即吊销该用户所有活跃会话；连续 5 次密码错误锁定 60 秒。
  - WebSocket `/ws/tasks/{taskId}` 同时校验 `token` 与 `session` 查询参数。

### 1.3 接口清单
- 认证接口 `/api/auth`：`GET /bootstrap`、`POST /login`、`GET /me`、`POST /logout`、`POST /change-password`
- 用户管理接口 `/api/users`：`GET /`、`POST /`、`PUT /{userId}`、`POST /{userId}/reset-password`、`DELETE /{userId}`

## 二、7 项全界面 UI / 交互深度优化

1. **全面替换 8 处原生 `window.confirm`**：独立 `ui-dialogs.js` 提供内置主题化确认弹窗，覆盖未保存确认、导出签名策略三选、工作区/孤立工作区结构化清理卡片、项目历史单次三选删除及运行时离开源码编辑确认。
2. **代码编辑器三大升级**：多文件标签栏（Tabs）、路径面包屑 + 一键复制路径、窗口底部 IDE 状态栏（Java 智能状态灯、光标行列号、语言标识、编码选择、当前登录用户）。
3. **文件树与侧栏升级**：Java 单子目录折叠（Compact Folders，支持开关）、左侧项目历史实时筛选框、侧栏收起态快捷打开图标、搜索结果关键词高亮与计数头。
4. **开发能力配置弹窗 3 标签页重构**：`编译 JDK` / `Java 代码智能` / `AI 代码助手`，带状态灯指示。
5. **嵌套 Jar 弹窗 & 差异确认弹窗升级**：嵌套 Jar 实时搜索筛选与已选计数；差异确认「全部展开/折叠」与中文彩色变更标签。
6. **项目历史弹窗 & 分析侧栏升级**：项目历史 3 标签页切换 + 操作人审计列 + 彩色状态标签；分析侧栏展示已修改文件清单并支持点击直达打开。
7. **运行时工作台连接区一键折叠**：连接成功后可折叠顶部 4 行表单为单行摘要条，最大化日志与源码视野。
