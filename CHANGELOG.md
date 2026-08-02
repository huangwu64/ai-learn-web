# 更新日志

本文件记录「提示词工程实战训练系统」所有版本的变更。

---

## V3.0 (2026-08-02)

### 🎯 版本目标

1. 分离管理员前端入口（可配置特殊 URL）
2. AI 模型与 API 配置迁移到管理员动态配置
3. 用户界面精简（移除训练模块）

### ✨ 新增功能

#### 管理员门户（全新）

- **可配置入口**：`admin.entry-path`（默认 `/admin`），前端启动时从公开接口 `GET /api/v1/public/admin-entry` 获取并动态注册路由
- **管理员登录**：默认 `admin` / `123456`，账号密码在配置文件（`admin.username` / `admin.password`）中可修改，支持明文或 BCrypt 哈希
- **管理员 Token**：独立 JWT（`role=ADMIN`），与用户 Token 隔离存储（前端 `admin_token`）

#### 动态 AI 配置

- 新增 `ai_config` 表，管理员在管理界面配置：
  - API 地址 / API Key（界面脱敏，`****后4位`）
  - 模型编码（可自由输入，如 `deepseek-chat` / `deepseek-reasoner` / v4 pro / flash）
  - 模型参数：`max_tokens` / `temperature` / `top_p` / `presence_penalty` / `frequency_penalty`
  - 初始提示词（System Prompt）
- `AiConfigService` 运行时缓存，保存后**即时生效，无需重启**
- 新增连接测试接口 `POST /api/v1/admin/ai-config/test`
- 模型编码下拉框**动态读取可用模型**（`GET /api/v1/admin/ai-config/models`，调用 OpenAI 兼容的 `GET /models`），支持手动刷新按钮，获取失败时兜底常用选项
- `api_key` 为空时自动回退到 `application-dev.yml` 的 `ai.deepseek.api-key`，平滑升级

#### 用户端精简

- 移除「训练」模块（`/training` 路由、导航、`TrainingView.vue`），训练能力合并入管理员初始提示词配置
- 用户端仅保留「对话」与「个人中心」

### 🔧 技术变更

- **JWT 角色体系**：Access Token 增加 `role` 载荷（USER / ADMIN），旧 Token 兼容（无 role 默认 USER）
- **安全配置**：`/api/v1/admin/**` 需 `ROLE_ADMIN`；`/api/v1/public/**`、`/api/v1/admin/auth/login` 放行
- **AI 抽象层改造**：`DeepSeekProvider` 改从 `AiConfigService` 读取动态配置；`AiRequest` 增加 `topP` / `presencePenalty` / `frequencyPenalty`；`AiProviderRegistry` 增加 `getActiveProvider()`
- **数据库迁移**：`V3__admin_and_ai_config.sql`（新建 `ai_config` 表 + 默认行）

### 📁 变更文件

| 文件 | 操作 |
|------|------|
| `src/main/resources/application.yml` | 修改：新增 `admin` 配置段 |
| `src/main/resources/db/migration/V3__admin_and_ai_config.sql` | 新增 |
| `ai/config/AiConfig.java` / `AiConfigMapper.java` / `AiConfigService.java` | 新增 |
| `ai/config/dto/AiConfigUpdateRequest.java` / `AiConfigResponse.java` | 新增 |
| `ai/AiRequest.java` / `AiProviderRegistry.java` | 修改 |
| `ai/deepseek/DeepSeekProvider.java` | 修改：动态配置 |
| `config/AdminConfig.java` / `SecurityConfig.java` | 新增 / 修改 |
| `security/JwtTokenProvider.java` / `JwtAuthenticationFilter.java` / `UserPrincipal.java` | 修改：角色体系 |
| `module/admin/`（AdminController/AdminAuthService/PublicController/AiConfigController + dto） | 新增 |
| `module/message/MessageServiceImpl.java` | 修改：接入动态 AI 配置 |
| `web/src/views/admin/AdminPortalView.vue` | 新增：管理员门户 |
| `web/src/api/admin.ts` / `adminRequest.ts` / `types/admin.ts` | 新增 |
| `web/src/utils/adminAuth.ts` / `adminEntry.ts` | 新增 |
| `web/src/main.ts` / `App.vue` / `router/index.ts` / `AppLayout.vue` | 修改 |
| `web/src/views/TrainingView.vue` | 删除 |

---

## V2.1 (2026-08-02)

### 🐛 Bug 修复

