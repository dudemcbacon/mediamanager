# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --version --no-daemon

COPY package.json package-lock.json tsconfig.json types.d.ts vite.config.ts ./
COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/workspace/node_modules \
    ./gradlew bootJar -Pvaadin.productionMode --no-daemon

RUN cp build/libs/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --create-home --shell /usr/sbin/nologin app
USER app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
