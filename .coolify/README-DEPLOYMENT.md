# Guide de Déploiement Coolify - LMP Application

## Configuration requise dans Coolify

### 1. Variables d'environnement à configurer

```bash
# Configuration de base
SPRING_PROFILES_ACTIVE=prod
PORT=8080
APP_BASE_URL=https://lmp-services.ca

# Configuration des headers pour reverse proxy (si utilisé)
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true

# Base de données (MySQL/PostgreSQL)
DATABASE_URL=jdbc:mysql://hostname:port/database
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password

# Stripe (Production)
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Configuration email
MAIL_USERNAME=lmp.assistance@gmail.com
MAIL_PASSWORD=your_app_password
```

### 2. Configuration du service Coolify

- **Type**: Docker Compose
- **Fichier**: `docker-compose.coolify.yml`
- **Port**: 8080
- **Health Check**: `/actuator/health`
- **Domaine**: `lmp-services.ca`

### 3. Configuration de la base de données

L'application supporte:
- **MySQL** (recommandé)
- **PostgreSQL**

Exemple de configuration MySQL:
```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/lmp_production
SPRING_DATASOURCE_USERNAME=lmp_user
SPRING_DATASOURCE_PASSWORD=secure_password
```

### 4. Configuration SSL/TLS

Coolify gère automatiquement:
- Certificats SSL Let's Encrypt
- Redirections HTTP → HTTPS
- Headers de sécurité

### 5. Monitoring et Logs

**Health Check**:
- URL: `https://lmp-services.ca/actuator/health`
- Interval: 30s

**Logs d'application**:
- Container logs via Coolify interface
- Application logs dans `/app/logs`

### 6. Volumes persistants

- `app-logs`: Logs de l'application
- `app-invoices`: Factures générées

### 7. Commandes de déploiement

```bash
# Build et déploiement
docker-compose -f docker-compose.coolify.yml up -d

# Vérification des logs
docker-compose -f docker-compose.coolify.yml logs -f

# Redémarrage
docker-compose -f docker-compose.coolify.yml restart
```

### 8. Configuration post-déploiement

1. Vérifier la connexion à la base de données
2. Tester les webhooks Stripe
3. Vérifier l'envoi d'emails
4. Configurer les redirections canoniques
5. Tester le sitemap et robots.txt
6. Vérifier la configuration des headers X-Forwarded-*

### 9. Architecture de Déploiement

#### Déploiement Direct (Sans Reverse Proxy)
```
Internet → Coolify (SSL) → Spring Boot App (Port 8080)
```

#### Déploiement avec Reverse Proxy (Optionnel)
```
Internet → Nginx/Traefik (SSL) → Coolify → Spring Boot App (Port 8080)
```

**Variables importantes pour les headers**:
- `SERVER_FORWARD_HEADERS_STRATEGY=framework`
- `SERVER_USE_FORWARD_HEADERS=true`

### 10. Troubleshooting

**Problèmes courants**:
- Port binding: Vérifier `PORT` variable
- Base de données: Vérifier connectivité et credentials
- SSL: Attendre propagation DNS avec le nouveau domaine
- Headers X-Forwarded: Vérifier `SERVER_FORWARD_HEADERS_STRATEGY`
- Logs: Consulter `/app/logs` et container logs

**URLs importantes**:
- Application: `https://lmp-services.ca`
- Health: `https://lmp-services.ca/actuator/health`
- Sitemap: `https://lmp-services.ca/sitemap.xml`
- Robots: `https://lmp-services.ca/robots.txt`
- Admin: `https://lmp-services.ca/admin/dashboard`

### 11. Tests de Validation

```bash
# Test de connectivité
curl -I https://lmp-services.ca

# Test du health check
curl https://lmp-services.ca/actuator/health

# Test des redirections
curl -I http://lmp-services.ca  # Doit rediriger vers HTTPS

# Test du sitemap
curl https://lmp-services.ca/sitemap.xml

# Vérification SSL
openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca
```

### 12. Monitoring et Alertes

**Métriques importantes à surveiller**:
- Disponibilité de l'application (uptime)
- Temps de réponse des pages
- Statut des bases de données
- Utilisation mémoire/CPU
- Logs d'erreur

**URLs de monitoring**:
- Health Check: `https://lmp-services.ca/actuator/health`
- Métriques: `https://lmp-services.ca/actuator/metrics` (si activé)