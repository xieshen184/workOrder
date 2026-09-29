-- F01 延期、SLA 与自动关闭。依赖 03-10，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @schema_name = DATABASE();

-- 工单必须快照预警提前量和延期许可，后续修改规则不得改变历史工单口径。
SET @column_count = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wo_order' AND COLUMN_NAME = 'sla_reminder_before_min');
SET @ddl = IF(@column_count = 0,
  'ALTER TABLE wo_order ADD COLUMN sla_reminder_before_min INT NOT NULL DEFAULT 15 COMMENT ''SLA预警提前分钟数快照'' AFTER finish_deadline',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_count = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wo_order' AND COLUMN_NAME = 'sla_allow_extension');
SET @ddl = IF(@column_count = 0,
  'ALTER TABLE wo_order ADD COLUMN sla_allow_extension CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''是否允许延期快照：0否 1是'' AFTER sla_reminder_before_min',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE wo_order o
JOIN wo_sla_rule r ON r.id = o.sla_rule_id
SET o.sla_reminder_before_min = r.reminder_before_min,
    o.sla_allow_extension = r.allow_extension
WHERE o.del_flag = '0';

CREATE TABLE IF NOT EXISTS wo_delay_request (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '延期申请ID',
  order_id              BIGINT        NOT NULL COMMENT '工单ID',
  order_no              VARCHAR(32)   NOT NULL COMMENT '工单编号快照',
  order_title           VARCHAR(128)  NOT NULL COMMENT '工单标题快照',
  engineer_id           BIGINT        NOT NULL COMMENT '申请维修人员ID',
  engineer_name         VARCHAR(64)   NOT NULL COMMENT '申请维修人员姓名快照',
  original_deadline     DATETIME      NOT NULL COMMENT '申请时有效完成截止时间',
  requested_deadline    DATETIME      NOT NULL COMMENT '申请的新截止时间',
  request_reason        VARCHAR(500)  NOT NULL COMMENT '延期原因',
  status                VARCHAR(16)   NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
  decided_by            BIGINT        NULL COMMENT '审批人ID',
  decided_by_name       VARCHAR(64)   NULL COMMENT '审批人姓名快照',
  decided_at            DATETIME      NULL COMMENT '审批时间',
  decision_reason       VARCHAR(500)  NULL COMMENT '审批说明或拒绝原因',
  create_time           DATETIME      NOT NULL COMMENT '申请时间',
  update_time           DATETIME      NULL COMMENT '更新时间',
  pending_order_id      BIGINT GENERATED ALWAYS AS
                         (CASE WHEN status = 'PENDING' THEN order_id ELSE NULL END) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_delay_pending_order (pending_order_id),
  KEY idx_wo_delay_order (order_id, create_time),
  KEY idx_wo_delay_status (status, create_time),
  KEY idx_wo_delay_engineer (engineer_id, create_time),
  CONSTRAINT chk_wo_delay_deadline CHECK (requested_deadline > original_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单延期申请';

-- 新增 F01 页面与按钮权限。
SET @wo_root_menu_id := (
  SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'workorder' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '延期审批', @wo_root_menu_id, 4, 'delay-requests', 'workorder/delay/index', NULL,
       'WorkorderDelayRequests', 1, 0, 'C', '0', '0', '', 'time', 'admin', NOW(), '', NULL, 'F01延期审批'
WHERE @wo_root_menu_id IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = @wo_root_menu_id AND path = 'delay-requests' AND menu_type = 'C'
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 'SLA规则', @wo_root_menu_id, 5, 'sla-rules', 'workorder/sla/index', NULL,
       'WorkorderSlaRules', 1, 0, 'C', '0', '0', '', 'timer', 'admin', NOW(), '', NULL, 'F01 SLA规则配置'
WHERE @wo_root_menu_id IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE parent_id = @wo_root_menu_id AND path = 'sla-rules' AND menu_type = 'C'
);

SET @wo_order_menu_id := (SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'orders' AND menu_type = 'C' ORDER BY menu_id LIMIT 1);
SET @wo_delay_menu_id := (SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'delay-requests' AND menu_type = 'C' ORDER BY menu_id LIMIT 1);
SET @wo_sla_menu_id := (SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'sla-rules' AND menu_type = 'C' ORDER BY menu_id LIMIT 1);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT seed.menu_name, seed.parent_id, seed.order_num, '', NULL, NULL, '', 1, 0, 'F', '0', '0',
       seed.perms, '#', 'admin', NOW(), '', NULL, seed.remark
FROM (
  SELECT '申请延期' menu_name, @wo_order_menu_id parent_id, 19 order_num, 'workorder:delay:add' perms, '当前维修人员申请延期' remark
  UNION ALL SELECT '延期详情', @wo_delay_menu_id, 1, 'workorder:delay:query', '查看可见工单的延期申请'
  UNION ALL SELECT '延期列表', @wo_delay_menu_id, 2, 'workorder:delay:list', '调度管理员查询授权范围延期申请'
  UNION ALL SELECT '延期审批', @wo_delay_menu_id, 3, 'workorder:delay:approve', '调度管理员审批延期申请'
  UNION ALL SELECT 'SLA列表', @wo_sla_menu_id, 1, 'workorder:sla:list', '查询SLA规则'
  UNION ALL SELECT 'SLA详情', @wo_sla_menu_id, 2, 'workorder:sla:query', '查看SLA规则详情'
  UNION ALL SELECT '新增SLA', @wo_sla_menu_id, 3, 'workorder:sla:add', '新增SLA规则'
  UNION ALL SELECT '编辑SLA', @wo_sla_menu_id, 4, 'workorder:sla:edit', '编辑或启停SLA规则'
) seed
WHERE seed.parent_id IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM sys_menu existing WHERE existing.perms = seed.perms AND existing.menu_type = 'F'
);

SET @wo_reporter_role_id := (SELECT role_id FROM sys_role WHERE role_key = 'workorder_reporter' AND del_flag = '0' ORDER BY role_id LIMIT 1);
SET @wo_engineer_role_id := (SELECT role_id FROM sys_role WHERE role_key = 'workorder_engineer' AND del_flag = '0' ORDER BY role_id LIMIT 1);
SET @wo_dispatcher_role_id := (SELECT role_id FROM sys_role WHERE role_key = 'workorder_dispatcher' AND del_flag = '0' ORDER BY role_id LIMIT 1);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT grants.role_id, menu.menu_id
FROM (
  SELECT @wo_reporter_role_id role_id, 'workorder:delay:query' perms
  UNION ALL SELECT @wo_engineer_role_id, 'workorder:delay:add'
  UNION ALL SELECT @wo_engineer_role_id, 'workorder:delay:query'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:delay:query'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:delay:list'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:delay:approve'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:sla:list'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:sla:query'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:sla:add'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:sla:edit'
) grants
JOIN sys_menu menu ON menu.perms = grants.perms AND menu.menu_type = 'F'
WHERE grants.role_id IS NOT NULL AND NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.role_id = grants.role_id AND existing.menu_id = menu.menu_id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_dispatcher_role_id, menu.menu_id FROM sys_menu menu
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND menu.menu_type = 'C' AND menu.component IN ('workorder/delay/index', 'workorder/sla/index')
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = @wo_dispatcher_role_id AND existing.menu_id = menu.menu_id);

-- Quartz 调用适配器只负责进入 SLA 生命周期模块，禁止并发执行同一任务。
INSERT INTO sys_job
  (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
   create_by, create_time, update_by, update_time, remark)
SELECT '工单SLA预警与超时扫描', 'WORKORDER', 'workOrderSlaTask.scan', '0 0/1 * * * ?', '3', '1', '0',
       'admin', NOW(), '', NULL, '每分钟刷新正交SLA标志并记录首次预警/超时审计'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_group = 'WORKORDER' AND invoke_target = 'workOrderSlaTask.scan');

INSERT INTO sys_job
  (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
   create_by, create_time, update_by, update_time, remark)
SELECT '工单评价窗口自动关闭', 'WORKORDER', 'workOrderSlaTask.autoClose', '0 15 2 * * ?', '3', '1', '0',
       'admin', NOW(), '', NULL, '每天扫描超过评价窗口仍未评价的已完成工单'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_group = 'WORKORDER' AND invoke_target = 'workOrderSlaTask.autoClose');
