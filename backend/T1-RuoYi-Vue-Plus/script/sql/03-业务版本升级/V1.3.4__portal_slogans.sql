CREATE TABLE IF NOT EXISTS fit_portal_slogans (
    tenant_id VARCHAR(20) NOT NULL,
    eyebrow VARCHAR(32) NOT NULL,
    title_lead VARCHAR(12) NOT NULL,
    title_accent VARCHAR(8) NOT NULL,
    title_second_line VARCHAR(16) NOT NULL,
    subtitle VARCHAR(60) NOT NULL,
    gallery_caption VARCHAR(40) NOT NULL,
    footer_caption VARCHAR(40) NOT NULL,
    version INT NOT NULL,
    PRIMARY KEY (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序和网页入口的首页励志标语';

-- Add assignable menu permissions; do not grant editing rights to existing coach or member roles.
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_time)
SELECT 202611050010001, '励志标语', 2042588802307977218, 13, 'portal', 'fitness/portal/index', 1, 0,
       'C', '0', '0', 'fitness:portal:query', 'edit', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 202611050010001);

UPDATE sys_menu SET menu_name='励志标语'
WHERE menu_id=202611050010001 AND menu_name='首页励志标语';

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_time)
SELECT 202611050010002, '修改首页标语', 202611050010001, 1, '#', '', 1, 0,
       'F', '0', '0', 'fitness:portal:edit', '#', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 202611050010002);
