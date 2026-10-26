-- MySQL 8.0+: align only legacy fitness tenant identifiers with framework tables.
-- Framework sys_* and new fitness tables use utf8mb4_0900_ai_ci; legacy fitness
-- tables explicitly used utf8mb4_general_ci. Equality joins fail across them.
-- Keep VARCHAR(20), nullable/default/comment and all existing indexes. Do not
-- convert whole tables: other text columns and their uniqueness stay unchanged.
-- DDL commits implicitly. Back up before applying; repeated application is safe.

ALTER TABLE fit_activity MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_card_plan MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_checkin_record MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_coach MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_coach_wechat_bind MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_course MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_course_reservation MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_course_schedule MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_member MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_member_card MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_order MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_order_item MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_schedule_student MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';
ALTER TABLE fit_store MODIFY COLUMN tenant_id VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '000000' COMMENT '租户ID';

-- Verification: all 22 fit_* tenant_id columns must use utf8mb4_0900_ai_ci.
-- SELECT table_name, column_type, is_nullable, column_default, collation_name
-- FROM information_schema.columns WHERE table_schema=DATABASE()
-- AND table_name LIKE 'fit\_%' AND column_name='tenant_id' ORDER BY table_name;
-- SELECT COUNT(*) FROM fit_coach c JOIN sys_user u
-- ON u.user_id=c.user_id AND u.tenant_id=c.tenant_id;
