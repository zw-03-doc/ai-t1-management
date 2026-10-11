-- 版本 1.1.0：移动端联调性能优化索引脚本
-- 目标：优化会员端/教练端高频查询（排课、预约、核销、学员、卡项）
-- 说明：
-- 1) 仅新增索引，不变更字段结构与业务语义；
-- 2) 使用 information_schema 判重，避免重复执行报错；
-- 3) 适用于 MySQL 8.x。

SET NAMES utf8mb4;
SET @db_name = DATABASE();

-- fit_course_schedule: 教练排课、会员排课高频筛选
SET @idx_name = 'idx_fit_schedule_coach_date_start';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_course_schedule' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_course_schedule ADD INDEX idx_fit_schedule_coach_date_start (coach_id, schedule_date, start_time, del_flag)',
    'SELECT ''idx_fit_schedule_coach_date_start exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_fit_schedule_store_date';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_course_schedule' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_course_schedule ADD INDEX idx_fit_schedule_store_date (store_id, schedule_date, del_flag)',
    'SELECT ''idx_fit_schedule_store_date exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fit_course_reservation: 会员预约列表、排课预约去重与状态筛选
SET @idx_name = 'idx_fit_res_member_status_time';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_course_reservation' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_course_reservation ADD INDEX idx_fit_res_member_status_time (member_id, reservation_status, reservation_time, del_flag)',
    'SELECT ''idx_fit_res_member_status_time exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_fit_res_schedule_member';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_course_reservation' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_course_reservation ADD INDEX idx_fit_res_schedule_member (schedule_id, member_id, del_flag)',
    'SELECT ''idx_fit_res_schedule_member exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fit_checkin_record: 教练待核销、会员签到状态查询
SET @idx_name = 'idx_fit_checkin_reservation';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_checkin_record' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_checkin_record ADD INDEX idx_fit_checkin_reservation (reservation_id, del_flag)',
    'SELECT ''idx_fit_checkin_reservation exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_fit_checkin_schedule_status_operator';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_checkin_record' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_checkin_record ADD INDEX idx_fit_checkin_schedule_status_operator (schedule_id, checkin_status, operator_user_id, del_flag)',
    'SELECT ''idx_fit_checkin_schedule_status_operator exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fit_member_card: 会员卡列表、教练学员列表
SET @idx_name = 'idx_fit_member_card_member_status';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_member_card' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_member_card ADD INDEX idx_fit_member_card_member_status (member_id, card_status, del_flag)',
    'SELECT ''idx_fit_member_card_member_status exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_name = 'idx_fit_member_card_bind_coach';
SET @exists_sql = (
    SELECT COUNT(1) FROM information_schema.statistics
    WHERE table_schema = @db_name AND table_name = 'fit_member_card' AND index_name = @idx_name
);
SET @ddl = IF(@exists_sql = 0,
    'ALTER TABLE fit_member_card ADD INDEX idx_fit_member_card_bind_coach (bind_coach_id, del_flag, create_time)',
    'SELECT ''idx_fit_member_card_bind_coach exists''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 建议执行后检查
-- SHOW INDEX FROM fit_course_schedule;
-- SHOW INDEX FROM fit_course_reservation;
-- SHOW INDEX FROM fit_checkin_record;
-- SHOW INDEX FROM fit_member_card;
