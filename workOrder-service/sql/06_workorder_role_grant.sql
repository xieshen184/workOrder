-- 工单三角色与最小菜单权限初始化（依赖 05-workorder-menu.sql）
-- 已存在的有效角色按 role_key 复用；仅补充缺失的角色菜单关联。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly,
   dept_check_strictly, status, del_flag, create_by, create_time,
   update_by, update_time, remark)
SELECT '工单报修人', 'workorder_reporter', 10, '5', 1,
       1, '0', '0', 'admin', NOW(), '', NULL,
       '仅本人数据；业务层继续校验工单创建人'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE role_key = 'workorder_reporter' AND del_flag = '0'
);

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly,
   dept_check_strictly, status, del_flag, create_by, create_time,
   update_by, update_time, remark)
SELECT '工单维修人员', 'workorder_engineer', 20, '5', 1,
       1, '0', '0', 'admin', NOW(), '', NULL,
       '仅本人负责或处理过的工单；业务层继续校验当前处理人'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE role_key = 'workorder_engineer' AND del_flag = '0'
);

INSERT INTO sys_role
  (role_name, role_key, role_sort, data_scope, menu_check_strictly,
   dept_check_strictly, status, del_flag, create_by, create_time,
   update_by, update_time, remark)
SELECT '工单调度管理员', 'workorder_dispatcher', 30, '2', 1,
       1, '0', '0', 'admin', NOW(), '', NULL,
       '自定义部门数据范围；上线前须配置授权部门'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE role_key = 'workorder_dispatcher' AND del_flag = '0'
);

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
SELECT @wo_reporter_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_reporter_role_id IS NOT NULL
  AND menu.menu_type = 'F'
  AND menu.perms IN (
    'workorder:order:query',
    'workorder:order:add',
    'workorder:order:cancel',
    'workorder:order:return',
    'workorder:order:confirm',
    'workorder:attachment:upload',
    'workorder:attachment:download',
    'workorder:evaluation:add',
    'workorder:evaluation:query'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu grant_row
    WHERE grant_row.role_id = @wo_reporter_role_id
      AND grant_row.menu_id = menu.menu_id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_engineer_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_engineer_role_id IS NOT NULL
  AND menu.menu_type = 'F'
  AND menu.perms IN (
    'workorder:order:assigned:list',
    'workorder:engineer:status',
    'workorder:order:query',
    'workorder:order:accept',
    'workorder:order:arrive',
    'workorder:order:assess',
    'workorder:order:progress',
    'workorder:order:finish',
    'workorder:attachment:upload',
    'workorder:attachment:download'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu grant_row
    WHERE grant_row.role_id = @wo_engineer_role_id
      AND grant_row.menu_id = menu.menu_id
  );

-- 调度管理员拥有PC目录和页面定义；页面当前为停用，不会下发不存在的前端组件。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_dispatcher_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND (
    (menu.parent_id = 0 AND menu.path = 'workorder' AND menu.menu_type = 'M')
    OR (menu.menu_type = 'C' AND menu.component IN (
      'workorder/order/index',
      'workorder/engineer/index',
      'workorder/category/index'
    ))
    OR (menu.menu_type = 'F' AND menu.perms IN (
      'workorder:order:list',
      'workorder:order:query',
      'workorder:order:cancel',
      'workorder:order:return',
      'workorder:order:assign',
      'workorder:order:reassign',
      'workorder:attachment:download',
      'workorder:evaluation:query',
      'workorder:engineer:list',
      'workorder:category:list',
      'workorder:category:query',
      'workorder:category:add',
      'workorder:category:edit'
    ))
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu grant_row
    WHERE grant_row.role_id = @wo_dispatcher_role_id
      AND grant_row.menu_id = menu.menu_id
  );
