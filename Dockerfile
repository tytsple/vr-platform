# Multi-stage build for Spring Boot backend
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build

# Use Aliyun Maven mirror for faster downloads in China
COPY maven-settings.xml /root/.m2/settings.xml

# Copy POM files first for dependency caching
COPY pom.xml .
COPY vr-common/pom.xml vr-common/
COPY vr-framework/pom.xml vr-framework/
COPY vr-system/pom.xml vr-system/
COPY vr-vr/pom.xml vr-vr/
COPY vr-quartz/pom.xml vr-quartz/
COPY vr-admin/pom.xml vr-admin/

RUN mvn dependency:go-offline -B -q -s /root/.m2/settings.xml

# Copy source and build
COPY . .
RUN mvn package -DskipTests -B -q -s /root/.m2/settings.xml

# Runtime image
FROM eclipse-temurin:17-jre-alpine
COPY --from=build /build/vr-admin/target/vr-admin-*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
