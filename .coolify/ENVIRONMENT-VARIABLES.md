# Variables d'Environnement - LMP Services Coolify

## Configuration pour le domaine `https://lmp-services.ca`

Ce document détaille toutes les variables d'environnement nécessaires pour déployer l'application LMP Services sur Coolify.

---

## 🔧 Variables Obligatoires

### Configuration de Base

```bash
# Profil Spring Boot
SPRING_PROFILES_ACTIVE=prod

# Port d'écoute (géré par Coolify)
PORT=8080
SERVER_PORT=8080

# URL de base de l'application
APP_BASE_URL=https://lmp-services.ca
```

### Configuration des Headers (Important pour Reverse Proxy)

```bash
# Gestion des headers X-Forwarded-*
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true
```

### Base de Données

```bash
# URL de connexion à la base de données
DATABASE_URL=jdbc:mysql://hostname:port/database_name

# Identifiants de connexion
DATABASE_USERNAME=your_username
DATABASE_PASSWORD=your_secure_password

# Configuration du pool de connexions (optionnel)
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
```

### Stripe (Paiements)

```bash
# Clés Stripe pour la production
STRIPE_SECRET_KEY=sk_live_xxxxxxxxxxxxxxxxxxxxx
STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxx
```

### Configuration Email

```bash
# Gmail SMTP
MAIL_USERNAME=lmp.assistance@gmail.com
MAIL_PASSWORD=your_app_specific_password

# Configuration SMTP (optionnel - valeurs par défaut)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
```

---

## ⚙️ Variables Optionnelles

### JVM et Performance

```bash
# Configuration JVM
JAVA_OPTS=-Xmx768m -Xms512m -XX:+UseG1GC

# Configuration Tomcat
SERVER_TOMCAT_MAX_THREADS=100
SERVER_TOMCAT_MIN_SPARE_THREADS=10
```

### Logging

```bash
# Niveau de logs
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_LMP=DEBUG

# Configuration des logs
LOGGING_FILE_PATH=/app/logs/application.log
LOGGING_PATTERN_CONSOLE=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Sécurité Avancée

```bash
# Session timeout (en secondes)
SERVER_SERVLET_SESSION_TIMEOUT=1800

# Configuration CSRF
SPRING_SECURITY_CSRF_ENABLED=true

# Configuration CORS (si nécessaire)
CORS_ALLOWED_ORIGINS=https://lmp-services.ca
```

### Monitoring et Actuator

```bash
# Activation des endpoints Actuator
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics

# Sécurité des endpoints
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized
```

---

## 🏗️ Configuration par Environnement

### Variables Coolify (Production)

À configurer dans l'interface Coolify :

```bash
# === OBLIGATOIRES ===
SPRING_PROFILES_ACTIVE=prod
APP_BASE_URL=https://lmp-services.ca
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true

# Base de données (à adapter selon votre configuration Coolify)
DATABASE_URL=jdbc:mysql://mysql-service:3306/lmp_production
DATABASE_USERNAME=lmp_user
DATABASE_PASSWORD=***SECURE_PASSWORD***

# Stripe (Production)
STRIPE_SECRET_KEY=sk_live_***
STRIPE_PUBLISHABLE_KEY=pk_live_***
STRIPE_WEBHOOK_SECRET=whsec_***

# Email
MAIL_USERNAME=lmp.assistance@gmail.com
MAIL_PASSWORD=***APP_PASSWORD***

# === OPTIONNELLES ===
JAVA_OPTS=-Xmx768m -Xms512m -XX:+UseG1GC
LOGGING_LEVEL_ROOT=INFO
SERVER_SERVLET_SESSION_TIMEOUT=1800
```

### Variables Locales (Développement)

Pour les tests locaux avec Docker :

```bash
# === DÉVELOPPEMENT ===
SPRING_PROFILES_ACTIVE=dev
APP_BASE_URL=http://localhost:8080
SERVER_FORWARD_HEADERS_STRATEGY=none

# Base de données locale
DATABASE_URL=jdbc:mysql://localhost:3306/lmp_dev
DATABASE_USERNAME=root
DATABASE_PASSWORD=password

# Stripe (Test)
STRIPE_SECRET_KEY=sk_test_***
STRIPE_PUBLISHABLE_KEY=pk_test_***
STRIPE_WEBHOOK_SECRET=whsec_***

