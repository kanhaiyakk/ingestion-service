# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies first for faster rebuilds.
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -e -DskipTests clean package

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# curl only for the Compose healthcheck below; not needed by the app itself.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
# Source definitions ship alongside the jar.
COPY sources ./sources
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
