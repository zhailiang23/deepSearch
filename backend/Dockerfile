# 构建阶段
FROM openjdk:17-jdk-slim as builder

# 设置工作目录
WORKDIR /app

# 安装必要工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制Maven配置文件和wrapper
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./

# 确保mvnw具有执行权限
RUN chmod +x mvnw

# 下载依赖（Docker层缓存优化）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src/ src/

# 构建应用
RUN ./mvnw clean package -DskipTests

# 运行阶段
FROM openjdk:17-jre-slim

WORKDIR /app

# 创建非root用户
RUN groupadd -r spring && useradd -r -g spring spring

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 设置文件权限
RUN chown spring:spring app.jar

# 创建日志目录
RUN mkdir -p /app/logs && chown spring:spring /app/logs

# 切换到非root用户
USER spring

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]