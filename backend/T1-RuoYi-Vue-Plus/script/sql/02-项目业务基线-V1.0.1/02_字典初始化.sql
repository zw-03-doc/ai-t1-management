-- ============================================================
-- 文件名: T-fitness-dict-init.sql
-- 用途: 初始化健身房一期业务字典与基础系统参数
-- 说明:
-- 1. 依赖基础库脚本 `ry_vue_5.X.sql` 已先执行；
-- 2. 本脚本采用“先删后插 + 固定ID”方式，支持重复执行；
-- 3. 包含 `sys_dict_type`、`sys_dict_data` 以及一期建议的 `sys_config` 默认规则参数。
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1) 清理旧字典类型 / 字典数据
-- ----------------------------
DELETE FROM sys_dict_data
WHERE dict_type IN (
    'fit_member_status',
    'fit_course_type',
    'fit_schedule_status',
    'fit_reservation_status',
    'fit_plan_type',
    'fit_card_status',
    'fit_order_type',
    'fit_order_status',
    'fit_order_source',
    'fit_pay_method'
);

DELETE FROM sys_dict_type
WHERE dict_type IN (
    'fit_member_status',
    'fit_course_type',
    'fit_schedule_status',
    'fit_reservation_status',
    'fit_plan_type',
    'fit_card_status',
    'fit_order_type',
    'fit_order_status',
    'fit_order_source',
    'fit_pay_method'
);

-- ----------------------------
-- 2) 清理旧系统参数（一期规则默认值）
-- ----------------------------
DELETE FROM sys_config
WHERE config_key IN (
    'fit.reservation.advanceHours',
    'fit.reservation.cancelBeforeMinutes',
    'fit.checkin.allowBeforeMinutes',
    'fit.checkin.allowAfterMinutes'
);

