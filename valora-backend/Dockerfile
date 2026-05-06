# =============================================================================
# VALORA Backend — Dockerfile multi-stage
#
# Stage 1 (build) — Maven + JDK 21 compila o jar.
# Stage 2 (runtime) — apenas JRE 21 + jar + curl (healthcheck).
#
# Otimizações:
# - Cache de dependências Maven antes do COPY do código (camada estável)
# - Heap limitado a 384m para caber em Render free tier (512MB total — sobra
#   margem para metaspace, threads, native code do Postgres driver)
# - Usuário não-root (boa prática + alguns provedores exigem)
# - HEALTHCHECK aponta para /actuator/health (já exposto em application.yml)
# =============================================================================

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Wrapper Maven primeiro (raramente muda) — cacheia bem.
COPY mvnw mvnw.cmd ./
COPY .mvn/ .mvn/
COPY pom.xml ./

# Pré-baixa dependências em camada separada para acelerar rebuilds.
# go-offline falha em alguns plugins; -B (batch mode) e -ntp (no transfer
# progress) deixam o log limpo.
RUN ./mvnw -B -ntp dependency:go-offline || true

# Copia código-fonte só agora — assim alteração em src/ não invalida o cache
# de dependências acima.
COPY src/ src/

RUN ./mvnw -B -ntp clean package -DskipTests \
 && cp target/*.jar /workspace/app.jar

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

# curl para HEALTHCHECK (Alpine não traz por default).
RUN apk add --no-cache curl \
 && addgroup -S valora \
 && adduser -S -G valora valora

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar
COPY --chmod=755 entrypoint.sh /app/entrypoint.sh
RUN chown -R valora:valora /app
USER valora

# Render injeta PORT — Spring Boot lê via SERVER_PORT (já mapeado em application.yml).
# JVM tunada pra 512MB total: MaxRAMPercentage=75 → ~384m heap em container 512m,
# sobra ~128m pra metaspace/native/thread stacks. UseContainerSupport (default em
# Java 21, redundante mas explícito) faz a JVM respeitar cgroup limits.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
