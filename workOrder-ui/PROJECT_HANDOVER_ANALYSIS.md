# workOrder-ui 项目接手与部署分析

> 分析日期：2026-08-18  
> 分析范围：当前仓库内可见的前端源码、配置、依赖锁文件及本地构建结果  
> 重要限制：当前目录不包含 `.git` 元数据，也不包含后端源码、数据库脚本、CI/CD、Dockerfile 或线上配置。因此本文会明确区分“源码已确认”和“基于若依标准架构推断，需向原团队确认”的内容。

## 1. 结论先行

当前仓库不是一个完整的“工单系统”，而是一个 **若依 RuoYi-Vue 3.9.0 的 Vue 2 管理后台前端工程**。

- 仓库内未发现“工单 / work order”业务页面、业务 API 或业务模型。
- 前端包含若依标准模块：登录、用户、角色、菜单、部门、岗位、字典、参数、通知、日志、在线用户、定时任务、缓存、服务器监控、代码生成、Swagger、Druid。
- 菜单和绝大部分页面路由由后端 `/getRouters` 动态下发，因此必须拿到实际后端、数据库菜单数据和线上账号后，才能确认真实业务范围。
- 所有接口统一通过一个 `VUE_APP_BASE_API` 访问。当前源码只确认 **一个统一后端入口**，开发环境代理到 `http://localhost:8080`，未发现前端直连多个微服务的配置。
- 前端生产构建已于 2026-08-18 在 Node `v22.18.0` 上实际通过，产物位于 `dist/`；但依赖安装报告 133 个已知漏洞，其中 11 个为 critical，技术债较重。
- 项目没有测试、ESLint 脚本、CI/CD、容器化和正式部署配置，接手的首要任务不是立即开发新功能，而是先补齐后端仓库、环境参数、数据库、发布流程和业务责任人。

## 2. 项目画像

### 2.1 技术栈

| 层级 | 技术/版本 | 现状 |
|---|---|---|
| 框架 | Vue 2.6.12 | Vue 2 已停止维护，属于存量技术栈 |
| UI | Element UI 2.15.14 | 与 Vue 2 绑定 |
| 路由 | Vue Router 3.4.9 | 使用 history 模式，部署必须配置 SPA 回退 |
| 状态管理 | Vuex 3.6.0 | 用户、权限、菜单、页签、设置等全局状态 |
| HTTP | Axios 0.28.1 | 统一 10 秒超时、Bearer Token、统一错误码处理 |
| 构建 | Vue CLI 4.4.6 / Webpack 4 | 版本较旧，但当前机器已构建成功 |
| 样式 | Sass 1.32.13 | 全局 SCSS + Element UI 主题 |
| 图表 | ECharts 5.4.0 | 用于首页/监控展示 |
| 富文本 | Quill 2.0.2 | 已配置图片上传 |
| 包管理 | npm + package-lock v3 | 锁文件存在，但被 `.gitignore` 忽略，需治理 |

### 2.2 代码规模与工程能力

| 项目 | 结果 |
|---|---:|
| `src` 文件数 | 275 |
| Vue 单文件组件 | 95 |
| JavaScript 文件 | 73 |
| 自动化测试 | 0 |
| 源码 API 定义 | 113 个请求定义，另有上传、下载、导入导出等页面内请求 |
| 生产构建产物 | 186 个文件，约 6.55 MiB |

### 2.3 目录说明

```text
workOrder-ui/
├─ src/
│  ├─ api/              # 后端接口封装，按 system/monitor/tool 分类
│  ├─ assets/           # 图片、图标、全局样式
│  ├─ components/       # 上传、编辑器、分页、字典等公共组件
│  ├─ directive/        # 权限、角色、拖拽、剪贴板指令
│  ├─ layout/           # 后台主框架、侧边栏、顶部栏、页签
│  ├─ plugins/          # 下载、弹窗、缓存、页签、权限工具
│  ├─ router/           # 固定路由和少量权限路由
│  ├─ store/            # Vuex 模块
│  ├─ utils/            # 请求、Token、字典、RSA、本地工具
│  └─ views/            # 若依标准页面；当前无工单业务目录
├─ public/              # HTML 模板、favicon、主题静态文件
├─ build/               # preview 构建/预览脚本
├─ bin/                 # Windows 安装、运行、构建批处理
├─ .env.*               # 开发、预发布、生产环境变量
├─ vue.config.js        # devServer、代理、Webpack、gzip 配置
├─ package.json
└─ package-lock.json
```

