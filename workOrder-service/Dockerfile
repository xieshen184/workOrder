FROM maven:3.9.9-eclipse-temurin-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY ruoyi-admin ruoyi-admin
COPY ruoyi-common ruoyi-common
COPY ruoyi-framework ruoyi-framework
COPY ruoyi-generator ruoyi-generator
COPY ruoyi-quartz ruoyi-quartz
COPY ruoyi-system ruoyi-system
COPY ruoyi-workorder ruoyi-workorder
RUN mvn -B clean package

FROM eclipse-temurin:8-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=builder /build/ruoyi-admin/target/ruoyi-admin.jar /app/workorder-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/workorder-service.jar"]
