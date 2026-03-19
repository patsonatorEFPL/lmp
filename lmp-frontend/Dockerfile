# ========================================
# Dockerfile pour Dokploy Deployment
# Application Angular LMP Frontend
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

# Build de production Angular
RUN npx ng build --configuration=production

# ========================================
# Étape 2: Servir avec Nginx
FROM nginx:alpine

# Configuration du timezone (Suisse)
RUN apk add --no-cache tzdata
ENV TZ=Europe/Zurich
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copier le build Angular depuis l'étape précédente
COPY --from=build /app/dist/lmp-frontend/browser /usr/share/nginx/html

# Port exposé
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

# Démarrer Nginx
CMD ["nginx", "-g", "daemon off;"]