## 3. 应用运行链路

```mermaid
flowchart LR
    U[浏览器] --> N[Nginx 或 Vue Dev Server]
    N -->|静态资源与 history 回退| F[Vue 2 前端]
    F -->|/captchaImage /login| B[统一后端入口]
    B -->|token| F
    F -->|Authorization: Bearer token| I[/getInfo]
    F -->|动态菜单| R[/getRouters]
    F -->|/system /monitor /tool /common| B
    B -.待后端仓库确认.-> DB[(MySQL)]
    B -.待后端仓库确认.-> REDIS[(Redis)]
    B -.文件接口.-> FS[(本地磁盘或对象存储)]
```

实际登录过程：

1. 登录页调用 `/captchaImage` 获取验证码开关、图片和 UUID。
2. 用户名、密码、验证码提交到 `/login`。
3. 返回的 token 写入 Cookie `Admin-Token`。
4. 后续请求自动增加 `Authorization: Bearer <token>`。
5. 前端调用 `/getInfo` 获取用户、角色和权限。
6. 前端调用 `/getRouters` 获取后端配置的菜单与组件路径，再动态加载 `src/views` 下的页面。
7. 后端业务响应约定为 `{ code, msg, data/rows/total... }`，其中 `200` 成功、`401` 登录失效、`500` 错误、`601` 警告。

这意味着：**数据库中的菜单记录、权限标识和前端组件路径必须一致**。只交付前端仓库无法恢复完整系统。

## 4. 前端现有功能

### 4.1 可从源码确认的模块

| 模块 | 页面/能力 | 主要接口前缀 |
|---|---|---|
| 认证与权限 | 验证码、登录、注册入口、退出、用户信息、动态菜单 | `/captchaImage`、`/login`、`/getInfo`、`/getRouters` |
| 用户体系 | 用户、个人中心、头像、改密、用户角色、导入导出 | `/system/user` |
| 组织权限 | 角色、数据权限、菜单、部门、岗位 | `/system/role`、`/system/menu`、`/system/dept`、`/system/post` |
| 基础配置 | 字典类型/数据、参数配置、通知公告 | `/system/dict`、`/system/config`、`/system/notice` |
| 审计监控 | 操作日志、登录日志、在线用户、服务器状态 | `/monitor/operlog`、`/monitor/logininfor`、`/monitor/online`、`/monitor/server` |
| 调度 | 定时任务、立即执行、任务日志 | `/monitor/job`、`/monitor/jobLog` |
| 缓存 | Redis 信息、缓存名称/键/值、清理缓存 | `/monitor/cache` |
| 开发工具 | 代码生成、表单构建、Swagger、Druid | `/tool/gen`、`/swagger-ui`、`/druid` |
| 文件能力 | 通用上传、下载、资源下载、头像、导入导出 | `/common/upload`、`/common/download` 等 |

### 4.2 当前没有找到的内容

- 没有 `workOrder`、`order`、`ticket` 或中文“工单”相关业务目录。
- 没有工单列表、创建、流转、审批、SLA、指派、评论、附件等业务 API。
- 没有 WebSocket、SSE、消息队列客户端或实时推送代码。
- 没有地图、IM、第三方登录、支付等业务集成。
- 没有任何测试文件。

可能性有三种：

1. 交付错了仓库或只交付了若依基础壳；
2. 工单模块在另一个前端仓库；
3. 菜单由后端下发，但对应页面源码没有包含在当前包中，此时访问会动态加载失败。

应把“获取正确业务仓库/分支”列为接手阻断项，而不是在当前壳工程上直接开始改造。

## 5. 涉及的后端有哪些

### 5.1 源码已确认：一个统一 HTTP 后端入口

| 环境 | 前端请求前缀 | 代理/转发目标 |
|---|---|---|
| 开发 | `/dev-api` | `vue.config.js` 固定代理到 `http://localhost:8080` |
| 预发布 | `/stage-api` | 仓库未提供服务器反向代理配置，需运维补充 |
| 生产 | `/prod-api` | 仓库未提供服务器反向代理配置，推荐由 Nginx 转发到实际后端 |

前端没有服务发现，也没有多个 base URL；因此从前端视角，它依赖的是一个单体后端或一个 API 网关。即使后端内部是微服务，前端也无法识别，必须查看网关和后端部署清单。

