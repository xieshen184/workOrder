# WorkOrder 后端服务

该目录是与 `workOrder-ui` 匹配的后端服务，基于官方 RuoYi-Vue `v3.9.0`：

- Spring Boot 2.5.15
- Spring Security 5.7.12
- MyBatis
- JWT
- MySQL/MariaDB
- Redis
- Quartz
- Druid
- Springfox Swagger 3

## 目录关系

```text
D:\person\workspace\workOrder-ui\
├─ workOrder-ui\       # 当前 Vue 2 前端
└─ workOrder-service\  # 当前后端
```

## 本地依赖

- JDK 8+（本机 JDK 17 可用于构建）
- Maven 3.6+
- MySQL 8.0
- Redis 5+

创建数据库 `workorder`，按顺序导入：

1. `sql/ry_20250522.sql`
2. `sql/quartz.sql`
3. `sql/03_workorder_core_schema.sql`
4. `sql/04_workorder_dict.sql`
5. `sql/05_workorder_menu.sql`
6. `sql/06_workorder_role_grant.sql`
7. `sql/07_workorder_seed.sql`
8. `sql/08_workorder_m1_core_adjust.sql`

默认本地连接：

| 服务 | 地址 | 用户名 | 密码 |
|---|---|---|---|
| MySQL | `localhost:3306/workorder` | `root` | `password` |
| Redis | `localhost:6379/0` | - | 空 |
| 后端 | `localhost:8080` | - | - |

通过环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 可覆盖默认值。

## 构建

```powershell
cd D:\person\workspace\workOrder-ui\workOrder-service
.\bin\build-local.ps1
```

构建产物：

```text
ruoyi-admin\target\ruoyi-admin.jar
```

## 启动

先启动 MySQL 和 Redis，再运行：

```powershell
.\bin\run-local.ps1
```

也可以使用 Maven：

```powershell
mvn.cmd -pl ruoyi-admin -am spring-boot:run `
  -Dspring-boot.run.profiles=local
```

## Docker（可选）

只启动基础设施：

```powershell
docker compose up -d mysql redis
```

构建并启动完整后端：

```powershell
docker compose --profile app up -d --build
```

Docker 配置用于开发联调。生产部署前必须替换数据库密码、JWT 密钥、上传目录、Swagger 开关和网络暴露规则。

## 前端联调

前端 `vue.config.js` 已将 `/dev-api` 代理到 `http://localhost:8080`，不需要额外修改。

联调流程：

1. 启动 MySQL 和 Redis；
2. 启动后端 8080；
3. 在 `workOrder-ui` 执行 `npm.cmd run dev`；
4. 浏览器访问前端地址；
5. 本地初始账号为 `admin/admin123`，首次联调后应立即修改。

核心接口：

```text
GET  /captchaImage
POST /login
GET  /getInfo
GET  /getRouters
POST /logout
```

## 环境配置

| 文件 | 用途 |
|---|---|
| `application.yml` | 公共配置和环境变量入口 |
| `application-local.yml` | 本地联调数据源 |
| `application-prod.yml` | 生产数据源，凭据必须从环境变量注入 |
| `application-druid.yml` | 官方原始 Druid 配置，保留用于对照 |

生产至少需要设置：

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://...
DB_USERNAME=...
DB_PASSWORD=...
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...
TOKEN_SECRET=<高强度随机值>
WORKORDER_UPLOAD_PATH=/data/workorder/upload
SWAGGER_ENABLED=false
```

## 当前范围

该服务完整覆盖当前前端已有的认证、用户、角色、菜单、部门、岗位、字典、配置、日志、在线用户、定时任务、Redis 监控、服务器监控、代码生成、上传下载和 Swagger 接口。

工单业务使用独立 Maven 模块 `ruoyi-workorder`。M0 已落地模块依赖、主状态与动作枚举、第一版状态机及单元测试；后续实体、Mapper、命令处理和 Controller 均在该模块内实现，不把工单业务代码放入 `ruoyi-system`。

状态机测试：

```powershell
mvn.cmd -pl ruoyi-workorder -am test
```
