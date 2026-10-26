-- MySQL 8, one-time migration after V1.1.2. May also follow V1.1.3.
-- Back up an existing database first. Existing duplicate active bookings/checkins
-- deliberately cause index creation to fail; never silently delete business history.
ALTER TABLE fit_course
 ADD display_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '展示价格，无线上付款',
 ADD listing_days INT NOT NULL DEFAULT 30,
 ADD published_at DATETIME NULL,
 ADD listing_until DATETIME NULL,
 ADD booking_advance_days INT NOT NULL DEFAULT 7,
 ADD booking_close_minutes INT NOT NULL DEFAULT 30,
 ADD cancel_close_minutes INT NOT NULL DEFAULT 60,
 ADD checkin_before_minutes INT NOT NULL DEFAULT 30,
 ADD checkin_after_minutes INT NOT NULL DEFAULT 60;
ALTER TABLE fit_coach ADD contact_wechat VARCHAR(100) NULL COMMENT '公开咨询微信号';
ALTER TABLE fit_course_schedule
 ADD display_price DECIMAL(10,2) NOT NULL DEFAULT 0,
 ADD reservation_open_time DATETIME NULL;
UPDATE fit_course SET published_at=NOW(), listing_until=DATE_ADD(NOW(), INTERVAL listing_days DAY) WHERE published_at IS NULL;
UPDATE fit_course_schedule s INNER JOIN fit_course c ON c.course_id=s.course_id AND c.tenant_id=s.tenant_id
 SET s.reservation_open_time=DATE_SUB(DATE(s.start_time), INTERVAL c.booking_advance_days DAY)
 WHERE s.reservation_open_time IS NULL;

-- The previous unique status index prevents cancelling a second reservation.
-- Replace it with uniqueness only for active bookings while retaining all history.
SET @old_index = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE()
 AND table_name='fit_course_reservation' AND index_name='uk_member_schedule_status');
SET @drop_index_sql = IF(@old_index > 0, 'ALTER TABLE fit_course_reservation DROP INDEX uk_member_schedule_status', 'SELECT 1');
PREPARE t1_stmt FROM @drop_index_sql; EXECUTE t1_stmt; DEALLOCATE PREPARE t1_stmt;
ALTER TABLE fit_course_reservation
 ADD active_booking TINYINT GENERATED ALWAYS AS (CASE WHEN del_flag='0' AND reservation_status IN ('reserved','signed') THEN 1 ELSE NULL END) STORED,
 ADD UNIQUE KEY uk_active_booking(tenant_id, member_id, schedule_id, active_booking);
ALTER TABLE fit_checkin_record
 ADD active_checkin TINYINT GENERATED ALWAYS AS (CASE WHEN del_flag='0' THEN 1 ELSE NULL END) STORED,
 ADD UNIQUE KEY uk_active_checkin(tenant_id, reservation_id, active_checkin);

CREATE TABLE IF NOT EXISTS fit_behavior_log (
 behavior_id BIGINT NOT NULL PRIMARY KEY,
 tenant_id VARCHAR(20) NOT NULL,
 actor_id BIGINT NOT NULL,
 actor_type VARCHAR(20) NOT NULL,
 member_id BIGINT NULL, store_id BIGINT NULL, schedule_id BIGINT NULL,
 reservation_id BIGINT NULL, checkin_id BIGINT NULL,
 action VARCHAR(64) NOT NULL, source VARCHAR(20) NOT NULL,
 remark VARCHAR(1000) NULL, created_at DATETIME NOT NULL,
 KEY idx_behavior_tenant_time(tenant_id,created_at),
 KEY idx_behavior_member(tenant_id,member_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仅追加的健身业务行为记录';
CREATE TABLE IF NOT EXISTS fit_course_interest (
 interest_id BIGINT NOT NULL PRIMARY KEY,
 tenant_id VARCHAR(20) NOT NULL, member_id BIGINT NOT NULL, course_id BIGINT NOT NULL,
 coach_id BIGINT NULL, session_id VARCHAR(64) NOT NULL, event_id VARCHAR(64) NOT NULL,
 active_seconds INT NOT NULL, source VARCHAR(20) NOT NULL, created_at DATETIME NOT NULL,
 UNIQUE KEY uk_interest_event(tenant_id,member_id,event_id),
 KEY idx_interest_coach(tenant_id,coach_id,created_at),
 KEY idx_interest_member(tenant_id,member_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='前台可见时长，教练近90天兴趣汇总';

INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_time)
SELECT 2042601000000000901,'行为记录',2042590129557729281,8,'behavior','fitness/behavior/index',1,0,'C','0','0','fitness:behavior:list','list',NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id=2042601000000000901);
