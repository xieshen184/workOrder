# ruoyi-workorder

工单业务模块负责工单聚合、状态迁移、权限与归属校验、幂等、审计副产物和持久化协调。

## 模块边界

- 对外入口：后续由命令处理和查询接口提供，Controller 只做协议转换与权限注解。
- 状态事实：所有主状态变化必须经过 `WorkOrderStateMachine`。
- 上下文规则：角色、当前处理人、附件、SLA 和版本号由命令处理层校验。
- 基础依赖：模块只依赖 `ruoyi-common`，不依赖 `ruoyi-admin` 或 `ruoyi-framework`。
- 外部能力：文件和通知后续通过适配器接入，领域代码不直接依赖供应商实现。

## 第一版状态范围

```text
DRAFT -> WAIT_ASSIGN -> WAIT_ACCEPT -> ACCEPTED -> PROCESSING
      -> WAIT_CONFIRM -> COMPLETED -> CLOSED
```

同时支持待派单取消、允许状态下改派和待确认退回。延期审批和自动关闭保留动作编码，但第一版状态机不开放迁移。

## 验证

```powershell
mvn.cmd -pl ruoyi-workorder -am test
mvn.cmd -pl ruoyi-admin -am package
```