### 5.2 高概率后端形态：RuoYi-Vue Java 后端（需确认）

以下判断来自接口契约、若依目录和官方 3.9.0 版本特征，并非当前仓库内的后端源码证据：

- Java Spring Boot 应用，默认监听 8080；
- Spring Security + JWT/Bearer Token；
- MyBatis、Druid 数据源；
- MySQL 保存用户、角色、菜单、字典、参数、日志和业务数据；
- Redis 保存登录会话、验证码、字典/配置缓存等；
- Quartz 负责 `/monitor/job` 定时任务；
- Springdoc/Swagger 暴露 `/swagger-ui/index.html` 和 `/v3/api-docs/*`；
- Druid 监控页为 `/druid/login.html`；
- 文件上传接口需要本地磁盘、共享盘或对象存储，当前前端无法判断具体实现。

若采用若依标准后端，官方环境文档给出的基础依赖是 JDK、Maven、MySQL 和 Redis，并要求导入业务 SQL 与 Quartz SQL。实际项目可能已升级版本或替换组件，最终以 **后端 `pom.xml`、`application*.yml` 和部署清单** 为准。

### 5.3 必须向原团队索取的后端材料

1. 后端源码仓库 URL、准确分支/tag、提交号和构建账号权限；
2. 是否单体 RuoYi-Vue、RuoYi-Cloud，还是公司自研网关后的多个服务；
3. 开发、测试、预发、生产的 API 域名及 Nginx/网关路由；
4. MySQL 地址、库名、初始化 SQL、增量脚本、备份与恢复流程；
5. Redis 地址、DB 索引、集群/单机、持久化和清理策略；
6. 文件上传保存位置、访问域名、容量、备份和生命周期；
7. Quartz 是否集群运行，任务定义由谁维护；
8. Swagger、Druid、Actuator/监控是否允许外网访问；
9. 邮件、短信、企业微信/钉钉/飞书、消息队列等后端侧集成；
10. 后端统一响应结构、错误码文档和接口变更流程；
11. 实际菜单 SQL，特别是工单模块的 `component` 路径和权限标识；
12. 生产密钥、JWT 配置、白名单、跨域和限流策略的保管人。

## 6. 本地开发启动

### 6.1 推荐环境

短期以“可复现旧项目”为目标，建议使用：

- Node.js `16.20.x`；
- npm 8.x；
- Windows PowerShell、CMD 或 Linux/macOS Shell；
- 后端运行在 `http://localhost:8080`。

理由：依赖树中的 `@achrinza/node-ipc@9.2.2` 只声明支持到 Node 17。Node 22 本次虽然构建成功，但会产生 engine 警告，不宜直接作为团队基线。长期应升级 Vue CLI/依赖后再切换到受支持的现代 Node LTS。

### 6.2 安装与运行

```powershell
# PowerShell 若因执行策略不能运行 npm.ps1，使用 npm.cmd
npm.cmd ci
npm.cmd run dev
```

默认行为：

- 前端监听 `0.0.0.0:80`；
- 浏览器访问 `http://localhost/`；
- `/dev-api/*` 被重写并代理到 `http://localhost:8080/*`；
- `/v3/api-docs/*` 也代理到 8080；
- 启动时自动打开浏览器。

端口 80 可能被 IIS/Nginx/其他软件占用，也可能需要额外权限。临时改端口可使用：

```powershell
$env:npm_config_port=8081
npm.cmd run dev
```

### 6.3 启动验收

后端未启动时只能看到登录页，无法完成系统验收。完整验收至少包括：

1. `/captchaImage` 返回 200；
2. 登录成功并写入 `Admin-Token`；
3. `/getInfo` 返回角色和权限；
4. `/getRouters` 的每个 `component` 都能映射到 `src/views`；
5. 用户、角色、菜单、字典列表可正常查询；
6. 上传、下载、导入、导出正常；
7. 刷新任意深层 URL 不出现 Nginx 404；
8. 普通账号看不到无权限按钮和菜单；
9. token 过期能正确返回 401 并引导重新登录。

## 7. 构建与部署

### 7.1 构建命令

```powershell
# 生产环境，API 前缀 /prod-api
npm.cmd run build:prod

# 预发布环境，API 前缀 /stage-api
npm.cmd run build:stage
```

构建输出固定为根目录 `dist/`。环境变量在编译时写入前端包，修改 `.env.*` 后必须重新构建。

