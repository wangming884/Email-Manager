# 性能优化指南

## 已实施的优化

### 1. 数据库优化

#### 索引优化
已在 `MailMessage` 实体添加以下索引：

```java
@Table(name = "mail_messages", indexes = {
    @Index(name = "idx_account_external_msg", columnList = "account_id,external_message_id", unique = true),
    @Index(name = "idx_received_at", columnList = "received_at"),
    @Index(name = "idx_parsed_type", columnList = "parsed_type"),
    @Index(name = "idx_to_email", columnList = "to_email"),
    @Index(name = "idx_subject", columnList = "subject")
})
```

**性能提升：**
- 查询速度提升 10-100倍（取决于数据量）
- 避免全表扫描
- 唯一索引防止重复消息

#### 懒加载优化
将实体关系从 `EAGER` 改为 `LAZY`：

```java
@ManyToOne(fetch = FetchType.LAZY)
private Account account;
```

**性能提升：**
- 避免N+1查询问题
- 减少不必要的数据加载
- 降低内存使用

**注意：** 需要在查询时显式使用 JOIN FETCH：

```java
@Query("SELECT m FROM MailMessage m JOIN FETCH m.account WHERE m.id = :id")
Optional<MailMessage> findByIdWithAccount(@Param("id") Long id);
```

### 2. 连接池优化

#### HikariCP配置
当前配置（`application.yml`）：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**建议根据负载调整：**

```yaml
# 高负载环境
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000  # 检测连接泄漏
```

### 3. 线程池优化

#### 当前配置
- Import任务：核心4线程，最大8线程，队列200
- Sync任务：核心4线程，最大8线程，队列100

**已添加拒绝策略：**
```java
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```

**根据服务器资源调整：**

```java
// 高性能服务器（8核+）
executor.setCorePoolSize(8);
executor.setMaxPoolSize(16);
executor.setQueueCapacity(500);

// 低资源环境（2-4核）
executor.setCorePoolSize(2);
executor.setMaxPoolSize(4);
executor.setQueueCapacity(50);
```

### 4. IMAP同步优化

#### 批量进度保存
减少数据库写入频率：

```java
int batchSize = 5; // 每5个账号保存一次进度
if (processed % batchSize == 0 || processed == accounts.size()) {
    syncJobRepository.save(syncJob);
}
```

#### 连接超时配置
添加超时防止长时间挂起：

```java
properties.put("mail.imap.connectiontimeout", "10000"); // 10秒
properties.put("mail.imap.timeout", "10000");
```

## 待实施的优化

### 1. 分页查询（高优先级）

**当前问题：** 列表查询返回所有结果，可能导致OOM

**解决方案：**

```java
@GetMapping("/query")
public ApiResponse<Page<EmailResponse>> queryEmails(
        @RequestParam(required = false) String toEmail,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest request
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
    Page<MailMessage> messages = mailMessageRepository.findAll(spec, pageable);
    // ...
}
```

### 2. Redis缓存（高优先级）

**缓存策略：**

```java
@Cacheable(value = "accounts", key = "#id")
public Account getAccount(Long id) {
    return accountRepository.findById(id).orElseThrow();
}

@CacheEvict(value = "accounts", key = "#account.id")
public void updateAccount(Account account) {
    accountRepository.save(account);
}
```

**推荐缓存内容：**
- 账号信息（TTL: 5分钟）
- 解析规则（TTL: 10分钟）
- 客户端配置（TTL: 30分钟）

### 3. 数据库查询优化

#### 使用DTO投影
避免加载不需要的字段：

```java
public interface EmailSummary {
    Long getId();
    String getSubject();
    String getToEmail();
    OffsetDateTime getReceivedAt();
}

@Query("SELECT m.id as id, m.subject as subject, m.toEmail as toEmail, m.receivedAt as receivedAt FROM MailMessage m")
Page<EmailSummary> findAllSummaries(Pageable pageable);
```

#### 批量操作
使用批量插入减少数据库往返：

