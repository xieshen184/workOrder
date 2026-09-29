-- F04 PC配置、消息、统计与绩效增量脚本。依赖 03-12，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 只保存短信账户的脱敏展示信息、运营设置和最后成功快照，供应商密钥由部署环境持有。
CREATE TABLE IF NOT EXISTS wo_sms_account_setting (
  id                    BIGINT         NOT NULL COMMENT '固定主键，当前仅使用1',
  configured_flag       CHAR(1)        NOT NULL DEFAULT '0' COMMENT '供应商是否已配置：0否 1是',
  provider_code         VARCHAR(32)    NULL COMMENT '供应商编码',
  provider_name         VARCHAR(64)    NULL COMMENT '供应商名称',
  account_alias         VARCHAR(64)    NULL COMMENT '账户别名',
  credential_mask       VARCHAR(128)   NULL COMMENT '不可逆脱敏后的凭据标识',
  account_status        VARCHAR(24)    NOT NULL DEFAULT 'UNCONFIGURED' COMMENT 'UNCONFIGURED/NORMAL/WARNING/UNAVAILABLE',
  available_balance     DECIMAL(14,2)  NULL COMMENT '最近一次成功查询的可用余额',
  warning_threshold     DECIMAL(14,2)  NOT NULL DEFAULT 100.00 COMMENT '余额预警阈值',
  retry_enabled         CHAR(1)        NOT NULL DEFAULT '0' COMMENT '短信失败自动重试：0关闭 1开启',
  last_success_time     DATETIME       NULL COMMENT '最近一次余额查询成功时间',
  last_error            VARCHAR(500)   NULL COMMENT '最近一次刷新失败摘要，不含密钥和完整请求',
  create_by             VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '创建者',
  create_time           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by             VARCHAR(64)    NOT NULL DEFAULT '' COMMENT '更新者',
  update_time           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  CONSTRAINT chk_wo_sms_configured CHECK (configured_flag IN ('0','1')),
  CONSTRAINT chk_wo_sms_retry CHECK (retry_enabled IN ('0','1')),
  CONSTRAINT chk_wo_sms_threshold CHECK (warning_threshold >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信账户脱敏快照与运营设置';

INSERT INTO wo_sms_account_setting
  (id, configured_flag, account_status, warning_threshold, retry_enabled, create_by, create_time)
SELECT 1, '0', 'UNCONFIGURED', 100.00, '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM wo_sms_account_setting WHERE id = 1);

SET @wo_root_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = 0 AND path = 'workorder' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

-- 将 F04 页面集中到运营管理目录，避免继续把所有页面平铺在工单根目录下。
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '运营管理', @wo_root_menu_id, 20, 'operations', NULL, NULL, 'WorkorderOperations',
       1, 0, 'M', '0', '0', '', 'dashboard',
       'admin', NOW(), '', NULL, 'F04运营页面目录'
WHERE @wo_root_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @wo_root_menu_id AND path = 'operations' AND menu_type = 'M'
  );

SET @wo_operations_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'operations' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT seed.menu_name, @wo_operations_menu_id, seed.order_num, seed.path, seed.component, NULL, seed.route_name,
       1, 0, 'C', '0', '0', seed.perms, seed.icon,
       'admin', NOW(), '', NULL, seed.remark
FROM (
  SELECT '维修绩效' menu_name, 1 order_num, 'performance' path, 'workorder/performance/index' component,
         'WorkorderPerformance' route_name, 'workorder:performance:view' perms, 'chart' icon, 'F04维修绩效页面' remark
  UNION ALL SELECT '消息管理', 2, 'messages', 'workorder/message/index',
         'WorkorderMessages', 'workorder:message:template', 'message', 'F04消息模板与发送记录页面'
  UNION ALL SELECT '短信账户', 3, 'sms', 'workorder/sms/index',
         'WorkorderSms', 'workorder:sms:view', 'phone', 'F04短信账户页面'
) seed
WHERE @wo_operations_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.parent_id = @wo_operations_menu_id AND existing.path = seed.path AND existing.menu_type = 'C'
  );

SET @wo_message_menu_id := (
  SELECT menu_id FROM sys_menu WHERE parent_id = @wo_operations_menu_id AND path = 'messages' AND menu_type = 'C'
  ORDER BY menu_id LIMIT 1
);
SET @wo_sms_menu_id := (
  SELECT menu_id FROM sys_menu WHERE parent_id = @wo_operations_menu_id AND path = 'sms' AND menu_type = 'C'
  ORDER BY menu_id LIMIT 1
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT seed.menu_name, seed.parent_id, seed.order_num, '', NULL, NULL, '',
       1, 0, 'F', '0', '0', seed.perms, '#',
       'admin', NOW(), '', NULL, seed.remark
FROM (
  SELECT '运营驾驶舱' menu_name, @wo_root_menu_id parent_id, 30 order_num,
         'workorder:dashboard:view' perms, 'F04首页驾驶舱查询权限' remark
  UNION ALL SELECT '发送记录', @wo_message_menu_id, 1, 'workorder:message:record', 'F04通知发送记录查询权限'
  UNION ALL SELECT '重试消息', @wo_message_menu_id, 2, 'workorder:message:retry', 'F04通知任务人工重试权限'
  UNION ALL SELECT '编辑短信设置', @wo_sms_menu_id, 1, 'workorder:sms:edit', 'F04短信运营设置权限'
  UNION ALL SELECT '刷新短信账户', @wo_sms_menu_id, 2, 'workorder:sms:refresh', 'F04短信余额刷新权限'
) seed
WHERE seed.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms AND existing.menu_type = 'F'
  );

-- 调度管理员承担运营管理职责；admin 角色仍使用若依内置的超级管理员授权。
SET @wo_dispatcher_role_id := (
  SELECT role_id FROM sys_role WHERE role_key = 'workorder_dispatcher' AND del_flag = '0'
  ORDER BY role_id LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_dispatcher_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND (menu.menu_id = @wo_operations_menu_id
       OR menu.parent_id = @wo_operations_menu_id
       OR menu.perms IN ('workorder:dashboard:view','workorder:message:record','workorder:message:retry',
                         'workorder:sms:edit','workorder:sms:refresh'))
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = @wo_dispatcher_role_id AND existing.menu_id = menu.menu_id
  );
