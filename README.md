# AI T1 Management

T1 健身管理项目的统一代码仓库，包含 Java 后端、管理后台和 uni-app 客户端。

## 目录结构

```text
backend/T1-RuoYi-Vue-Plus/  Java 17 / Spring Boot 后端
frontend/T-plus-ui/         Vue 3 / Vite 管理后台
frontend/T-uniApp/          uni-app 客户端
docs/                       项目设计文档
```

## 本地环境

- JDK 17
- Maven 3.8+
- Node.js 20.19+
- npm 8.19+
- 微信开发者工具（小程序运行）；HBuilderX 可选

## 配置

仓库不保存真实密码、令牌或私钥。首次运行前，请从相邻的 `.example` 文件复制本地配置，并通过环境变量或本机配置填写真实值。

后端示例：

```powershell
Copy-Item backend/T1-RuoYi-Vue-Plus/ruoyi-admin/src/main/resources/application-dev.example.yml `
  backend/T1-RuoYi-Vue-Plus/ruoyi-admin/src/main/resources/application-dev.yml
```

管理后台示例：

```powershell
Copy-Item frontend/T-plus-ui/.env.development.example frontend/T-plus-ui/.env.development
```

## 启动

后端：

```powershell
Set-Location backend/T1-RuoYi-Vue-Plus
mvn clean package -DskipTests
```

管理后台：

```powershell
Set-Location frontend/T-plus-ui
npm ci --legacy-peer-deps
npm run dev
```

uni-app 会员与教练端（网页与小程序共用）：

```powershell
Set-Location frontend/T-uniApp
npm ci --legacy-peer-deps
npm run build:h5
npm run build:mp-weixin
```

网页产物为 `dist/h5`，微信开发者工具打开 `dist/mp-weixin`。默认后端地址为 `https://t1.wedodiary.top/prod-api`；本地联调可用 `VITE_API_BASE_URL` 覆盖。微信 AppID 保留 `wx556bef49cde7a986`，后端必须配置对应的 secret，并在微信平台配置合法域名。

## 业务与部署文档

- [课程、门店、教练、预约与签到规则](docs/fitness-booking-and-checkin.md)
- [训练计划、顺延、实际记录与附件授权](docs/training-design.md)
- [隔离部署、初始化、升级与回退](deploy/README.md)
- [2026-10-26 部署记录](deploy/DEPLOYMENT-2026-10-26.md)
- [2026-10-26 验证结果与待验证事项](docs/verification-2026-10-26.md)

管理端 `https://t1.wedodiary.top/`，会员/教练网页 `https://t1.wedodiary.top/app/`。价格只展示，课程咨询通过负责私教微信号进行，不接入线上支付。教练网页使用管理员创建的账号；会员网页使用小程序“我的”中生成的一次性登录码。
