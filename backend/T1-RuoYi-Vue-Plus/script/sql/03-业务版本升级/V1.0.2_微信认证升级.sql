-- ============================================================
-- 文件名: T-fitness-wechat-auth-upgrade.sql
-- 用途: 会员无感登录 + 教练微信激活登录升级
-- 说明:
-- 1. 适用于一期健身业务库升级到 1.0.2；
-- 2. 会员侧改为 openid 自动注册/自动登录；
-- 3. 教练侧新增激活码与微信绑定中间表；
-- 4. 不建立物理外键，关联一致性由应用层事务保障。
-- ============================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1) 会员表：清理重复 openid，并补唯一约束
-- ----------------------------
UPDATE fit_member m
INNER JOIN (
    SELECT wx_openid, MIN(member_id) AS keep_member_id
    FROM fit_member
    WHERE del_flag = '0'
      AND wx_openid IS NOT NULL
      AND wx_openid <> ''
    GROUP BY wx_openid
    HAVING COUNT(1) > 1
) d ON d.wx_openid = m.wx_openid
SET m.wx_openid = NULL,
    m.update_time = NOW(),
    m.remark = CONCAT(
        IFNULL(m.remark, ''),
        CASE WHEN IFNULL(m.remark, '') = '' THEN '' ELSE '；' END,
        '1.0.2升级清理重复openid'
    )
WHERE m.member_id <> d.keep_member_id;

ALTER TABLE fit_member
    ADD UNIQUE KEY uk_fit_member_wx_openid (wx_openid, del_flag);

-- ----------------------------
-- 2) 教练表：新增激活码字段并补索引
-- ----------------------------
ALTER TABLE fit_coach
    ADD COLUMN activation_code VARCHAR(32) NULL COMMENT '教练激活码' AFTER mobile;

UPDATE fit_coach
SET activation_code = CONCAT(
        'A',
        LPAD(
            MOD(ABS(CRC32(CONCAT(CAST(coach_id AS CHAR), IFNULL(mobile, '0')))), 10000),
            4,
            '0'
        )
    )
WHERE activation_code IS NULL
   OR activation_code = '';

ALTER TABLE fit_coach
    MODIFY COLUMN activation_code VARCHAR(32) NOT NULL COMMENT '教练激活码';

ALTER TABLE fit_coach
    ADD KEY idx_fit_coach_activation_code (tenant_id, activation_code),
    ADD KEY idx_fit_coach_mobile_hire_status (tenant_id, mobile, hire_status);

-- ----------------------------
-- 3) 教练微信绑定中间表
-- ----------------------------
CREATE TABLE IF NOT EXISTS fit_coach_wechat_bind (
  bind_id bigint NOT NULL COMMENT '绑定ID',
  tenant_id varchar(20) DEFAULT '000000' COMMENT '租户ID',
  coach_id bigint NOT NULL COMMENT '教练ID',
  coach_mobile varchar(20) NOT NULL COMMENT '教练手机号',
  activation_code varchar(32) NOT NULL COMMENT '教练激活码',
  wechat_openid varchar(100) DEFAULT NULL COMMENT '微信openid',
  activate_time datetime DEFAULT NULL COMMENT '激活时间',
  bind_status char(1) DEFAULT '0' COMMENT '绑定状态（0正常 1停用）',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by bigint DEFAULT NULL COMMENT '修改人',
  update_time datetime DEFAULT NULL COMMENT '修改时间',
  del_flag char(1) DEFAULT '0' COMMENT '删除标记（0存在 2删除）',
  version int DEFAULT 0 COMMENT '版本号',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (bind_id),
  UNIQUE KEY uk_fit_coach_bind_coach (tenant_id, coach_id, del_flag),
  UNIQUE KEY uk_fit_coach_bind_mobile_code (tenant_id, coach_mobile, activation_code, del_flag),
  UNIQUE KEY uk_fit_coach_bind_openid (wechat_openid, del_flag),
  KEY idx_fit_coach_bind_status (tenant_id, bind_status),
  KEY idx_fit_coach_bind_mobile (tenant_id, coach_mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教练微信激活绑定表';

UPDATE fit_coach_wechat_bind b
INNER JOIN fit_coach c ON c.coach_id = b.coach_id
SET b.tenant_id = c.tenant_id,
    b.coach_mobile = c.mobile,
    b.activation_code = c.activation_code,
    b.bind_status = CASE WHEN c.hire_status = '1' THEN '1' ELSE '0' END,
    b.update_time = NOW(),
    b.del_flag = c.del_flag
WHERE b.del_flag = '0';

INSERT INTO fit_coach_wechat_bind (
    bind_id,
    tenant_id,
    coach_id,
    coach_mobile,
    activation_code,
    wechat_openid,
    activate_time,
    bind_status,
    create_dept,
    create_by,
    create_time,
    update_by,
    update_time,
    del_flag,
    version,
    remark
)
SELECT c.coach_id,
       c.tenant_id,
       c.coach_id,
       c.mobile,
       c.activation_code,
       NULL,
       NULL,
       CASE WHEN c.hire_status = '1' THEN '1' ELSE '0' END,
       c.create_dept,
       c.create_by,
       NOW(),
       c.update_by,
       NOW(),
       c.del_flag,
       0,
       '由1.0.2升级脚本初始化'
FROM fit_coach c
WHERE c.del_flag = '0'
  AND c.mobile IS NOT NULL
  AND c.mobile <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM fit_coach_wechat_bind b
      WHERE b.coach_id = c.coach_id
        AND b.del_flag = '0'
  );

-- ----------------------------
-- 4) 升级后建议检查
-- ----------------------------
SELECT member_id, member_no, wx_openid FROM fit_member WHERE del_flag = '0' ORDER BY create_time DESC LIMIT 10;
SELECT coach_id, coach_name, mobile, activation_code FROM fit_coach WHERE del_flag = '0' ORDER BY create_time DESC LIMIT 10;
SELECT bind_id, coach_id, coach_mobile, activation_code, wechat_openid, bind_status
FROM fit_coach_wechat_bind
WHERE del_flag = '0'
ORDER BY create_time DESC, bind_id DESC
LIMIT 20;
