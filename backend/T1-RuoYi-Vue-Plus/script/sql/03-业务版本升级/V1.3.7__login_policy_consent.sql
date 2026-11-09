CREATE TABLE IF NOT EXISTS fit_policy_consent (
    tenant_id VARCHAR(20) NOT NULL,
    account_role VARCHAR(12) NOT NULL,
    account_id BIGINT NOT NULL,
    policy_version VARCHAR(20) NOT NULL,
    first_agreed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_agreed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, account_role, account_id, policy_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健身会员与教练登录协议同意记录';
