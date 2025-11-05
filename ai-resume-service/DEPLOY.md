# AI 简历优化服务部署指南

## 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本
- Docker
- 微信云托管账号

## 部署到微信云托管

### 1. 配置环境变量

在微信云托管控制台中，为服务配置以下环境变量：

| 环境变量名 | 说明 | 默认值 |
|------------|------|--------|
| `DB_URL` | 数据库连接URL | `jdbc:mysql://localhost:3306/ai_resume?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `root` |
| `REDIS_HOST` | Redis主机地址 | `localhost` |
| `REDIS_PORT` | Redis端口 | `6379` |
| `REDIS_PASSWORD` | Redis密码 | 空 |
| `REDIS_DATABASE` | Redis数据库索引 | `0` |
| `WECHAT_APPID` | 微信小程序AppID | `your_app_id` |
| `WECHAT_APPSECRET` | 微信小程序AppSecret | `your_app_secret` |
| `OPENAI_API_KEY` | OpenAI API Key | `your_openai_api_key` |
| `TONGYI_API_KEY` | 通义千问API Key | `your_tongyi_api_key` |
| `WENXIN_API_KEY` | 文心一言API Key | `your_wenxin_api_key` |
| `OSS_ENDPOINT` | OSS端点 | `your_oss_endpoint` |
| `OSS_ACCESS_KEY` | OSS访问密钥 | `your_oss_access_key` |
| `OSS_SECRET_KEY` | OSS密钥 | `your_oss_secret_key` |
| `OSS_BUCKET_NAME` | OSS存储桶名称 | `your_bucket_name` |
| `SERVER_PORT` | 服务端口 | `8080` |
| `SPRING_PROFILES_ACTIVE` | Spring配置环境 | `prod` |

### 2. 修改 wxcloud.config.js

根据你的云托管环境信息，修改 `wxcloud.config.js` 文件中的 `envId` 字段。

### 3. 部署服务

使用微信云托管CLI工具或控制台上传代码并部署：

1. 安装微信云托管CLI：
```bash
npm install -g @cloudbase/cli
```

2. 登录：
```bash
cloudbase login
```

3. 部署服务：
```bash
cd d:\owner_project\mini-program\resume\ai-resume-service
cloudbase service deploy
```

### 4. 配置公网访问

在云托管控制台中，为服务配置公网访问，获取服务的公网域名。

## 本地开发

### 1. 修改配置文件

根据本地开发环境配置 `application.properties` 文件。

### 2. 启动服务

```bash
mvn spring-boot:run
```

或使用IDE直接运行 `AiResumeServiceApplication.java` 类。

## 注意事项

1. 确保数据库已创建并可访问
2. 首次部署时，服务会自动创建数据表结构
3. 配置的API Key需要有相应的权限
4. 建议在生产环境中关闭 `spring.jpa.show-sql` 选项