- **修复 Token 过期后界面锁定、不自动跳转登录页**
  - 根因：后端认证失败统一返回 `HTTP 200 + body code=401`（`SecurityConfig.writeJsonResponse` 与 `GlobalExceptionHandler` 均写 200 状态码），而前端 Axios 仅在真实 HTTP 401 时触发刷新/跳转，body 级 401 只弹错误提示
  - 修复：
    - `utils/auth.ts` 新增 `refreshAccessToken()`（全局共享刷新锁，并发 401 只发一次刷新请求）与 `redirectToLogin()`（统一清除登录态并跳转登录页）
    - `api/request.ts` 响应拦截器同时识别 HTTP 401 与 body code=401，统一走「刷新 → 重放原请求 → 失败自动退出」流程；登录接口的 401（用户名/密码错误）仅提示、不触发退出
    - `ChatPanel.vue` SSE 流式请求封装 `streamFetchWithAuth`，检测到认证过期自动刷新并重试一次，仍失败则清除登录态并跳转登录页

### ✨ 新增功能

- **退出登录按钮**
  - 侧边栏底部：由纯图标改为「图标 + 文字」红色退出按钮
  - 个人中心：新增「账号操作」卡片，含退出登录按钮
- **UI 图片资源目录**
  - 新增 `web/src/assets/ui/` 目录（含 README 说明与 `.gitkeep`），为后期用外部图片替换矢量图 UI 预留

### 📁 变更文件

| 文件 | 操作 |
|------|------|
| `web/src/utils/auth.ts` | 修改 |
| `web/src/api/request.ts` | 修改 |
| `web/src/components/chat/ChatPanel.vue` | 修改 |
| `web/src/components/layout/Sidebar.vue` | 修改 |
| `web/src/views/ProfileView.vue` | 修改 |
| `web/src/assets/ui/README.md` | 新增 |
| `web/src/assets/ui/.gitkeep` | 新增 |
| `README.md` | 修改 |
| `CHANGELOG.md` | 修改 |

无后端变更，无数据库变更，无新增依赖。

---

## V2.0 (2026-07-26)

### 🎯 版本目标

1. 完善对话功能（上下文优化、消息操作增强）
2. 引入完整的 JWT 登录体系
3. 暂时注释知识库模块

### ✨ 新增功能

#### 登录模块（全新）

- **用户注册** (`POST /api/v1/auth/register`)：用户名 + 密码注册，BCrypt 加密存储
- **用户登录** (`POST /api/v1/auth/login`)：JWT 双 Token 机制（Access Token 2h + Refresh Token 7d）
- **Token 自动刷新** (`POST /api/v1/auth/refresh`)：Axios 拦截器无感刷新，请求锁防并发
- **退出登录** (`POST /api/v1/auth/logout`)：撤销所有 Refresh Token
- **路由鉴权**：前后端双重守卫，未登录重定向 `/login`

#### 对话功能增强

- **复制消息**：hover 显示 📋 按钮，一键复制 AI 回复到剪贴板
- **重新生成回复**：删除旧回复 → 流式重建，支持 SSE
- **停止生成**：AbortController 打断 fetch，保留已生成内容并标注
- **消息时间展示**：hover 气泡时显示（同天 HH:mm / 不同天 MM-DD HH:mm）
- **清空对话**：顶部菜单 → 确认 → 软删除当前会话所有消息
- **上下文优化**：
  - `ai.context.max-messages` 可配置（默认 20，V1 硬编码 10）
  - `AiRequest` 新增 `systemPrompt` 字段
  - 智能上下文裁剪

#### 会话管理增强

- **会话搜索**：侧边栏顶部搜索框，后端 LIKE 查询，前端高亮匹配
- **重命名优化**：双击标题 → 内联编辑 → 回车/失焦自动保存
- **批量删除**：编辑模式 → 多选 → 一键批量软删除

#### 个人中心

- **信息展示**：用户名 / 昵称 / 头像 / 注册时间 / 最后登录时间
- **编辑信息** (`PATCH /api/v1/users/me`)：修改昵称和头像 URL
- **修改密码** (`PATCH /api/v1/users/me/password`)：旧密码验证 → 更新 → 撤销所有 Token

#### 知识库注释

- 前端路由和导航注释隐藏，代码文件保留不删除
- 注释标注 `[V3 启用]`，便于后续恢复

### 🔧 技术变更

