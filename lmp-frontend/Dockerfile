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

# Debug: show the complete build output structure
RUN echo "=== Full dist tree ===" && find dist/ -type f -name "*.mjs" -o -name "*.js" | head -30

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

# Debug: show what's in /app
RUN echo "=== /app contents ===" && find /app -maxdepth 3 -type f -name "*.mjs" -o -name "*.js" | head -30

# Copier package files and install only production dependencies needed at runtime
COPY --from=build /app/package.json /app/package-lock.json ./
RUN npm ci --legacy-peer-deps --omit=dev --ignore-scripts

# Backend URL (Dokploy Docker network)
ENV BACKEND_URL=http://lmp-test-back:8080
ENV PORT=4000

# Port exposé
EXPOSE 4000

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:4000/ || exit 1

# Démarrer le serveur SSR — will be updated after we see the build output
CMD ["node", "server/server.mjs"]
