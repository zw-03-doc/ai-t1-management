-- MySQL 8. Apply after the V1.1.x business migrations. No existing data is removed.
-- Schedule dates use Asia/Shanghai; audit timestamps are UTC.
CREATE TABLE IF NOT EXISTS fit_training_plan (
    plan_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    member_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(2000) NULL,
    visibility VARCHAR(10) NOT NULL DEFAULT 'private',
    cycle_days INT NOT NULL,
    days_json LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    creator_id BIGINT NOT NULL,
    creator_type VARCHAR(20) NOT NULL,
    start_date DATE NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_training_plan_tenant (tenant_id, plan_id),
    KEY idx_training_member (tenant_id, member_id, create_time),
    KEY idx_training_public (tenant_id, visibility, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员训练计划与可复制公开模板';

CREATE TABLE IF NOT EXISTS fit_training_workout (
    workout_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    plan_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    day_number INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    original_date DATE NOT NULL,
    planned_date DATE NOT NULL,
    performed_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    exercises_json LONGTEXT NOT NULL,
    PRIMARY KEY (workout_id),
    UNIQUE KEY uk_training_workout_tenant (tenant_id, workout_id),
    UNIQUE KEY uk_training_sequence (tenant_id, plan_id, sequence_number),
    KEY idx_training_workout_date (tenant_id, plan_id, planned_date),
    CONSTRAINT fk_training_workout_plan FOREIGN KEY (tenant_id, plan_id)
        REFERENCES fit_training_plan (tenant_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练日程与动作目标快照';

CREATE TABLE IF NOT EXISTS fit_training_set (
    set_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    workout_id BIGINT NOT NULL,
    set_number INT NOT NULL,
    exercise_key VARCHAR(64) NOT NULL,
    exercise_name VARCHAR(100) NOT NULL,
    set_type VARCHAR(20) NOT NULL COMMENT 'warmup/working',
    reps INT NOT NULL,
    weight DECIMAL(12,4) NOT NULL,
    unit VARCHAR(2) NOT NULL,
    weight_kg DECIMAL(16,6) NOT NULL,
    rpe DECIMAL(5,2) NULL,
    PRIMARY KEY (set_id),
    UNIQUE KEY uk_training_set_number (tenant_id, workout_id, set_number),
    CONSTRAINT fk_training_set_workout FOREIGN KEY (tenant_id, workout_id)
        REFERENCES fit_training_workout (tenant_id, workout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每组实际训练重量、热身与正式组';

CREATE TABLE IF NOT EXISTS fit_training_audit (
    audit_id BIGINT NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    plan_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    action VARCHAR(30) NOT NULL,
    detail LONGTEXT NOT NULL,
    create_time DATETIME NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_training_audit (tenant_id, plan_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练行为审计，与业务变更同事务写入';

CREATE TABLE IF NOT EXISTS fit_training_media (
    asset_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(20) NOT NULL,
    plan_id BIGINT NOT NULL,
    file_name VARCHAR(50) NOT NULL,
    media_type VARCHAR(30) NOT NULL,
    size BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    PRIMARY KEY (asset_id),
    UNIQUE KEY uk_training_media_tenant (tenant_id, asset_id),
    KEY idx_training_media_plan (tenant_id, plan_id),
    CONSTRAINT fk_training_media_plan FOREIGN KEY (tenant_id, plan_id)
        REFERENCES fit_training_plan (tenant_id, plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='受计划权限控制的动作图片与短视频';

CREATE TABLE IF NOT EXISTS fit_training_media_ref (
    tenant_id VARCHAR(20) NOT NULL,
    plan_id BIGINT NOT NULL,
    asset_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (tenant_id, plan_id, asset_id),
    KEY idx_training_media_ref_asset (tenant_id, asset_id),
    CONSTRAINT fk_training_media_ref_plan FOREIGN KEY (tenant_id, plan_id)
        REFERENCES fit_training_plan (tenant_id, plan_id),
    CONSTRAINT fk_training_media_ref_asset FOREIGN KEY (tenant_id, asset_id)
        REFERENCES fit_training_media (tenant_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复制公开模板后的附件引用授权，独立于源模板可见性';

-- Tenant administrators already have access; assign this explicit permission for other administrators.
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
SELECT 202610260080000001, '训练计划', 2042588802307977218, 12, 'training', 'fitness/training/index', '', 1, 0,
    'C', '0', '0', 'fitness:training:manage', 'chart', 103, 1, NOW(), '训练计划、记录、统计和行为审计；免费且无线上付费'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 202610260080000001);
