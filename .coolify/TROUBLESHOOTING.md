# Guide de Dépannage Coolify - LMP Services

## Problèmes courants et solutions pour `https://lmp-services.ca`

Ce guide couvre les problèmes les plus fréquents lors du déploiement et de l'exploitation de l'application LMP Services sur Coolify.

---

## 🚨 Problèmes de Déploiement

### 1. Échec du Build Docker

#### **Symptôme**
```
Build failed: Unable to build Docker image
Error: Could not find or load main class com.lmp.LmpApplication
```

#### **Solutions**

1. **Vérifier le Dockerfile**
   ```bash
   # Dans l'interface Coolify, vérifier que le Dockerfile est détecté
   # Chemin : ./Dockerfile (racine du projet)
   ```

2. **Variables d'environnement manquantes**
   ```bash
   # Ajouter dans Coolify
   SPRING_PROFILES_ACTIVE=prod
   ```

3. **Ressources insuffisantes**
   ```bash
   # Augmenter les limites dans docker-compose.coolify.yml
   deploy:
     resources:
       limits:
         memory: 2G
         cpus: '1.0'
   ```

### 2. Service ne démarre pas

#### **Symptôme**
```
Service is unhealthy
Health check failed
```

#### **Solutions**

1. **Vérifier le health check**
   ```bash
   # Test manuel
   curl http://localhost:8080/actuator/health
   
   # Dans Coolify, vérifier la configuration health check :
   # Path: /actuator/health
   # Port: 8080
   # Interval: 30s
   ```

2. **Base de données inaccessible**
   ```bash
   # Vérifier les variables dans Coolify
   DATABASE_URL=jdbc:mysql://hostname:port/database
   DATABASE_USERNAME=your_username
   DATABASE_PASSWORD=your_password
   ```

3. **Port binding**
   ```bash
   # Vérifier dans docker-compose.coolify.yml
   ports:
     - "${PORT:-8080}:8080"
   
   # Variable PORT doit être définie dans Coolify
   PORT=8080
   ```

---

## 🌐 Problèmes de Réseau et SSL

### 1. Erreur 502 Bad Gateway

#### **Symptôme**
```
502 Bad Gateway
nginx/1.x.x
```

#### **Solutions**

1. **Application non démarrée**
   ```bash
   # Vérifier dans Coolify → Services → Status
   # L'application doit être "Running"
   
   # Vérifier les logs
   # Coolify → Services → Logs
   ```

2. **Port interne incorrect**
   ```bash
   # Dans docker-compose.coolify.yml
   ports:
     - "8080:8080"  # Port externe:interne
   
   # Variable d'environnement
   SERVER_PORT=8080
   ```

3. **Health check échoue**
   ```bash
   # Test depuis le container
   docker exec -it container_name curl http://localhost:8080/actuator/health
   ```

### 2. Certificat SSL non valide

#### **Symptôme**
```
SSL certificate problem: certificate verify failed
```

#### **Solutions**

1. **Attendre la propagation DNS**
   ```bash
   # Vérifier la résolution DNS
   nslookup lmp-services.ca
   
   # Attendre 24-48h pour la propagation complète
   ```

2. **Forcer le renouvellement SSL dans Coolify**
   ```bash
   # Interface Coolify → Services → SSL → Renew Certificate
   ```

3. **Vérifier la configuration du domaine**
   ```bash
   # Dans Coolify, vérifier :
   # Domain: lmp-services.ca
   # SSL: Auto-generated
   ```

---

## 🗄️ Problèmes de Base de Données

### 1. Connexion refusée

#### **Symptôme**
```
java.sql.SQLException: Connection refused
Access denied for user 'username'@'host'
```

#### **Solutions**

1. **Vérifier la connectivité réseau**
   ```bash
   # Test depuis le container
   docker exec -it container_name ping database_host
   
   # Test du port
   docker exec -it container_name telnet database_host 3306
   ```

