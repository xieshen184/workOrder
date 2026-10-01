-- 全链路联调与第一轮回归测试账号初始化。
--
-- 安全边界：
-- 1. 本脚本仅用于本地开发、联调和独立测试环境，禁止在生产环境执行。
-- 2. 下列账号统一使用初始密码 admin123，进入共享测试环境后必须立即修改密码。
-- 3. 脚本只补充缺失数据，可重复执行，不会覆盖已存在账号的密码或个人资料。
-- 4. 依赖 03_workorder_core_schema.sql 和 06_workorder_role_grant.sql 已执行完成。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用若依默认 BCrypt 密文，明文仅用于本地联调：admin123。
SET @wo_integration_password_hash := '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2';

-- 报修人：用于小程序创建、撤销、退回、确认和评价工单。
INSERT INTO sys_user
  (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
   password, status, del_flag, login_ip, login_date, pwd_update_date,
   create_by, create_time, update_by, update_time, remark)
SELECT 105, 'wo_reporter', '联调报修人', '00', 'wo.reporter@example.test',
       '13800000001', '2', '', @wo_integration_password_hash, '0', '0', '',
       NULL, NOW(), 'admin', NOW(), '', NULL,
       '仅用于本地和测试环境的全链路联调账号'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user
  WHERE user_name = 'wo_reporter' AND del_flag = '0'
);

-- 维修人员：用于接单、到场、评估、处理进度和完工流程。
INSERT INTO sys_user
  (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
   password, status, del_flag, login_ip, login_date, pwd_update_date,
   create_by, create_time, update_by, update_time, remark)
SELECT 107, 'wo_engineer', '联调维修员', '00', 'wo.engineer@example.test',
       '13800000002', '2', '', @wo_integration_password_hash, '0', '0', '',
       NULL, NOW(), 'admin', NOW(), '', NULL,
       '仅用于本地和测试环境的全链路联调账号'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user
  WHERE user_name = 'wo_engineer' AND del_flag = '0'
);

-- 调度管理员：用于 PC 端分派、改派、分类、SLA、通知和运营配置。
INSERT INTO sys_user
  (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
   password, status, del_flag, login_ip, login_date, pwd_update_date,
   create_by, create_time, update_by, update_time, remark)
SELECT 103, 'wo_dispatcher', '联调调度员', '00', 'wo.dispatcher@example.test',
       '13800000003', '2', '', @wo_integration_password_hash, '0', '0', '',
       NULL, NOW(), 'admin', NOW(), '', NULL,
       '仅用于本地和测试环境的全链路联调账号'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_user
  WHERE user_name = 'wo_dispatcher' AND del_flag = '0'
);

-- 读取实际主键，避免依赖自增 ID 的具体数值。
SET @wo_reporter_user_id := (
  SELECT user_id FROM sys_user
  WHERE user_name = 'wo_reporter' AND del_flag = '0'
  ORDER BY user_id LIMIT 1
);
SET @wo_engineer_user_id := (
  SELECT user_id FROM sys_user
  WHERE user_name = 'wo_engineer' AND del_flag = '0'
  ORDER BY user_id LIMIT 1
);
SET @wo_dispatcher_user_id := (
  SELECT user_id FROM sys_user
  WHERE user_name = 'wo_dispatcher' AND del_flag = '0'
  ORDER BY user_id LIMIT 1
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
SET @wo_default_post_id := (
  SELECT post_id FROM sys_post
  WHERE post_code = 'user' AND status = '0'
  ORDER BY post_id LIMIT 1
);

-- 为每个联调账号补充对应的工单角色，确保权限回归结果可解释。
INSERT INTO sys_user_role (user_id, role_id)
SELECT @wo_reporter_user_id, @wo_reporter_role_id
WHERE @wo_reporter_user_id IS NOT NULL
  AND @wo_reporter_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role
    WHERE user_id = @wo_reporter_user_id AND role_id = @wo_reporter_role_id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT @wo_engineer_user_id, @wo_engineer_role_id
WHERE @wo_engineer_user_id IS NOT NULL
  AND @wo_engineer_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role
    WHERE user_id = @wo_engineer_user_id AND role_id = @wo_engineer_role_id
  );

INSERT INTO sys_user_role (user_id, role_id)
SELECT @wo_dispatcher_user_id, @wo_dispatcher_role_id
WHERE @wo_dispatcher_user_id IS NOT NULL
  AND @wo_dispatcher_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role
    WHERE user_id = @wo_dispatcher_user_id AND role_id = @wo_dispatcher_role_id
  );

-- 为测试账号补充普通员工岗位，保持用户资料与后台用户管理页面一致。
INSERT INTO sys_user_post (user_id, post_id)
SELECT integration_user.user_id, @wo_default_post_id
FROM sys_user integration_user
WHERE integration_user.user_name IN ('wo_reporter', 'wo_engineer', 'wo_dispatcher')
  AND integration_user.del_flag = '0'
  AND @wo_default_post_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_post user_post
    WHERE user_post.user_id = integration_user.user_id
      AND user_post.post_id = @wo_default_post_id
  );

-- 调度角色使用自定义部门数据范围；测试环境授权全部有效部门，避免列表被数据权限误过滤。
INSERT INTO sys_role_dept (role_id, dept_id)
SELECT @wo_dispatcher_role_id, department.dept_id
FROM sys_dept department
WHERE @wo_dispatcher_role_id IS NOT NULL
  AND department.status = '0'
  AND department.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_dept role_dept
    WHERE role_dept.role_id = @wo_dispatcher_role_id
      AND role_dept.dept_id = department.dept_id
  );

-- 维修员默认设为在岗且可接单；移动端首次上报状态后由业务逻辑继续维护。
INSERT INTO wo_engineer_status
  (engineer_id, engineer_name, engineer_dept_id, duty_status, work_status,
   current_order_id, active_order_count, overdue_order_count, status_source,
   last_changed_at, last_heartbeat_at, update_time)
SELECT integration_engineer.user_id,
       integration_engineer.nick_name,
       integration_engineer.dept_id,
       'ON_DUTY', 'AVAILABLE',
       NULL, 0, 0, 'MANUAL', NOW(), NULL, NOW()
FROM sys_user integration_engineer
WHERE integration_engineer.user_id = @wo_engineer_user_id
  AND NOT EXISTS (
    SELECT 1 FROM wo_engineer_status
    WHERE engineer_id = integration_engineer.user_id
  );

-- 执行结果：三行均应显示有效账号、目标角色和已绑定状态。
SELECT integration_user.user_name,
       integration_user.nick_name,
       role_row.role_key,
       CASE WHEN user_role.user_id IS NULL THEN 'MISSING' ELSE 'READY' END AS grant_status
FROM sys_user integration_user
LEFT JOIN sys_user_role user_role
       ON user_role.user_id = integration_user.user_id
LEFT JOIN sys_role role_row
       ON role_row.role_id = user_role.role_id
      AND role_row.role_key IN (
        'workorder_reporter', 'workorder_engineer', 'workorder_dispatcher'
      )
WHERE integration_user.user_name IN ('wo_reporter', 'wo_engineer', 'wo_dispatcher')
  AND integration_user.del_flag = '0'
ORDER BY integration_user.user_name;
