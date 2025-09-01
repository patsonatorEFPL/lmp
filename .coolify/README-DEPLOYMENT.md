# Guide de Déploiement Coolify - LMP Application

## Configuration requise dans Coolify

### 1. Variables d'environnement à configurer

```bash
# Configuration de base
SPRING_PROFILES_ACTIVE=prod
PORT=8080
APP_BASE_URL=https://lmp.run.place

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
- **Domaine**: `lmp.run.place`

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
- URL: `https://lmp.run.place/actuator/health`
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

### 9. Troubleshooting

**Problèmes courants**:
- Port binding: Vérifier `PORT` variable
- Base de données: Vérifier connectivité et credentials
- SSL: Attendre propagation DNS
- Logs: Consulter `/app/logs` et container logs

**URLs importantes**:
- Application: `https://lmp.run.place`
- Health: `https://lmp.run.place/actuator/health`
- Sitemap: `https://lmp.run.place/sitemap.xml`
- Robots: `https://lmp.run.place/robots.txt`