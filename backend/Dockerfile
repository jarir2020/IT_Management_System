FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app /var/itsm/uploads/tickets /var/itsm/uploads/assets \
    && chown -R 10001:10001 /app /var/itsm/uploads
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/service-desk-0.1.0-SNAPSHOT.jar /app/service-desk.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/service-desk.jar"]