# Email (développement)
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=test_password
```

---

## 🔒 Sécurité des Variables

### Variables Sensibles (Secrets)

Ces variables contiennent des informations sensibles et doivent être configurées comme **secrets** dans Coolify :

- `DATABASE_PASSWORD`
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `MAIL_PASSWORD`

### Variables Publiques

Ces variables peuvent être configurées comme variables d'environnement normales :

- `SPRING_PROFILES_ACTIVE`
- `APP_BASE_URL`
- `SERVER_FORWARD_HEADERS_STRATEGY`
- `STRIPE_PUBLISHABLE_KEY` (clé publique)

---

## 🧪 Validation de Configuration

### Script de Test

```bash
#!/bin/bash
# Test des variables d'environnement essentielles

echo "=== Test des variables LMP Services ==="

# Variables obligatoires
variables_required=(
    "SPRING_PROFILES_ACTIVE"
    "APP_BASE_URL"
    "DATABASE_URL"
    "DATABASE_USERNAME"
    "DATABASE_PASSWORD"
    "STRIPE_SECRET_KEY"
    "STRIPE_PUBLISHABLE_KEY"
    "MAIL_USERNAME"
    "MAIL_PASSWORD"
)

for var in "${variables_required[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Variable manquante: $var"
    else
        echo "✅ Variable configurée: $var"
    fi
done

# Test de connectivité
echo "=== Test de connectivité ==="
curl -s https://lmp-services.ca/actuator/health > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Application accessible"
else
    echo "❌ Application inaccessible"
fi
```

### Endpoints de Validation

```bash
# Vérifier la configuration actuelle
GET https://lmp-services.ca/actuator/health

# Vérifier les informations de build
GET https://lmp-services.ca/actuator/info

# Test des webhooks Stripe (si configuré)
# Via Stripe Dashboard → Webhooks → Test
```

---

## 🔄 Migration depuis l'ancien domaine

### Checklist de Migration

1. **Mettre à jour `APP_BASE_URL`**
   ```bash
   # Ancien
   APP_BASE_URL=https://lmp.run.place
   
   # Nouveau
   APP_BASE_URL=https://lmp-services.ca
   ```

2. **Vérifier les webhooks Stripe**
   - Mettre à jour l'URL dans Stripe Dashboard
   - Ancienne : `https://lmp.run.place/api/webhooks/stripe`
   - Nouvelle : `https://lmp-services.ca/api/webhooks/stripe`

3. **Tester les redirections**
   ```bash
   curl -I https://lmp-services.ca
   curl https://lmp-services.ca/actuator/health
   ```

4. **Vérifier les emails**
   - Templates d'emails contiennent les bonnes URLs
   - Liens de confirmation utilisent le nouveau domaine

---

## 📋 Templates de Configuration

### Fichier .env pour développement local

```bash
# .env.local
SPRING_PROFILES_ACTIVE=dev
APP_BASE_URL=http://localhost:8080
DATABASE_URL=jdbc:mysql://localhost:3306/lmp_dev
DATABASE_USERNAME=root
DATABASE_PASSWORD=password
STRIPE_SECRET_KEY=sk_test_xxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
MAIL_USERNAME=test@example.com
MAIL_PASSWORD=test
```

### Configuration Coolify (Production)

```yaml
# Variables à configurer dans l'interface Coolify
environment:
  SPRING_PROFILES_ACTIVE: "prod"
  APP_BASE_URL: "https://lmp-services.ca"
  SERVER_FORWARD_HEADERS_STRATEGY: "framework"
  SERVER_USE_FORWARD_HEADERS: "true"
  
secrets:
  DATABASE_PASSWORD: "***"
  STRIPE_SECRET_KEY: "sk_live_***"
  STRIPE_WEBHOOK_SECRET: "whsec_***"
  MAIL_PASSWORD: "***"
```

---

## 🆘 Troubleshooting Variables

### Problèmes Courants

#### 1. Headers X-Forwarded manquants

```bash
# Solution
SERVER_FORWARD_HEADERS_STRATEGY=framework
SERVER_USE_FORWARD_HEADERS=true
```

#### 2. URL de base incorrecte

```bash
# Vérifier dans les logs
grep "APP_BASE_URL" /app/logs/application.log

# Corriger
APP_BASE_URL=https://lmp-services.ca
```

#### 3. Connexion base de données échouée

```bash
# Tester la connectivité
DATABASE_URL=jdbc:mysql://correct-host:3306/database
DATABASE_USERNAME=correct_user
DATABASE_PASSWORD=correct_password
```

#### 4. Webhooks Stripe non reçus

```bash
# Vérifier l'URL dans Stripe Dashboard
STRIPE_WEBHOOK_SECRET=whsec_correct_secret

# URL webhook : https://lmp-services.ca/api/webhooks/stripe
```

---

**📝 Note**: Toutes les variables sensibles doivent être configurées comme secrets dans Coolify pour maintenir la sécurité de l'application.