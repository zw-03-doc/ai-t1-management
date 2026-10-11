-- 版本 1.1.2：预约重构（到店体验/卡项预约）
-- 目标：
-- 1) 预约区分到店体验预约与卡项预约
-- 2) 时间卡（月卡/年卡）不走预约与核销
-- 3) 次卡/私教课包/小班课包按预约+核销扣减

-- ----------------------------
-- 1) 预约表新增预约类型
-- ----------------------------
-- MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，改为 information_schema 兼容写法
SET @db_name = DATABASE();
SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course_reservation' AND COLUMN_NAME = 'reservation_type') = 0,
  "ALTER TABLE `fit_course_reservation` ADD COLUMN `reservation_type` varchar(30) DEFAULT 'card' COMMENT '预约类型（store_experience/card）' AFTER `card_id`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- 历史数据回填：有卡项默认卡项预约，无卡默认到店体验预约
UPDATE `fit_course_reservation`
SET `reservation_type` = CASE WHEN `card_id` IS NULL THEN 'store_experience' ELSE 'card' END
WHERE `reservation_type` IS NULL OR `reservation_type` = '';

-- 到店体验预约统一按免费模式
UPDATE `fit_course_reservation`
SET `consume_mode` = 'free'
WHERE `reservation_type` = 'store_experience';

-- 为时间卡历史预约兜底改为free（新版本业务已禁止时间卡预约）
UPDATE `fit_course_reservation` r
INNER JOIN `fit_member_card` mc ON mc.card_id = r.card_id AND mc.del_flag = '0'
SET r.consume_mode = 'free'
WHERE r.del_flag = '0'
  AND mc.plan_type = 'time_card';

-- ----------------------------
-- 2) 索引优化
-- ----------------------------
SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course_reservation' AND INDEX_NAME = 'idx_fit_reservation_type') = 0,
  "ALTER TABLE `fit_course_reservation` ADD INDEX `idx_fit_reservation_type` (`tenant_id`, `reservation_type`, `reservation_status`)",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ----------------------------
-- 3) 卡项方案&课程结构补充（私教/小班包自动建课）
-- ----------------------------
SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND COLUMN_NAME = 'custom_coach_id') = 0,
  "ALTER TABLE `fit_card_plan` ADD COLUMN `custom_coach_id` bigint DEFAULT NULL COMMENT '私教/小班包绑定教练ID' AFTER `is_private_bind_coach`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND COLUMN_NAME = 'custom_member_ids') = 0,
  "ALTER TABLE `fit_card_plan` ADD COLUMN `custom_member_ids` varchar(255) DEFAULT NULL COMMENT '私教/小班包指定会员ID串（1-5名）' AFTER `custom_coach_id`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND COLUMN_NAME = 'bind_course_id') = 0,
  "ALTER TABLE `fit_card_plan` ADD COLUMN `bind_course_id` bigint DEFAULT NULL COMMENT '自动关联基础课程ID' AFTER `custom_member_ids`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course' AND COLUMN_NAME = 'source_plan_id') = 0,
  "ALTER TABLE `fit_course` ADD COLUMN `source_plan_id` bigint DEFAULT NULL COMMENT '来源卡项方案ID（自动建课）' AFTER `support_card_type`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course' AND COLUMN_NAME = 'auto_generated') = 0,
  "ALTER TABLE `fit_course` ADD COLUMN `auto_generated` char(1) DEFAULT '0' COMMENT '是否自动生成（0否 1是）' AFTER `source_plan_id`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND INDEX_NAME = 'idx_fit_plan_bind_course') = 0,
  "ALTER TABLE `fit_card_plan` ADD INDEX `idx_fit_plan_bind_course` (`tenant_id`, `bind_course_id`)",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course' AND INDEX_NAME = 'idx_fit_course_source_plan') = 0,
  "ALTER TABLE `fit_course` ADD INDEX `idx_fit_course_source_plan` (`tenant_id`, `source_plan_id`)",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 4) 联调核对SQL（可选执行）
-- ----------------------------
-- SELECT reservation_type, reservation_status, COUNT(1)
-- FROM fit_course_reservation
-- WHERE del_flag = '0'
-- GROUP BY reservation_type, reservation_status;

