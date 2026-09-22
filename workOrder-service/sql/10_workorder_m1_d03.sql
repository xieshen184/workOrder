-- M1-D03 人员状态与基础分类配置。依赖 03、05、06 初始化脚本，可重复执行。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 页面文件与真实接口已经落地，启用调度管理员的人员状态和分类管理菜单。
UPDATE sys_menu
SET status = '0', visible = '0', update_by = 'admin', update_time = NOW(),
    remark = 'M1-D03人员状态页面已启用'
WHERE menu_type = 'C' AND component = 'workorder/engineer/index';

UPDATE sys_menu
SET status = '0', visible = '0', update_by = 'admin', update_time = NOW(),
    remark = 'M1-D03分类管理页面已启用'
WHERE menu_type = 'C' AND component = 'workorder/category/index';

-- 兼容已经先执行过角色脚本的环境，只补充缺失授权，不产生重复关系。
SET @wo_dispatcher_role_id := (
  SELECT role_id FROM sys_role
  WHERE role_key = 'workorder_dispatcher' AND del_flag = '0'
  ORDER BY role_id LIMIT 1
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @wo_dispatcher_role_id, menu.menu_id
FROM sys_menu menu
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND (
    (menu.menu_type = 'C' AND menu.component IN (
      'workorder/engineer/index',
      'workorder/category/index'
    ))
    OR (menu.menu_type = 'F' AND menu.perms IN (
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
