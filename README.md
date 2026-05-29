# DevMatch

DevMatch 是一个前后端分离的软件开发项目供需对接系统，包含企业端、开发者端和管理端。系统实现了需求发布、任务审核、开发者投标、企业选标、项目里程碑交付、钱包托管结算、评价信用和后台管理等基础功能。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.3
- Spring Security + JWT
- MyBatis-Plus 3.5.5
- MySQL 8.x
- Redis
- Spring WebSocket
- Knife4j / OpenAPI
- Lombok
- Hutool

### 前端

- Vue 3
- Vite 5
- Vue Router 4
- Pinia
- Element Plus
- Axios
- ECharts / vue-echarts
- dayjs
- SCSS

## 项目结构

```text
BiYeSheJi/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/devmatch/
│       │   ├── common/          # 统一响应、分页、异常、枚举
│       │   ├── config/          # Security、Redis、MyBatis、WebSocket 等配置
│       │   ├── controller/      # REST API 控制器
│       │   ├── dto/             # 请求 DTO
│       │   ├── entity/          # 数据库实体
│       │   ├── mapper/          # MyBatis-Plus Mapper
│       │   ├── schedule/        # 定时任务
│       │   ├── security/        # JWT 与 Spring Security
│       │   ├── service/         # 业务逻辑
│       │   └── websocket/       # WebSocket 通知
│       └── resources/
│           ├── application.yml
│           └── db/              # 建表和迁移 SQL
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/                 # Axios API 封装
│       ├── layouts/             # 开发者/企业/管理/认证布局
│       ├── router/              # 路由和角色守卫
│       ├── stores/              # Pinia store
│       ├── utils/               # WebSocket、格式化工具
│       └── views/               # 页面
└── uploads/                     # 本地上传目录，开发环境使用
```

## 环境要求

- JDK 17
- Node.js 18+，npm
- MySQL 8.x
- Redis 6/7
- Maven 3.8+，当前仓库未包含 `mvnw`

## 本地启动

### 1. 初始化数据库

创建数据库：

```sql
CREATE DATABASE devmatch DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

导入表结构和初始数据：

```bash
mysql -u root -p devmatch < backend/src/main/resources/db/schema.sql
```

旧数据库升级时，按需执行：

```text
backend/src/main/resources/db/migration_task_publish_deposit.sql
backend/src/main/resources/db/migration_project_delivery.sql
```

默认管理员账号来自 `schema.sql`：

```text
username: admin
password: admin123456
```

### 2. 启动 Redis

后端默认连接：

```text
host: localhost
port: 6379
database: 0
```

Redis 当前用途：

- 存储模拟短信验证码，默认 TTL 为 5 分钟。
- 基础并发控制锁，key 前缀为 `devmatch:lock:`。

### 3. 配置后端

配置文件：

```text
backend/src/main/resources/application.yml
```

需要根据本地环境修改：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.data.redis.host`
- `spring.data.redis.port`
- `jwt.secret`
- `upload.path`

公开仓库中的 `application.yml` 使用环境变量占位。常用变量如下：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_DATABASE
JWT_SECRET
UPLOAD_PATH
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

也可以在 IDE 中运行：

```text
com.devmatch.DevMatchApplication
```

本地调试时，如果 IDE 生成了 classpath argfile，也可以用 Java 命令启动。该方式通常包含本机绝对路径，不适合作为通用启动脚本。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:3000
```

Vite 开发代理配置在 `frontend/vite.config.js`，默认转发：

- `/api` -> `http://localhost:8080`
- `/uploads` -> `http://localhost:8080`
- `/ws` -> `ws://localhost:8080`

## API 文档

后端启动后访问：

```text
http://localhost:8080/doc.html
```

主要接口前缀：

- 认证：`/api/auth`
- 用户与认证材料：`/api/users`
- 企业认证：`/api/enterprise`
- 任务：`/api/tasks`
- 投标：`/api/bids`
- 项目与交付物：`/api/projects`
- 钱包：`/api/wallet`
- 通知：`/api/messages`、`/api/notifications`
- 管理端：`/api/admin`
- WebSocket：`/ws/chat?token=<accessToken>`

