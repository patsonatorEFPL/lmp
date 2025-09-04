# Architecture de Déploiement Coolify - LMP Services

## Vue d'ensemble

L'application Spring Boot LMP est déployée sur le domaine `https://lmp-services.ca` avec une architecture flexible supportant différentes configurations de déploiement.

---

## 🏗️ Architectures Supportées

### 1. Déploiement Direct (Recommandé pour Coolify)

```
Internet 
   ↓
DNS (lmp-services.ca)
   ↓
Coolify Platform
   ↓ (SSL/TLS automatique)
Load Balancer/Reverse Proxy (Coolify)
   ↓
Spring Boot Application (Port 8080)
   ↓
Base de données (MySQL/PostgreSQL)
```

**Avantages:**
- Configuration simplifiée
- SSL/TLS géré automatiquement par Coolify
- Monitoring intégré
- Déploiement en un clic

### 2. Déploiement avec Reverse Proxy Externe (Optionnel)

```
Internet 
   ↓
DNS (lmp-services.ca)
   ↓
Nginx/Traefik (Reverse Proxy)
   ↓ (Headers X-Forwarded-*)
Coolify Platform
   ↓
Spring Boot Application (Port 8080)
   ↓
Base de données (MySQL/PostgreSQL)
```

**Avantages:**
- Contrôle fin du cache et des headers
- Rate limiting avancé
- Gestion multi-domaines
- Optimisations de performance

---

## ⚙️ Configuration Spring Boot

### Headers pour Reverse Proxy

L'application est configurée pour gérer automatiquement les headers X-Forwarded-* :

```properties
# Configuration dans application-prod.properties
server.forward-headers-strategy=framework
server.use-forward-headers=true
```

### Variables d'environnement Coolify

```bash
# Configuration de base
SPRING_PROFILES_ACTIVE=prod
APP_BASE_URL=https://lmp-services.ca
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true

# Base de données
DATABASE_URL=jdbc:mysql://hostname:port/database
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_password

# Stripe
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

---

## 🔧 Coolify - Configuration Spécifique

### Service Configuration

- **Type**: Docker Compose
- **Build Pack**: Docker
- **Fichier**: `docker-compose.coolify.yml`
- **Port interne**: 8080
- **Domaine**: `lmp-services.ca`

### Health Checks

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### Volumes Persistants

- **app-logs**: `/app/logs` - Logs de l'application
- **app-invoices**: `/app/invoices` - Factures générées

---

## 🚀 Flux de Déploiement

### 1. Préparation

1. Configurer les variables d'environnement dans Coolify
2. Vérifier la configuration de la base de données
3. Configurer les clés Stripe pour la production

### 2. Déploiement

1. Push du code vers le repository Git
2. Coolify détecte automatiquement les changements
3. Build de l'image Docker
4. Déploiement de l'application
5. Tests de health check

### 3. Validation

1. Vérifier `https://lmp-services.ca/actuator/health`
2. Tester les fonctionnalités principales
3. Vérifier les webhooks Stripe
4. Contrôler les logs d'application

---

## 📊 Monitoring et Observabilité

### Métriques Importantes

- **Disponibilité**: Uptime de l'application
- **Performance**: Temps de réponse des endpoints
- **Erreurs**: Taux d'erreur 5xx
- **Base de données**: Temps de réponse des requêtes

### Endpoints de Monitoring

```bash
# Health Check
GET https://lmp-services.ca/actuator/health

# Informations système (si activé)
GET https://lmp-services.ca/actuator/info

# Métriques (si activé)
GET https://lmp-services.ca/actuator/metrics
```

### Logs

```bash
# Logs Coolify
Via l'interface Coolify → Services → Logs

# Logs application (dans les volumes)
/app/logs/spring.log
/app/logs/error.log
```

---

## 🔒 Sécurité

### SSL/TLS

- Certificats gérés automatiquement par Coolify
- Redirection HTTP → HTTPS automatique
- Headers de sécurité configurés

### Variables Sensibles

```bash
# Variables à définir dans Coolify (secrets)
DATABASE_PASSWORD=***
STRIPE_SECRET_KEY=sk_live_***
MAIL_PASSWORD=***
STRIPE_WEBHOOK_SECRET=whsec_***
```

### Headers de Sécurité

L'application configure automatiquement :
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block
- Strict-Transport-Security (si HTTPS)

---

## 🐛 Troubleshooting

### Problèmes Courants

#### 1. Erreur 502 Bad Gateway

```bash
# Vérifier le health check
curl https://lmp-services.ca/actuator/health

# Vérifier les logs Coolify
# Interface Coolify → Logs

# Vérifier la base de données
# Tester la connectivité depuis les logs d'application
```

#### 2. Headers X-Forwarded Manquants

```bash
# Vérifier la configuration Spring Boot
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true

# Vérifier dans les logs d'application
# Rechercher "X-Forwarded-Proto" et "X-Forwarded-Host"
```

#### 3. Problèmes SSL

```bash
# Tester le certificat
openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca

# Vérifier la redirection HTTP→HTTPS
curl -I http://lmp-services.ca
```

### Commandes de Diagnostic

```bash
# Test complet de l'application
curl -I https://lmp-services.ca
curl https://lmp-services.ca/actuator/health
curl https://lmp-services.ca/sitemap.xml

# Vérification des services Coolify
# Via l'interface Coolify → Services → Status
```

---

## 📈 Optimisations

### Performance

- Cache statique configuré (CSS, JS, images)
- Compression gzip activée
- Connection pooling pour la base de données
- Optimisation JVM pour les containers

### Ressources

```yaml
# Configuration dans docker-compose.coolify.yml
deploy:
  resources:
    limits:
      memory: 1G
      cpus: '0.5'
    reservations:
      memory: 512M
      cpus: '0.25'
```

---

## 🔄 Maintenance

### Tâches Régulières

#### Quotidien
- Vérifier les health checks
- Contrôler les logs d'erreur
- Surveiller l'utilisation des ressources

#### Hebdomadaire
- Analyser les métriques de performance
- Vérifier l'espace disque des volumes
- Contrôler les certificats SSL

#### Mensuel
- Mettre à jour les dépendances
- Réviser les configurations de sécurité
- Analyser les rapports d'utilisation

---

## 📞 Support

### Logs Importants

- **Application**: Volumes Coolify `/app/logs/`
- **Coolify**: Interface web → Services → Logs
- **Base de données**: Logs du service de base de données

### Contacts et Escalation

1. **Logs d'application**: Vérifier en premier
2. **Status Coolify**: Interface de monitoring
3. **Base de données**: Vérifier la connectivité
4. **DNS**: Vérifier la résolution de domaine

---

**📝 Note**: Cette architecture est optimisée pour Coolify et le domaine `lmp-services.ca`. Pour toute modification, référez-vous à la documentation spécifique de chaque composant.