### 7.2 推荐 Nginx 部署

当前路由使用 `history` 模式，必须配置 `try_files ... /index.html`。生产可采用：

```nginx
server {
    listen 80;
    server_name workorder.example.com;
    charset utf-8;

    root /opt/workorder-ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /prod-api/ {
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_pass http://127.0.0.1:8080/;
    }

    location ~ ^/v3/api-docs/(.*) {
        proxy_pass http://127.0.0.1:8080/v3/api-docs/$1;
    }

    location ~* \.(js|css|png|jpg|jpeg|gif|svg|ico|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public";
        try_files $uri =404;
    }

    gzip on;
    gzip_types text/plain text/css application/json application/javascript application/xml image/svg+xml;
}
```

部署步骤：

```bash
npm ci
npm run build:prod
# 将 dist/ 原子化发布到 /opt/workorder-ui/releases/<version>/
# 健康检查通过后再切换 current 软链接或发布目录
nginx -t
nginx -s reload
```

不要直接覆盖正在服务的目录；推荐保留最近 2～3 个版本，以便快速回滚。若前后端跨域部署，需要由后端正确配置 CORS；更推荐同域名下用 `/prod-api` 反向代理，减少 Cookie、CORS 和安全策略问题。

### 7.3 当前缺失的部署工程化

- 无 Dockerfile / docker-compose；
- 无 Nginx 配置文件；
- 无 Jenkins、GitLab CI、GitHub Actions 等流水线；
- 无环境配置模板和密钥管理说明；
- 无版本号、制品校验和、回滚脚本、健康检查；
- 无错误监控、前端日志采集和 Source Map 管理。

因此目前只能手工构建和部署。接手后应优先把现有人工步骤固化为流水线，而不是依赖个人电脑上的 `bin/*.bat`。

## 8. 本次实际验证结果

验证环境：Windows、Node `v22.18.0`、npm `10.9.3`。

| 检查项 | 结果 | 说明 |
|---|---|---|
| `npm ci` | 通过 | 系统 npm 缓存无写权限后，改用项目内缓存完成安装 |
| 依赖数量 | 1477 个包 | 依赖树较大且陈旧 |
| `npm audit` 摘要 | 133 个漏洞 | 12 low / 68 moderate / 42 high / 11 critical |
| `npm run build:prod` | 通过 | 编译有包体积告警，无编译错误 |
| 生产产物 | 约 6.55 MiB | 186 个文件，包含 gzip 预压缩文件 |
| 最大 JS chunk | 约 1.02 MiB | gzip 后约 333 KiB |
| 自动化测试 | 不存在 | 无法验证业务正确性和回归 |
| `npm run preview` | 已确认失败 | 报错 `Cannot find module 'runjs'`；`build/index.js` 引用了它，但 `package.json` 和已安装依赖中均没有它 |

构建成功只代表语法和依赖可以打包，不代表后端联调、权限、菜单或工单流程可用。

## 9. 风险清单与处理优先级

### P0：接手阻断

1. **业务代码疑似缺失**：项目名是工单 UI，但源码只有若依标准壳。
2. **后端和数据库未交付**：动态菜单、权限、登录和所有数据均依赖后端。
3. **没有真实环境配置**：生产文件只有 `/prod-api` 前缀，没有域名、机器或网关信息。
4. **没有 Git 历史**：无法确认来源、分支、改动人、发布版本和回滚点。

### P1：安全与稳定性

1. `npm audit` 存在 11 个 critical 和 42 个 high 漏洞，需逐项评估；不要直接执行会破坏兼容性的 `npm audit fix --force`。
2. Vue 2、Vue CLI 4、Webpack 4、highlight.js 9 等已停止或进入低维护状态。
3. 登录页源码写有默认账号密码 `admin/admin123`，生产应移除默认值并确认默认账号已改密/禁用。
4. “记住密码”把可解密的密码保存 30 天 Cookie，且解密私钥随前端代码下发；这只是混淆，不是安全存储。建议只记住用户名，禁止保存密码。
5. Token 存在 JavaScript 可读 Cookie 中，当前未设置 `HttpOnly`、`Secure`、`SameSite`；需要评估 XSS 和会话保护方案。
6. Swagger、Druid、代码生成、服务器监控、缓存清理等管理能力风险高，应限制权限和网络来源。
7. Axios 全局超时仅 10 秒，导入导出等长耗时接口可能误判超时，需要按接口设置。

