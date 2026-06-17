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

# New Relic Java agent: fetched from Maven Central here in the build stage and copied into the runtime
# image below. Attached via -javaagent only when NEW_RELIC_LICENSE_KEY is set (see docker-entrypoint.sh).
# Keep this version in sync with the `newrelic-agent` version in gradle/libs.versions.toml.
ARG NEW_RELIC_AGENT_VERSION=9.3.0
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && curl -fsSL -o newrelic.jar \
        "https://repo1.maven.org/maven2/com/newrelic/agent/java/newrelic-agent/${NEW_RELIC_AGENT_VERSION}/newrelic-agent-${NEW_RELIC_AGENT_VERSION}.jar" \
    && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:25-jre
WORKDIR /app

# gosu lets the entrypoint drop from root to the "app" user after optionally
# remapping it to a host-provided PUID/PGID (see docker-entrypoint.sh).
# ffmpeg provides the ffprobe binary used by the "Scan with FFprobe" action (FfprobeScanService).
RUN apt-get update \
    && apt-get install -y --no-install-recommends gosu ffmpeg \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
# New Relic agent jar (from the build stage) + its config. newrelic.yml sits next to the jar so the agent
# auto-discovers it; NEW_RELIC_* env vars (e.g. NEW_RELIC_LICENSE_KEY) override values in the yml.
COPY --from=build --chown=app:app /workspace/newrelic.jar /app/newrelic/newrelic.jar
COPY --chown=app:app newrelic.yml /app/newrelic/newrelic.yml
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080
ENV JAVA_OPTS=""
# Starts as root so it can apply PUID/PGID, then execs the JVM as "app" via gosu.
ENTRYPOINT ["docker-entrypoint.sh"]