2. **Credentials incorrects**
   ```bash
   # Vérifier dans Coolify → Variables
   DATABASE_USERNAME=correct_username
   DATABASE_PASSWORD=correct_password
   
   # Test de connexion
   mysql -h database_host -u username -p database_name
   ```

3. **URL de base de données malformée**
   ```bash
   # Format correct
   DATABASE_URL=jdbc:mysql://hostname:3306/database_name?useSSL=false&serverTimezone=UTC
   ```

### 2. Migrations Flyway échouées

#### **Symptôme**
```
FlywayException: Migration failed
Schema validation failed
```

#### **Solutions**

1. **Vérifier les permissions**
   ```sql
   -- L'utilisateur doit avoir les permissions DDL
   GRANT ALL PRIVILEGES ON database_name.* TO 'username'@'%';
   FLUSH PRIVILEGES;
   ```

2. **Réinitialiser Flyway**
   ```bash
   # Dans l'application, ajouter temporairement
   SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
   SPRING_FLYWAY_REPAIR=true
   ```

3. **Vérifier les scripts de migration**
   ```bash
   # Fichiers dans src/main/resources/db/migration/
   # Format : V1__Initial_schema.sql
   ```

---

## 💳 Problèmes Stripe

### 1. Webhooks non reçus

#### **Symptôme**
```
Stripe webhooks failing
No webhook events in logs
```

#### **Solutions**

1. **Vérifier l'URL webhook dans Stripe**
   ```bash
   # URL correcte dans Stripe Dashboard
   https://lmp-services.ca/api/webhooks/stripe
   ```

2. **Secret webhook incorrect**
   ```bash
   # Dans Coolify → Secrets
   STRIPE_WEBHOOK_SECRET=whsec_correct_secret_from_stripe
   ```

3. **Test webhook manuellement**
   ```bash
   # Via Stripe Dashboard → Webhooks → Test webhook
   # Vérifier les logs de l'application
   ```

### 2. Paiements échouent

#### **Symptôme**
```
Payment failed: Invalid API key
Checkout session creation failed
```

#### **Solutions**

1. **Clés Stripe incorrectes**
   ```bash
   # Production
   STRIPE_SECRET_KEY=sk_live_xxxxxxxxxxxxx
   STRIPE_PUBLISHABLE_KEY=pk_live_xxxxxxxxxxxxx
   
   # Test
   STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxx
   STRIPE_PUBLISHABLE_KEY=pk_test_xxxxxxxxxxxxx
   ```

2. **Mode test/production mélangé**
   ```bash
   # Vérifier la cohérence des clés
   # Toutes les clés doivent être "test" ou "live"
   ```

---

## 📧 Problèmes Email

### 1. Emails non envoyés

#### **Symptôme**
```
Mail server connection failed
Authentication failed
```

#### **Solutions**

1. **Mot de passe d'application Gmail**
   ```bash
   # Utiliser un mot de passe d'application, pas le mot de passe du compte
   MAIL_USERNAME=lmp.assistance@gmail.com
   MAIL_PASSWORD=app_specific_password_16_chars
   ```

2. **Configuration SMTP**
   ```bash
   # Variables dans Coolify
   SPRING_MAIL_HOST=smtp.gmail.com
   SPRING_MAIL_PORT=587
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
   ```

3. **Test d'envoi**
   ```bash
   # Via l'endpoint de test (si disponible)
   curl -X POST https://lmp-services.ca/admin/test-email
   ```

---

## 🔐 Problèmes d'Authentification

### 1. Session expirée trop rapidement

#### **Symptôme**
```
Users logged out after few minutes
Session timeout too short
```

#### **Solutions**

1. **Augmenter le timeout de session**
   ```bash
   # Dans Coolify → Variables
   SERVER_SERVLET_SESSION_TIMEOUT=1800  # 30 minutes
   ```

