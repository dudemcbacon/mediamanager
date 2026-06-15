# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --version --no-daemon

# gradle.properties carries the project version (CI bumps it each release); without it,
# project.version falls back to Gradle's "unspecified" default and the in-app footer shows "vunspecified".
COPY gradle.properties package.json package-lock.json tsconfig.json types.d.ts vite.config.ts ./
COPY src ./src

# Commit sha for the in-app version footer; .git isn't in the build context, so CI passes it in.
ARG GIT_SHA=""

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/workspace/node_modules \
    ./gradlew bootJar -Pvaadin.productionMode -PgitSha="$GIT_SHA" --no-daemon

RUN cp build/libs/*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

# gosu lets the entrypoint drop from root to the "app" user after optionally
# remapping it to a host-provided PUID/PGID (see docker-entrypoint.sh).
RUN apt-get update \
    && apt-get install -y --no-install-recommends gosu \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080
ENV JAVA_OPTS=""
# Starts as root so it can apply PUID/PGID, then execs the JVM as "app" via gosu.
ENTRYPOINT ["docker-entrypoint.sh"]
