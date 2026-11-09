-- A coach chooses the password for the coach workspace in WeChat.
-- A NULL hash keeps the existing coach web credential until it is changed.
SET @has_coach_web_password = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'fit_coach_wechat_bind' AND column_name = 'web_password_hash');
SET @ddl = IF(@has_coach_web_password = 0,
    'ALTER TABLE fit_coach_wechat_bind ADD COLUMN web_password_hash VARCHAR(60) NULL AFTER wechat_openid',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
