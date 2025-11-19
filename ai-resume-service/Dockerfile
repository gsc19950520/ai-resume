#########################################################
# 1️⃣ 构建阶段
#########################################################
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /app
COPY pom.xml . 
COPY src ./src
RUN mvn clean package -DskipTests

#########################################################
# 2️⃣ 运行阶段
#########################################################
FROM openjdk:8-jre-slim
WORKDIR /app

# 安装 Puppeteer/Chrome 依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    fonts-wqy-microhei \
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libx11-xcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libpangocairo-1.0-0 \
    libasound2 \
    libxshmfence1 \
    xdg-utils \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 复制编译后的 jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
