-- The V1.0.1 baseline creates pages under these seven predefined directories.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_time)
SELECT d.menu_id, d.menu_name, 0, d.order_num, d.path, NULL, 1, 0,
       'M', '0', '0', '', d.icon, NOW()
FROM (
    SELECT 2042588802307977218 AS menu_id, '门店管理' AS menu_name, 10 AS order_num, 'fitness-store' AS path, 'build' AS icon
    UNION ALL SELECT 2042589474197729282, '人员管理', 11, 'fitness-staff', 'peoples'
    UNION ALL SELECT 2042589689940144129, '会员管理', 12, 'fitness-member', 'user'
    UNION ALL SELECT 2042590129557729281, '课程中心', 13, 'fitness-course', 'education'
    UNION ALL SELECT 2042590365416026113, '卡项中心', 14, 'fitness-card', 'money'
    UNION ALL SELECT 2042590527005782017, '订单中心', 15, 'fitness-order', 'shopping'
    UNION ALL SELECT 2042590627627134978, '报表中心', 16, 'fitness-report', 'chart'
) d
LEFT JOIN sys_menu existing ON existing.menu_id = d.menu_id
WHERE existing.menu_id IS NULL;

-- Repair titles imported by earlier clients that used the wrong connection charset.
UPDATE sys_menu SET menu_name='励志标语'
WHERE menu_id=202611050010001 AND perms='fitness:portal:query'
  AND BINARY menu_name <> BINARY '励志标语';
UPDATE sys_menu SET menu_name='修改首页标语'
WHERE menu_id=202611050010002 AND perms='fitness:portal:edit'
  AND BINARY menu_name <> BINARY '修改首页标语';

-- Non-superadmins need the parent in their menu tree. Grant a directory only to
-- roles already assigned a page inside it; no business page or button is granted.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT granted.role_id, child.parent_id
FROM sys_role_menu granted
JOIN sys_role r ON r.role_id=granted.role_id AND r.del_flag='0' AND r.status='0'
JOIN sys_menu child ON child.menu_id=granted.menu_id AND child.menu_type='C'
    AND child.component LIKE 'fitness/%' AND child.status='0'
JOIN sys_menu parent ON parent.menu_id=child.parent_id AND parent.menu_type='M'
LEFT JOIN sys_role_menu existing ON existing.role_id=granted.role_id AND existing.menu_id=parent.menu_id
WHERE child.parent_id IN (2042588802307977218,2042589474197729282,2042589689940144129,
    2042590129557729281,2042590365416026113,2042590527005782017,2042590627627134978)
  AND existing.role_id IS NULL;

COMMIT;
