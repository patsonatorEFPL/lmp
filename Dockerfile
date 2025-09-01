# ========================================
# Dockerfile pour Railway.com
# Application Spring Boot LMP - Java 21
# ========================================

# Étape 1: Build de l'application
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

WORKDIR /app

# build phase
COPY . /app/.
RUN --mount=type=cache,id=mcc4sw48okw8wcossc8ckksk-m2/repository,target=/app/.m2/repository chmod +x ./mvnw && ./mvnw -DoutputFile=target/mvn-dependency-list.log -B -DskipTests clean dependency:list install

# ========================================
# Étape 2: Runtime optimisé
FROM eclipse-temurin:21-jre-alpine

# Installation des outils nécessaires
RUN apk add --no-cache \
    curl \
    tzdata \
    fontconfig \
    ttf-dejavu

# Configuration du timezone
ENV TZ=America/Toronto
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

# Répertoire de travail
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Créer les répertoires nécessaires
RUN mkdir -p /app/invoices /app/logs && \
    chown -R spring:spring /app

# Basculer vers l'utilisateur non-root
USER spring

# Variables d'environnement Spring Boot
ENV SERVER_PORT=8080

# Port exposé (Railway utilisera la variable PORT)
EXPOSE 8080

# Health check désactivé pour Railway (Railway utilise ses propres mécanismes)
# HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
#     CMD curl -f http://localhost:8080/actuator/health || exit 1

# Point d'entrée utilisant application.properties par défaut
ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseG1GC", "-jar", "app.jar"]
