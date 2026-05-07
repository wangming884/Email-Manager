# 快速开始指南

## 🚀 5分钟快速启动

### 前提条件
- Docker 和 Docker Compose
- （可选）Node.js 18+ 和 Maven 3.9+（本地开发）

### 1. 克隆项目
```bash
git clone <repository-url>
cd youxiangguanliqi
```

### 2. 配置环境变量（重要！）

复制环境变量模板：
```bash
cp infra/.env.example infra/.env
```

**⚠️ 生产环境必须修改以下配置：**

```env
# 管理员账号（使用BCrypt加密的密码）
ADMIN_USERNAME=your-admin
ADMIN_PASSWORD=$2a$10$your-bcrypt-hash

# JWT密钥（至少64字符）
JWT_SECRET=your-random-64-char-string

# 加密密钥（32字符）
ENCRYPTION_SECRET=your-32-char-secret

# 数据库密码
MYSQL_ROOT_PASSWORD=your-mysql-root-pass
MYSQL_PASSWORD=your-mysql-pass

# WebSocket允许的源
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:*
```

**生成BCrypt密码：**
```bash
# 在线工具：https://bcrypt-generator.com/
# 或使用Python
python -c "import bcrypt; print(bcrypt.hashpw(b'your-password', bcrypt.gensalt()).decode())"
```

**生成随机密钥：**
```bash
# Linux/Mac
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 3. 启动服务

```bash
cd infra
docker compose up --build
```

等待所有服务启动（约2-3分钟）。

### 4. 访问应用

- **前端管理后台：** http://localhost:5173
- **后端API：** http://localhost:8080
- **MySQL：** localhost:3306
- **Redis：** localhost:6379

### 5. 登录

使用配置的管理员账号登录：
- 用户名：`ADMIN_USERNAME` 的值
- 密码：BCrypt加密前的原始密码

## 📖 本地开发

### 后端开发

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，自动代理API请求到后端。

## 🔍 验证安装

### 1. 检查服务状态

```bash
docker compose ps
```

所有服务应该显示 `Up` 状态。

### 2. 测试API

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 管理员登录
curl -X POST http://localhost:8080/api/v1/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}'
```

### 3. 查看日志

```bash
# 查看所有日志
docker compose logs

# 查看特定服务日志
docker compose logs backend
docker compose logs frontend
```

## 📚 核心功能快速测试

### 1. 导入邮箱账号

```bash
curl -X POST http://localhost:8080/api/v1/accounts/import \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "format": "json",
    "content": "{\"email\":\"test@example.com\",\"password\":\"pass123\",\"imapHost\":\"imap.example.com\",\"imapPort\":993}"
  }'
```

### 2. 测试账号连通性

```bash
curl -X POST http://localhost:8080/api/v1/accounts/{id}/test \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. 创建同步任务

```bash
curl -X POST http://localhost:8080/api/v1/sync/jobs \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"accountIds": [1, 2, 3]}'
```

### 4. 查询邮件

```bash
curl "http://localhost:8080/api/v1/emails/query?subject_keyword=验证码" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 🛠️ 常见问题

### 问题1：容器启动失败

**检查端口占用：**
```bash
# Windows
netstat -ano | findstr "8080"
netstat -ano | findstr "3306"

# Linux/Mac
lsof -i :8080
lsof -i :3306
```

**解决方案：** 修改 `infra/.env` 中的端口配置。

### 问题2：数据库连接失败

**检查MySQL是否启动：**
```bash
docker compose logs mysql
```

**解决方案：** 等待MySQL完全启动（约30秒）。

### 问题3：前端无法连接后端

**检查后端是否运行：**
```bash
curl http://localhost:8080/actuator/health
```

**检查Vite代理配置：**
```javascript
// frontend/vite.config.js
export default {
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
}
```

### 问题4：登录失败

**原因：** 密码格式不正确

**解决方案：**
1. 确认 `ADMIN_PASSWORD` 是BCrypt哈希值
2. 使用正确的原始密码登录
3. 检查后端日志：`docker compose logs backend`

### 问题5：WebSocket连接失败

**原因：** CORS配置问题

**解决方案：** 配置 `ALLOWED_ORIGINS` 环境变量：
```env
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:*
```

## 🔧 开发工具

### 数据库管理

使用任何MySQL客户端连接：
- Host: `localhost`
- Port: `3306`
- User: `root`
- Password: `MYSQL_ROOT_PASSWORD` 的值
- Database: `mailmanager`

推荐工具：
- DBeaver
- MySQL Workbench
- DataGrip

### Redis管理

使用Redis客户端连接：
- Host: `localhost`
- Port: `6379`

推荐工具：
- RedisInsight
- Redis Desktop Manager

### API测试

推荐工具：
- Postman
- Insomnia
- curl

## 📊 性能优化提示

### 开发环境
```env
# 减少资源使用
BACKEND_JVM_OPTS=-Xmx512m -Xms256m
```

### 生产环境
```env
# 增加资源配置
BACKEND_JVM_OPTS=-Xmx2g -Xms1g
```

## 🔐 安全提示

### 开发环境
- ✅ 可以使用默认密码
- ✅ 可以使用简单的密钥

### 生产环境
- ❌ 绝不使用默认密码
- ❌ 绝不使用示例密钥
- ✅ 必须使用强密码和随机密钥
- ✅ 必须启用HTTPS
- ✅ 必须限制CORS和WebSocket源

**详见：** [SECURITY.md](SECURITY.md)

## 📖 更多文档

- [README.md](README.md) - 项目概述
- [SECURITY.md](SECURITY.md) - 安全配置指南
- [OPTIMIZATION.md](OPTIMIZATION.md) - 性能优化指南
- [CHANGELOG.md](CHANGELOG.md) - 版本变更日志
- [BUG_FIXES_SUMMARY.md](BUG_FIXES_SUMMARY.md) - Bug修复总结

## 🆘 获取帮助

1. 查看文档
2. 检查日志：`docker compose logs`
3. 查看已知问题：[CHANGELOG.md](CHANGELOG.md)
4. 提交Issue

## 🎉 下一步

1. ✅ 完成快速启动
2. 📖 阅读 [SECURITY.md](SECURITY.md)
3. 🔧 配置生产环境
4. 🚀 开始使用！

---

**祝你使用愉快！** 🎊
