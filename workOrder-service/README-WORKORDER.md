# WorkOrder 后端服务

该目录是与 workOrder-ui 匹配的后端服务，基于 RuoYi-Vue v3.9.0：

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

~~~text
D:\person\workspace\workOrder-ui\
├─ workOrder-ui\       # 当前 Vue 2 前端
└─ workOrder-service\  # 当前后端
~~~

## 前置条件

- JDK 8+
- Maven 3.6+
- Docker Engine 和 Docker Compose v2（使用 Compose 时）

## 数据库初始化脚本

开发 Compose 首次创建 MySQL 数据卷时，按以下顺序执行 01–13 脚本：

| 序号 | 文件 |
|---|---|
| 01 | sql/ry_20250522.sql |
| 02 | sql/quartz.sql |
| 03 | sql/03_workorder_core_schema.sql |
| 04 | sql/04_workorder_dict.sql |
| 05 | sql/05_workorder_menu.sql |
| 06 | sql/06_workorder_role_grant.sql |
| 07 | sql/07_workorder_seed.sql |
| 08 | sql/08_workorder_m1_core_adjust.sql |
| 09 | sql/09_workorder_m1_b02.sql |
| 10 | sql/10_workorder_m1_d03.sql |
| 11 | sql/11_workorder_f01_sla_delay.sql |
| 12 | sql/12_workorder_f02_notification.sql |
| 13 | sql/13_workorder_f04_operations.sql |

这些脚本只会由 MySQL 镜像在空数据目录初始化时自动执行。已有数据卷不会自动重放新增脚本，需要按变更要求手动执行，或在可丢弃的本地环境中重新创建数据卷。

## 构建与本地启动

~~~powershell
cd D:\person\workspace\workOrder-ui\workOrder-service
.\bin\build-local.ps1
.\bin\run-local.ps1
~~~

run-local.ps1 使用 local Spring profile。也可以使用 Maven：

~~~powershell
mvn.cmd -pl ruoyi-admin -am spring-boot:run -Dspring-boot.run.profiles=local
~~~

本地非 Docker 启动时，数据库和 Redis 连接通过 DB_URL、DB_USERNAME、DB_PASSWORD、REDIS_HOST、REDIS_PORT、REDIS_PASSWORD 等环境变量提供。

## 本地 Docker Compose

复制环境模板并填写本地值：

~~~powershell
cd D:\person\workspace\workOrder-ui\workOrder-service
Copy-Item .env.example .env
~~~

本地 Compose 必须至少填写以下变量：

| 变量 | 用途 |
|---|---|
| MYSQL_USER | MySQL 独立应用用户；后端不会使用 root 账号 |
| MYSQL_PASSWORD | MySQL 应用用户凭据 |
| MYSQL_ROOT_PASSWORD | MySQL 初始化所需的 root 凭据 |
| DB_URL | 后端连接 Compose 内部 mysql 服务的 JDBC 地址 |
| TOKEN_SECRET | 本地 JWT 密钥 |
| SWAGGER_ENABLED | 本地 Swagger 开关，由 .env 明确指定 |

MYSQL_DATABASE 可以保持与初始化脚本匹配的数据库名。开发 Compose 默认在 127.0.0.1 上暴露 MySQL、Redis 和后端端口；需要调整时使用 MYSQL_HOST_PORT、REDIS_HOST_PORT、BACKEND_HOST_PORT 等本地变量。Redis 服务默认不启用认证，REDIS_PASSWORD 留空即可。

校验并启动基础设施：

~~~powershell
docker compose config
docker compose up -d mysql redis
~~~

构建并启动后端（local profile）：

~~~powershell
docker compose --profile app up -d --build backend
docker compose logs -f backend
~~~

停止本地服务：

~~~powershell
docker compose --profile app down
~~~

## 生产 Docker Compose

生产 Compose 只部署 backend，不创建 MySQL 或 Redis。请复制 .env.example 中的生产变量到独立的 .env.production，替换所有占位符后执行：

~~~powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\bin\check-production-env.ps1 -EnvFile .env.production
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose --env-file .env.production -f docker-compose.prod.yml logs -f backend
~~~

生产必须设置以下变量：

| 变量 | 用途 |
|---|---|
| DB_URL | 托管 MySQL JDBC 地址 |
| DB_USERNAME | 托管 MySQL 应用用户 |
| DB_PASSWORD | 托管 MySQL 凭据 |
| REDIS_HOST | 托管 Redis 地址 |
| REDIS_PORT | 托管 Redis 端口 |
| REDIS_DATABASE | Redis 数据库索引 |
| REDIS_PASSWORD | 托管 Redis 凭据 |
| TOKEN_SECRET | 生产 JWT 密钥 |
| WORKORDER_UPLOAD_PATH | 容器内上传目录，通常为 /data/workorder/upload |
| CORS_ALLOWED_ORIGINS | 明确的生产前端来源列表 |

生产配置将 Swagger、Druid 管理页面和 devtools 固定关闭，默认只绑定 127.0.0.1:8080。容器启用非 root 用户、cap_drop: ALL、no-new-privileges、只读根文件系统和 /tmp 临时文件系统；上传数据和日志分别保存到命名卷。若由反向代理访问，需在受控网络边界上配置代理，并谨慎调整绑定地址。

## 安全注意事项

- .env、.env.production 及真实密钥不得提交到仓库；使用权限受控的部署凭据管理方式注入变量。
- 不要在 Dockerfile、Compose 文件或镜像层中写入数据库密码、JWT 密钥或 Redis 凭据。
- 修改 MySQL 初始化凭据后，已有数据卷中的用户不会自动更新；生产环境应按数据库变更流程处理。
- docker compose down -v 会删除本地数据库、上传和日志数据卷，只能用于确认可丢弃的本地环境。
- 首次登录后立即按团队账号策略修改密码，并避免在日志、截图或工单中暴露凭据。

## 前端联调

前端 vue.config.js 已将 /dev-api 代理到 http://localhost:8080。启动后端和前端后，在 workOrder-ui 目录执行：

~~~powershell
npm.cmd run dev
~~~

核心接口：

~~~text
GET  /captchaImage
POST /login
GET  /getInfo
GET  /getRouters
POST /logout
~~~

## 环境配置文件

| 文件 | 用途 |
|---|---|
| application.yml | 公共配置和环境变量入口 |
| application-local.yml | 本地联调数据源 |
| application-prod.yml | 生产数据源和外部托管服务参数 |
| application-druid.yml | 旧版独立 Druid 配置入口，凭据仍必须由环境变量提供 |

工单业务使用独立 Maven 模块 ruoyi-workorder。状态机测试：

~~~powershell
mvn.cmd -pl ruoyi-workorder -am test
~~~
