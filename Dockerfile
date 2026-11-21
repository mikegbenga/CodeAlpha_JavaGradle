# syntax=docker/dockerfile:1.7
# CodeAlpha DevOps Task 3 — Java app built with Gradle

########## build ##########
FROM eclipse-temurin:24-jdk AS build
WORKDIR /src

# 1) wrapper + build files first, so dependency downloads are cached as their own layer
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --configuration runtimeClasspath > /dev/null

# 2) then the source
COPY src ./src
ARG APP_VERSION=1.0.0-dev
RUN ./gradlew --no-daemon fatJar -PappVersion=${APP_VERSION} \
 && cp build/libs/*-all.jar /src/app.jar

########## runtime ##########
FROM eclipse-temurin:24-jre-alpine AS runtime
ARG APP_VERSION=1.0.0-dev
LABEL org.opencontainers.image.title="codealpha-task-api" \
      org.opencontainers.image.description="CodeAlpha DevOps Task 3 - Java + Gradle" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.authors="Michael Gbenga Abajemito"

RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
COPY --from=build --chown=app:app /src/app.jar ./app.jar
USER 10001

ENV PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD ["wget", "-q", "--spider", "http://127.0.0.1:8080/health"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
