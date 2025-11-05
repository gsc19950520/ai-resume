# 微信小程序AI简历助手部署指南

本文档详细介绍如何将AI简历助手的前后端项目部署到微信云托管环境。

## 项目结构

```
mini-program/resume/
├── ai-resume-service/      # 后端Spring Boot服务
│   ├── Dockerfile          # 后端服务Docker构建文件
│   ├── wxcloud.config.js   # 微信云托管配置
│   └── src/                # 后端源代码
├── ai-resume-miniprogram/  # 前端小程序
│   ├── app.js              # 小程序入口文件
│   └── wxcloud.config.js   # 前端静态网站托管配置
└── DEPLOY_GUIDE.md         # 部署指南（当前文件）
```

## 一、后端服务部署

### 1. 环境准备

- 微信公众平台账号（已开通云开发功能）
- 微信开发者工具
- 微信云托管CLI工具（`npm install -g wxcloud-cli`）

### 2. 配置环境变量

在微信云托管控制台中，为后端服务配置以下环境变量：

| 环境变量名 | 描述 | 默认值 | 是否必须 |
|-----------|------|-------|--------|
| SPRING_PROFILES_ACTIVE | Spring配置文件环境 | prod | 是 |
| SERVER_PORT | 服务端口 | 8080 | 是 |
| DB_URL | 数据库连接URL | - | 是 |
| DB_USERNAME | 数据库用户名 | - | 是 |
| DB_PASSWORD | 数据库密码 | - | 是 |
| REDIS_HOST | Redis主机地址 | - | 否 |
| REDIS_PORT | Redis端口 | 6379 | 否 |
| REDIS_PASSWORD | Redis密码 | - | 否 |
| OPENAI_API_KEY | OpenAI API密钥 | - | 是 |
| WECHAT_APPID | 微信小程序AppID | - | 是 |
| WECHAT_SECRET | 微信小程序密钥 | - | 是 |

### 3. 部署步骤

#### 方式一：使用微信云托管CLI部署

```bash
# 进入后端服务目录
cd ai-resume-service

# 登录微信云托管
wxcloud login

# 部署服务
wxcloud deploy
```

#### 方式二：通过微信开发者工具部署

1. 打开微信开发者工具
2. 导入项目，选择`ai-resume-service`目录
3. 在云开发面板中，选择「云托管」
4. 点击「新建服务」，填写服务名称
5. 选择「本地上传代码」，上传整个后端目录
6. 等待构建完成

### 4. 配置公网访问

部署成功后，在云托管控制台为后端服务开启公网访问权限，获取服务访问地址。

## 二、前端小程序部署

### 1. 静态网站托管部署

前端小程序使用静态网站托管方式部署：

```bash
# 进入前端目录
cd ai-resume-miniprogram

# 登录微信云托管
wxcloud login

# 部署静态网站
wxcloud deploy
```

### 2. 修改小程序配置

在小程序中，需要配置以下信息：

1. 在`app.js`中更新云开发环境ID：

```javascript
// app.js
globalData: {
  // ...
  useCloud: true, // 使用云托管
  cloudEnvId: 'your-env-id' // 替换为你的云开发环境ID
}
```

### 3. 小程序发布

1. 完成代码开发和调试后，在微信开发者工具中提交审核
2. 审核通过后，发布小程序正式版本

## 三、注意事项

### 1. 安全性

- 所有敏感信息（如数据库密码、API密钥等）必须通过环境变量配置，不要硬编码在代码中
- 定期更新密钥和密码
- 配置适当的访问控制策略

### 2. 性能优化

- 合理配置容器资源（CPU、内存）
- 根据业务需求设置合适的实例数量
- 配置CDN加速静态资源访问

### 3. 监控与日志

- 开启云托管的监控和日志功能
- 设置合理的告警策略
- 定期查看日志，及时发现和解决问题

### 4. 版本管理

- 建议使用Git管理代码版本
- 每次部署前进行充分测试
- 保存部署历史记录

## 四、常见问题

### 1. 云托管调用失败

- 检查云开发环境ID是否正确
- 确认后端服务是否正常运行
- 验证网络连接是否正常

### 2. 数据库连接失败

- 检查数据库连接字符串是否正确
- 确认数据库服务是否运行正常
- 验证数据库用户名和密码

### 3. 小程序登录问题

- 检查微信小程序AppID和Secret是否正确
- 确认服务器域名是否已在小程序后台配置
- 查看网络请求日志，排查具体错误原因

---

如有其他问题，请参考[微信云托管官方文档](https://developers.weixin.qq.com/miniprogram/dev/wxcloud/basis/getting-started.html)或联系技术支持。