## 主要业务状态

### 任务状态

```text
DRAFT -> AUDITING -> PUBLISHED -> IN_PROGRESS -> COMPLETED
              |           |
              v           v
          REJECTED     CLOSED / EXPIRED
```

### 里程碑状态

```text
PENDING -> IN_PROGRESS -> SUBMITTED -> ACCEPTED
                              |
                              v
                         IN_PROGRESS
```

里程碑被驳回时会回到 `IN_PROGRESS`，开发者可重新上传交付物并再次提交验收。

## Redis 并发控制

代码位置：

```text
backend/src/main/java/com/devmatch/service/RedisLockService.java
```

实现方式：

- 使用 `SET key value NX EX` 获取锁。
- value 使用随机 token。
- 释放锁时通过 Lua 脚本校验 token，避免删除其他请求持有的锁。
- 默认锁过期时间为 15 秒，部分操作单独设置 10 秒或 20 秒。
- 事务内调用释放锁时，使用 Spring 事务同步在事务完成后释放。

当前加锁范围：

- 同一开发者对同一任务重复投标。
- 同一任务的发布、编辑、关闭、审核、选标等状态变更。
- 同一里程碑重复验收。
- 钱包充值、提现、冻结、发布押金扣除/退回、提现审核。

当前 Redis 锁是基础实现，适合单 Redis 实例的开发和演示环境。多实例或生产环境建议补充 Redisson、数据库唯一约束、乐观锁版本号、请求幂等号等机制。

## 当前演示实现和生产替换项

| 模块 | 当前实现 | 后续处理 |
| --- | --- | --- |
| 数据库配置 | `application.yml` 使用环境变量占位，并提供开发默认值 | 部署时通过环境变量、外部配置或密钥管理注入真实配置 |
| JWT 密钥 | `jwt.secret` 使用环境变量占位，并提供开发默认值 | 部署时替换为高强度密钥，并从安全配置源读取 |
| 默认管理员 | `schema.sql` 内置 `admin/admin123456` | 部署前修改、删除或改为初始化脚本创建 |
| 短信验证码 | 固定模拟验证码 `123456`，写入 Redis，有效期 5 分钟 | 接入真实短信服务，增加频率限制、验证码风控和日志脱敏 |
| 短信登录 | 使用 Redis 中的模拟验证码完成登录校验 | 接入真实短信验证码发送与校验流程 |
| 邮箱验证码 | `/auth/email/send` 为预留接口，当前直接返回成功 | 接入邮件服务，并补全验证码存储和校验 |
| 充值支付 | 充值订单创建后直接置为 `SUCCESS` | 接入真实支付渠道和异步回调验签 |
| 提现转账 | 审核通过只改变系统内提现状态 | 接入代付或转账接口，处理失败、重试和对账 |
| 平台佣金 | 里程碑结算时仅记录流水，未转入平台账户 | 增加平台账户、清分记录和财务对账 |
| 文件存储 | 上传文件保存在本地 `uploads/` | 替换为对象存储，如 OSS、S3、COS，并使用私有访问或签名 URL |
| CORS | 允许所有来源 | 限制为指定前端域名 |
| SQL 日志 | MyBatis SQL 日志开启 | 生产环境关闭或降低日志级别 |
| WebSocket token | WebSocket 使用 query token | 生产环境可改为更严格的鉴权方式或短期连接凭证 |
| 交付物下载 token | 下载接口支持 `access_token` query 参数 | 生产环境建议使用短期签名下载链接 |
| Redis 锁 | 基础单实例锁 | 多节点场景使用 Redisson 或引入数据库幂等约束 |

## GitHub 提交建议

建议不要提交以下内容：

```text
frontend/node_modules/
frontend/dist/
backend/target/
uploads/
.vscode/
*.log
```

`uploads/` 可能包含头像、KYC 图片、任务附件和交付物，不应提交到公开仓库。

仓库已包含基础 `.gitignore`。本地开发配置与公开配置建议分离，例如使用：

```text
application.yml
application-local.yml
```

公开仓库中保留示例配置，真实密码和密钥只保存在本地或部署环境。

## 常用命令

后端：

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
npm run build
```

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
