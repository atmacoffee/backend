FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests \
    && test -f /app/target/app.jar

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 -g root appuser
WORKDIR /app
COPY --from=build --chown=appuser:root /app/target/app.jar /app/app.jar
USER appuser
EXPOSE 8761
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 CMD curl -fsS http://127.0.0.1:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
