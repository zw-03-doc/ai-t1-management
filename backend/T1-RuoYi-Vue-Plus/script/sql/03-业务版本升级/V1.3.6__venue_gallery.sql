SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS fit_venue_gallery (
    tenant_id VARCHAR(20) PRIMARY KEY,
    version INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fit_venue_photo (
    tenant_id VARCHAR(20) NOT NULL,
    photo_id VARCHAR(36) NOT NULL,
    sort_order INT NOT NULL,
    image_jpeg MEDIUMBLOB NULL,
    PRIMARY KEY (tenant_id, photo_id),
    KEY idx_gallery_order (tenant_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

START TRANSACTION;
INSERT IGNORE INTO fit_venue_gallery VALUES ('000000', 0);
SET @initialize_gallery = ROW_COUNT();
INSERT INTO fit_venue_photo (tenant_id, photo_id, sort_order)
SELECT '000000', CONCAT('live-', n), n
FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) defaults_
WHERE @initialize_gallery=1;

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_time)
SELECT 202611090010001, '场馆图片', 2042588802307977218, 12, 'venue', 'fitness/venue/index', 1, 0, 'C', '0', '0', 'fitness:venue:query', 'picture', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=202611090010001);
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_time)
SELECT 202611090010002, '管理场馆图片', 202611090010001, 1, '#', '', 1, 0, 'F', '0', '0', 'fitness:venue:edit', '#', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=202611090010002);
-- Transfer only the homepage permissions already assigned to an active operator.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, 202611090010001 FROM sys_role_menu rm
JOIN sys_role r ON r.role_id=rm.role_id AND r.status='0' AND r.del_flag='0'
JOIN sys_menu m ON m.menu_id=rm.menu_id AND m.status='0'
WHERE (m.menu_id=202611050010001 AND m.perms='fitness:portal:query')
   OR (m.menu_id=202611050010002 AND m.perms='fitness:portal:edit');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, 202611090010002 FROM sys_role_menu rm
JOIN sys_role r ON r.role_id=rm.role_id AND r.status='0' AND r.del_flag='0'
JOIN sys_menu m ON m.menu_id=rm.menu_id AND m.status='0'
WHERE m.menu_id=202611050010002 AND m.perms='fitness:portal:edit';
UPDATE sys_menu SET visible='1', status='1'
WHERE (menu_id=202611050010001 AND perms='fitness:portal:query')
   OR (menu_id=202611050010002 AND perms='fitness:portal:edit');
COMMIT;
