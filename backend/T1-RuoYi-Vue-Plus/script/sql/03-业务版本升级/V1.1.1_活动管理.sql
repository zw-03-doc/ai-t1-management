-- 版本 1.1.1：活动运营化 + 定向课程编排结构升级
-- 目标：
-- 1) 活动封面/详情图通过OSS上传回填URL，不再手填
-- 2) 活动仅保留两类：优惠卡项活动、定制推送活动
-- 3) 优惠卡项活动绑定卡项；私教/小班仅活动引导，不对外课程列表展示
-- 4) 为私教/小班定向排课预留学员绑定结构，支撑提醒与过期任务

-- ----------------------------
-- 1) 活动管理表（新建或增量升级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fit_activity` (
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '适用门店ID（为空表示全门店）',
  `activity_no` varchar(64) NOT NULL COMMENT '活动编号',
  `title` varchar(120) NOT NULL COMMENT '活动标题',
  `sub_title` varchar(200) DEFAULT NULL COMMENT '活动副标题',
  `price_highlight` varchar(120) DEFAULT NULL COMMENT '价格亮点',
  `target_audience` varchar(200) DEFAULT NULL COMMENT '适用人群',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '活动封面图URL',
  `detail_image_url` varchar(500) DEFAULT NULL COMMENT '活动详情图URL',
  `detail_content` text COMMENT '活动详情文案',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `countdown_end_time` datetime DEFAULT NULL COMMENT '倒计时截止时间',
  `weekly_featured` char(1) DEFAULT '0' COMMENT '是否本周主推（0否 1是）',
  `activity_type` varchar(30) DEFAULT 'card_discount' COMMENT '活动类型（card_discount/custom_push）',
  `action_type` varchar(20) DEFAULT 'detail' COMMENT '动作类型（detail/contact）',
  `discount_label` varchar(80) DEFAULT NULL COMMENT '折扣标签（如7折/立减100）',
  `rule_note` varchar(255) DEFAULT NULL COMMENT '规则备注（使用规则/附加优惠）',
  `sort_no` int DEFAULT 0 COMMENT '排序号',
  `status` char(1) DEFAULT '0' COMMENT '状态（0上架 1下架）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`activity_id`),
  UNIQUE KEY `uk_fit_activity_no` (`tenant_id`, `activity_no`, `del_flag`),
  KEY `idx_fit_activity_store` (`tenant_id`, `store_id`, `status`),
  KEY `idx_fit_activity_time` (`tenant_id`, `status`, `start_time`, `end_time`),
  KEY `idx_fit_activity_featured` (`tenant_id`, `weekly_featured`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房活动管理表';

-- MySQL 5.7 不支持 ADD COLUMN IF NOT EXISTS，改为 information_schema 兼容写法
SET @db_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_activity' AND COLUMN_NAME = 'activity_type') = 0,
  "ALTER TABLE `fit_activity` ADD COLUMN `activity_type` varchar(30) DEFAULT 'card_discount' COMMENT '活动类型（card_discount/custom_push）' AFTER `weekly_featured`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_activity' AND COLUMN_NAME = 'discount_label') = 0,
  "ALTER TABLE `fit_activity` ADD COLUMN `discount_label` varchar(80) DEFAULT NULL COMMENT '折扣标签（如7折/立减100）' AFTER `action_type`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_activity' AND COLUMN_NAME = 'rule_note') = 0,
  "ALTER TABLE `fit_activity` ADD COLUMN `rule_note` varchar(255) DEFAULT NULL COMMENT '规则备注（使用规则/附加优惠）' AFTER `discount_label`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- 历史动作目标字段在新规则下弃用，可保留兼容；如确认无存量依赖可解除注释执行
-- ALTER TABLE `fit_activity` DROP COLUMN IF EXISTS `action_target`;

-- ----------------------------
-- 2) 活动与卡项绑定关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fit_activity_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `plan_id` bigint NOT NULL COMMENT '卡项方案ID',
  `sort_no` int DEFAULT 0 COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fit_activity_plan` (`activity_id`, `plan_id`),
  KEY `idx_fit_activity_plan_activity` (`activity_id`, `sort_no`),
  KEY `idx_fit_activity_plan_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='活动绑定卡项关系表';

-- ----------------------------
-- 3) 私教/小班定向排课学员绑定表（支撑提醒与过期）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fit_schedule_student` (
  `schedule_student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',

  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `schedule_id` bigint NOT NULL COMMENT '排课ID',
  `member_id` bigint NOT NULL COMMENT '学员ID',
  `attendance_status` varchar(30) DEFAULT 'pending' COMMENT '出勤状态（pending/signed/expired）',
  `remind_status` char(1) DEFAULT '0' COMMENT '提醒状态（0未提醒 1已提醒）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  PRIMARY KEY (`schedule_student_id`),
  UNIQUE KEY `uk_fit_schedule_student` (`schedule_id`, `member_id`, `del_flag`),
  KEY `idx_fit_schedule_student_member` (`tenant_id`, `member_id`, `attendance_status`),
  KEY `idx_fit_schedule_student_schedule` (`tenant_id`, `schedule_id`, `attendance_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='定向排课学员绑定表';

-- ----------------------------
-- 4) 排课/卡项增强字段（定向私教包与小班）
-- ----------------------------
SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_course_schedule' AND COLUMN_NAME = 'schedule_mode') = 0,
  "ALTER TABLE `fit_course_schedule` ADD COLUMN `schedule_mode` varchar(30) DEFAULT 'normal' COMMENT '排课模式（normal/private/small_group）' AFTER `schedule_status`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND COLUMN_NAME = 'custom_coach_id') = 0,
  "ALTER TABLE `fit_card_plan` ADD COLUMN `custom_coach_id` bigint DEFAULT NULL COMMENT '定制私教包绑定教练ID' AFTER `is_private_bind_coach`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(1) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'fit_card_plan' AND COLUMN_NAME = 'custom_member_ids') = 0,
  "ALTER TABLE `fit_card_plan` ADD COLUMN `custom_member_ids` varchar(255) DEFAULT NULL COMMENT '定制私教包指定学员ID串（最多5人）' AFTER `custom_coach_id`",
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- ----------------------------
-- 5) 活动管理菜单与按钮（挂在卡项中心 parent_id=2042590365416026113）
-- ----------------------------
DELETE FROM sys_menu WHERE menu_id IN (
  '2042601000000000511','2042601000000000512','2042601000000000513','2042601000000000514','2042601000000000515','2042601000000000516','2042601000000000517'
);

INSERT INTO sys_menu VALUES ('2042601000000000511', '活动管理', '2042590365416026113', '2', 'activity', 'fitness/activity/index', '', 1, 0, 'C', '0', '0', 'fitness:activity:list', '#', 103, 1, sysdate(), NULL, NULL, '健身房活动管理菜单');
INSERT INTO sys_menu VALUES ('2042601000000000512', '活动查询', '2042601000000000511', 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:query', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000513', '活动新增', '2042601000000000511', 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:add', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000514', '活动修改', '2042601000000000511', 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000515', '活动删除', '2042601000000000511', 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:remove', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000516', '活动导出', '2042601000000000511', 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:export', '#', 103, 1, sysdate(), NULL, NULL, '');
INSERT INTO sys_menu VALUES ('2042601000000000517', '活动状态', '2042601000000000511', 6, '#', '', NULL, 1, 0, 'F', '0', '0', 'fitness:activity:edit', '#', 103, 1, sysdate(), NULL, NULL, '');
