#########################################################
# 1️⃣ 构建阶段
#########################################################
FROM maven:3.8.6-openjdk-11-slim AS builder
WORKDIR /app

# 复制依赖文件并下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

# 复制源代码并构建
COPY src ./src
RUN mvn package -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dno-snapshot-update

# 清理不必要的文件
RUN rm -rf /root/.m2
#########################################################
# 2️⃣ 运行阶段
######################################################### 运行阶段
FROM openjdk:11-jre-slim

WORKDIR /app

# 安装必要依赖（仅保留wkhtmltopdf和基本证书）
RUN apk add --no-cache \
    ca-certificates \
    wkhtmltopdf \
    tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 复制编译后的 jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8079

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV WKHTMLTOPDF_PATH=/usr/bin/wkhtmltopdf
ENV JAVA_OPTS="-Xmx300m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

# 创建云托管平台所需的生命周期钩子脚本目录和文件
RUN mkdir -p /app/cert && echo '#!/bin/sh' > /app/cert/initenv.sh && chmod +x /app/cert/initenv.sh

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]