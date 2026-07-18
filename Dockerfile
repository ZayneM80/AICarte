# ============================================================
# sky-take-out-ai — 多阶段 Docker 构建
# ============================================================
# Stage 1: 编译
FROM maven:3.8-eclipse-temurin-11 AS build
WORKDIR /build

# 先复制 POM 文件，利用 Docker 层缓存
COPY pom.xml ./
COPY sky-common/pom.xml sky-common/pom.xml
COPY sky-pojo/pom.xml sky-pojo/pom.xml
COPY sky-server/pom.xml sky-server/pom.xml
RUN mvn dependency:go-offline -B || true

# 复制全部源码并打包
COPY sky-common/src sky-common/src
COPY sky-pojo/src sky-pojo/src
COPY sky-server/src sky-server/src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: 运行
FROM eclipse-temurin:11-jre
WORKDIR /app

# 从 build 阶段复制 JAR
COPY --from=build /build/sky-server/target/sky-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
