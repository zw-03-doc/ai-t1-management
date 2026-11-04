-- Training lifecycle and resumable actual-set recording. MySQL 8, safe to reapply.
-- Existing completed records remain unchanged; old clients omit finish and keep completion behavior.
SET @training_column_exists = (SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'fit_training_set' AND column_name = 'target_set_index');
SET @training_ddl = IF(@training_column_exists = 0,
  'ALTER TABLE fit_training_set ADD COLUMN target_set_index INT NULL COMMENT ''Zero-based target position; NULL for extra or legacy groups'' AFTER rpe',
  'SELECT 1');
PREPARE training_stmt FROM @training_ddl;
EXECUTE training_stmt;
DEALLOCATE PREPARE training_stmt;

-- Repair the presentation state of previously exhausted schedules without touching workouts or weights.
UPDATE fit_training_plan p SET p.status = 'completed', p.version = p.version + 1, p.update_time = NOW()
WHERE p.status = 'active'
  AND EXISTS (SELECT 1 FROM fit_training_workout w WHERE w.tenant_id = p.tenant_id AND w.plan_id = p.plan_id)
  AND NOT EXISTS (SELECT 1 FROM fit_training_workout w WHERE w.tenant_id = p.tenant_id AND w.plan_id = p.plan_id
    AND w.status IN ('planned', 'in_progress'));
