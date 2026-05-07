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
# Étape 1: Build Spring Boot Backend
# ----------------------------------------
FROM maven:3-eclipse-temurin-25-alpine AS maven-build

WORKDIR /app

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
# Étape 2: Runtime optimisé avec Java 25
# ----------------------------------------
FROM eclipse-temurin:25-jre-alpine

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

# Point d'entrée — profil piloté par SPRING_PROFILES_ACTIVE (défaut: prod)
#
# JVM tuning (mesuré sur staging idle, 843 MB RSS avant) :
#   -Xms256m -Xmx512m  : heap committed 545 → 280 MB (idle utilise 456 MB max,
#                        on a marge confortable; bench déclenchera GC plus tôt)
#   -XX:MaxMetaspaceSize=192m            : cap (idle = 139 MB, avec marge)
#   -XX:CompressedClassSpaceSize=64m     : reserved 1 GB virtuel → 64 MB
#   -XX:ReservedCodeCacheSize=128m       : default 240 MB → 128 MB (idle = 31 MB)
#   -XX:+UseStringDeduplication          : G1 dedup char[] (gain memoire petit)
#   -XX:MaxRAMPercentage=50              : sécurité si Xmx supprimé/override
ENTRYPOINT ["java", \
    "-Xms256m", "-Xmx512m", \
    "-XX:MaxMetaspaceSize=192m", \
    "-XX:CompressedClassSpaceSize=64m", \
    "-XX:ReservedCodeCacheSize=128m", \
    "-XX:+UseStringDeduplication", \
    "-jar", "app.jar"]
