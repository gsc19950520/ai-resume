#########################################################
# 1️⃣ 构建阶段
#########################################################
FROM maven:3.8.6-openjdk-11-slim AS builder
WORKDIR /app
COPY pom.xml . 
COPY src ./src
RUN mvn clean package -DskipTests
#########################################################
# 2️⃣ 运行阶段
######################################################### 运行阶段
FROM openjdk:11-jre-slim-buster
WORKDIR /app

# 安装必要依赖（仅保留wkhtmltopdf和基本证书）
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    wkhtmltopdf \
    && rm -rf /var/lib/apt/lists/*

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 复制编译后的 jar
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8079

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV WKHTMLTOPDF_PATH=/usr/bin/wkhtmltopdf
ENV JAVA_OPTS="-Xmx300m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:+UseStringDeduplication -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]