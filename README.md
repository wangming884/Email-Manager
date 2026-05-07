# 邮箱管理器 MVP

基于 `Spring Boot 3 + Vue 3 + MySQL 8 + Redis 7 + Docker Compose` 的邮箱资源化管理平台，覆盖账号导入、状态校验、收件同步、邮件规则解析、Webhook 回调、客户端鉴权和基础运维后台。

## 项目结构

- `backend/`：Spring Boot API、账号状态机、导入任务、IMAP 同步、解析规则、Webhook、审计日志。
- `frontend/`：Vue 3 运维后台，提供总览、账号、邮件、客户端、规则、回调和日志页面。
- `infra/`：`docker-compose.yml` 和 `.env` 模板，一键启动 MySQL、Redis、后端与前端。

## 主要能力

- 批量导入账号，支持文本文件与 JSON 行数据。
- 账号状态流转：`ACTIVE / TESTING / INVALID / LOCKED`。
- 基于 IMAP 的账号连通性测试与收件同步。
- 规则驱动的验证码/激活链接提取，解析结果保存为结构化 JSON。
- 通过 `client_id / client_secret` 为外部平台签发访问令牌。
- WebSocket 推送导入进度和新邮件提醒。
- Webhook 回调和接口审计日志。
- 账号与邮件导出。

## 默认凭证

- 管理员：`admin / admin123456`
- 默认客户端：
  - `client_id`: `core-platform`
  - `client_secret`: `core-platform-secret`

## 本地开发

### 后端

```powershell
cd backend
mvn spring-boot:run
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，并通过 Vite 代理转发到 `http://localhost:8080`。

## 容器启动

Docker Compose 会优先读取 `infra/.env`。仓库里提供了示例模板 [infra/.env.example](</d:/youxiangguanliqi/infra/.env.example>)，可用于自定义端口、数据库账号、管理员账号和默认客户端密钥。

```powershell
cd infra
docker compose up --build
```

启动后：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- MySQL：`localhost:3306`
- Redis：`localhost:6379`

### 常用可配置项

- `MYSQL_PORT`：MySQL 映射端口
- `REDIS_PORT`：Redis 映射端口
- `BACKEND_PORT`：后端 API 映射端口
- `FRONTEND_PORT`：前端映射端口
- `ADMIN_USERNAME` / `ADMIN_PASSWORD`：后台管理员账号
- `DEFAULT_CLIENT_ID` / `DEFAULT_CLIENT_SECRET`：默认平台客户端凭证
- `JWT_SECRET` / `ENCRYPTION_SECRET`：JWT 与账号密码加密密钥

### 示例

下面是一份更适合生产前最小调整的 `.env` 思路：

```env
MYSQL_ROOT_PASSWORD=change-me-root
MYSQL_PASSWORD=change-me-db
ADMIN_USERNAME=ops-admin
ADMIN_PASSWORD=change-me-admin
DEFAULT_CLIENT_ID=platform-a
DEFAULT_CLIENT_SECRET=change-me-client
JWT_SECRET=replace-with-a-long-random-string-at-least-64-chars
ENCRYPTION_SECRET=replace-with-32-char-secret-key
BACKEND_PORT=18080
FRONTEND_PORT=15173
```

## 关键 API

- `POST /api/v1/admin/login`
- `POST /api/v1/auth/token`
- `POST /api/v1/accounts/import`
- `GET /api/v1/accounts`
- `POST /api/v1/accounts/{id}/test`
- `GET /api/v1/accounts/export`
- `POST /api/v1/sync/jobs`
- `GET /api/v1/emails/query`
- `POST /api/v1/parser-rules`
- `POST /api/v1/webhooks`

## 后续建议

- 为 OAuth2 邮箱接入补充真正的授权码/刷新令牌流程。
- 将代理绑定管理和邮件发送编排纳入后台界面。
- 为同步任务增加独立队列、重试和限流指标。

## 安全和性能

### 最新优化（v1.1）

本版本包含重要的安全修复和性能优化：

**安全修复：**
- ✅ 修复管理员密码使用BCrypt加密验证
- ✅ 修复SQL注入风险（LIKE查询转义）
- ✅ 修复WebSocket跨域安全问题
- ✅ 修复前端路由器上下文错误
- ✅ Docker容器改为非root用户运行
- ✅ IMAP连接资源泄漏修复

**性能优化：**
- ✅ 添加数据库索引（查询速度提升10-100倍）
- ✅ 实体关系改为懒加载（避免N+1查询）
- ✅ 线程池添加拒绝策略
- ✅ IMAP连接添加超时配置
- ✅ 批量保存同步进度

**详细文档：**
- 查看 [SECURITY.md](SECURITY.md) 了解安全配置
- 查看 [OPTIMIZATION.md](OPTIMIZATION.md) 了解性能优化

### ⚠️ 生产部署前必读

**必须修改以下默认配置：**

1. **管理员密码**：使用BCrypt加密的强密码
2. **JWT密钥**：至少64字符的随机字符串
3. **加密密钥**：32字符的随机字符串
4. **数据库密码**：强密码
5. **WebSocket源限制**：配置 `ALLOWED_ORIGINS` 环境变量

详见 [SECURITY.md](SECURITY.md) 中的完整配置指南。
