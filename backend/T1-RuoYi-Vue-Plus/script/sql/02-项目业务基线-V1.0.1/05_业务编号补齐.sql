-- ============================================================
-- 文件名: T-fitness-biz-code-upgrade.sql
-- 用途: 健身业务编号补齐 + 字典缺口修复
-- 说明:
-- 1. 适用于一期健身业务库升级；
-- 2. 补充“技术主键 + 业务编号”双标识结构；
-- 3. 补齐管理端与移动端已使用但数据库尚未初始化的字典项。
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1) 表结构补充业务编号字段
-- ----------------------------
ALTER TABLE fit_coach
    ADD COLUMN coach_no VARCHAR(64) NULL COMMENT '教练编号' AFTER coach_id,
    ADD UNIQUE KEY uk_fit_coach_no (tenant_id, coach_no, del_flag);

ALTER TABLE fit_course
    ADD COLUMN course_no VARCHAR(64) NULL COMMENT '课程编号' AFTER course_id,
    ADD UNIQUE KEY uk_fit_course_no (tenant_id, course_no, del_flag);

ALTER TABLE fit_card_plan
    ADD COLUMN plan_no VARCHAR(64) NULL COMMENT '方案编号' AFTER plan_id,
    ADD UNIQUE KEY uk_fit_plan_no (tenant_id, plan_no, del_flag);

ALTER TABLE fit_course_schedule
    ADD COLUMN schedule_no VARCHAR(64) NULL COMMENT '排课编号' AFTER schedule_id,
    ADD UNIQUE KEY uk_fit_schedule_no (tenant_id, schedule_no, del_flag);

ALTER TABLE fit_course_reservation
    ADD COLUMN reservation_no VARCHAR(64) NULL COMMENT '预约编号' AFTER reservation_id,
    ADD UNIQUE KEY uk_fit_reservation_no (tenant_id, reservation_no, del_flag);

ALTER TABLE fit_checkin_record
    ADD COLUMN checkin_no VARCHAR(64) NULL COMMENT '签到编号' AFTER checkin_id,
    ADD UNIQUE KEY uk_fit_checkin_no (tenant_id, checkin_no, del_flag);

ALTER TABLE fit_order_item
    ADD COLUMN item_no VARCHAR(64) NULL COMMENT '订单明细编号' AFTER item_id,
    ADD UNIQUE KEY uk_fit_order_item_no (tenant_id, item_no, del_flag);

-- ----------------------------
-- 2) 历史数据回填业务编号
-- ----------------------------
UPDATE fit_coach
SET coach_no = CONCAT('COA', LPAD(RIGHT(CAST(coach_id AS CHAR), 8), 8, '0'))
WHERE coach_no IS NULL OR coach_no = '';

UPDATE fit_course
SET course_no = CONCAT('CRS', LPAD(RIGHT(CAST(course_id AS CHAR), 8), 8, '0'))
WHERE course_no IS NULL OR course_no = '';

UPDATE fit_card_plan
SET plan_no = CONCAT('PLN', LPAD(RIGHT(CAST(plan_id AS CHAR), 8), 8, '0'))
WHERE plan_no IS NULL OR plan_no = '';

UPDATE fit_course_schedule
SET schedule_no = CONCAT('SCH', LPAD(RIGHT(CAST(schedule_id AS CHAR), 8), 8, '0'))
WHERE schedule_no IS NULL OR schedule_no = '';

UPDATE fit_course_reservation
SET reservation_no = CONCAT('RSV', LPAD(RIGHT(CAST(reservation_id AS CHAR), 8), 8, '0'))
WHERE reservation_no IS NULL OR reservation_no = '';

UPDATE fit_checkin_record
SET checkin_no = CONCAT('CHK', LPAD(RIGHT(CAST(checkin_id AS CHAR), 8), 8, '0'))
WHERE checkin_no IS NULL OR checkin_no = '';

UPDATE fit_order_item
SET item_no = CONCAT('ITM', LPAD(RIGHT(CAST(item_id AS CHAR), 8), 8, '0'))
WHERE item_no IS NULL OR item_no = '';

-- ----------------------------
-- 3) 清理待补字典数据
-- ----------------------------
DELETE FROM sys_dict_data
WHERE dict_type IN (
    'fit_hire_status',
    'fit_reservation_source',
    'fit_consume_mode',
    'fit_checkin_status',
    'fit_checkin_type',
    'fit_order_source',
    'fit_pay_method'
);

DELETE FROM sys_dict_type
WHERE dict_type IN (
    'fit_hire_status',
    'fit_reservation_source',
    'fit_consume_mode',
    'fit_checkin_status',
    'fit_checkin_type'
);

