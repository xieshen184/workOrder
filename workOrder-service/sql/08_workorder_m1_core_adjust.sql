-- M1 incremental compatibility for databases initialized before pre-bound attachments were supported.
SET @schema_name = DATABASE();

SET @order_id_nullable = (
  SELECT IS_NULLABLE
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wo_attachment' AND COLUMN_NAME = 'order_id'
  LIMIT 1
);
SET @alter_order_id_sql = IF(
  @order_id_nullable = 'NO',
  'ALTER TABLE wo_attachment MODIFY order_id BIGINT NULL COMMENT ''工单ID，上传暂存时为空，提交后逻辑关联wo_order.id''',
  'SELECT 1'
);
PREPARE alter_order_id_stmt FROM @alter_order_id_sql;
EXECUTE alter_order_id_stmt;
DEALLOCATE PREPARE alter_order_id_stmt;

SET @object_key_index_count = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'wo_attachment'
    AND INDEX_NAME = 'uk_wo_attachment_object_key'
);
SET @add_object_key_index_sql = IF(
  @object_key_index_count = 0,
  'ALTER TABLE wo_attachment ADD UNIQUE KEY uk_wo_attachment_object_key (object_key)',
  'SELECT 1'
);
PREPARE add_object_key_index_stmt FROM @add_object_key_index_sql;
EXECUTE add_object_key_index_stmt;
DEALLOCATE PREPARE add_object_key_index_stmt;
