-- Required identity seed for FitnessCoachServiceImpl.resolveCoachRoleId().
-- V1.0.3 added the unique role key but left this role as a commented example.
-- This migration only initializes the deployed default tenant. It does not grant
-- administrator rights, backend menu permissions, custom departments or packages.
-- Existing roles (including disabled roles) are preserved without modification.
-- A role_id collision with an unrelated role fails explicitly instead of hiding it.

INSERT INTO sys_role (
    role_id, tenant_id, role_name, role_key, role_sort, data_scope,
    menu_check_strictly, dept_check_strictly, status, del_flag,
    create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
    2042601000000001001, '000000', '教练', 'jiaolian', 10, '5',
    1, 1, '0', '0',
    NULL, NULL, NOW(), NULL, NULL,
    '教练身份基础角色：仅本人数据范围，不授予后台菜单权限；教练端按授课和会员绑定关系授权'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role
    WHERE tenant_id = '000000' AND role_key = 'jiaolian' AND del_flag = '0'
);

-- Read-only verification after applying (repeat application must keep count = 1):
-- SELECT role_id, tenant_id, role_key, data_scope, status, del_flag
-- FROM sys_role WHERE tenant_id='000000' AND role_key='jiaolian' AND del_flag='0';
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=2042601000000001001;
-- Expected for a fresh deployment: one active jiaolian role, data_scope='5', zero menus.
