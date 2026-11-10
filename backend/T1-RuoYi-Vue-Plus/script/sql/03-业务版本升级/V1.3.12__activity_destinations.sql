SET @ddl = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fit_activity' AND column_name='contact_coach_id'), 'SELECT 1', 'ALTER TABLE fit_activity ADD COLUMN contact_coach_id BIGINT NULL');
PREPARE ddl FROM @ddl;
EXECUTE ddl;
DEALLOCATE PREPARE ddl;

SET @ddl = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fit_activity' AND column_name='course_id'), 'SELECT 1', 'ALTER TABLE fit_activity ADD COLUMN course_id BIGINT NULL');
PREPARE ddl FROM @ddl;
EXECUTE ddl;
DEALLOCATE PREPARE ddl;

SET @ddl = IF(EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fit_coach' AND column_name='contact_qr_url'), 'SELECT 1', 'ALTER TABLE fit_coach ADD COLUMN contact_qr_url VARCHAR(512) NULL');
PREPARE ddl FROM @ddl;
EXECUTE ddl;
DEALLOCATE PREPARE ddl;
