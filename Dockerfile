# ========================================
# Dockerfile pour Dokploy Deployment
# Application Spring Boot LMP + Angular Frontend — Monolithique
# ========================================

# ----------------------------------------
# Étape 0: Build Angular Frontend (CSR)
# ----------------------------------------
FROM node:20-alpine AS angular-build

WORKDIR /app

# Copier les fichiers de dépendances du frontend (mise en cache Docker)
COPY lmp-frontend/package.json lmp-frontend/package-lock.json ./
RUN npm ci --legacy-peer-deps

# Copier tout le code source du frontend
COPY lmp-frontend/ .

# Générer le client API OpenAPI (si openapi.json est présent)
RUN if [ -f ng-openapi-gen.json ]; then npx ng-openapi-gen --config ng-openapi-gen.json; fi

# Build de production Angular en mode CSR (pas SSR)
# Le résultat va dans dist/lmp-frontend/browser/
RUN npx ng build --configuration=production --ssr=false

# ----------------------------------------
# Étape 1: Build Spring Boot Backend (Java 26 EA via mvnw wrapper)
# ----------------------------------------
FROM eclipse-temurin:26-jdk-alpine AS maven-build

WORKDIR /app

# bash requis par mvnw wrapper
RUN apk add --no-cache bash

# Copier les fichiers de configuration Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Télécharger les dépendances (mise en cache des layers Docker)
RUN ./mvnw dependency:go-offline -B

# Copier le code source Java
COPY src ./src

# Copier le build Angular dans les ressources statiques du backend
# Spring Boot servira automatiquement ces fichiers depuis classpath:/static/
COPY --from=angular-build /app/dist/lmp-frontend/browser/ ./src/main/resources/static/

# Compiler l'application avec le frontend embarqué
RUN ./mvnw clean package -DskipTests -B

# ----------------------------------------
# Étape 2: Runtime Java 26 (JDK full pour AOT cache + JFR remote attach)
# ----------------------------------------
FROM eclipse-temurin:26-jdk-alpine

# Installation des outils nécessaires
RUN apk add --no-cache \
    curl \
    tzdata \
    fontconfig \
    ttf-dejavu

# Configuration du timezone
ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=maven-build --chown=spring:spring /app/target/*.jar app.jar

# Créer les répertoires nécessaires
RUN mkdir -p /app/invoices /app/logs && \
    chown -R spring:spring /app

# Basculer vers l'utilisateur non-root
USER spring

# Variables d'environnement Spring Boot
ENV SERVER_PORT=8080

# Port exposé (Dokploy utilisera la variable PORT)
EXPOSE 8080

# Health check pour Dokploy
# Healthcheck assoupli — sous load test (50+ VUs) le thread pool Tomcat traite
# les requêtes utilisateur en priorité et /actuator/health peut tomber au-delà
# de 10s. Avec 30s × 5 retries on tolère un pic CPU jusqu'à ~150s avant kill.
HEALTHCHECK --interval=30s --timeout=30s --start-period=90s --retries=5 \
    CMD curl -f -m 25 http://localhost:8080/actuator/health/liveness || exit 1

# Point d'entrée — profil piloté par SPRING_PROFILES_ACTIVE.
#
# Phase 0 Java 26 baseline (Track Rust-discipline) :
#   -XX:+UseShenandoahGC + -XX:ShenandoahGCMode=generational
#       Generational Shenandoah GA en Java 26 (JEP 524). Low-pause alternative
#       à ZGC avec footprint mémoire moindre — pas de pointer color shmem ×2.
#   -XX:+UseCompactObjectHeaders
#       Production en Java 26 (était experimental Java 25). Headers 12B → 8B,
#       -10% heap, moins cache misses sur hot objects.
#   --enable-preview
#       Active preview JEPs (Stable Values, Scoped Values, primitive patterns,
#       Module Import) — utilisés par track Phase 1+ incrémental.
#   -XX:+EnableDynamicAgentLoading
#       async-profiler / JFR remote attach. Required JDK 24+.
#   -Xms512m -Xmx1536m : container cap 4096M (Shenandoah footprint plus serré
#       que ZGC, donc cap 4G reste large marge).
#   -XX:MaxMetaspaceSize=192m / CompressedClassSpaceSize=64m / ReservedCodeCacheSize=128m :
#       caps mesurés idle + marge.
ENTRYPOINT ["java", \
    "--enable-preview", \
    "-Xms512m", "-Xmx1536m", \
    "-Xss512k", \
    "-XX:+UseShenandoahGC", \
    "-XX:ShenandoahGCMode=generational", \
    "-XX:+UseCompactObjectHeaders", \
    "-XX:+EnableDynamicAgentLoading", \
    "-XX:MaxMetaspaceSize=192m", \
    "-XX:CompressedClassSpaceSize=64m", \
    "-XX:ReservedCodeCacheSize=128m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
    "-XX:StartFlightRecording=settings=profile,delay=180s,duration=120s,filename=/app/profile.jfr,dumponexit=true,name=bench", \
    "-jar", "app.jar"]
