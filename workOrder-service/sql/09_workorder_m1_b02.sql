-- M1-B02 dispatch/processing slice. Safe to run repeatedly after 03-08.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- The PC order page now exists. Engineer/category pages intentionally remain disabled.
UPDATE sys_menu
SET status = '0', visible = '0', update_by = 'admin', update_time = NOW(),
    remark = 'M1-B02工单列表页面已启用'
WHERE menu_type = 'C' AND component = 'workorder/order/index';

-- Older installations must have the action-level idempotency constraint used by the command module.
SET @schema_name = DATABASE();
SET @action_idempotency_count = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wo_action_log'
    AND INDEX_NAME = 'uk_wo_action_idempotency'
);
SET @add_action_idempotency_sql = IF(
  @action_idempotency_count = 0,
  'ALTER TABLE wo_action_log ADD UNIQUE KEY uk_wo_action_idempotency (operator_id, action_type, idempotency_key)',
  'SELECT 1'
);
PREPARE add_action_idempotency_stmt FROM @add_action_idempotency_sql;
EXECUTE add_action_idempotency_stmt;
DEALLOCATE PREPARE add_action_idempotency_stmt;
