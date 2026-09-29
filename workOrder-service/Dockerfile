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

RUN groupadd --system --gid 10001 workorder \
    && useradd --system --uid 10001 --gid workorder --home-dir /nonexistent --no-create-home --shell /usr/sbin/nologin workorder \
    && mkdir -p /data/workorder/upload /var/log/workorder /home/ruoyi \
    && ln -s /var/log/workorder /home/ruoyi/logs \
    && chown -R workorder:workorder /app /data/workorder/upload /var/log/workorder /home/ruoyi

USER workorder:workorder
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/workorder-service.jar"]