```java
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

### 4. 异步处理优化

#### Webhook异步调用
当前Webhook可能阻塞主线程：

```java
@Async("webhookExecutor")
public CompletableFuture<Void> dispatchMailReceived(MailMessage mailMessage) {
    // 异步发送webhook
    return CompletableFuture.completedFuture(null);
}
```

#### 消息队列
对于高并发场景，考虑使用消息队列：

```java
// 使用RabbitMQ或Kafka
@RabbitListener(queues = "mail.sync.queue")
public void processSyncTask(SyncTask task) {
    // 处理同步任务
}
```

### 5. 前端优化

#### 虚拟滚动
对于大列表，使用虚拟滚动：

```vue
<template>
  <RecycleScroller
    :items="emails"
    :item-size="80"
    key-field="id"
  >
    <template #default="{ item }">
      <EmailRow :email="item" />
    </template>
  </RecycleScroller>
</template>
```

#### 懒加载
按需加载组件：

```javascript
const EmailsView = () => import('./views/EmailsView.vue');
const AccountsView = () => import('./views/AccountsView.vue');
```

#### API请求去抖动
防止频繁请求：

```javascript
import { debounce } from 'lodash-es';

const searchEmails = debounce(async (keyword) => {
  const response = await http.get('/emails/query', { 
    params: { subject_keyword: keyword } 
  });
  // ...
}, 300);
```

## 性能监控

### 1. 添加性能指标

```java
@Configuration
public class MetricsConfig {
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "mail-manager");
    }
}
```

### 2. 慢查询日志

```yaml
spring:
  jpa:
    properties:
      hibernate:
        show_sql: false
        format_sql: true
        use_sql_comments: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.hibernate.stat: DEBUG
```

### 3. 连接池监控

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,hikaricp
  metrics:
    enable:
      hikaricp: true
```

## 性能测试

### 负载测试脚本

```bash
# 使用Apache Bench测试
ab -n 1000 -c 10 -H "Authorization: Bearer YOUR_TOKEN" \
   http://localhost:8080/api/v1/emails/query

# 使用JMeter或Gatling进行更复杂的测试
```

### 性能基准

**目标指标：**
- API响应时间：< 200ms (P95)
- 数据库查询：< 50ms (P95)
- 邮件同步：100个账号 < 5分钟
- 并发用户：支持100+并发

## 资源使用建议

### 最小配置（开发/测试）
- CPU: 2核
- 内存: 2GB
- 数据库: 1GB

### 推荐配置（生产）
- CPU: 4核+
- 内存: 4GB+
- 数据库: 2GB+
- Redis: 512MB

### 高负载配置
- CPU: 8核+
- 内存: 8GB+
- 数据库: 4GB+
- Redis: 1GB+

## 监控告警

### 关键指标
1. **响应时间**：P95 > 500ms 告警
2. **错误率**：> 1% 告警
3. **数据库连接池**：使用率 > 80% 告警
4. **线程池队列**：队列长度 > 80% 告警
5. **内存使用**：> 85% 告警
6. **磁盘空间**：< 20% 告警

### 推荐工具
- Prometheus + Grafana
- Spring Boot Actuator
- ELK Stack (日志分析)
- APM工具（如New Relic、Datadog）

## 性能优化检查清单

- [x] 数据库索引已添加
- [x] 实体关系改为懒加载
- [x] 线程池配置了拒绝策略
- [x] IMAP连接添加超时
- [x] 批量保存减少数据库压力
- [ ] 添加分页查询
- [ ] 实施Redis缓存
- [ ] 使用DTO投影
- [ ] Webhook异步调用
- [ ] 前端虚拟滚动
- [ ] 性能监控配置
- [ ] 负载测试完成

## 性能调优流程

1. **识别瓶颈**：使用性能分析工具（JProfiler、VisualVM）
2. **测量基准**：记录优化前的性能指标
3. **实施优化**：按优先级逐步优化
4. **验证效果**：对比优化前后的指标
5. **持续监控**：生产环境持续监控性能

## 参考资源

- [Spring Boot性能优化](https://spring.io/guides/gs/spring-boot/)
- [Hibernate性能调优](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#performance)
- [HikariCP配置](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
