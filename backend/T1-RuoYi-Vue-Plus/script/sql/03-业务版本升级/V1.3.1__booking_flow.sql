-- Explicit audience without changing the effective audience of any existing session.
-- Repeatable: only legacy values are migrated; already explicit values are preserved.
UPDATE fit_course_schedule s
SET s.schedule_mode = CASE WHEN EXISTS (
    SELECT 1 FROM fit_schedule_student ss
    WHERE ss.schedule_id=s.schedule_id AND ss.tenant_id=s.tenant_id AND ss.del_flag='0'
) THEN 'directed' ELSE 'normal' END
WHERE s.schedule_mode IS NULL OR s.schedule_mode NOT IN ('normal','directed')
   OR (s.schedule_mode='normal' AND EXISTS (
       SELECT 1 FROM fit_schedule_student ss
       WHERE ss.schedule_id=s.schedule_id AND ss.tenant_id=s.tenant_id AND ss.del_flag='0'));
-- No record is deleted. Attendance recovery and absent/leave resolution reuse existing
-- status fields and append to fit_behavior_log under existing edit permissions.
