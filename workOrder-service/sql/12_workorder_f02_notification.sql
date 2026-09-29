-- F02 工单通知与站内消息增量脚本。依赖 03-11，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 通知模板按事件和渠道唯一；当前只初始化站内信模板，SMS 渠道由后续渠道接入时补充。
CREATE TABLE IF NOT EXISTS wo_notification_template (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '通知模板ID',
  event_code            VARCHAR(64)   NOT NULL COMMENT '通知事件编码',
  channel               VARCHAR(16)   NOT NULL COMMENT '通知渠道：IN_APP/SMS',
  category              VARCHAR(32)   NOT NULL COMMENT '通知分类',
  template_name         VARCHAR(64)   NOT NULL COMMENT '模板名称',
  title_template        VARCHAR(200)  NOT NULL COMMENT '标题模板',
  content_template      VARCHAR(2000) NOT NULL COMMENT '内容模板',
  enabled_flag          CHAR(1)       NOT NULL DEFAULT '1' COMMENT '是否启用：0否 1是',
  max_retry             INT           NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  create_by             VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建者',
  create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by             VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新者',
  update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark                VARCHAR(500)  NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_notification_template_event_channel (event_code, channel),
  CONSTRAINT chk_wo_notification_template_enabled CHECK (enabled_flag IN ('0', '1')),
  CONSTRAINT chk_wo_notification_template_retry CHECK (max_retry >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单通知模板';

-- 通知任务保存投递快照，业务层以 event_key/channel/recipient_id 保证同一事件不重复投递。
CREATE TABLE IF NOT EXISTS wo_notification_task (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '通知任务ID',
  event_key             VARCHAR(128)  NOT NULL COMMENT '业务事件幂等键',
  event_code            VARCHAR(64)   NOT NULL COMMENT '通知事件编码',
  category              VARCHAR(32)   NOT NULL COMMENT '通知分类',
  channel               VARCHAR(16)   NOT NULL COMMENT '通知渠道：IN_APP/SMS',
  recipient_id          BIGINT        NOT NULL COMMENT '接收人用户ID',
  recipient_name        VARCHAR(64)   NULL COMMENT '接收人姓名快照',
  recipient_address     VARCHAR(255)  NULL COMMENT '接收地址快照',
  business_type         VARCHAR(32)   NULL COMMENT '业务类型',
  business_id           BIGINT        NULL COMMENT '业务ID',
  order_no              VARCHAR(32)   NULL COMMENT '工单编号快照',
  title                 VARCHAR(200)  NOT NULL COMMENT '通知标题快照',
  content               VARCHAR(2000) NOT NULL COMMENT '通知内容快照',
  route_path            VARCHAR(255)  NULL COMMENT '站内信跳转路径',
  route_params          JSON          NULL COMMENT '站内信跳转参数',
  status                VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RETRY/SUCCESS/DEAD',
  retry_count           INT           NOT NULL DEFAULT 0 COMMENT '已重试次数',
  max_retry             INT           NOT NULL DEFAULT 5 COMMENT '最大重试次数',
  next_retry_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次可投递时间',
  locked_by             VARCHAR(64)   NULL COMMENT '处理节点标识',
  locked_at             DATETIME      NULL COMMENT '加锁时间',
  last_error            VARCHAR(2000) NULL COMMENT '最近一次失败原因',
  sent_at               DATETIME      NULL COMMENT '成功投递时间',
  create_by             VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '创建者',
  create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by             VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '更新者',
  update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_notification_task_event_recipient (event_key, channel, recipient_id),
  KEY idx_wo_notification_task_due (status, next_retry_at, id),
  KEY idx_wo_notification_task_business (business_type, business_id, order_no, create_time),
  KEY idx_wo_notification_task_recipient (recipient_id, status, create_time),
  CONSTRAINT chk_wo_notification_task_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY', 'SUCCESS', 'DEAD')),
  CONSTRAINT chk_wo_notification_task_retry_count CHECK (retry_count >= 0),
  CONSTRAINT chk_wo_notification_task_max_retry CHECK (max_retry >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单通知投递任务';

-- 站内信是通知任务成功后的用户消息视图；每个任务最多生成一条站内信。
CREATE TABLE IF NOT EXISTS wo_notification_message (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '站内消息ID',
  task_id               BIGINT        NOT NULL COMMENT '通知任务ID',
  recipient_id          BIGINT        NOT NULL COMMENT '接收人用户ID',
  category              VARCHAR(32)   NOT NULL COMMENT '通知分类',
  event_code            VARCHAR(64)   NOT NULL COMMENT '通知事件编码',
  title                 VARCHAR(200)  NOT NULL COMMENT '消息标题',
  content               VARCHAR(2000) NOT NULL COMMENT '消息内容',
  business_type         VARCHAR(32)   NULL COMMENT '业务类型',
  business_id           BIGINT        NULL COMMENT '业务ID',
  order_no              VARCHAR(32)   NULL COMMENT '工单编号快照',
  route_path            VARCHAR(255)  NULL COMMENT '站内信跳转路径',
  route_params          JSON          NULL COMMENT '站内信跳转参数',
  read_flag             CHAR(1)       NOT NULL DEFAULT '0' COMMENT '是否已读：0否 1是',
  read_at               DATETIME      NULL COMMENT '读取时间',
  create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wo_notification_message_task (task_id),
  KEY idx_wo_notification_message_recipient_read_time (recipient_id, read_flag, create_time),
  KEY idx_wo_notification_message_recipient_category_time (recipient_id, category, create_time),
  CONSTRAINT chk_wo_notification_message_read CHECK (read_flag IN ('0', '1'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单站内消息';

-- 初始化站内信模板。模板占位符仅使用 orderNo/title/operatorName/reason/deadline。
INSERT INTO wo_notification_template
  (event_code, channel, category, template_name, title_template, content_template,
   enabled_flag, max_retry, create_by, create_time, update_by, update_time, remark)
SELECT seed.event_code, seed.channel, seed.category, seed.template_name,
       seed.title_template, seed.content_template, seed.enabled_flag, seed.max_retry,
       'admin', NOW(), '', NOW(), seed.remark
FROM (
  SELECT 'SUBMIT' event_code, 'IN_APP' channel, 'WORK_ORDER' category, '工单提交通知' template_name,
         '工单已提交：${orderNo}' title_template,
         '工单“${title}”已提交，等待调度处理。' content_template,
         '1' enabled_flag, 5 max_retry, 'F02工单提交站内信' remark
  UNION ALL SELECT 'ASSIGN', 'IN_APP', 'WORK_ORDER', '工单派单通知',
         '工单已派单：${orderNo}', '工单“${title}”已由${operatorName}分配给你，请及时处理。', '1', 5, 'F02工单派单站内信'
  UNION ALL SELECT 'ACCEPT', 'IN_APP', 'WORK_ORDER', '工单接单通知',
         '维修人员已接单：${orderNo}', '工单“${title}”已由${operatorName}接单。', '1', 5, 'F02工单接单站内信'
  UNION ALL SELECT 'ARRIVE', 'IN_APP', 'WORK_ORDER', '维修人员到场通知',
         '维修人员已到场：${orderNo}', '工单“${title}”的维修人员已到场处理。', '1', 5, 'F02到场站内信'
  UNION ALL SELECT 'PROGRESS', 'IN_APP', 'WORK_ORDER', '工单进度通知',
         '工单进度已更新：${orderNo}', '工单“${title}”有新的处理进度。', '1', 5, 'F02进度站内信'
  UNION ALL SELECT 'REASSIGN_OLD', 'IN_APP', 'WORK_ORDER', '工单改派原处理人通知',
         '工单已改派：${orderNo}', '工单“${title}”已改派给其他维修人员。', '1', 5, 'F02工单改派原处理人站内信'
  UNION ALL SELECT 'REASSIGN_NEW', 'IN_APP', 'WORK_ORDER', '工单改派新处理人通知',
         '工单待处理：${orderNo}', '工单“${title}”已改派给你，请及时处理。', '1', 5, 'F02工单改派新处理人站内信'
  UNION ALL SELECT 'FINISH', 'IN_APP', 'WORK_ORDER', '工单完工通知',
         '工单已完工：${orderNo}', '工单“${title}”已由${operatorName}提交完工。', '1', 5, 'F02工单完工站内信'
  UNION ALL SELECT 'RETURN', 'IN_APP', 'WORK_ORDER', '工单退回通知',
         '工单已退回：${orderNo}', '工单“${title}”被退回，原因：${reason}。', '1', 5, 'F02工单退回站内信'
  UNION ALL SELECT 'CONFIRM', 'IN_APP', 'WORK_ORDER', '完工确认通知',
         '完工已确认：${orderNo}', '工单“${title}”已由报修人确认完成。', '1', 5, 'F02完工确认站内信'
  UNION ALL SELECT 'EVALUATE', 'IN_APP', 'WORK_ORDER', '工单评价通知',
         '收到工单评价：${orderNo}', '工单“${title}”已收到报修人评价。', '1', 5, 'F02评价站内信'
  UNION ALL SELECT 'CANCEL', 'IN_APP', 'WORK_ORDER', '工单取消通知',
         '工单已取消：${orderNo}', '工单“${title}”已取消，原因：${reason}。', '1', 5, 'F02取消站内信'
  UNION ALL SELECT 'DELAY_REQUEST', 'IN_APP', 'APPROVAL', '延期申请通知',
         '工单延期待审批：${orderNo}', '工单“${title}”申请延期至${deadline}，原因：${reason}。', '1', 5, 'F02延期申请站内信'
  UNION ALL SELECT 'DELAY_APPROVED', 'IN_APP', 'APPROVAL', '延期通过通知',
         '工单延期已通过：${orderNo}', '工单“${title}”已批准延期至${deadline}。', '1', 5, 'F02延期通过站内信'
  UNION ALL SELECT 'DELAY_REJECTED', 'IN_APP', 'APPROVAL', '延期驳回通知',
         '工单延期未通过：${orderNo}', '工单“${title}”延期申请未通过，原因：${reason}。', '1', 5, 'F02延期驳回站内信'
  UNION ALL SELECT 'SLA_WARNING', 'IN_APP', 'SYSTEM', 'SLA预警通知',
         '工单即将超时：${orderNo}', '工单“${title}”的处理期限为${deadline}，请及时处理。', '1', 5, 'F02 SLA预警站内信'
  UNION ALL SELECT 'SLA_OVERDUE', 'IN_APP', 'SYSTEM', 'SLA超时通知',
         '工单已超时：${orderNo}', '工单“${title}”已超过处理期限${deadline}。', '1', 5, 'F02 SLA超时站内信'
  UNION ALL SELECT 'AUTO_CLOSE', 'IN_APP', 'SYSTEM', '工单自动关闭通知',
         '工单已自动关闭：${orderNo}', '工单“${title}”在完成后超过评价窗口，已自动关闭。', '1', 5, 'F02工单自动关闭站内信'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM wo_notification_template existing
  WHERE existing.event_code = seed.event_code AND existing.channel = seed.channel
);

-- 只创建 F 类型权限，不创建 F02/P09 页面菜单；页面菜单由后续页面增量另行处理。
SET @wo_root_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = 0 AND path = 'workorder' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache,
   menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT seed.menu_name, @wo_root_menu_id, seed.order_num, '', NULL, NULL, '',
       1, 0, 'F', '0', '0', seed.perms, '#', 'admin', NOW(), '', NULL, seed.remark
FROM (
  SELECT '消息列表' menu_name, 20 order_num, 'workorder:notification:list' perms, 'F02消息列表权限' remark
  UNION ALL SELECT '读取消息', 21, 'workorder:notification:read', 'F02消息读取权限'
  UNION ALL SELECT '发送记录', 22, 'workorder:message:record', 'F02通知发送记录权限'
  UNION ALL SELECT '重试消息', 23, 'workorder:message:retry', 'F02通知任务重试权限'
) seed
WHERE @wo_root_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.perms = seed.perms AND existing.menu_type = 'F'
  );

-- 严格按 role_key 查找三类工单角色；三类角色均可查看消息，只有 dispatcher 可管理发送记录和重试。
SET @wo_reporter_role_id := (
  SELECT role_id FROM sys_role
  WHERE role_key = 'workorder_reporter' AND del_flag = '0'
  ORDER BY role_id LIMIT 1
);
SET @wo_engineer_role_id := (
  SELECT role_id FROM sys_role
  WHERE role_key = 'workorder_engineer' AND del_flag = '0'
  ORDER BY role_id LIMIT 1
);
SET @wo_dispatcher_role_id := (
  SELECT role_id FROM sys_role
  WHERE role_key = 'workorder_dispatcher' AND del_flag = '0'
  ORDER BY role_id LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT grants.role_id, menu.menu_id
FROM (
  SELECT @wo_reporter_role_id role_id, 'workorder:notification:list' perms
  UNION ALL SELECT @wo_reporter_role_id, 'workorder:notification:read'
  UNION ALL SELECT @wo_engineer_role_id, 'workorder:notification:list'
  UNION ALL SELECT @wo_engineer_role_id, 'workorder:notification:read'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:notification:list'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:notification:read'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:message:record'
  UNION ALL SELECT @wo_dispatcher_role_id, 'workorder:message:retry'
) grants
JOIN sys_menu menu ON menu.perms = grants.perms AND menu.menu_type = 'F'
WHERE grants.role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = grants.role_id AND existing.menu_id = menu.menu_id
  );

-- sys_job 来自 ry_20250522.sql：job_id 自增，以下为其余字段的显式列清单。
-- 任务禁止并发，定期派发 PENDING/RETRY 通知任务。
INSERT INTO sys_job
  (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
   create_by, create_time, update_by, update_time, remark)
SELECT '工单通知任务派发', 'WORKORDER', 'workOrderNotificationTask.dispatch', '0/10 * * * * ?',
       '3', '1', '0', 'admin', NOW(), '', NULL, '每10秒派发到期的工单通知任务'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_job
  WHERE job_group = 'WORKORDER' AND invoke_target = 'workOrderNotificationTask.dispatch'
);
