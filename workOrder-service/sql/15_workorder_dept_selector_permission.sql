-- 维修绩效筛选项权限回补（面向已完成旧版初始化的联调数据库）。
-- 可在 00_workorder_full_init.sql 已执行过的数据库中重复执行，不会删除或覆盖现有数据。
--
-- 历史初始化脚本分阶段创建菜单与功能权限。若数据库曾在部分阶段之间初始化，
-- 调度员虽然能进入维修绩效页面，但可能缺少组织、工程师或分类筛选接口权限。
-- 这里一次性回补该页面实际依赖的三个只读权限，保证筛选项与页面访问权限一致。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @wo_dispatcher_role_id := (
  SELECT role_id
  FROM sys_role
  WHERE role_key = 'workorder_dispatcher' AND del_flag = '0'
  ORDER BY role_id
  LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_dispatcher_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND menu.perms IN (
    'system:dept:list',
    'workorder:engineer:list',
    'workorder:category:list'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existing
    WHERE existing.role_id = @wo_dispatcher_role_id
      AND existing.menu_id = menu.menu_id
  );

-- 执行后应返回三条 READY；若出现 MISSING，说明菜单基础数据尚未初始化完整。
SELECT required_permission.perms,
       CASE WHEN role_menu.menu_id IS NULL THEN 'MISSING' ELSE 'READY' END AS grant_status
FROM (
  SELECT 'system:dept:list' AS perms
  UNION ALL SELECT 'workorder:engineer:list'
  UNION ALL SELECT 'workorder:category:list'
) required_permission
LEFT JOIN sys_menu menu
       ON menu.perms = required_permission.perms
LEFT JOIN sys_role_menu role_menu
       ON role_menu.role_id = @wo_dispatcher_role_id
      AND role_menu.menu_id = menu.menu_id
ORDER BY required_permission.perms;
