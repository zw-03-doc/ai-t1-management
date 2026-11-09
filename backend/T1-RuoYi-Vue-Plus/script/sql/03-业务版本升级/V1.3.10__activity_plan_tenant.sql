-- Activity detail reads and writes these links through the tenant interceptor.
SET @has_tenant = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'fit_activity_plan' AND column_name = 'tenant_id');
SET @ddl = IF(@has_tenant = 0,
    'ALTER TABLE fit_activity_plan ADD COLUMN tenant_id VARCHAR(20) NULL AFTER id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE fit_activity_plan link
JOIN fit_activity activity ON activity.activity_id = link.activity_id
SET link.tenant_id = activity.tenant_id
WHERE link.tenant_id IS NULL AND activity.tenant_id IS NOT NULL;
