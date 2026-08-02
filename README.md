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

### V3.0（当前版本 · 2026-08-02）

- ✅ 独立管理员门户（可配置特殊 URL 入口，默认 `/admin`）
- ✅ 管理员动态 AI 配置（API 地址 / Key / 模型编码 / 参数 / 初始提示词），保存即时生效无需重启
- ✅ 管理员账号密码配置文件可修改（默认 admin / 123456，支持明文或 BCrypt）
- ✅ 模型参数细调（max_tokens / temperature / top_p / presence_penalty / frequency_penalty）
- ✅ 连接测试功能
- ✅ 用户端精简：移除训练模块，仅保留对话 + 个人中心
- 🔲 知识库（代码已预留，后续启用）

### V2.1（2026-08-02）

- ✅ 修复 Token 过期后界面锁定：认证过期自动刷新 Token，刷新失败自动跳转登录页（覆盖 Axios 请求与 SSE 流式请求）
- ✅ 用户界面新增退出登录按钮（侧边栏底部 + 个人中心）
- ✅ 新增前端 UI 图片资源目录 `web/src/assets/ui/`（为后期用外部图片替换矢量图 UI 预留）

### V2.0（2026-07-26）

- ✅ AI 网页对话，支持 SSE 流式响应（打字机效果）
- ✅ JWT 用户认证体系（注册 / 登录 / Token 自动刷新）
- ✅ 多会话管理（搜索、重命名、批量删除）
- ✅ 消息操作增强（复制、重新生成、停止生成、清空对话）
- ✅ 个人信息管理（编辑资料、修改密码）
- 🔲 训练模块（框架已搭建，待实现）
- 🚫 知识库（代码已预留，V3 启用）

### 版本历史

| 版本 | 日期 | 进展 |
|------|------|------|
| V1.0 | 2026-07-19 | 初始版本，完成基础 AI 对话 + SSE 流式响应 |
| V1.1 | 2026-07-19 | Bug 修复，解决切换会话后消息不加载的问题 |
| V2.0 | 2026-07-26 | JWT 登录体系 + 对话功能增强 + 个人中心 |
| V2.1 | 2026-08-02 | 修复 Token 过期自动退出 + 退出登录按钮 + UI 图片目录 |
| V3.0 | 2026-08-02 | 管理员门户 + 动态 AI 配置 + 用户端精简 |

详见 [CHANGELOG.md](./CHANGELOG.md)

---

## 快速开始

### 前置条件

- JDK 17+ / MySQL 8.0 / Maven 3.8+
- Node.js 18+ / pnpm

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
