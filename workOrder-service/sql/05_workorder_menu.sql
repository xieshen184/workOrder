-- 工单菜单与按钮权限初始化（依赖 01-ruoyi.sql）
-- C 类型页面菜单暂设为停用，前端页面落地后再显式启用，避免返回不存在的组件。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '工单管理', 0, 4, 'workorder', NULL, NULL, 'Workorder',
       1, 0, 'M', '0', '0', '', 'clipboard',
       'admin', NOW(), '', NULL, '工单业务目录'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu
  WHERE parent_id = 0 AND path = 'workorder' AND menu_type = 'M'
);

SET @wo_root_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = 0 AND path = 'workorder' AND menu_type = 'M'
  ORDER BY menu_id LIMIT 1
);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '工单列表', @wo_root_menu_id, 1, 'orders', 'workorder/order/index', NULL, 'WorkorderOrders',
       1, 0, 'C', '0', '1', '', 'list',
       'admin', NOW(), '', NULL, '页面未落地，暂时停用'
WHERE @wo_root_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @wo_root_menu_id AND path = 'orders' AND menu_type = 'C'
  );

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '人员状态', @wo_root_menu_id, 2, 'engineers', 'workorder/engineer/index', NULL, 'WorkorderEngineers',
       1, 0, 'C', '0', '1', '', 'peoples',
       'admin', NOW(), '', NULL, '页面未落地，暂时停用'
WHERE @wo_root_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @wo_root_menu_id AND path = 'engineers' AND menu_type = 'C'
  );

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '分类管理', @wo_root_menu_id, 3, 'categories', 'workorder/category/index', NULL, 'WorkorderCategories',
       1, 0, 'C', '0', '1', '', 'tree-table',
       'admin', NOW(), '', NULL, '页面未落地，暂时停用'
WHERE @wo_root_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu
    WHERE parent_id = @wo_root_menu_id AND path = 'categories' AND menu_type = 'C'
  );

SET @wo_order_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'orders' AND menu_type = 'C'
  ORDER BY menu_id LIMIT 1
);

SET @wo_engineer_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'engineers' AND menu_type = 'C'
  ORDER BY menu_id LIMIT 1
);

SET @wo_category_menu_id := (
  SELECT menu_id FROM sys_menu
  WHERE parent_id = @wo_root_menu_id AND path = 'categories' AND menu_type = 'C'
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
  SELECT '工单池查询' menu_name, @wo_order_menu_id parent_id, 1 order_num, 'workorder:order:list' perms, '调度管理员查询授权范围工单' remark
  UNION ALL SELECT '工单详情', @wo_order_menu_id, 2, 'workorder:order:query', '查看通过数据范围校验的工单详情'
  UNION ALL SELECT '提交工单', @wo_order_menu_id, 3, 'workorder:order:add', '报修人创建并提交工单'
  UNION ALL SELECT '取消工单', @wo_order_menu_id, 4, 'workorder:order:cancel', '取消待派单工单'
  UNION ALL SELECT '派单', @wo_order_menu_id, 5, 'workorder:order:assign', '调度管理员派单'
  UNION ALL SELECT '改派', @wo_order_menu_id, 6, 'workorder:order:reassign', '调度管理员改派'
  UNION ALL SELECT '接单', @wo_order_menu_id, 7, 'workorder:order:accept', '当前维修人员接单'
  UNION ALL SELECT '到场确认', @wo_order_menu_id, 8, 'workorder:order:arrive', '当前维修人员到场确认'
  UNION ALL SELECT '故障评估', @wo_order_menu_id, 9, 'workorder:order:assess', '当前维修人员提交评估'
  UNION ALL SELECT '更新进度', @wo_order_menu_id, 10, 'workorder:order:progress', '当前维修人员更新进度'
  UNION ALL SELECT '提交完工', @wo_order_menu_id, 11, 'workorder:order:finish', '当前维修人员提交完工'
  UNION ALL SELECT '退回处理', @wo_order_menu_id, 12, 'workorder:order:return', '报修人或调度管理员退回处理'
  UNION ALL SELECT '确认完工', @wo_order_menu_id, 13, 'workorder:order:confirm', '报修人确认完工'
  UNION ALL SELECT '上传附件', @wo_order_menu_id, 14, 'workorder:attachment:upload', '上传报修、到场、过程或完工附件'
  UNION ALL SELECT '下载附件', @wo_order_menu_id, 15, 'workorder:attachment:download', '经过工单数据权限校验后预览或下载附件'
  UNION ALL SELECT '提交评价', @wo_order_menu_id, 16, 'workorder:evaluation:add', '报修人提交一次性评价'
  UNION ALL SELECT '查看评价', @wo_order_menu_id, 17, 'workorder:evaluation:query', '查看通过数据范围校验的评价详情'
  UNION ALL SELECT '我的维修任务', @wo_order_menu_id, 18, 'workorder:order:assigned:list', '维修人员查询本人当前负责或处理过的工单'
  UNION ALL SELECT '查看人员状态', @wo_engineer_menu_id, 1, 'workorder:engineer:list', '调度管理员查询维修人员状态与负载'
  UNION ALL SELECT '更新本人状态', @wo_engineer_menu_id, 2, 'workorder:engineer:status', '维修人员更新本人工作状态'
  UNION ALL SELECT '分类列表', @wo_category_menu_id, 1, 'workorder:category:list', '查询授权范围内的分类列表'
  UNION ALL SELECT '分类详情', @wo_category_menu_id, 2, 'workorder:category:query', '查看分类详情和引用状态'
  UNION ALL SELECT '新增分类', @wo_category_menu_id, 3, 'workorder:category:add', '新增工单分类'
  UNION ALL SELECT '编辑分类', @wo_category_menu_id, 4, 'workorder:category:edit', '编辑或启停工单分类并执行引用保护'
) seed
WHERE seed.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.perms = seed.perms AND existing.menu_type = 'F'
  );
