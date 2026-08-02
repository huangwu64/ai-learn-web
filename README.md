# 提示词工程实战训练系统

> 一个以练习驱动的 AI 提示词能力训练平台。通过与 AI 真实对话来学习和优化提示词，而非被动阅读教程。

---

## 项目目标

打造一个**从入门到精通的提示词工程训练平台**，让用户在实际对话中掌握提示词技巧。最终形态包含四大模块：

| 模块 | 目标 |
|------|------|
| **对话** | 与 AI 自由对话，在实践中感知提示词的效果差异 |
| **训练** | 结构化练习场景，从基础到高级逐步提升提示词设计能力 |
| **知识库** | 系统化的提示词方法论，配合实战案例加深理解 |
| **评估** | 自动评估提示词质量，给出量化反馈和改进建议 |

---

## 当前进度

### V3.2（当前版本 · 2026-08-02）

- ✅ 管理员用户管理（新增/删除/编辑/重置密码）
- ✅ 头像文件上传（本地存储 `uploads/avatars/`）+ 资料变更管理员审核
- ✅ 综合入口（根 URL `/`）+ 用户端移至 `/chat`
- ✅ 云服务器配置预留（`config/`，V4 填写）
- ✅ 一键启动脚本（`start-dev.bat` / `start-dev.sh`）

### 版本历史

| 版本 | 日期 | 进展 |
|------|------|------|
| V1.0 | 2026-07-19 | 初始版本，完成基础 AI 对话 + SSE 流式响应 |
| V1.1 | 2026-07-19 | Bug 修复，解决切换会话后消息不加载的问题 |
| V2.0 | 2026-07-26 | JWT 登录体系 + 对话功能增强 + 个人中心 |
| V2.1 | 2026-08-02 | 修复 Token 过期自动退出 + 退出登录按钮 + UI 图片目录 |
| V3.0 | 2026-08-02 | 管理员门户 + 动态 AI 配置 + 用户端精简 |
| V3.1 | 2026-08-02 | 推理模型兼容 + 动态模型列表 + AI 自报模型名 |
| V3.2 | 2026-08-02 | 用户管理 + 头像上传审核 + 综合入口 + URL 调整 |

详见 [CHANGELOG.md](./CHANGELOG.md)

---

## 快速开始

### 前置条件

- JDK 17+ / MySQL 8.0 / Maven 3.8+
- Node.js 18+ / pnpm

### 一键启动（推荐）

Windows 双击 `start-dev.bat`，或 Git Bash 运行 `./start-dev.sh`，自动同时启动后端与前端。

访问地址：
- 综合入口：http://localhost:5173/
- 用户端：http://localhost:5173/chat
- 管理后台：http://localhost:5173/admin（默认 admin / 123456）

### 数据库

```sql
CREATE DATABASE IF NOT EXISTS prompt_training DEFAULT CHARACTER SET utf8mb4;
```

依次执行 `src/main/resources/db/migration/` 下的 `V1__init.sql` 和 `V2__add_login_and_model_fields.sql`。

### 后端

```bash
# 1. 复制配置模板（不含真实密钥，可提交）
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# 2. 修改 application-dev.yml 中的数据库连接
# 3. 在项目根目录创建 application-local.yml（被 gitignore 忽略，不会入库），填入 API Key：
#      ai:
#        deepseek:
#          api-key: sk-你的Key
#    （也可不创建，启动后在管理员界面 /admin 配置 Key，保存到数据库）
mvn spring-boot:run
# 服务端口：8080
# API 文档：http://localhost:8080/doc.html
# 管理员入口：http://localhost:5173/admin（默认账号 admin / 123456，可在配置中修改）
```

> 💡 **密钥安全**：`application-*.yml`（含 `application-local.yml`、`application-dev.yml`）已被 `.gitignore` 忽略、不会进入仓库。API Key 只会以「本地文件」或「数据库 ai_config 表」两种形式存在，仓库公开也不会泄露。

### 前端

```bash
cd web
pnpm install
pnpm run dev
# 访问：http://localhost:5173
```

---

## 技术栈

后端：Spring Boot 3.2 · Spring Security + JWT · MyBatis-Plus · MySQL · Redis · Knife4j

前端：Vue 3 · TypeScript · Vite · Element Plus · Pinia

AI：DeepSeek（已预留多模型扩展接口）

---

## 后续规划

- **后续**：知识库模块、多模型切换、提示词评估引擎、第三方登录（OAuth2）、对话导出、消息反馈
