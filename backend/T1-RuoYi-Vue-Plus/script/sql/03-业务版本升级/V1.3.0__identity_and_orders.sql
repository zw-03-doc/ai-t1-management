-- Preserve delayed-payment order coach selection. Safe to run repeatedly on MySQL 8.
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='fit_order' AND column_name='bind_coach_id')=0,
  'ALTER TABLE fit_order ADD COLUMN bind_coach_id BIGINT NULL COMMENT ''待支付订单选择的绑定教练'' AFTER member_id', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- Existing paid orders can recover their actual issued-card relationship. Pending orders intentionally remain NULL:
-- no historical selection was stored, so an administrator must choose the correct coach before confirming them.
UPDATE fit_order o JOIN (SELECT tenant_id, order_id, MIN(bind_coach_id) AS coach_id FROM fit_member_card
    WHERE del_flag='0' AND bind_coach_id IS NOT NULL GROUP BY tenant_id, order_id HAVING COUNT(DISTINCT bind_coach_id)=1) mc
 ON mc.tenant_id=o.tenant_id AND mc.order_id=o.order_id
SET o.bind_coach_id=mc.coach_id WHERE o.bind_coach_id IS NULL AND o.order_status='paid';
