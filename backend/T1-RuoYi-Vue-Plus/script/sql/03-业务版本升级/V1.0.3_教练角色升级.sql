-- 版本 1.0.3：教练账号自动创建与角色标识唯一约束升级脚本
-- 说明：本次变更只放版本迭代脚本，不改动基线库脚本。
-- 执行前如已有重复 role_key，请先清理：
-- SELECT tenant_id, role_key, del_flag, COUNT(1)
-- FROM sys_role
-- GROUP BY tenant_id, role_key, del_flag
-- HAVING COUNT(1) > 1;

ALTER TABLE sys_role
    ADD UNIQUE KEY uk_sys_role_tenant_key (tenant_id, role_key, del_flag);

-- 如尚未创建教练角色，可参考以下示例手动补充（请按实际 role_id / 菜单权限调整）
-- INSERT INTO sys_role (
--     role_id, tenant_id, role_name, role_key, role_sort, data_scope,
--     menu_check_strictly, dept_check_strictly, status, del_flag,
--     create_dept, create_by, create_time, update_by, update_time, remark
-- )
-- VALUES (
--     9001, '000000', '教练', 'jiaolian', 10, '5',
--     1, 1, '0', '0',
--     103, 1, NOW(), NULL, NULL, '教练默认角色'
-- );