- **新增依赖**：`jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.12.3
- **新增模块**：`security/`（JWT Token Provider、认证过滤器、UserDetailsService）
- **新增模块**：`module/auth/`（认证 Controller + Service）
- **SecurityConfig 改造**：从 V1 全放行模式 → JWT 认证模式，逐路径精确配置
- **数据库迁移**：`V2__add_login_and_model_fields.sql`
  - `user` 表新增 `password`、`status`、`last_login_at` 字段
  - 新建 `refresh_token` 表

### ⚠️ 兼容性

- V1 API 路径和响应格式完全兼容，仅增加 JWT 鉴权
- V1 匿名用户（ID=1）保留，新用户从 ID=2 开始
- V1 历史会话归属 anonymous 用户，登录后不可见（user_id 不匹配）

### 📋 新增 API（7 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/register` | 用户注册 |
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| POST | `/api/v1/auth/logout` | 退出登录 |
| PATCH | `/api/v1/users/me` | 更新用户信息 |
| PATCH | `/api/v1/users/me/password` | 修改密码 |
| GET | `/api/v1/sessions/search` | 搜索会话 |
| POST | `/api/v1/sessions/batch-delete` | 批量删除 |
| POST | `/api/v1/sessions/{id}/messages/{mid}/regenerate/stream` | 重新生成（SSE） |
| DELETE | `/api/v1/sessions/{id}/messages/{mid}` | 删除单条消息 |
| DELETE | `/api/v1/sessions/{id}/messages` | 清空会话消息 |

---

## V1.1 (2026-07-19)

### 🐛 Bug 修复

- **核心修复**：`ChatPanel.vue` watch 添加 `{ immediate: true }`
  - 问题：切换会话后消息历史不加载
  - 根因：`:key` 导致组件重建，watch 检测不到变化
  - 修复后组件挂载时立即执行消息加载
- **防御修复**：`chat.ts` `setActiveSession` 始终清空消息
  - 问题：切换不同会话时旧消息残留
  - 原先仅在 `sessionId === null` 时清空
- **清理**：`Sidebar.vue` 移除冗余 `setMessages` 调用

### 📁 变更文件

| 文件 | 操作 |
|------|------|
| `web/src/components/chat/ChatPanel.vue` | 修改 |
| `web/src/stores/chat.ts` | 修改 |
| `web/src/components/layout/Sidebar.vue` | 修改 |

无后端变更，无数据库变更，无新增依赖。

---

## V1.0 (2026-07-19)

### 🎉 初始版本

首个可用版本，实现核心功能：**AI 网页对话**。

### ✨ 功能

- **创建新对话会话**：生成 UUID 主键，关联默认用户
- **发送消息与 AI 回复**：持久化到 MySQL，携带最近 10 轮上下文
- **SSE 流式响应**：打字机效果逐字渲染，支持 emoji 和中文字符
  - 60 秒无数据超时保护
  - 流式失败自动回退到同步请求
- **对话历史加载**：游标分页，默认 50 条，支持上拉加载更早消息
- **会话列表管理**：侧边栏展示、切换、删除（软删除 + 级联删除消息）

### 🏗️ 架构

- 前后端分离（Spring Boot :8080 + Vue 3 :5173）
- AI 接入抽象层：`AiProvider` 接口 + `AiProviderFactory` 工厂
- DeepSeek Provider 完整实现（同步 + SSE 流式）
- Spring Security 宽松模式（V1 仅格式校验）
- 统一响应格式 `Result<T>` + 全局异常处理

### 📊 数据模型

| 表名 | 说明 |
|------|------|
| `user` | 用户表（V1 仅一条匿名用户记录） |
| `session` | 会话表（UUID 主键 + 软删除） |
| `message` | 消息表（游标分页 + 软删除） |
| `model_provider` | 模型提供商配置 |

### 🐛 V1.0 开发中修复的关键 Bug

1. 前端 SSE 解析器 `data: ` 空格不匹配 → 兼容有无空格两种格式
2. 后端手动拼 JSON → 改用 Jackson `ObjectMapper`
3. 流式调用无 token 统计 → 从最后 chunk 提取 `usage.total_tokens`
4. 异步线程池用 `ForkJoinPool` → 创建专用 `ThreadPoolTaskExecutor`
5. `pnpm-workspace.yaml` 配置错误 → 修复 `allowBuilds` 值
6. 重复消息上下文 → 过滤刚保存的当前用户消息
7. DeepSeek 响应被跳过 → 兼容纯 JSON 和 `data:` 前缀格式
8. MyBatis SQL 日志绕过 Logback → 移除 `StdOutImpl` 配置

### 🔮 预留扩展点

- 用户系统：`user` 表 + `UserController` 骨架
- 多模型切换：`AiProvider` 接口 + 工厂模式
- 训练模块：前端 `/training` 路由 + `TrainingView.vue` 占位
- 知识库：前端 `/knowledge` 路由 + `KnowledgeView.vue` 占位
