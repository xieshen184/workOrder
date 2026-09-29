# F04 PC 配置、消息、统计与绩效节点验收清单

## 交付范围

本节点完成 P01 运营驾驶舱、P08 维修绩效、P09 消息模板与发送记录、P10 短信账户四个 PC 页面，以及对应后端接口、聚合查询、菜单权限、数据库增量和自动化测试。沿用“开发完成、联调暂缓”的当前节奏，不包含真实短信供应商接入、MySQL 实库迁移执行、浏览器接口联调和客户数据验收。

## 页面闭环

| 页面 | 已完成能力 | 操作闭环 |
|---|---|---|
| 运营驾驶舱 | 今日新增、待派单、处理中、SLA 风险、趋势、分类占比、风险清单、短信低余额告警 | 指标卡携带真实筛选进入工单列表；风险行按工单号定位；短信告警进入短信账户 |
| 维修绩效 | 日期、组织、工程师、分类筛选；六项指标；工程师明细；样本数；CSV 导出 | 点击工程师明细后按该人员重新统计；导出内容与当前成功快照一致 |
| 消息管理 | 模板筛选、详情、变量目录、保存前预览、启停、发送记录、失败重试 | 未预览不能保存；模板变化后必须重新预览；订单号进入工单列表；失败任务重新进入发送队列 |
| 短信账户 | 脱敏账户信息、余额与发送统计、余额预警、失败重试设置、刷新 | 未配置时不展示虚构余额；不能启用重试；刷新失败保留最后成功快照；设置保存前二次确认 |

## 后端接口

| 方法 | 地址 | 权限 | 结果 |
|---|---|---|---|
| GET | `/workorder/dashboard` | `workorder:dashboard:view` | 当前指标、区间趋势、分类占比、SLA 风险和运营告警 |
| GET | `/workorder/performance` | `workorder:performance:view` | 同一筛选样本下的绩效指标与工程师明细 |
| GET | `/workorder/notification-templates` | `workorder:message:template` | 模板分页列表 |
| GET | `/workorder/notification-templates/{id}` | `workorder:message:template` | 模板详情，事件和渠道只读 |
| POST | `/workorder/notification-templates/preview` | `workorder:message:template` | 使用通用示例值生成标题、正文和变量目录 |
| PUT | `/workorder/notification-templates/{id}` | `workorder:message:template` | 保存已登记模板的可编辑字段 |
| GET | `/workorder/notification-tasks` | `workorder:message:record` | 发送记录分页列表，接收地址脱敏 |
| PUT | `/workorder/notification-tasks/{id}/retry` | `workorder:message:retry` | 仅允许重试中或已终止任务重新入队 |
| GET | `/workorder/sms-account` | `workorder:sms:view` | 脱敏账户快照和真实任务统计 |
| PUT | `/workorder/sms-account/settings` | `workorder:sms:edit` | 更新余额阈值和失败重试策略 |
| PUT | `/workorder/sms-account/refresh` | `workorder:sms:refresh` | 未配置供应商时明确拒绝，不修改最后成功快照 |

## 统计口径

| 指标 | 口径 |
|---|---|
| 今日新增 | 当前自然日创建的授权范围工单数 |
| 待派单 | 当前状态为 `WAIT_ASSIGN` 的授权范围工单数 |
| 处理中 | 当前状态为 `PROCESSING` 的授权范围工单数 |
| SLA 风险 | `warning_flag=1` 或 `overdue_flag=1` 且未关闭、未取消的工单 |
| 完成量 | 统计区间内创建且当前进入 `COMPLETED/CLOSED` 的工单数 |
| 平均响应时长 | 最近分派时间到接单时间的平均分钟数 |
| 平均到场时长 | 接单时间到到场时间的平均分钟数 |
| 按时完成率 | 具有完工时间及有效完工截止时间的样本中，未超期完工的占比 |
| 满意度 | 已提交评价样本的总体评分平均值 |
| 返工率 | 已完成样本中存在 `RETURN` 动作的工单占比 |

所有统计查询复用工单列表的报修人和报修科室数据范围。无样本的比率和平均值返回空值，前端显示 `—`，不使用零值代替未知结果。

## 状态与安全控制

- 驾驶舱和绩效页只有在接口成功后才替换当前快照；刷新失败继续展示上一次成功数据并显示警告。
- 单次统计区间最长 366 天，防止误操作触发无边界聚合。
- 模板事件和渠道不可通过管理接口修改；模板名称、标题、正文、备注和重试次数均执行长度或范围校验。
- 模板只接受 `${orderNo}`、`${title}`、`${operatorName}`、`${reason}`、`${deadline}`；未知或残缺变量在预览和保存前拒绝。
- 短信表不含供应商明文密钥，只保存不可逆脱敏标识、运营设置和最后成功余额快照。
- 未配置短信供应商时，余额保持空值、账户状态为 `UNCONFIGURED`，并禁止开启失败自动重试。
- PC 菜单集中在“工单管理 / 运营管理”，调度管理员获得驾驶舱、绩效、模板、记录和短信运营权限。

## 数据库交付

- 新增 `13_workorder_f04_operations.sql`，可重复创建 `wo_sms_account_setting`、运营目录、页面菜单和按钮权限。
- `docker-compose.yml` 已按 03 至 13 的顺序挂载初始化脚本。
- 消息模板和发送任务继续复用 F02 的 `wo_notification_template`、`wo_notification_task`、`wo_notification_message`，未创建重复消息表。
- 驾驶舱与绩效使用现有工单、评价和动作日志实时聚合，未创建容易失真的重复统计表。

## 自动化验证

| 验证项 | 结果 |
|---|---|
| 工单模块与后台模块 Maven 测试 | 通过 |
| 统计 Mapper 构建期解析与数据范围断言 | 通过 |
| 短信账户 Mapper 构建期解析 | 通过 |
| F04 SQL 菜单、权限、脱敏字段和 Compose 挂载契约 | 通过 |
| PC `npm run build:prod` | 通过，有既有资源体积和 Browserslist 数据陈旧警告 |

## 联调前置条件

1. 在测试库按顺序执行 01 至 13 脚本，确认菜单、角色授权和短信账户初始化行。
2. 使用调度管理员分别访问四个页面，核对数据范围与工单列表一致。
3. 准备跨状态、跨科室、含评价、含退回和含 SLA 超时的测试工单，核对聚合结果。
4. 短信供应商确定后，单独补充真实余额查询和发送适配器；在此之前保持短信模板未启用。
5. 完成浏览器接口联调、数据库执行验证和回归后，再进入客户验收。

## 节点结论

F04 的代码、页面、数据库增量和自动化验证已经完成。真实数据库、真实登录权限、浏览器接口和短信供应商仍属于后续联调事项，不以模拟成功替代。
