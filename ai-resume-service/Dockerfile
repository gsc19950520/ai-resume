# 构建阶段
FROM maven:3.8.6-openjdk-8-slim AS builder
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 执行构建，跳过测试以加快构建速度
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:8-jre-slim
WORKDIR /app

# 从构建阶段复制构建好的jar文件
COPY --from=builder /app/target/ai-resume-optimizer-1.0.0.jar app.jar

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

# 暴露应用端口
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "app.jar"]
