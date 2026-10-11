-- 并发安全修复
-- 修复场景:
-- 1. 核销双花 (fit_checkin_record.operator_user_id IS NULL 防护)
-- 2. 超卖 (fit_course_schedule 原子容量检查)
-- 3. 重复预约 (唯一约束兜底)

-- 1. 清理重复预约记录，保留最早的一条
DELETE t1 FROM fit_course_reservation t1
INNER JOIN fit_course_reservation t2 ON t2.member_id = t1.member_id
    AND t2.schedule_id = t1.schedule_id
    AND t2.reservation_status = t1.reservation_status
    AND t2.del_flag = t1.del_flag
    AND t2.create_time < t1.create_time;

-- 2. 添加唯一约束，防止同一会员对同一排课有多个相同状态的预约
ALTER TABLE fit_course_reservation
    ADD UNIQUE INDEX uk_member_schedule_status (member_id, schedule_id, reservation_status(50), del_flag);
