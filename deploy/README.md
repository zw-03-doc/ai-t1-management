# T1 独立部署

部署目标：阿里云实例 `i-7xvbo7bswbug7bn1m8gt`，公网地址 `8.148.222.89`，域名 `t1.wedodiary.top`。

管理端位于 `/`，会员与教练 H5 位于 `/app/`，两端使用 `/prod-api/`。微信小程序请求地址也为 `https://t1.wedodiary.top/prod-api`，需要在小程序管理平台配置合法请求与上传域名。

## 资源隔离

- 发布目录：`/opt/t1-management/releases/<release>`；当前发布指针：`/opt/t1-management/current`。
- 仅 T1 使用 `/etc/t1-management` 秘密配置与 `/var/lib/t1-management` 数据目录。
- Compose 项目名称固定 `t1-management`，使用独立 MySQL、Redis 和网络。数据库及缓存不开放宿主端口。
- T1 网关仅监听宿主现有 Docker 网桥 `172.19.0.1:18868`，由已有 HTTPS 入口转发。不得对外开放该端口。
- MySQL、Redis、API、Web 内存上限分别为 640/128/768/64 MiB；API Java 堆上限 448 MiB，MySQL连接上限40、应用连接池8。
- 原有 OPS 的 PostgreSQL、API、Web，以及 studio 服务不得替换或重建。初始化脚本只接受空的 T1 数据库；已有库必须通过具体增量迁移升级。

## 准备与初始化

1. 管理端构建设置 `VITE_APP_CONTEXT_PATH=/`、`VITE_APP_BASE_API=/prod-api`、`VITE_APP_ENCRYPT=false`；H5 构建路径 `/app/`。
2. 用 `stage_bundle.py --release <release> --jar <jar> --admin <admin-dist> --h5 <h5-dist>` 打包。发布包含文件SHA-256清单和明确SQL来源。不得把机器秘密打进发布包。
3. 上传并解包至发布目录。单独将随机管理员密码的 BCrypt 初始化语句交付 `/etc/t1-management/bootstrap-admin.sql`，权限0600。管理员明文密码仅存本机 `deploy/local/administrator.json`，该目录已忽略且应限制ACL。框架 `sys.user.initPassword` 同样设置独立随机强密码，并在该本机文件的 `coachInitialPassword` 字段交付；管理员创建教练后须在系统用户管理为每位教练重置独立密码并单独交付本人，不沿用框架默认密码。
4. 在新发布目录执行 `python3 initialize_server.py`。脚本随机生成各用途独立秘密、启动MySQL/Redis、仅在空库执行基线与按版本排序的迁移，并禁用示例账号、移除示例OSS配置。新库明确跳过旧 `V1.1.3`，其约束由不删除历史的 `V1.2.0` 替代。数据库成功后删除服务器管理员初始化语句并写入初始化标记。
5. 配置匹配本应用 AppID 的真实微信 secret 到 `/etc/t1-management/production.env`。没有匹配 secret 时保留缺项，不复制另一个应用的密钥。
6. `docker compose -f <release>/compose.yml up -d api web` 启动新应用；检查API、静态页面和实际登录后更新 `current` 指针。该步骤不会改变 OPS 的容器。

媒体挂载至 API `/app/media`，由 `fitness.media.path` 指定。媒体通过应用鉴权接口读取；不要把整个媒体目录交给 Nginx 公开列目录。

## 共用入口的持久配置

现有 OPS Web 容器占用80/443，并把 `/etc/wedo-studio/nginx.conf` 挂载为 `/etc/nginx/templates/studio.conf.template`。TLS目录 `/etc/sunmoonlake-ops/tls` 已只读挂载在 `/etc/nginx/tls`。

为了不重建正在运行的入口，新 T1 配置保存到宿主 `/etc/sunmoonlake-ops/tls/t1/gateway.conf`。在上述 **宿主持久模板** 末尾添加唯一一行 `include /etc/nginx/tls/t1/gateway.conf;`，并把同一 include 添加到容器现行 `/etc/nginx/conf.d/studio.conf`。修改前分别备份；先 `nginx -t`，通过才 reload。若验证失败恢复原文件。不能只改容器内临时配置，否则容器重建会丢失新入口。

后续 OPS 发布必须保留 studio 模板挂载、T1 include 及TLS目录；不能用仓库旧 Compose 覆盖线上额外挂载。T1发布只更新自己的后端与静态容器。

证书由本机 acme.sh HTTP-01 申请，challenge webroot沿用 `/var/lib/sunmoonlake-ops/acme-webroot`；T1证书安装到 `/etc/sunmoonlake-ops/tls/t1/{fullchain.pem,privkey.pem}`。既有 `sunmoonlake-ops-cert-renew.timer` 调用 acme cron，会覆盖已登记的T1证书；T1安装时登记独立reload hook，hook验证现行Nginx配置后reload。不能把自签名证书当作正式验收。

## 升级、回退与验收

升级前备份T1数据库和媒体，保存当前发布指针、服务镜像与配置摘要。先执行向后兼容迁移，再把API/Web指向新的发布目录。回退只切换兼容的应用和静态资源，不恢复旧数据库覆盖新业务数据。

上线须实际检查：DNS A记录、可信TLS、管理端与H5静态资源、后端登录、角色授权、预约/签到和训练计划；构建通过不能替代这些检查。还需比较原OPS三容器ID、`https://ops.wedodiary.top/` 与 `https://studio.wedodiary.top/` 状态。微信开发工具打开、真实微信身份、上传与正式发布分别记录，不相互替代。

云CLI凭据只从已有授权服务器配置读入短时进程环境，不复制到T1机器或输出日志。相关官方参考：[CLI环境变量](https://www.alibabacloud.com/help/en/cli/environment-variables)。
