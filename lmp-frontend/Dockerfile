# ========================================
# Dockerfile pour Dokploy Deployment
# Application Angular LMP Frontend (SSR)
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

# Build de production Angular (SSR)
RUN npx ng build --configuration=production

# Debug: list the build output
RUN echo "=== Build output ===" && ls -la dist/lmp-frontend/ && echo "=== Server dir ===" && ls -la dist/lmp-frontend/server/ 2>/dev/null || echo "NO SERVER DIRECTORY"

# ========================================
# Étape 2: Servir avec Node.js (SSR)
FROM node:20-alpine

# Configuration du timezone (Suisse)
RUN apk add --no-cache tzdata curl
ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# Copier le build Angular SSR depuis l'étape précédente
COPY --from=build /app/dist/lmp-frontend .

# Copier package files and install only production dependencies needed at runtime
COPY --from=build /app/package.json /app/package-lock.json ./
RUN npm ci --legacy-peer-deps --omit=dev --ignore-scripts

# Debug: list the runtime files
RUN echo "=== App dir ===" && ls -la /app/ && echo "=== Server dir ===" && ls -la /app/server/ 2>/dev/null || echo "NO SERVER DIR IN RUNTIME"

# Backend URL (Dokploy Docker network)
ENV BACKEND_URL=http://lmp-test-back:8080
ENV PORT=4000

# Port exposé
EXPOSE 4000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:4000/ || exit 1

# Démarrer le serveur SSR
CMD ["node", "server/server.mjs"]
