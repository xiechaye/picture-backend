# 第一阶段：Maven构建
# 使用Java 17与pom.xml中的java.version保持一致
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 先复制pom.xml利用Docker缓存层加速依赖下载
COPY pom.xml .

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 使用Maven执行打包
RUN mvn clean package -DskipTests

# 第二阶段：最终运行时镜像
# 使用Java 17运行时镜像
FROM eclipse-temurin:17-jre

# 工作目录
WORKDIR /app

# 从Maven构建阶段复制JAR文件
# 注意：JAR文件名与pom.xml中的artifactId和version保持一致
COPY --from=build /app/target/picture-backend-0.0.1-SNAPSHOT.jar app.jar

# 暴露应用端口
EXPOSE 8123

# JVM优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 使用生产环境配置启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]