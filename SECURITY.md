# 安全配置指南

## 关键安全修复

本项目已修复以下关键安全问题：

### 1. 密码安全
- ✅ 管理员密码现在使用 BCrypt 加密存储和验证
- ✅ 使用常量时间比较防止时序攻击

### 2. SQL注入防护
- ✅ LIKE查询中的特殊字符已转义 (`%`, `_`, `\`)
- ✅ 所有用户输入都经过参数化处理

### 3. WebSocket安全
- ✅ 限制了允许的源域名（通过 `ALLOWED_ORIGINS` 环境变量配置）
- ⚠️ 建议：在生产环境中添加 WebSocket 认证拦截器

### 4. 资源管理
- ✅ IMAP连接现在正确关闭，防止资源泄漏
- ✅ 添加了连接超时配置（10秒）
- ✅ 线程池配置了拒绝策略（CallerRunsPolicy）

### 5. Docker安全
- ✅ 容器现在以非root用户运行

### 6. 性能优化
- ✅ 数据库索引已添加到关键字段
- ✅ 实体关系改为LAZY加载，避免N+1查询
- ✅ 同步任务批量保存进度，减少数据库压力

## 生产环境部署前必须完成的配置

### 1. 更改默认密码和密钥

**必须修改 `infra/.env` 中的以下配置：**

```env
# 管理员账号 - 使用BCrypt加密的密码
ADMIN_USERNAME=your-admin-username
ADMIN_PASSWORD=$2a$10$your-bcrypt-hashed-password

# JWT密钥 - 至少64字符的随机字符串
JWT_SECRET=your-cryptographically-secure-random-string-at-least-64-chars

# 加密密钥 - 32字符的随机字符串
ENCRYPTION_SECRET=your-32-character-secret-key

# 客户端凭证
DEFAULT_CLIENT_ID=your-client-id
DEFAULT_CLIENT_SECRET=your-client-secret

# MySQL密码
MYSQL_ROOT_PASSWORD=your-mysql-root-password
MYSQL_PASSWORD=your-mysql-password
```

**生成BCrypt密码哈希：**

```bash
# 使用在线工具或命令行
# 例如：https://bcrypt-generator.com/
# 或使用 Spring Boot CLI
```

**生成随机密钥：**

```bash
# Linux/Mac
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 2. 配置WebSocket允许的源

```env
# 仅允许特定域名连接WebSocket
ALLOWED_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
```

### 3. 数据库迁移策略

**修改 `application.yml`：**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 生产环境使用 validate，不要使用 update
```

**建议使用 Flyway 或 Liquibase 进行数据库版本管理。**

### 4. 启用HTTPS

在生产环境中，必须使用HTTPS：

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### 5. 配置CORS

**修改 `SecurityConfig.java`，限制允许的源：**

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 6. 添加速率限制

建议使用 Redis 实现API速率限制，特别是认证端点：

```java
// 示例：使用 Bucket4j 或 Spring Cloud Gateway
@RateLimiter(name = "authLimiter", fallbackMethod = "rateLimitFallback")
public String login(String username, String password) {
    // ...
}
```

### 7. 日志配置

**不要在生产环境日志中记录敏感信息：**

- 密码
- JWT令牌
- 加密密钥
- 邮箱密码

**使用结构化日志框架（SLF4J + Logback）：**

```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

## 已知限制和待改进项

### 高优先级
1. **WebSocket认证**：当前WebSocket连接未验证JWT令牌
2. **速率限制**：认证端点缺少速率限制，可能遭受暴力破解
3. **输入验证**：需要添加更严格的输入验证（邮箱格式、正则表达式等）

### 中优先级
4. **分页**：列表查询端点需要添加分页支持
5. **审计日志**：增强审计日志，记录所有敏感操作
6. **会话管理**：实现会话过期和刷新令牌机制

### 低优先级
7. **监控告警**：添加性能监控和异常告警
8. **备份策略**：实现自动化数据库备份

## 安全检查清单

部署前请确认：

- [ ] 所有默认密码已更改
- [ ] JWT和加密密钥已生成并配置
- [ ] WebSocket允许的源已限制
- [ ] HTTPS已启用
- [ ] CORS配置已限制到特定域名
- [ ] 数据库迁移策略已配置（使用validate而非update）
- [ ] 日志不包含敏感信息
- [ ] 容器以非root用户运行
- [ ] 防火墙规则已配置
- [ ] 定期安全更新计划已制定

## 报告安全问题

如果发现安全漏洞，请通过私密渠道报告，不要公开披露。

## 参考资源

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security文档](https://spring.io/projects/spring-security)
- [Docker安全最佳实践](https://docs.docker.com/engine/security/)
