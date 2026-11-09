-- Keep existing IDs, department hierarchy and permission grants for historical references.
-- A real account with the same name, an edited role, or a used example is left alone.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE sys_tenant
   SET company_name = '荆州市沙市区梯壹休闲健身工作室',
       contact_user_name = NULL, contact_phone = NULL, intro = NULL
 WHERE id = 1 AND tenant_id = '000000' AND BINARY company_name = BINARY 'XXX有限公司'
   AND contact_user_name = '管理组' AND contact_phone = '15888888888'
   AND intro = '多租户通用后台管理管理系统'
   AND license_number IS NULL AND address IS NULL AND domain IS NULL AND remark IS NULL
   AND package_id IS NULL AND expire_time IS NULL AND account_count = -1
   AND create_by = 1 AND create_dept = 103 AND update_by IS NULL AND update_time IS NULL
   AND status = '0' AND del_flag = '0';

UPDATE sys_user AS u
  JOIN sys_user_role AS link ON link.user_id = u.user_id AND link.role_id = u.user_id
  JOIN sys_role AS r ON r.role_id = link.role_id AND r.tenant_id = u.tenant_id
  LEFT JOIN sys_role AS archived ON archived.tenant_id = r.tenant_id
    AND archived.role_key = r.role_key AND archived.del_flag = '1'
   SET u.del_flag = '1', r.del_flag = '1'
 WHERE u.tenant_id = '000000' AND u.status = '1' AND u.del_flag = '0'
   AND u.user_type = 'sys_user' AND u.email = '' AND u.phonenumber = '' AND u.sex = '0'
   AND u.avatar IS NULL AND u.remark IS NULL AND u.login_ip = '127.0.0.1'
   AND u.login_date = u.create_time AND u.update_time = u.create_time AND u.update_by = u.user_id
   AND u.create_by = 1 AND u.create_dept = 103
   AND SHA2(u.password, 256) = '8e970aeb31fa126ae7af1f7c9df7366d6a15d63b0aaff425d41864cfc5f1ab48'
   AND r.status = '0' AND r.del_flag = '0' AND archived.role_id IS NULL
   AND r.menu_check_strictly = 1 AND r.dept_check_strictly = 1 AND r.remark = ''
   AND r.create_by = 1 AND r.create_dept = 103 AND r.update_by IS NULL AND r.update_time IS NULL
   AND ((u.user_id = 3 AND BINARY u.user_name = BINARY 'test' AND u.dept_id = 108 AND BINARY r.role_key = BINARY 'test1'
          AND SHA2(u.nick_name,256) = 'de19eb95ecffca35cd869f74831eb9919bbab3bea6dc691c53485df91c2e3dad'
          AND r.role_name = '本部门及以下' AND r.role_sort = 3 AND r.data_scope = '4')
     OR (u.user_id = 4 AND BINARY u.user_name = BINARY 'test1' AND u.dept_id = 102 AND BINARY r.role_key = BINARY 'test2'
          AND SHA2(u.nick_name,256) = '1d28abe655e9b06432c209bd88a4fe05538d38318e0be204a007742f9bb38fea'
          AND r.role_name = '仅本人' AND r.role_sort = 4 AND r.data_scope = '5'))
   -- Exact baseline grants. Even a single extra or removed grant preserves the entire pair.
   AND (SELECT SHA2(GROUP_CONCAT(menu_id ORDER BY menu_id),256) FROM sys_role_menu WHERE role_id = r.role_id)
       = CASE r.role_id WHEN 3 THEN '45b1fb71d832137d98a668fef1d78e2c8f9519ed26eb43ecff90d86a4552a644'
                            WHEN 4 THEN 'd42e2e4b348c07726a726c94e4b45e0bff71b27a618a085f242d2ff7288d677f' END
   AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.role_id = r.role_id AND ur.user_id <> u.user_id)
   AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id AND ur.role_id <> r.role_id)
   AND NOT EXISTS (SELECT 1 FROM sys_user_post WHERE user_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM sys_role_dept WHERE role_id = r.role_id)
   AND NOT EXISTS (SELECT 1 FROM sys_dept WHERE leader = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM sys_social WHERE user_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM sys_logininfor WHERE tenant_id = u.tenant_id AND user_name = u.user_name AND status = '0')
   AND NOT EXISTS (SELECT 1 FROM sys_oper_log WHERE tenant_id = u.tenant_id AND oper_name = u.user_name)
   AND NOT EXISTS (SELECT 1 FROM flow_user WHERE processed_by IN (CAST(u.user_id AS CHAR), CONCAT('user:',u.user_id), CONCAT('role:',r.role_id)))
   AND NOT EXISTS (SELECT 1 FROM flow_his_task WHERE approver = CAST(u.user_id AS CHAR))
   AND NOT EXISTS (SELECT 1 FROM flow_node WHERE permission_flag REGEXP CONCAT('(^|[^[:alnum:]])(user:|role:)?',u.user_id,'([^[:digit:]]|$)'))
   -- Include disabled and archived business records; retaining an uncertain example is safe.
   AND NOT EXISTS (SELECT 1 FROM fit_coach x WHERE x.user_id = u.user_id OR x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_order x WHERE x.cashier_user_id = u.user_id OR x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_checkin_record x WHERE x.operator_user_id = u.user_id OR x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_behavior_log x WHERE x.actor_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_training_audit x WHERE x.actor_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_training_plan x WHERE x.creator_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_policy_consent x WHERE x.account_id = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_activity x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_card_plan x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_coach_wechat_bind x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_course x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_course_reservation x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_course_schedule x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_member x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_member_card x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_order_item x WHERE x.create_by = u.user_id OR x.update_by = u.user_id)
   AND NOT EXISTS (SELECT 1 FROM fit_store x WHERE x.create_by = u.user_id OR x.update_by = u.user_id);

COMMIT;
