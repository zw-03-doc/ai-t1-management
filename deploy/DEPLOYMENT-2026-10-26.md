# 2026-10-26 T1 部署记录

北京时间 16:36 已将 `20261026-fitness-r3` 设为当前发布，应用源码基准为 `9adf0aa`。本轮只替换T1 API，前端继续运行字节一致的r2产物，数据库不重复初始化。

| 项目 | 实际结果 |
| --- | --- |
| 实例 | `i-7xvbo7bswbug7bn1m8gt`，实例内文件已核对 |
| 公网地址 | `8.148.222.89` |
| 管理端 | `https://t1.wedodiary.top/` |
| 会员／教练 H5 | `https://t1.wedodiary.top/app/` |
| API | `https://t1.wedodiary.top/prod-api/` |
| 当前发布 | `/opt/t1-management/current` → `/opt/t1-management/releases/20261026-fitness-r3` |
| 发布包 SHA-256 | `25abea7237a2d37d9ee05bfc9bf5e91e22b77911c222c1c622ff1b106cf074cf` |
| 后端 JAR SHA-256 | `8d9fa387cbde1bf5e81e7ff4a1f6ede05fb1154750cf5f13020b7d67e10e77de` |

## 已执行的验证

- 从独立服务器在 16:06 查询 DNS，返回 `8.148.222.89`；HTTPS 按系统可信根和准确主机名验证通过，使用 TLS 1.3。
- Let's Encrypt 证书匹配 `t1.wedodiary.top`。首次签发遇到目标机到CA的出站网络问题，经本次临时SSH通道完成后关闭通道；16:40:58–16:42:30 又清除代理环境，直接从目标机执行一次真实强制续签，成功签发、安装并reload。16:46从独立服务器验证公网实际使用新证书，TLS 1.3及系统可信链通过，有效期至北京时间2027-01-24 15:43。既有续签timer为active，安装路径与reload hook已登记，临时通道端口未监听；这验证了当前可直接续签，未提前宣称未来定时周期已发生。
- r2包548个文件、最终r3包550个文件逐项 SHA-256 校验通过；r3所有前端文件与r2完全一致。管理端与H5主页、入口JS/CSS/图标均HTTP200，实际响应字节与最终构建一致；H5 `/app/` 路径正常。
- 最终后端成功启动，数据库连接与 Redis 正常；`/prod-api/auth/code` 返回业务码200。未登录访问会员门店、课程、主页及训练接口仍按鉴权规则拒绝，不能将HTTP200包装内的401说成业务成功。
- 正式库按顺序执行14份SQL。独立临时空库验证最初发现训练SQL列注释语法错误，修复后14份全部通过，确认训练6表；两轮临时库均已删除。新部署跳过旧V1.1.3以及开发OSS配置，执行V1.2.0和V1.2.1。
- 16:13:31 另外执行增量 `V1.2.2_教练基础角色.sql`，修复原V1.0.3把必需 `jiaolian` 角色仅写在注释而未初始化的问题。真实临时库连续执行两次，均只有一个默认租户教练角色、仅本人数据范围、零菜单授权；正式库执行及同样核验通过，临时库删除。保留既有验收数据，不重建服务。
- 16:19:52 执行增量 `V1.2.3_租户关联排序规则.sql`：14个旧业务表的 `tenant_id` 从 `utf8mb4_general_ci` 统一为框架与新表所用的 `utf8mb4_0900_ai_ci`，解决教练身份等跨表关联失败。先把正式T1全库备份并恢复到独立临时库，连续执行两次；临时及正式验证都确认列类型、NULL/default、全库索引结构和14表行数保持，身份/行为/兴趣/训练四类JOIN通过。正式验收数据保留、临时库删除，无需服务重启。r2包仍保留原14份清单，这两条增量单独记录，当前共应用16份SQL。
- T1独立MySQL、Redis、API、Web均运行，最终应用重启计数0、无OOM；MySQL/Redis健康检查通过。API使用生产配置，Java/数据库连接/Jackson统一北京时间。
- 原OPS三个容器ID保持不变，`ops.wedodiary.top` 返回200，`studio.wedodiary.top` 返回302；共用入口只追加独立vhost并reload，没有重建原业务容器。r3另外核对T1 Web/MySQL/Redis容器ID也保持不变，共6个受保护容器全部相同。

## 修复与配置说明

候选启动检查补齐了框架必需的 Redisson 超时和 `snail-job.port`；SnailJob及外部监控仍关闭。Nginx上游变量设置移至rewrite之前。r2和r3都包含这些修复。

初始32字符密码被原登录接口的30字符上限拒绝，后续已同步更新线上管理员 BCrypt、教练初始密码配置和本机受限凭据为独立24字符随机值。`prepare_admin_credentials.py` 固化长度检查与显式轮换，防止重复发生。实际密码仅在忽略目录 `deploy/local/administrator.json` 交付；目录及文件ACL限定当前Windows用户、SYSTEM和Administrators。

网关首次reload后立即验证曾命中尚未退出的旧worker证书，稍后完整可信TLS回读通过。`activate_gateway.py` 随后增加短暂重试，该工具修复已纳入r3。

真实业务验收发现训练媒体GET响应被框架标为JSON，r3后端修复响应MIME类型；相关训练17项、约课10项回归测试通过后重新构建。运行容器内JAR哈希已核对，真实HTTPS复测确认图片与真实MP4字节及MIME正确，61秒视频被拒绝。最新业务验收22项通过，历史失败与修复过程另行保留。

## 待完成的独立验证

- 管理员／教练的22项真实接口检查已通过，具体业务范围与验收数据清理由[验证报告](../docs/verification-2026-10-26.md)记录；接口检查不代替浏览器交互和真实微信身份验收。
- 保留小程序AppID `wx556bef49cde7a986`，AppSecret尚未提供。已有其他项目微信密钥的AppID不同，未混用。微信登录、真实会员签到和真机联调须在匹配密钥与平台合法域名配置后继续验收。
- 网页交互与视觉验收因本轮Windows UI工具无法识别浏览器URL而停止；未改用其他UI方式，也未把HTTP和文件哈希检查冒称浏览器交互验收。

服务器证据：`/opt/t1-management/deployment-receipt.json`、`certificate-renewal-direct.json`、`gateway-backups/20261026T160049/receipt.json`、`schema-check-v2/report.json`、`migrations/20261026-role-seed/receipt.json`、`migrations/20261026-tenant-collation/receipt.json`、r3发布目录中的 `manifest.json` 和前端r2目录中的 `static-verification.json`。V1.2.2 SHA-256为 `10cf9b645f65d79ebb03991f86f01e60832a7ee0cf19b9ec19613e20ca406350`，V1.2.3为 `9c265fca8e3c432d616bfa07a415637f4453bffcabeb7e84b749aedf0e124a4f`。本机受限副本：`deploy/local/deployment-receipt.json`、`certificate-renewal-direct.json`、`public-verification.json`、`role-seed-receipt.json`、`tenant-collation-receipt.json`。证据均不包含秘密。迁移前全库备份单独保存在服务器 `/opt/t1-management/backups/pre-tenant-collation-20261026.sql.gz`，权限0600，不放Git或公开交付包。