-- ----------------------------
-- 3) 插入字典类型
-- ----------------------------
INSERT INTO sys_dict_type VALUES (2042610000000000001, '000000', '健身会员状态', 'fit_member_status', 103, 1, sysdate(), NULL, NULL, '健身房会员状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000002, '000000', '健身课程类型', 'fit_course_type', 103, 1, sysdate(), NULL, NULL, '健身房课程类型字典');
INSERT INTO sys_dict_type VALUES (2042610000000000003, '000000', '健身排课状态', 'fit_schedule_status', 103, 1, sysdate(), NULL, NULL, '健身房排课状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000004, '000000', '健身预约状态', 'fit_reservation_status', 103, 1, sysdate(), NULL, NULL, '健身房预约状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000005, '000000', '健身卡项类型', 'fit_plan_type', 103, 1, sysdate(), NULL, NULL, '健身房卡项方案类型字典');
INSERT INTO sys_dict_type VALUES (2042610000000000006, '000000', '健身会员卡状态', 'fit_card_status', 103, 1, sysdate(), NULL, NULL, '健身房会员卡状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000007, '000000', '健身订单类型', 'fit_order_type', 103, 1, sysdate(), NULL, NULL, '健身房订单类型字典');
INSERT INTO sys_dict_type VALUES (2042610000000000008, '000000', '健身订单状态', 'fit_order_status', 103, 1, sysdate(), NULL, NULL, '健身房订单状态字典');
INSERT INTO sys_dict_type VALUES (2042610000000000009, '000000', '健身订单来源', 'fit_order_source', 103, 1, sysdate(), NULL, NULL, '健身房订单来源字典');
INSERT INTO sys_dict_type VALUES (2042610000000000010, '000000', '健身支付方式', 'fit_pay_method', 103, 1, sysdate(), NULL, NULL, '健身房支付方式字典');

-- ----------------------------
-- 4) 插入字典数据
-- ----------------------------
-- 4.1 会员状态
INSERT INTO sys_dict_data VALUES (2042610000000000101, '000000', 1, '正常', 'normal', 'fit_member_status', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '正常会员');
INSERT INTO sys_dict_data VALUES (2042610000000000102, '000000', 2, '冻结', 'frozen', 'fit_member_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '冻结会员');
INSERT INTO sys_dict_data VALUES (2042610000000000103, '000000', 3, '流失', 'lost', 'fit_member_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '流失会员');

-- 4.2 课程类型
INSERT INTO sys_dict_data VALUES (2042610000000000201, '000000', 1, '团课', 'group', 'fit_course_type', '', 'success', 'Y', 103, 1, sysdate(), NULL, NULL, '团课');
INSERT INTO sys_dict_data VALUES (2042610000000000202, '000000', 2, '私教课', 'private', 'fit_course_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '私教课');
INSERT INTO sys_dict_data VALUES (2042610000000000203, '000000', 3, '小班课', 'small', 'fit_course_type', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '小班课');
INSERT INTO sys_dict_data VALUES (2042610000000000204, '000000', 4, '体验课', 'experience', 'fit_course_type', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '体验课');

-- 4.3 排课状态
INSERT INTO sys_dict_data VALUES (2042610000000000301, '000000', 1, '草稿', 'draft', 'fit_schedule_status', '', 'info', 'Y', 103, 1, sysdate(), NULL, NULL, '草稿排课');
INSERT INTO sys_dict_data VALUES (2042610000000000302, '000000', 2, '开放预约', 'open', 'fit_schedule_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '开放预约');
INSERT INTO sys_dict_data VALUES (2042610000000000303, '000000', 3, '已关闭', 'closed', 'fit_schedule_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '关闭预约');
INSERT INTO sys_dict_data VALUES (2042610000000000304, '000000', 4, '已取消', 'cancelled', 'fit_schedule_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '取消排课');
INSERT INTO sys_dict_data VALUES (2042610000000000305, '000000', 5, '已完成', 'finished', 'fit_schedule_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '已完成排课');

-- 4.4 预约状态
INSERT INTO sys_dict_data VALUES (2042610000000000401, '000000', 1, '已预约', 'reserved', 'fit_reservation_status', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '正常预约');
INSERT INTO sys_dict_data VALUES (2042610000000000402, '000000', 2, '已取消', 'cancelled', 'fit_reservation_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '已取消预约');
INSERT INTO sys_dict_data VALUES (2042610000000000403, '000000', 3, '已签到', 'signed', 'fit_reservation_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '已签到');
INSERT INTO sys_dict_data VALUES (2042610000000000404, '000000', 4, '缺席', 'absent', 'fit_reservation_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '未到场');
INSERT INTO sys_dict_data VALUES (2042610000000000405, '000000', 5, '请假', 'leave', 'fit_reservation_status', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '请假');

-- 4.5 卡项类型
INSERT INTO sys_dict_data VALUES (2042610000000000501, '000000', 1, '时效卡', 'time_card', 'fit_plan_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '月卡/季卡/年卡');
INSERT INTO sys_dict_data VALUES (2042610000000000502, '000000', 2, '次卡', 'count_card', 'fit_plan_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '次卡');
INSERT INTO sys_dict_data VALUES (2042610000000000503, '000000', 3, '储值卡', 'stored_card', 'fit_plan_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '储值卡');
INSERT INTO sys_dict_data VALUES (2042610000000000504, '000000', 4, '私教课包', 'private_package', 'fit_plan_type', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '私教课包');
INSERT INTO sys_dict_data VALUES (2042610000000000505, '000000', 5, '体验卡', 'experience', 'fit_plan_type', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '体验卡');

-- 4.6 会员卡状态
INSERT INTO sys_dict_data VALUES (2042610000000000601, '000000', 1, '未启用', 'unused', 'fit_card_status', '', 'info', 'Y', 103, 1, sysdate(), NULL, NULL, '未启用会员卡');
INSERT INTO sys_dict_data VALUES (2042610000000000602, '000000', 2, '生效中', 'active', 'fit_card_status', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '可正常使用');
INSERT INTO sys_dict_data VALUES (2042610000000000603, '000000', 3, '已冻结', 'frozen', 'fit_card_status', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '人工冻结');
INSERT INTO sys_dict_data VALUES (2042610000000000604, '000000', 4, '已过期', 'expired', 'fit_card_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '已过期');
INSERT INTO sys_dict_data VALUES (2042610000000000605, '000000', 5, '已耗尽', 'exhausted', 'fit_card_status', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '次数或余额已耗尽');

-- 4.7 订单类型
INSERT INTO sys_dict_data VALUES (2042610000000000701, '000000', 1, '购卡', 'buy_card', 'fit_order_type', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '购买卡项');
INSERT INTO sys_dict_data VALUES (2042610000000000702, '000000', 2, '充值', 'recharge', 'fit_order_type', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '充值订单');
INSERT INTO sys_dict_data VALUES (2042610000000000703, '000000', 3, '购课', 'buy_course', 'fit_order_type', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '购买课程');
INSERT INTO sys_dict_data VALUES (2042610000000000704, '000000', 4, '体验单', 'experience', 'fit_order_type', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '体验订单');

-- 4.8 订单状态
INSERT INTO sys_dict_data VALUES (2042610000000000801, '000000', 1, '待支付', 'pending', 'fit_order_status', '', 'warning', 'Y', 103, 1, sysdate(), NULL, NULL, '待支付订单');
INSERT INTO sys_dict_data VALUES (2042610000000000802, '000000', 2, '已支付', 'paid', 'fit_order_status', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '支付完成');
INSERT INTO sys_dict_data VALUES (2042610000000000803, '000000', 3, '已取消', 'cancelled', 'fit_order_status', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '已取消订单');
INSERT INTO sys_dict_data VALUES (2042610000000000804, '000000', 4, '已退款', 'refunded', 'fit_order_status', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '已退款订单');
INSERT INTO sys_dict_data VALUES (2042610000000000805, '000000', 5, '已关闭', 'closed', 'fit_order_status', '', 'default', 'N', 103, 1, sysdate(), NULL, NULL, '已关闭订单');

-- 4.9 订单来源
INSERT INTO sys_dict_data VALUES (2042610000000000901, '000000', 1, '后台录单', 'admin', 'fit_order_source', '', 'primary', 'Y', 103, 1, sysdate(), NULL, NULL, '后台创建订单');
INSERT INTO sys_dict_data VALUES (2042610000000000902, '000000', 2, '小程序下单', 'mini', 'fit_order_source', '', 'success', 'N', 103, 1, sysdate(), NULL, NULL, '小程序创建订单');

-- 4.10 支付方式
INSERT INTO sys_dict_data VALUES (2042610000000001001, '000000', 1, '微信支付', 'wechat', 'fit_pay_method', '', 'success', 'Y', 103, 1, sysdate(), NULL, NULL, '微信支付');
INSERT INTO sys_dict_data VALUES (2042610000000001002, '000000', 2, '现金', 'cash', 'fit_pay_method', '', 'warning', 'N', 103, 1, sysdate(), NULL, NULL, '现金支付');
INSERT INTO sys_dict_data VALUES (2042610000000001003, '000000', 3, '转账', 'transfer', 'fit_pay_method', '', 'primary', 'N', 103, 1, sysdate(), NULL, NULL, '银行转账');
INSERT INTO sys_dict_data VALUES (2042610000000001004, '000000', 4, '会员卡抵扣', 'card', 'fit_pay_method', '', 'info', 'N', 103, 1, sysdate(), NULL, NULL, '会员卡抵扣');
INSERT INTO sys_dict_data VALUES (2042610000000001005, '000000', 5, '组合支付', 'mix', 'fit_pay_method', '', 'danger', 'N', 103, 1, sysdate(), NULL, NULL, '多种支付方式组合');

-- ----------------------------
-- 5) 插入一期默认系统参数
-- ----------------------------
INSERT INTO sys_config VALUES (2042610000000002001, '000000', '健身预约-提前开放小时数', 'fit.reservation.advanceHours', '168', 'N', 103, 1, sysdate(), NULL, NULL, '默认支持提前 7 天预约');
INSERT INTO sys_config VALUES (2042610000000002002, '000000', '健身预约-最晚取消分钟数', 'fit.reservation.cancelBeforeMinutes', '60', 'N', 103, 1, sysdate(), NULL, NULL, '开课前 60 分钟内不允许取消');
INSERT INTO sys_config VALUES (2042610000000002003, '000000', '健身签到-允许提前签到分钟数', 'fit.checkin.allowBeforeMinutes', '30', 'N', 103, 1, sysdate(), NULL, NULL, '开课前 30 分钟允许签到');
INSERT INTO sys_config VALUES (2042610000000002004, '000000', '健身签到-允许补签分钟数', 'fit.checkin.allowAfterMinutes', '120', 'N', 103, 1, sysdate(), NULL, NULL, '开课后 120 分钟内允许补签到');

-- ----------------------------
-- 6) 查看结果
-- ----------------------------
SELECT dict_id, dict_name, dict_type
FROM sys_dict_type
WHERE dict_type IN (
    'fit_member_status',
    'fit_course_type',
    'fit_schedule_status',
    'fit_reservation_status',
    'fit_plan_type',
    'fit_card_status',
    'fit_order_type',
    'fit_order_status',
    'fit_order_source',
    'fit_pay_method'
)
ORDER BY dict_id;

SELECT dict_code, dict_sort, dict_label, dict_value, dict_type
FROM sys_dict_data
WHERE dict_type IN (
    'fit_member_status',
    'fit_course_type',
    'fit_schedule_status',
    'fit_reservation_status',
    'fit_plan_type',
    'fit_card_status',
    'fit_order_type',
    'fit_order_status',
    'fit_order_source',
    'fit_pay_method'
)
ORDER BY dict_type, dict_sort, dict_code;

SELECT config_id, config_name, config_key, config_value
FROM sys_config
WHERE config_key IN (
    'fit.reservation.advanceHours',
    'fit.reservation.cancelBeforeMinutes',
    'fit.checkin.allowBeforeMinutes',
    'fit.checkin.allowAfterMinutes'
)
ORDER BY config_id;