### P2：工程质量

1. 无单元测试、端到端测试和 lint 命令。
2. `package-lock.json` 已存在却被 `.gitignore` 忽略，构建不可追溯；应纳入版本控制。
3. `preview` 脚本缺少 `runjs` 直接依赖。
4. `package.json` 的 Node 约束仅为 `>=8.9`，过于宽泛；应增加 `.nvmrc`/`.node-version` 和 `engines` 上界或固定 CI 镜像。
5. 生产包首屏约 1.87 MiB，多个 chunk 超过推荐阈值，需做依赖按需加载和包分析。
6. `vue.config.js` 把开发后端地址硬编码为 localhost，不利于多人和容器开发。

## 10. 建议的接手计划

### 第 1～2 天：恢复全貌

- 找原负责人逐项完成第 5.3 节材料交接；
- 获取正确 Git 仓库和后端仓库，记录当前生产提交号；
- 导出线上/测试环境的菜单表，核对所有 `component` 与当前源码文件；
- 获取一个最低权限测试账号和一个管理员测试账号；
- 画出真实的 DNS → LB/Nginx → 网关/后端 → MySQL/Redis/存储拓扑；
- 在隔离环境完成数据库恢复和前后端联调。

### 第 3～5 天：建立可重复交付

- 固定 Node/npm 版本，提交锁文件；
- 修复 `preview` 脚本或删除无效命令；
- 建立至少包含 install、build、制品归档的 CI；
- 增加 Nginx 配置模板和各环境变量说明；
- 为登录、菜单加载、核心工单流程编写最小 E2E 冒烟测试；
- 建立发布、回滚、数据库变更和故障处理 runbook。

### 第 2 周：安全和技术债

- 对 133 个依赖漏洞做可利用性分析并分批升级；
- 移除默认密码和前端“可逆保存密码”；
- 收紧 Druid、Swagger、代码生成、缓存清理权限；
- 接入错误监控、接口监控、静态资源命中率和发布版本标识；
- 制定 Vue 2 → Vue 3/Vite 的渐进迁移计划，避免一次性重写。

## 11. 交接会议建议问题

业务方面：

- 工单的创建入口、状态机、角色、SLA、催办、转派、关闭和重开规则是什么？
- 哪些流程由代码决定，哪些由数据库字典/菜单/配置决定？
- 是否对接 OA、客服、设备、CRM、消息通知或审批平台？
- 哪些功能是生产正在使用、哪些已经废弃？

技术方面：

- 当前生产前端具体对应哪个 commit/tag？
- 生产后端是哪个应用，是否经过网关？
- `/prod-api` 和 `/stage-api` 在哪里配置转发？
- 文件存在哪里，备份和清理策略是什么？
- 数据库变更用什么工具管理，谁有执行权限？
- 发布窗口、审批人、回滚时间目标和应急联系人是谁？
- 最近三个月有哪些高频故障、慢接口和遗留缺陷？

## 12. 接手完成标准

满足以下条件后，才算真正完成接手：

- 能从干净机器按文档构建前后端；
- 能用脱敏备份恢复开发/测试数据库；
- 能登录并跑通真实工单核心流程；
- 能说明每个外部依赖、域名、端口、账号保管人和告警负责人；
- 能从指定 Git tag 自动生成制品并部署到测试环境；
- 能在 15 分钟内回滚到上一版；
- 有至少一套冒烟测试覆盖登录、菜单和核心工单流程；
- 已确认生产版本、数据备份、监控告警和应急联系人。

## 13. 关键源码定位

- 环境与代理：`.env.development`、`.env.staging`、`.env.production`、`vue.config.js`
- 请求和错误码：`src/utils/request.js`
- Token：`src/utils/auth.js`
- 登录协议：`src/api/login.js`、`src/views/login.vue`
- 固定路由：`src/router/index.js`
- 动态菜单：`src/store/modules/permission.js`、`src/api/menu.js`
- 用户权限：`src/store/modules/user.js`、`src/permission.js`
- API 清单：`src/api/`
- 上传下载：`src/components/FileUpload`、`src/components/ImageUpload`、`src/plugins/download.js`

## 14. 参考资料

- [若依官方环境部署文档](https://doc.ruoyi.vip/ruoyi-vue/document/hjbs.html)
- [RuoYi-Vue v3.9.0 官方发布记录](https://gitee.com/y_project/RuoYi-Vue/releases/tag/v3.9.0)
