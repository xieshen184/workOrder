-- 工单稳定字典初始化（依赖 01-ruoyi.sql）
-- 使用业务键判重，不依赖自增ID，可重复执行。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '工单状态', 'wo_order_status', '0', 'admin', NOW(), '', NULL, '工单主状态机稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_order_status');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '工单紧急程度', 'wo_urgency_level', '0', 'admin', NOW(), '', NULL, '工单紧急程度稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_urgency_level');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '工单影响范围', 'wo_impact_scope', '0', 'admin', NOW(), '', NULL, '工单影响范围稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_impact_scope');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '工单来源', 'wo_source_type', '0', 'admin', NOW(), '', NULL, '工单来源稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_source_type');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '派单状态', 'wo_assignment_status', '0', 'admin', NOW(), '', NULL, '派单历史状态稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_assignment_status');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '处理阶段', 'wo_process_stage', '0', 'admin', NOW(), '', NULL, '维修处理阶段稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_process_stage');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '维修人员状态', 'wo_engineer_status', '0', 'admin', NOW(), '', NULL, '人员调度展示状态稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_engineer_status');

INSERT INTO sys_dict_type
  (dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
SELECT '工单附件阶段', 'wo_attachment_stage', '0', 'admin', NOW(), '', NULL, '附件所属业务阶段稳定字典'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'wo_attachment_stage');

INSERT INTO sys_dict_data
  (dict_sort, dict_label, dict_value, dict_type, css_class, list_class,
   is_default, status, create_by, create_time, update_by, update_time, remark)
SELECT seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       seed.css_class, seed.list_class, seed.is_default, '0',
       'admin', NOW(), '', NULL, seed.remark
FROM (
  SELECT 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value, 'wo_order_status' dict_type, '' css_class, 'info' list_class, 'N' is_default, '未提交的内部状态' remark
  UNION ALL SELECT 2, '待派单', 'WAIT_ASSIGN', 'wo_order_status', '', 'warning', 'N', '已提交，等待调度派单'
  UNION ALL SELECT 3, '待接单', 'WAIT_ACCEPT', 'wo_order_status', '', 'warning', 'N', '已派单，等待维修人员接单'
  UNION ALL SELECT 4, '已接单', 'ACCEPTED', 'wo_order_status', '', 'primary', 'N', '已接单，等待到场'
  UNION ALL SELECT 5, '处理中', 'PROCESSING', 'wo_order_status', '', 'primary', 'N', '维修处理中'
  UNION ALL SELECT 6, '待确认', 'WAIT_CONFIRM', 'wo_order_status', '', 'warning', 'N', '已完工，等待报修人确认'
  UNION ALL SELECT 7, '已完成', 'COMPLETED', 'wo_order_status', '', 'success', 'N', '已确认，等待评价'
  UNION ALL SELECT 8, '已关闭', 'CLOSED', 'wo_order_status', '', 'info', 'N', '已评价并关闭'
  UNION ALL SELECT 9, '已取消', 'CANCELLED', 'wo_order_status', '', 'danger', 'N', '派单前取消'

  UNION ALL SELECT 1, '一般', '1', 'wo_urgency_level', '', 'info', 'Y', '一般工单'
  UNION ALL SELECT 2, '紧急', '2', 'wo_urgency_level', '', 'warning', 'N', '紧急工单'
  UNION ALL SELECT 3, '特急', '3', 'wo_urgency_level', '', 'danger', 'N', '特急工单'

  UNION ALL SELECT 1, '个人', '1', 'wo_impact_scope', '', 'info', 'Y', '影响个人'
  UNION ALL SELECT 2, '科室', '2', 'wo_impact_scope', '', 'primary', 'N', '影响单个科室'
  UNION ALL SELECT 3, '多科室', '3', 'wo_impact_scope', '', 'warning', 'N', '影响多个科室'
  UNION ALL SELECT 4, '全院', '4', 'wo_impact_scope', '', 'danger', 'N', '影响全院'

  UNION ALL SELECT 1, '微信小程序', '1', 'wo_source_type', '', 'primary', 'Y', '由微信小程序提交'
  UNION ALL SELECT 2, 'PC端', '2', 'wo_source_type', '', 'info', 'N', '由PC管理端提交'
  UNION ALL SELECT 3, '接口导入', '3', 'wo_source_type', '', 'warning', 'N', '由外部接口导入'

  UNION ALL SELECT 1, '生效中', 'ACTIVE', 'wo_assignment_status', '', 'primary', 'N', '当前生效的派单'
  UNION ALL SELECT 2, '已接单', 'ACCEPTED', 'wo_assignment_status', '', 'success', 'N', '维修人员已接单'
  UNION ALL SELECT 3, '已改派', 'REASSIGNED', 'wo_assignment_status', '', 'warning', 'N', '该派单已被改派替代'
  UNION ALL SELECT 4, '已退回', 'RETURNED', 'wo_assignment_status', '', 'danger', 'N', '该派单已退回'
  UNION ALL SELECT 5, '已取消', 'CANCELLED', 'wo_assignment_status', '', 'info', 'N', '该派单已取消'

  UNION ALL SELECT 1, '到场', 'ARRIVAL', 'wo_process_stage', '', 'primary', 'N', '到场确认记录'
  UNION ALL SELECT 2, '故障评估', 'ASSESSMENT', 'wo_process_stage', '', 'warning', 'N', '故障评估记录'
  UNION ALL SELECT 3, '处理进度', 'PROGRESS', 'wo_process_stage', '', 'primary', 'N', '处理进度记录'
  UNION ALL SELECT 4, '完工', 'FINISH', 'wo_process_stage', '', 'success', 'N', '完工记录'

  UNION ALL SELECT 1, '未值班', 'OFF_DUTY', 'wo_engineer_status', '', 'info', 'N', '当前未值班'
  UNION ALL SELECT 2, '空闲', 'AVAILABLE', 'wo_engineer_status', '', 'success', 'N', '值班且可接单'
  UNION ALL SELECT 3, '工作中', 'WORKING', 'wo_engineer_status', '', 'primary', 'N', '正在处理工单'
  UNION ALL SELECT 4, '繁忙', 'BUSY', 'wo_engineer_status', '', 'warning', 'N', '负载较高'
  UNION ALL SELECT 5, '离线', 'OFFLINE', 'wo_engineer_status', '', 'danger', 'N', '心跳超时或不可用'

  UNION ALL SELECT 1, '报修提交', 'SUBMIT', 'wo_attachment_stage', '', 'info', 'N', '报修提交附件'
  UNION ALL SELECT 2, '到场确认', 'ARRIVAL', 'wo_attachment_stage', '', 'primary', 'N', '到场证据附件'
  UNION ALL SELECT 3, '处理过程', 'PROCESS', 'wo_attachment_stage', '', 'primary', 'N', '处理过程附件'
  UNION ALL SELECT 4, '完工提交', 'FINISH', 'wo_attachment_stage', '', 'success', 'N', '完工证据附件'
  UNION ALL SELECT 5, '服务评价', 'EVALUATION', 'wo_attachment_stage', '', 'info', 'N', '评价附件'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data existing
  WHERE existing.dict_type = seed.dict_type
    AND existing.dict_value = seed.dict_value
);
