SET NAMES utf8mb4;

-- ----------------------------
-- 1. 门店表
-- ----------------------------
DROP TABLE IF EXISTS `fit_store`;
CREATE TABLE `fit_store` (
  `store_id` bigint NOT NULL COMMENT '门店ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_code` varchar(64) NOT NULL COMMENT '门店编码',
  `store_name` varchar(100) NOT NULL COMMENT '门店名称',
  `store_short_name` varchar(50) DEFAULT NULL COMMENT '门店简称',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `province_code` varchar(20) DEFAULT NULL COMMENT '省编码',
  `city_code` varchar(20) DEFAULT NULL COMMENT '市编码',
  `district_code` varchar(20) DEFAULT NULL COMMENT '区县编码',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `business_hours` varchar(100) DEFAULT NULL COMMENT '营业时间',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '门店封面图',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`store_id`),
  UNIQUE KEY `uk_fit_store_code` (`tenant_id`, `store_code`, `del_flag`),
  KEY `idx_fit_store_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房门店表';

-- ----------------------------
-- 2. 会员表
-- ----------------------------
DROP TABLE IF EXISTS `fit_member`;
CREATE TABLE `fit_member` (
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `member_no` varchar(64) NOT NULL COMMENT '会员编号',
  `home_store_id` bigint DEFAULT NULL COMMENT '归属门店ID',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `gender` char(1) DEFAULT '0' COMMENT '性别（0未知 1男 2女）',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `wx_openid` varchar(100) DEFAULT NULL COMMENT '微信openid',
  `wx_unionid` varchar(100) DEFAULT NULL COMMENT '微信unionid',
  `register_source` varchar(30) DEFAULT 'mini' COMMENT '注册来源',
  `member_status` varchar(30) DEFAULT 'normal' COMMENT '会员状态',
  `last_store_id` bigint DEFAULT NULL COMMENT '最近消费门店ID',
  `last_visit_time` datetime DEFAULT NULL COMMENT '最近到店时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_fit_member_no` (`tenant_id`, `member_no`, `del_flag`),
  KEY `idx_fit_member_mobile` (`tenant_id`, `mobile`),
  KEY `idx_fit_member_store` (`home_store_id`),
  KEY `idx_fit_member_status` (`tenant_id`, `member_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房会员表';

-- ----------------------------
-- 3. 教练表
-- ----------------------------
DROP TABLE IF EXISTS `fit_coach`;
CREATE TABLE `fit_coach` (
  `coach_id` bigint NOT NULL COMMENT '教练ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `user_id` bigint DEFAULT NULL COMMENT '关联后台用户ID',
  `store_id` bigint DEFAULT NULL COMMENT '所属门店ID',
  `coach_name` varchar(50) NOT NULL COMMENT '教练姓名',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `gender` char(1) DEFAULT '0' COMMENT '性别（0未知 1男 2女）',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `skill_tags` varchar(255) DEFAULT NULL COMMENT '擅长标签',
  `level_code` varchar(30) DEFAULT NULL COMMENT '教练等级',
  `intro` text COMMENT '教练简介',
  `hire_status` char(1) DEFAULT '0' COMMENT '任职状态（0在职 1停用）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`coach_id`),
  KEY `idx_fit_coach_user` (`user_id`),
  KEY `idx_fit_coach_store` (`tenant_id`, `store_id`),
  KEY `idx_fit_coach_status` (`tenant_id`, `hire_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房教练表';

-- ----------------------------
-- 4. 课程表
-- ----------------------------
DROP TABLE IF EXISTS `fit_course`;
CREATE TABLE `fit_course` (
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `course_type` varchar(30) DEFAULT NULL COMMENT '课程类型',
  `store_id` bigint DEFAULT NULL COMMENT '归属门店ID',
  `default_coach_id` bigint DEFAULT NULL COMMENT '默认教练ID',
  `duration_minutes` int DEFAULT 60 COMMENT '课程时长（分钟）',
  `capacity` int DEFAULT 0 COMMENT '默认人数上限',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '课程封面图',
  `description` text COMMENT '课程介绍',
  `support_card_type` varchar(100) DEFAULT NULL COMMENT '支持卡项类型',
  `status` char(1) DEFAULT '0' COMMENT '状态（0上架 1下架）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`course_id`),
  KEY `idx_fit_course_store` (`tenant_id`, `store_id`),
  KEY `idx_fit_course_type` (`tenant_id`, `course_type`),
  KEY `idx_fit_course_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房课程表';

-- ----------------------------
-- 5. 排课表
-- ----------------------------
DROP TABLE IF EXISTS `fit_course_schedule`;
CREATE TABLE `fit_course_schedule` (
  `schedule_id` bigint NOT NULL COMMENT '排课ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '门店ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `coach_id` bigint DEFAULT NULL COMMENT '授课教练ID',
  `schedule_date` date NOT NULL COMMENT '排课日期',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `capacity` int DEFAULT 0 COMMENT '可预约人数',
  `reserved_count` int DEFAULT 0 COMMENT '已预约人数',
  `signed_count` int DEFAULT 0 COMMENT '已签到人数',
  `reservation_deadline` datetime DEFAULT NULL COMMENT '预约截止时间',
  `cancel_deadline` datetime DEFAULT NULL COMMENT '取消截止时间',
  `schedule_status` varchar(30) DEFAULT 'draft' COMMENT '排课状态',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`schedule_id`),
  KEY `idx_fit_schedule_store_date` (`tenant_id`, `store_id`, `schedule_date`),
  KEY `idx_fit_schedule_coach_date` (`tenant_id`, `coach_id`, `schedule_date`),
  KEY `idx_fit_schedule_course` (`course_id`),
  KEY `idx_fit_schedule_status` (`tenant_id`, `schedule_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房排课表';

-- ----------------------------
-- 6. 预约表
-- ----------------------------
DROP TABLE IF EXISTS `fit_course_reservation`;
CREATE TABLE `fit_course_reservation` (
  `reservation_id` bigint NOT NULL COMMENT '预约ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '门店ID',
  `schedule_id` bigint NOT NULL COMMENT '排课ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `card_id` bigint DEFAULT NULL COMMENT '会员卡/课包ID',
  `reservation_source` varchar(30) DEFAULT 'mini' COMMENT '预约来源',
  `reservation_status` varchar(30) DEFAULT 'reserved' COMMENT '预约状态',
  `reservation_time` datetime DEFAULT NULL COMMENT '预约时间',
  `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `consume_mode` varchar(30) DEFAULT 'times' COMMENT '扣费模式',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`reservation_id`),
  KEY `idx_fit_reservation_member` (`tenant_id`, `member_id`, `reservation_status`),
  KEY `idx_fit_reservation_schedule` (`tenant_id`, `schedule_id`, `reservation_status`),
  KEY `idx_fit_reservation_store` (`tenant_id`, `store_id`, `reservation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房课程预约表';

-- ----------------------------
-- 7. 签到核销表
-- ----------------------------
DROP TABLE IF EXISTS `fit_checkin_record`;
CREATE TABLE `fit_checkin_record` (
  `checkin_id` bigint NOT NULL COMMENT '签到ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '门店ID',
  `reservation_id` bigint DEFAULT NULL COMMENT '预约ID',
  `schedule_id` bigint DEFAULT NULL COMMENT '排课ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `card_id` bigint DEFAULT NULL COMMENT '会员卡/课包ID',
  `checkin_type` varchar(30) DEFAULT 'normal' COMMENT '签到类型',
  `checkin_time` datetime DEFAULT NULL COMMENT '签到时间',
  `consume_times` int DEFAULT 0 COMMENT '消耗次数',
  `consume_amount` decimal(10,2) DEFAULT 0.00 COMMENT '消耗金额',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人',
  `checkin_status` varchar(30) DEFAULT 'success' COMMENT '签到状态',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uk_fit_checkin_reservation` (`reservation_id`, `del_flag`),
  KEY `idx_fit_checkin_member` (`tenant_id`, `member_id`, `checkin_time`),
  KEY `idx_fit_checkin_store` (`tenant_id`, `store_id`, `checkin_time`),
  KEY `idx_fit_checkin_schedule` (`tenant_id`, `schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房签到核销表';

-- ----------------------------
-- 8. 卡项方案表
-- ----------------------------
DROP TABLE IF EXISTS `fit_card_plan`;
CREATE TABLE `fit_card_plan` (
  `plan_id` bigint NOT NULL COMMENT '卡项方案ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '适用门店ID',
  `plan_name` varchar(100) NOT NULL COMMENT '卡项名称',
  `plan_type` varchar(30) NOT NULL COMMENT '卡项类型',
  `sale_price` decimal(10,2) DEFAULT 0.00 COMMENT '销售价',
  `original_price` decimal(10,2) DEFAULT 0.00 COMMENT '原价',
  `valid_days` int DEFAULT 0 COMMENT '有效天数',
  `total_times` int DEFAULT 0 COMMENT '总次数',
  `gift_times` int DEFAULT 0 COMMENT '赠送次数',
  `stored_amount` decimal(10,2) DEFAULT 0.00 COMMENT '储值金额',
  `support_course_type` varchar(100) DEFAULT NULL COMMENT '支持课程类型',
  `is_private_bind_coach` char(1) DEFAULT '0' COMMENT '是否绑定教练（0否 1是）',
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
  PRIMARY KEY (`plan_id`),
  KEY `idx_fit_plan_store` (`tenant_id`, `store_id`, `status`),
  KEY `idx_fit_plan_type` (`tenant_id`, `plan_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房卡项方案表';

-- ----------------------------
-- 9. 会员持卡表
-- ----------------------------
DROP TABLE IF EXISTS `fit_member_card`;
CREATE TABLE `fit_member_card` (
  `card_id` bigint NOT NULL COMMENT '会员卡ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '发卡门店ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `plan_id` bigint NOT NULL COMMENT '卡项方案ID',
  `card_no` varchar(64) NOT NULL COMMENT '会员卡编号',
  `card_name` varchar(100) NOT NULL COMMENT '卡项名称快照',
  `plan_type` varchar(30) NOT NULL COMMENT '卡项类型快照',
  `bind_coach_id` bigint DEFAULT NULL COMMENT '绑定教练ID',
  `active_time` datetime DEFAULT NULL COMMENT '生效时间',
  `expire_time` datetime DEFAULT NULL COMMENT '到期时间',
  `total_times` int DEFAULT 0 COMMENT '总次数',
  `used_times` int DEFAULT 0 COMMENT '已用次数',
  `remain_times` int DEFAULT 0 COMMENT '剩余次数',
  `balance_amount` decimal(10,2) DEFAULT 0.00 COMMENT '剩余余额',
  `card_status` varchar(30) DEFAULT 'unused' COMMENT '卡状态',
  `order_id` bigint DEFAULT NULL COMMENT '来源订单ID',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`card_id`),
  UNIQUE KEY `uk_fit_member_card_no` (`tenant_id`, `card_no`, `del_flag`),
  KEY `idx_fit_member_card_member` (`tenant_id`, `member_id`, `card_status`),
  KEY `idx_fit_member_card_order` (`order_id`),
  KEY `idx_fit_member_card_expire` (`tenant_id`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房会员持卡表';

-- ----------------------------
-- 10. 订单主表
-- ----------------------------
DROP TABLE IF EXISTS `fit_order`;
CREATE TABLE `fit_order` (
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `store_id` bigint DEFAULT NULL COMMENT '门店ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `order_type` varchar(30) NOT NULL COMMENT '订单类型',
  `order_source` varchar(30) DEFAULT 'admin' COMMENT '订单来源',
  `order_status` varchar(30) DEFAULT 'pending' COMMENT '订单状态',
  `pay_method` varchar(30) DEFAULT NULL COMMENT '支付方式',
  `total_amount` decimal(10,2) DEFAULT 0.00 COMMENT '订单总金额',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `payable_amount` decimal(10,2) DEFAULT 0.00 COMMENT '应付金额',
  `paid_amount` decimal(10,2) DEFAULT 0.00 COMMENT '实付金额',
  `paid_time` datetime DEFAULT NULL COMMENT '支付时间',
  `trade_no` varchar(100) DEFAULT NULL COMMENT '第三方流水号',
  `cashier_user_id` bigint DEFAULT NULL COMMENT '收银员',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_fit_order_no` (`order_no`),
  KEY `idx_fit_order_member` (`tenant_id`, `member_id`, `order_status`),
  KEY `idx_fit_order_store_paid` (`tenant_id`, `store_id`, `paid_time`),
  KEY `idx_fit_order_type_status` (`tenant_id`, `order_type`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房订单主表';

-- ----------------------------
-- 11. 订单明细表
-- ----------------------------
DROP TABLE IF EXISTS `fit_order_item`;
CREATE TABLE `fit_order_item` (
  `item_id` bigint NOT NULL COMMENT '订单明细ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `item_type` varchar(30) NOT NULL COMMENT '明细类型',
  `item_ref_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `item_name` varchar(100) NOT NULL COMMENT '商品/卡项名称快照',
  `quantity` int DEFAULT 1 COMMENT '数量',
  `unit_price` decimal(10,2) DEFAULT 0.00 COMMENT '单价',
  `total_price` decimal(10,2) DEFAULT 0.00 COMMENT '小计金额',
  `discount_amount` decimal(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `real_amount` decimal(10,2) DEFAULT 0.00 COMMENT '实付金额',
  `grant_card_id` bigint DEFAULT NULL COMMENT '发放的会员卡ID',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  `version` int DEFAULT 0 COMMENT '版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`item_id`),
  KEY `idx_fit_order_item_order` (`order_id`),
  KEY `idx_fit_order_item_ref` (`tenant_id`, `item_type`, `item_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='健身房订单明细表';