-- Existing member profiles do not need to repeat the first-login flow.
-- New WeChat registrations explicitly set profile_setup_status to pending.
SET @has_contact = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'fit_member' AND column_name = 'contact_mobile');
SET @ddl = IF(@has_contact = 0,
    'ALTER TABLE fit_member ADD COLUMN contact_mobile VARCHAR(20) NULL COMMENT ''Self-reported contact number, not a verified identity'' AFTER mobile',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_setup = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'fit_member' AND column_name = 'profile_setup_status');
SET @ddl = IF(@has_setup = 0,
    'ALTER TABLE fit_member ADD COLUMN profile_setup_status VARCHAR(12) NOT NULL DEFAULT ''completed'' COMMENT ''Optional profile: pending, skipped, completed''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
