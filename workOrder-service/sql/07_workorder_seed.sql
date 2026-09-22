-- M0/M1 基础分类与无歧义全局 SLA 初始化
-- 每个 urgency_level 最多补一条全局兜底；分类规则后续可使用更高 priority 覆盖。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO wo_category
  (parent_id, category_code, category_name, manager_dept_id, default_sla_rule_id,
   order_num, status, create_by, create_time, update_by, update_time, remark, del_flag)
SELECT 0, 'LOGISTICS', '后勤维护', NULL, NULL,
       1, '0', 'admin', NOW(), '', NULL, 'M1基础分类', '0'
WHERE NOT EXISTS (SELECT 1 FROM wo_category WHERE category_code = 'LOGISTICS');

INSERT INTO wo_category
  (parent_id, category_code, category_name, manager_dept_id, default_sla_rule_id,
   order_num, status, create_by, create_time, update_by, update_time, remark, del_flag)
SELECT 0, 'EQUIPMENT', '设备维护', NULL, NULL,
       2, '0', 'admin', NOW(), '', NULL, 'M1基础分类', '0'
WHERE NOT EXISTS (SELECT 1 FROM wo_category WHERE category_code = 'EQUIPMENT');

INSERT INTO wo_category
  (parent_id, category_code, category_name, manager_dept_id, default_sla_rule_id,
   order_num, status, create_by, create_time, update_by, update_time, remark, del_flag)
SELECT 0, 'IT', '信息化维护', NULL, NULL,
       3, '0', 'admin', NOW(), '', NULL, 'M1基础分类', '0'
WHERE NOT EXISTS (SELECT 1 FROM wo_category WHERE category_code = 'IT');

INSERT INTO wo_sla_rule
  (rule_code, rule_name, category_id, urgency_level, priority,
   effective_from, effective_to, response_minutes, arrival_minutes, finish_minutes,
   reminder_before_min, allow_extension, status, create_by, create_time,
   update_by, update_time, remark, del_flag)
SELECT 'SLA_GLOBAL_NORMAL', '一般工单全局兜底', NULL, 1, 100,
       '2026-01-01 00:00:00', NULL, 360, 360, 1440,
       60, '1', '0', 'admin', NOW(), '', NULL,
       '全局兜底；分类规则应使用高于100的优先级', '0'
WHERE NOT EXISTS (
  SELECT 1 FROM wo_sla_rule WHERE category_id IS NULL AND urgency_level = 1
);

INSERT INTO wo_sla_rule
  (rule_code, rule_name, category_id, urgency_level, priority,
   effective_from, effective_to, response_minutes, arrival_minutes, finish_minutes,
   reminder_before_min, allow_extension, status, create_by, create_time,
   update_by, update_time, remark, del_flag)
SELECT 'SLA_GLOBAL_URGENT', '紧急工单全局兜底', NULL, 2, 100,
       '2026-01-01 00:00:00', NULL, 120, 120, 480,
       30, '1', '0', 'admin', NOW(), '', NULL,
       '全局兜底；分类规则应使用高于100的优先级', '0'
WHERE NOT EXISTS (
  SELECT 1 FROM wo_sla_rule WHERE category_id IS NULL AND urgency_level = 2
);

INSERT INTO wo_sla_rule
  (rule_code, rule_name, category_id, urgency_level, priority,
   effective_from, effective_to, response_minutes, arrival_minutes, finish_minutes,
   reminder_before_min, allow_extension, status, create_by, create_time,
   update_by, update_time, remark, del_flag)
SELECT 'SLA_GLOBAL_CRITICAL', '特急工单全局兜底', NULL, 3, 100,
       '2026-01-01 00:00:00', NULL, 15, 15, 120,
       5, '1', '0', 'admin', NOW(), '', NULL,
       '全局兜底；分类规则应使用高于100的优先级', '0'
WHERE NOT EXISTS (
  SELECT 1 FROM wo_sla_rule WHERE category_id IS NULL AND urgency_level = 3
);