2. **Vérifier la configuration de sécurité**
   ```bash
   # Dans l'application
   SPRING_SECURITY_SESSION_TIMEOUT=1800
   ```

### 2. Headers X-Forwarded manquants

#### **Symptôme**
```
Redirections vers HTTP au lieu de HTTPS
URLs incorrectes dans les emails
```

#### **Solutions**

1. **Activer les headers forwarded**
   ```bash
   # Variables obligatoires dans Coolify
   SERVER_FORWARD_HEADERS_STRATEGY=framework
   SERVER_USE_FORWARD_HEADERS=true
   ```

2. **Vérifier la configuration Coolify**
   ```bash
   # Coolify doit envoyer les headers X-Forwarded-*
   # X-Forwarded-Proto: https
   # X-Forwarded-Host: lmp-services.ca
   ```

---

## 📊 Problèmes de Performance

### 1. Application lente

#### **Symptôme**
```
Slow response times
High memory usage
```

#### **Solutions**

1. **Augmenter les ressources**
   ```yaml
   # Dans docker-compose.coolify.yml
   deploy:
     resources:
       limits:
         memory: 2G
         cpus: '1.0'
       reservations:
         memory: 1G
         cpus: '0.5'
   ```

2. **Optimiser la JVM**
   ```bash
   # Dans Coolify → Variables
   JAVA_OPTS=-Xmx1536m -Xms1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
   ```

3. **Pool de connexions BD**
   ```bash
   # Variables d'optimisation
   SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
   SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5
   ```

### 2. Erreurs Out of Memory

#### **Symptôme**
```
java.lang.OutOfMemoryError: Java heap space
Container killed by OOMKiller
```

#### **Solutions**

1. **Augmenter la mémoire heap**
   ```bash
   JAVA_OPTS=-Xmx2048m -Xms1024m
   ```

2. **Augmenter les limites Docker**
   ```yaml
   deploy:
     resources:
       limits:
         memory: 3G
   ```

---

## 🛠️ Outils de Diagnostic

### Commandes Utiles

```bash
# Status général
curl -I https://lmp-services.ca
curl https://lmp-services.ca/actuator/health

# Test de la base de données
curl https://lmp-services.ca/actuator/health | jq '.components.db'

# Vérification SSL
openssl s_client -connect lmp-services.ca:443 -servername lmp-services.ca

# Test DNS
dig lmp-services.ca
nslookup lmp-services.ca

# Logs en temps réel (interface Coolify)
# Services → Logs → Live logs
```

### Endpoints de Debug

```bash
# Health check détaillé
GET https://lmp-services.ca/actuator/health

# Informations de l'application
GET https://lmp-services.ca/actuator/info

# Métriques (si activé)
GET https://lmp-services.ca/actuator/metrics

# Configuration (attention - sensible)
GET https://lmp-services.ca/actuator/configprops
```

---

## 📞 Escalation

### Niveaux de Support

1. **Niveau 1 - Vérifications basiques**
   - Health checks
   - Logs d'application
   - Variables d'environnement

2. **Niveau 2 - Infrastructure**
   - Configuration Coolify
   - Réseau et DNS
   - Base de données

3. **Niveau 3 - Code**
   - Bugs applicatifs
   - Configuration Spring Boot
   - Intégrations externes (Stripe, Email)

### Informations à Collecter

Avant l'escalation, collecter :

```bash
# Logs récents
# Interface Coolify → Services → Logs (dernières 100 lignes)

# Configuration actuelle
# Coolify → Services → Environment Variables (masquer les secrets)

# Status des services
# Coolify → Services → Status

# Tests de connectivité
curl -I https://lmp-services.ca
curl https://lmp-services.ca/actuator/health

# Informations DNS
dig lmp-services.ca
```

---

**⚠️ Important**: Pour tout problème persistant, vérifiez d'abord les logs dans l'interface Coolify et assurez-vous que toutes les variables d'environnement sont correctement configurées pour le domaine `lmp-services.ca`.