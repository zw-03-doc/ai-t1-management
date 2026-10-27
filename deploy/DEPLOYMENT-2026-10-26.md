# 2026-10-26 T1 部署记录

北京时间 17:59:49 已将 `20261026-fitness-r4-final2` 设为当前发布，会员／教练 H5 对应源码 `024ab30f7a805b9be14deed1df04c9b5808cd7a2`。此次只更新T1 Web容器中的H5，管理后台与网关配置继续使用r2，API继续使用16:36发布的r3（源码 `9adf0aa`），数据库和Redis不变。完整r3发布目录与发布回执备份均保留。

| 项目 | 实际结果 |
| --- | --- |
| 实例 | `i-7xvbo7bswbug7bn1m8gt`，实例内文件已核对 |
| 公网地址 | `8.148.222.89` |
| 管理端 | `https://t1.wedodiary.top/` |
| 会员／教练 H5 | `https://t1.wedodiary.top/app/` |
| API | `https://t1.wedodiary.top/prod-api/` |
| 当前发布 | `/opt/t1-management/current` → `/opt/t1-management/releases/20261026-fitness-r4-final2` |
| H5发布包 SHA-256 | `36ff9d000f60514e22148c4e79145c41cd0de489d6f311e0bdf31b822a5792ff` |
| H5 manifest SHA-256 | `fc62dc93d31cbd8e69eccd8bec0bac6f63a24ec0da7cc1ec883df751fb1a1592` |
| API发布 | `20261026-fitness-r3`，本次未重建 |
| 后端 JAR SHA-256 | `8d9fa387cbde1bf5e81e7ff4a1f6ede05fb1154750cf5f13020b7d67e10e77de`，保持不变 |
| 管理后台发布 | `20261026-fitness-r2`，476个文件保持不变 |

## H5改版发布与验证（r4-final2）

- 先核对源码与 `024ab30` 一致且前端工作区无未提交修改，再打包最新H5。发布包61个文件逐项校验通过；manifest记录17个改版页面、24项动作说明，以及5张与改版前Git版本逐字节一致的原图。
- 新H5共59个文件，全部通过真实HTTPS请求与SHA-256逐项校验，包含页面入口、分包JS、CSS和图片。管理后台476个文件与r2 manifest逐项一致，管理主页及3个入口资源另外通过HTTP内容哈希检查。
- 只执行指定 `web` 服务的 `up --no-deps --no-build --pull never`。API、MySQL、Redis、OPS Web/API/PostgreSQL共6个受保护容器的ID、启动时间、重启计数完全不变；共用网关未reload，未运行任何SQL。
- 发布前后管理端与H5均HTTP200，`/prod-api/auth/code` HTTP200且业务码200；OPS为200，studio为302，结果一致。18:00:26从独立服务器再次核对系统可信证书链、准确主机名和TLS 1.3，H5主页字节与最终构建一致。
- 新compose明确固定API JAR及配置挂载r3、管理后台及 `web.conf` 挂载r2、仅H5挂载r4-final2。因此 `current` 表示当前组合发布，不能把API误记为已升级到r4。
- 先前r4、r4-final仅作为历史候选保留，不是回退目标。第一次候选切换因校验脚本把 `/app/assets/` 拼成错误路径而自动回退；修正后先完整核验旧H5的51个文件，再发布包含微信禁用按钮样式修复的r4-final2。本段最终验证均针对r4-final2。

本次服务器证据：`/opt/t1-management/before-r4-final2.json`、当前回执 `deployment-receipt.json`、r4-final2目录中的 `manifest.json` 与 `static-verification.json`。切换前回执保存在 `/opt/t1-management/deployment-receipt-r3-before-ui-final2.json`；本机受限副本位于 `deploy/local/`，不提交Git。

如需回退本次UI，先仅恢复Web：

```sh
docker compose -f /opt/t1-management/releases/20261026-fitness-r2/compose.yml up --no-deps --no-build --pull never -d web
```

核对旧H5、管理端、API与受保护容器后，把 `current` 恢复到完整r3目录，并用 `deployment-receipt-r3-before-ui-final2.json` 恢复当前回执。无需数据库回退、重建API或重启OPS。

## 后端与基础设施验证（r3）

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
