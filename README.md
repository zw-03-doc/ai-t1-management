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
- HBuilderX（运行 uni-app 时需要）

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
npm ci
npm run dev
```

uni-app 客户端可使用 HBuilderX 打开 `frontend/T-uniApp`。

