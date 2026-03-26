# ========================================
# Dockerfile pour Dokploy Deployment
# Application Angular LMP Frontend — SSR
# ========================================

# Étape 1: Build de l'application Angular
FROM node:20-alpine AS build

WORKDIR /app

# Copier les fichiers de dépendances (mise en cache des layers Docker)
COPY package.json package-lock.json ./

# Installer les dépendances
RUN npm ci --legacy-peer-deps

# Copier le code source
COPY . .

# Générer le client API depuis openapi.json
RUN npx ng-openapi-gen --config ng-openapi-gen.json

# Build de production Angular (SSR)
RUN npx ng build --configuration=production

# ========================================
# Étape 2: Runtime Node.js pour SSR
FROM node:20-alpine

# Configuration du timezone
RUN apk add --no-cache curl tzdata
ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Créer un utilisateur non-root
RUN addgroup -g 1001 -S angular && \
    adduser -S angular -u 1001 -G angular

WORKDIR /app

# Copier le build SSR depuis l'étape précédente
COPY --from=build --chown=angular:angular /app/dist/lmp-frontend/ ./dist/lmp-frontend/

# Copier package.json pour les dépendances de production
COPY --from=build --chown=angular:angular /app/package.json ./

# Installer uniquement les dépendances de production
RUN npm install --omit=dev --legacy-peer-deps && npm cache clean --force

# Basculer vers l'utilisateur non-root
USER angular

# Variables d'environnement
ENV PORT=80
ENV BACKEND_URL=http://lmp-spring-app:8080

# Port exposé
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

# Démarrer le serveur SSR Node.js
CMD ["node", "dist/lmp-frontend/server/server.mjs"]
