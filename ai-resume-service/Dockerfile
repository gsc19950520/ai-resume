######################################################
# 1️⃣ 编译阶段：使用 Maven 构建项目
######################################################
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

# 先拉依赖再打包，加速构建
RUN mvn -B -q dependency:resolve dependency:resolve-plugins && \
    mvn clean package -DskipTests


######################################################
# 2️⃣ 运行阶段：安装 Chromium + 启动 Spring Boot
######################################################
FROM openjdk:8-jre-slim

WORKDIR /app

# ==== 安装 Chromium 及所有必要依赖 ====
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    chromium chromium-common chromium-driver \
    libasound2 libatk1.0-0 libatk-bridge2.0-0 \
    libatspi2.0-0 libcups2 libxkbcommon0 \
    libpangocairo-1.0-0 libgtk-3-0 libgbm-dev \
    libnss3 libxss1 fonts-wqy-microhei \
    tzdata ca-certificates wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*



# ==== 设置 Puppeteer 可执行浏览器路径 ====
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium


# ==== 设置时区（可选）====
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 复制编译后的 jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露服务端口
EXPOSE 8080

# 生产配置
ENV SPRING_PROFILES_ACTIVE=prod

# ==== 容器启动时运行 ====
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
