# 提示词工程实战训练系统

> 一个以练习驱动的提示词能力训练平台。用户通过与 AI 对话来学习和优化提示词，平台提供即时反馈和技能成长路径。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D)](https://vuejs.org/)
[![JDK](https://img.shields.io/badge/JDK-17-orange)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [版本历程](#版本历程)
- [后续规划](#后续规划)

---

## 项目简介

提示词工程实战训练系统旨在为用户提供一个**练习驱动**的 AI 提示词学习平台。核心理念是：通过与 AI 真实对话来实践和优化提示词技巧，而非被动阅读教程。

### 当前版本：V2.0

- ✅ AI 网页对话（SSE 流式响应，打字机效果）
- ✅ JWT 用户认证体系（注册/登录/Token 管理）
- ✅ 对话功能增强（复制/重新生成/停止/清空）
- ✅ 会话管理增强（搜索/重命名/批量删除）
- ✅ 个人信息管理（编辑资料/修改密码）
- 🔲 训练模块（占位，V3 实现）
- 🚫 知识库（已注释，V3 启用）

---

## 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **前端框架** | Vue 3 + TypeScript | 3.5 / 5.x | 视图层框架 |
| **构建工具** | Vite | 5.x | 前端构建 |
| **UI 组件库** | Element Plus | 2.14 | 界面组件 |
| **状态管理** | Pinia | 2.x | 前端状态管理 |
| **HTTP 客户端** | Axios + Fetch (SSE) | 1.x | API 请求与流式响应 |
| **后端框架** | Spring Boot | 3.2.0 | 应用框架 |
| **安全框架** | Spring Security + JWT | 6.x / 0.12.3 | 认证与授权 |
| **ORM** | MyBatis-Plus | 3.5.5 | 持久层 |
| **数据库** | MySQL | 8.0 | 关系型数据存储 |
| **缓存** | Redis | 7.x | 会话缓存 |
| **API 文档** | Knife4j | 4.3.0 | 接口文档自动生成 |
| **工具库** | Hutool | 5.8.25 | 通用工具 |
| **AI 模型** | DeepSeek (`deepseek-chat`) | — | 默认对话模型 |

---

## 功能特性

### 对话模块

- **多会话管理**：创建、切换、重命名、删除对话会话
- **SSE 流式响应**：打字机效果逐字显示 AI 回复，支持 emoji
- **上下文感知**：携带最近 20 轮对话上下文，可配置窗口大小
- **System Prompt**：支持系统级指令定制 AI 行为
- **消息操作**：
  - 📋 一键复制 AI 回复
  - 🔄 重新生成回复
  - ⏹ 停止生成（保留已生成内容）
  - 🗑 清空对话 / 删除单条消息
- **游标分页**：消息历史按需加载，支持上拉加载更早消息
- **会话搜索**：按标题模糊搜索会话
- **批量删除**：选中多个会话一键删除

### 用户系统

- **注册与登录**：用户名 + 密码注册，BCrypt 加密存储
- **JWT 双 Token 机制**：
  - Access Token（2h）— 访问凭证
  - Refresh Token（7d）— 自动续期，一次性使用防重放
- **Token 自动刷新**：Axios 拦截器无感刷新，请求锁防并发
- **路由鉴权**：前后端双重路由守卫
- **个人信息管理**：编辑昵称/头像、修改密码
- **数据隔离**：用户仅可访问自己的会话和消息

### AI 接入抽象层

- **Provider 接口**：统一的 AI 调用规范（`AiProvider`）
- **多模型架构**：扩展新模型只需实现接口并注册
- **流式回调**：`StreamCallback` 接口支持 SSE 流式推送
- 当前已实现：**DeepSeek Provider**

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                    │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ 对话模块 │  │ 训练模块  │  │ 知识库   │  │ 个人中心  │ │
│  │  ✅ 实现 │  │  占位    │  │ V3启用   │  │  ✅ 实现  │ │
│  └────┬────┘  └──────────┘  └──────────┘  └──────────┘ │
│       │    Vue Router + Pinia + Axios / Fetch (SSE)     │
└───────┼────────────────────────────────────────────────┘
        │  HTTP REST + SSE Stream
┌───────┼────────────────────────────────────────────────┐
│       │          后端 (Spring Boot :8080)                 │
│  ┌────┴─────────────────────────────────────────────┐  │
│  │               JWT 认证过滤器                        │  │
│  │     /auth/** 放行 · 其余 /api/v1/** 需认证          │  │
│  └────┬─────────────────────────────────────────────┘  │
│  ┌────┴──────────┐  ┌──────────┐  ┌──────────────┐     │
│  │   对话服务      │  │  认证服务  │  │  用户服务     │     │
│  │  会话+消息     │  │  注册登录  │  │  个人信息     │     │
│  └────┬──────────┘  └──────────┘  └──────────────┘     │
│  ┌────┴─────────────────────────────────────────────┐  │
│  │              AI 接入抽象层                          │  │
│  │  AiProvider 接口 → DeepSeekProvider ✅             │  │
│  │  扩展：豆包 / GPT / ... （实现接口即可）             │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────┐  ┌──────────┐                           │
│  │  MySQL   │  │  Redis   │                           │
│  └──────────┘  └──────────┘                           │
└─────────────────────────────────────────────────────────┘
```

### 架构分层

| 层级 | 位置 | 职责 |
|------|------|------|
| 前端视图层 | Vue 3 | 页面渲染、用户交互、状态管理 |
| API 网关层 | Spring Security Filter | JWT 鉴权、CORS、请求统一入口 |
| 业务服务层 | Spring Boot Service | 核心业务逻辑编排 |
| AI 接入抽象层 | `AiProvider` 接口 + 实现 | 统一多模型调用，屏蔽厂商差异 |
| 数据持久层 | MySQL + MyBatis-Plus | 用户/会话/消息数据存储 |

---

## 项目结构

```
prompt-training-server/
├── pom.xml                           # Maven 配置
├── README.md                         # 项目文档
├── CHANGELOG.md                      # 版本更新日志
├── .gitignore
├── src/main/
│   ├── java/com/prompttraining/
│   │   ├── PromptTrainingApplication.java    # 启动类
│   │   ├── ai/                               # AI 接入抽象层
│   │   │   ├── AiProvider.java               # 统一调用接口
│   │   │   ├── AiProviderRegistry.java       # Provider 注册与路由
│   │   │   ├── AiRequest.java                # 统一请求对象
│   │   │   ├── AiResponse.java               # 统一响应对象
│   │   │   ├── StreamCallback.java           # 流式回调接口
│   │   │   ├── deepseek/
│   │   │   │   ├── DeepSeekProvider.java     # DeepSeek 实现
│   │   │   │   └── DeepSeekConfig.java
│   │   │   └── factory/
│   │   │       └── AiProviderFactory.java
│   │   ├── common/                           # 公共组件
│   │   │   ├── Result.java                   # 统一响应体
│   │   │   ├── PageResult.java               # 分页响应体
│   │   │   ├── BusinessException.java        # 业务异常
│   │   │   ├── GlobalExceptionHandler.java   # 全局异常处理
│   │   │   └── Constant.java
│   │   ├── config/                           # 配置类
│   │   │   ├── SecurityConfig.java           # Spring Security + JWT
│   │   │   ├── CorsConfig.java               # 跨域配置
│   │   │   ├── Knife4jConfig.java            # API 文档
│   │   │   ├── RedisConfig.java
│   │   │   └── MyMetaObjectHandler.java
│   │   ├── security/                         # [V2] 安全模块
│   │   │   ├── JwtTokenProvider.java         # JWT 生成与解析
│   │   │   ├── JwtAuthenticationFilter.java  # JWT 认证过滤器
│   │   │   ├── UserDetailsServiceImpl.java
│   │   │   ├── UserPrincipal.java
│   │   │   ├── SecurityUtils.java
│   │   │   ├── RefreshToken.java
│   │   │   └── RefreshTokenMapper.java
│   │   └── module/
│   │       ├── auth/                         # [V2] 认证模块
│   │       │   ├── AuthController.java
│   │       │   ├── AuthService.java
│   │       │   └── entity/dto/
│   │       ├── session/                      # 会话模块
│   │       │   ├── SessionController.java
│   │       │   ├── SessionService.java
│   │       │   ├── SessionMapper.java
│   │       │   └── entity/
│   │       ├── message/                      # 消息模块
│   │       │   ├── MessageController.java
│   │       │   ├── MessageService.java
│   │       │   ├── MessageMapper.java
│   │       │   ├── SseEmitterService.java    # SSE 流式管理
│   │       │   └── entity/
│   │       └── user/                         # 用户模块
│   │           ├── UserController.java
│   │           ├── UserService.java
│   │           ├── UserMapper.java
│   │           └── entity/
│   └── resources/
│       ├── application.yml                   # 主配置
│       ├── application-dev.yml               # 开发环境（不纳入版本控制）
│       ├── logback-spring.xml                # 日志配置
│       └── db/migration/
│           ├── V1__init.sql                  # V1 初始化 DDL
│           └── V2__add_login_and_model_fields.sql  # V2 增量迁移
└── web/                                      # 前端 Vue 3 项目
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── pnpm-workspace.yaml
    ├── index.html
    └── src/
        ├── App.vue
        ├── main.ts
        ├── router/index.ts                   # 路由配置 + 守卫
        ├── api/                              # API 封装
        │   ├── request.ts                    # Axios + Token 拦截器
        │   ├── auth.ts                       # 认证接口 [V2]
        │   ├── session.ts
        │   └── message.ts
        ├── stores/                           # Pinia 状态管理
        │   ├── chat.ts                       # 对话状态
        │   ├── session.ts                    # 会话列表状态
        │   └── user.ts                       # 用户状态 [V2]
        ├── types/                            # TypeScript 类型定义
        ├── utils/
        │   └── auth.ts                       # Token 存储工具 [V2]
        ├── views/
        │   ├── ChatView.vue                  # 主对话界面
        │   ├── LoginView.vue                 # 登录页 [V2]
        │   ├── RegisterView.vue              # 注册页 [V2]
        │   ├── ProfileView.vue               # 个人中心 [V2]
        │   ├── TrainingView.vue              # 训练模块（占位）
        │   └── KnowledgeView.vue             # 知识库（V3 启用）
        └── components/
            ├── layout/
            │   ├── AppLayout.vue             # 全局布局
            │   └── Sidebar.vue               # 侧边栏
            ├── chat/
            │   ├── ChatPanel.vue             # 核心对话面板
            │   ├── MessageBubble.vue         # 消息气泡
            │   ├── MessageInput.vue          # 消息输入框
            │   └── WelcomeScreen.vue         # 欢迎页
            └── common/
                ├── EmptyState.vue
                └── LoadingSpinner.vue
```

---

## 快速开始

### 前置条件

- **JDK** 17+
- **MySQL** 8.0
- **Redis** 7.x（V2 可选）
- **Node.js** 18+
- **pnpm**（前端包管理器）
- **Maven** 3.8+

### 1. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS prompt_training DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE prompt_training;
```

依次执行迁移脚本：

```bash
# V1 基础表结构
mysql -u root -p prompt_training < src/main/resources/db/migration/V1__init.sql

# V2 增量迁移（登录模块 + 模型字段）
mysql -u root -p prompt_training < src/main/resources/db/migration/V2__add_login_and_model_fields.sql
```

### 2. 配置环境

复制开发配置模板并填入实际值：

```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`，配置数据库连接和 DeepSeek API Key：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prompt_training?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: your_password

ai:
  deepseek:
    api-key: your_deepseek_api_key
```

### 3. 启动后端

```bash
# 方式一：IDEA 运行
# 直接运行 PromptTrainingApplication.main()

# 方式二：命令行
mvn spring-boot:run
```

服务端口：`8080`
API 文档：http://localhost:8080/doc.html

### 4. 启动前端

```bash
cd web
pnpm install
pnpm run dev
```

访问地址：http://localhost:5173

---

## API 文档

### 接口总览

所有接口前缀：`/api/v1`

#### 认证接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/refresh` | 刷新 Token |

#### 用户接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/me` | 获取当前用户信息 |
| PATCH | `/users/me` | 更新用户信息 |
| PATCH | `/users/me/password` | 修改密码 |

#### 会话接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sessions` | 创建新会话 |
| GET | `/sessions` | 获取会话列表 |
| GET | `/sessions/search?keyword=` | 搜索会话 |
| GET | `/sessions/{id}` | 获取会话详情 |
| PATCH | `/sessions/{id}` | 更新会话标题 |
| DELETE | `/sessions/{id}` | 删除会话（软删除） |
| POST | `/sessions/batch-delete` | 批量删除会话 |

#### 消息接口（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sessions/{id}/messages` | 发送消息（同步） |
| POST | `/sessions/{id}/messages/stream` | 发送消息（SSE 流式） |
| GET | `/sessions/{id}/messages` | 获取消息历史（游标分页） |
| POST | `/sessions/{id}/messages/{mid}/regenerate/stream` | 重新生成（SSE） |
| DELETE | `/sessions/{id}/messages/{mid}` | 删除单条消息 |
| DELETE | `/sessions/{id}/messages` | 清空会话消息 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 / Token 过期 |
| 404 | 资源不存在 |
| 500 | 服务端错误（含 AI 调用失败） |

---

## 版本历程

| 版本 | 日期 | 主要变更 |
|------|------|---------|
| V1.0 | 2026-07-19 | 初始版本：AI 对话 + SSE 流式响应 |
| V1.1 | 2026-07-19 | Bug 修复：聊天记录加载问题 |
| V2.0 | 2026-07-26 | JWT 登录体系 + 对话功能增强 + 个人中心 |

详见 [CHANGELOG.md](./CHANGELOG.md)

---

## 后续规划

### V3.0 规划

- 知识库模块（已预留代码，路由注释标注 `[V3 启用]`）
- 训练模块（占位 → 功能实现）
- 多模型切换（GPT、豆包等，架构已就绪）
- 提示词评估引擎
- 第三方登录（OAuth2：微信、GitHub）
- 对话导出（PDF / Markdown）
- 消息点赞/踩反馈

### 扩展架构

新增 AI 模型只需三步（架构已就绪）：

1. 实现 `AiProvider` 接口
2. 添加对应的 Config 配置类
3. 在 `AiProviderFactory` 中注册

业务层代码零修改。

---

## 开发说明

### 日志

日志文件位于 `logs/`：

| 文件 | 内容 | 保留天数 |
|------|------|---------|
| `prompt-training.log` | 全量日志（业务 DEBUG，框架 INFO） | 30 天 |
| `prompt-training-error.log` | WARN 及以上错误 | 30 天 |
| `prompt-training-sql.log` | SQL 查询日志 | 7 天 |

### 安全

- 密码加密：BCrypt（Spring Security 默认）
- JWT 签名：HMAC-SHA256
- 防重放：Refresh Token 一次性使用
- 数据隔离：所有操作通过 `SecurityUtils.getCurrentUserId()` 过滤