-- ----------------------------
-- 4) 新增缺失字典类型
-- ----------------------------
INSERT INTO sys_dict_type VALUES (2042610000000000011, '000000', '健身教练任职状态', 'fit_hire_status', 103, 1, sysdate(), NULL, NULL, '健身房教练任职状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000012, '000000', '健身预约来源', 'fit_reservation_source', 103, 1, sysdate(), NULL, NULL, '健身房预约来源字典');
INSERT INTO sys_dict_type VALUES (2042610000000000013, '000000', '健身扣费模式', 'fit_consume_mode', 103, 1, sysdate(), NULL, NULL, '健身房预约扣费模式字典');
INSERT INTO sys_dict_type VALUES (2042610000000000014, '000000', '健身签到状态', 'fit_checkin_status', 103, 1, sysdate(), NULL, NULL, '健身房签到状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000015, '000000', '健身签到类型', 'fit_checkin_type', 103, 1, sysdate(), NULL, NULL, '健身房签到类型字典');

-- ----------------------------
-- 5) 新增/刷新字典数据
-- ----------------------------
-- 5.1 教练任职状态
INSERT INTO sys_dict_data VALUES (2042610000000001101, '000000', 1, '在职', '0', 'fit_hire_status', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '正常在职');
INSERT INTO sys_dict_data VALUES (2042610000000001102, '000000', 2, '停用', '1', 'fit_hire_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '已停用');

-- 5.2 预约来源
INSERT INTO sys_dict_data VALUES (2042610000000001201, '000000', 1, '后台', 'admin', 'fit_reservation_source', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '后台创建预约');
INSERT INTO sys_dict_data VALUES (2042610000000001202, '000000', 2, '小程序', 'mini', 'fit_reservation_source', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '小程序创建预约');
INSERT INTO sys_dict_data VALUES (2042610000000001203, '000000', 3, '教练端', 'coach_app', 'fit_reservation_source', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '教练端创建预约');

-- 5.3 扣费模式
INSERT INTO sys_dict_data VALUES (2042610000000001301, '000000', 1, '次卡扣次', 'times', 'fit_consume_mode', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '按次数扣减');
INSERT INTO sys_dict_data VALUES (2042610000000001302, '000000', 2, '储值扣费', 'amount', 'fit_consume_mode', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '按金额扣减');
INSERT INTO sys_dict_data VALUES (2042610000000001303, '000000', 3, '免费体验', 'free', 'fit_consume_mode', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '免费体验课');

-- 5.4 签到状态
INSERT INTO sys_dict_data VALUES (2042610000000001401, '000000', 1, '已签到', 'success', 'fit_checkin_status', '', 'success', 'Y', 103, 1, sysdate(), NULL, NULL, '签到成功');
INSERT INTO sys_dict_data VALUES (2042610000000001402, '000000', 2, '异常', 'reversed', 'fit_checkin_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '签到异常');

-- 5.5 签到类型
INSERT INTO sys_dict_data VALUES (2042610000000001501, '000000', 1, '普通签到', 'normal', 'fit_checkin_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '常规签到');
INSERT INTO sys_dict_data VALUES (2042610000000001502, '000000', 2, '手工签到', 'manual', 'fit_checkin_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '后台手工签到');
INSERT INTO sys_dict_data VALUES (2042610000000001503, '000000', 3, '补签', 'supplement', 'fit_checkin_type', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '课程补签');

-- 5.6 刷新订单来源，补齐教练端
INSERT INTO sys_dict_data VALUES (2042610000000000901, '000000', 1, '后台录单', 'admin', 'fit_order_source', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '后台创建订单');
INSERT INTO sys_dict_data VALUES (2042610000000000902, '000000', 2, '小程序下单', 'mini', 'fit_order_source', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '小程序创建订单');
INSERT INTO sys_dict_data VALUES (2042610000000000903, '000000', 3, '教练端录单', 'coach_app', 'fit_order_source', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '教练端创建订单');

-- 5.7 刷新支付方式，补齐手工确认
INSERT INTO sys_dict_data VALUES (2042610000000001001, '000000', 1, '微信支付', 'wechat', 'fit_pay_method', '', 'success', 'Y', 103, 1, sysdate(), NULL, NULL, '微信支付');
INSERT INTO sys_dict_data VALUES (2042610000000001002, '000000', 2, '现金', 'cash', 'fit_pay_method', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '现金支付');
INSERT INTO sys_dict_data VALUES (2042610000000001003, '000000', 3, '转账', 'transfer', 'fit_pay_method', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '银行转账');
INSERT INTO sys_dict_data VALUES (2042610000000001004, '000000', 4, '会员卡抵扣', 'card', 'fit_pay_method', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '会员卡抵扣');
INSERT INTO sys_dict_data VALUES (2042610000000001005, '000000', 5, '组合支付', 'mix', 'fit_pay_method', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '多种支付方式组合');
INSERT INTO sys_dict_data VALUES (2042610000000001006, '000000', 6, '手工确认', 'manual', 'fit_pay_method', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '手工确认支付');

-- ----------------------------
-- 6) 升级后建议检查
-- ----------------------------
SELECT coach_id, coach_no, coach_name FROM fit_coach ORDER BY create_time DESC LIMIT 10;
SELECT course_id, course_no, course_name FROM fit_course ORDER BY create_time DESC LIMIT 10;
SELECT plan_id, plan_no, plan_name FROM fit_card_plan ORDER BY create_time DESC LIMIT 10;
SELECT schedule_id, schedule_no, schedule_date FROM fit_course_schedule ORDER BY create_time DESC LIMIT 10;
SELECT reservation_id, reservation_no, reservation_time FROM fit_course_reservation ORDER BY create_time DESC LIMIT 10;
SELECT checkin_id, checkin_no, checkin_time FROM fit_checkin_record ORDER BY create_time DESC LIMIT 10;
SELECT item_id, item_no, item_name FROM fit_order_item ORDER BY create_time DESC LIMIT 10;
