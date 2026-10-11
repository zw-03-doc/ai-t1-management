-- ============================================================
-- 文件名: T-sys_oss_config.sql
-- 用途: 导入 ry_vue_5.X.sql 后，修正 sys_oss_config 中的 MinIO 配置
-- 适用场景: 本地开发连接云服务器 MinIO
-- 当前策略: 私有桶 + 后端预签名 URL 访问
-- 注意:
-- 1. 当前按你的云服务器公网 IP 120.26.18.27 配置
-- 2. 当前按你的 MinIO 账号 minioadmin / Minio@2026 配置
-- 3. 执行前请确保 MinIO 中已创建 bucket: ruoyi
-- 4. 本脚本仅切换系统配置，不会自动修改 MinIO 桶匿名访问策略
-- 5. 执行后请重启后端或刷新 OSS 配置缓存
-- ============================================================


SET NAMES utf8mb4;

-- ----------------------------
-- 1) 修正默认 minio 配置（默认启用）
-- ----------------------------
UPDATE sys_oss_config
SET tenant_id = '000000',
    config_key = 'minio',
    access_key = 'minioadmin',
    secret_key = 'Minio@2026',
    bucket_name = 'ruoyi',
    prefix = '',
    endpoint = '120.26.18.27:9000',
    domain = '',
    is_https = 'N',
    region = '',
    access_policy = '0',
    status = '0',
    ext1 = '',
    update_by = 1,
    update_time = sysdate(),
    remark = '云服务器 MinIO 默认配置（私有桶 + 预签名访问）'
WHERE oss_config_id = 1;


INSERT INTO sys_oss_config (
    oss_config_id, tenant_id, config_key, access_key, secret_key, bucket_name,
    prefix, endpoint, domain, is_https, region, access_policy, status,
    ext1, create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
    1, '000000', 'minio', 'minioadmin', 'Minio@2026', 'ruoyi',
    '', '120.26.18.27:9000', '', 'N', '', '0', '0',
    '', 103, 1, sysdate(), 1, sysdate(), '云服务器 MinIO 默认配置（私有桶 + 预签名访问）'
FROM DUAL

WHERE NOT EXISTS (
    SELECT 1 FROM sys_oss_config WHERE oss_config_id = 1
);

-- ----------------------------
-- 2) 修正 image 前缀配置（非默认）
-- ----------------------------
UPDATE sys_oss_config
SET tenant_id = '000000',
    config_key = 'image',
    access_key = 'minioadmin',
    secret_key = 'Minio@2026',
    bucket_name = 'ruoyi',
    prefix = 'image',
    endpoint = '120.26.18.27:9000',
    domain = '',
    is_https = 'N',
    region = '',
    access_policy = '0',
    status = '1',
    ext1 = '',
    update_by = 1,
    update_time = sysdate(),
    remark = '云服务器 MinIO 图片目录配置（私有桶 + 预签名访问）'
WHERE oss_config_id = 5;


INSERT INTO sys_oss_config (
    oss_config_id, tenant_id, config_key, access_key, secret_key, bucket_name,
    prefix, endpoint, domain, is_https, region, access_policy, status,
    ext1, create_dept, create_by, create_time, update_by, update_time, remark
)
SELECT
    5, '000000', 'image', 'minioadmin', 'Minio@2026', 'ruoyi',
    'image', '120.26.18.27:9000', '', 'N', '', '0', '1',
    '', 103, 1, sysdate(), 1, sysdate(), '云服务器 MinIO 图片目录配置（私有桶 + 预签名访问）'
FROM DUAL

WHERE NOT EXISTS (
    SELECT 1 FROM sys_oss_config WHERE oss_config_id = 5
);

-- ----------------------------
-- 3) 查看结果
-- ----------------------------
SELECT oss_config_id, config_key, bucket_name, prefix, endpoint, access_policy, status, remark
FROM sys_oss_config
WHERE oss_config_id IN (1, 5)
ORDER BY oss_config_id;

