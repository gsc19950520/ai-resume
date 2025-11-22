# AI简历优化小程序后端

## 项目介绍

差异化AI简历优化小程序后端服务，支持岗位模板化、一键导出PDF/Word、历史记录、AI评分、批量优化等功能。

## 技术栈

- Java 8
- Spring Boot 2.7.x
- Spring Data JPA
- MySQL
- Redis
- Maven
- JWT
- 阿里云OSS
- OpenAI API

## 功能特性

- **用户管理**：微信登录、用户信息管理、VIP状态管理
- **简历处理**：上传简历（PDF/Word）、AI优化、批量优化、历史记录
- **模板系统**：模板列表、免费模板、VIP模板、模板购买
- **订阅服务**：会员购买、优化次数包、模板包购买
- **文件导出**：PDF导出、Word导出、自定义模板导出
- **AI评分**：简历评分、优化建议

## 项目结构

```
├── src/main/java/com/aicv/airesume/
│   ├── AiResumeOptimizerApplication.java  # 应用主类
│   ├── config/                            # 配置类
│   ├── controller/                        # 控制器
│   ├── entity/                            # 实体类
│   ├── exception/                         # 异常处理
│   ├── repository/                        # 数据访问层
│   ├── service/                           # 业务逻辑层
│   │   └── impl/                          # 业务实现类
│   └── utils/                             # 工具类
├── src/main/resources/
│   ├── application.properties             # 应用配置
└── pom.xml                                # Maven配置
```

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 5.0+

### 2. 配置修改

修改 `src/main/resources/application.properties` 文件：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/ai_resume?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=root

# Redis配置
spring.redis.host=localhost
spring.redis.port=6379

# 微信小程序配置
wechat.app-id=your_app_id
wechat.app-secret=your_app_secret

# AI API配置

# 阿里云OSS配置
oss.endpoint=your_oss_endpoint
oss.access-key=your_oss_access_key
oss.secret-key=your_oss_secret_key
oss.bucket-name=your_bucket_name
```

### 3. 构建和运行

```bash
# 编译项目
mvn clean package

# 运行项目
java -jar target/ai-resume-optimizer-1.0.0.jar
```

## API接口说明

### 用户相关
- POST /api/user/login - 微信登录
- GET /api/user/info - 获取用户信息
- GET /api/user/vip/check - 检查VIP状态

### 简历相关
- POST /api/resume/upload - 上传简历
- POST /api/resume/batch-upload - 批量上传简历
- POST /api/resume/optimize - 优化简历
- POST /api/resume/batch-optimize - 批量优化简历
- GET /api/resume/history - 获取历史记录
- GET /api/resume/{id} - 获取简历详情
- GET /api/resume/export/pdf - 导出为PDF
- GET /api/resume/export/word - 导出为Word

### 模板相关
- GET /api/template/list - 获取模板列表
- GET /api/template/free - 获取免费模板
- GET /api/template/vip - 获取VIP模板

### 订阅相关
- POST /api/subscribe/buy/membership - 购买会员
- POST /api/subscribe/buy/optimize - 购买优化次数包
- POST /api/subscribe/buy/template - 购买模板包
- GET /api/subscribe/orders - 获取订单列表

## 注意事项

1. 首次运行需要创建数据库 `ai_resume`
2. 微信小程序相关配置需要替换为实际的AppID和AppSecret
3. AI API配置需要替换为实际的API密钥
4. 阿里云OSS配置需要替换为实际的账号信息

## 部署说明

推荐使用Docker容器化部署，或者部署到云服务器如阿里云、腾讯云等。

## License

MIT