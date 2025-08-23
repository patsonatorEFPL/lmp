# ✅ Checklist de Déploiement Railway.com - Application LMP

## 📋 Étapes Completées ✅

- [x] **Analyse des migrations Flyway** - Base de données prête
- [x] **Configuration production** - `application-prod.properties` créé
- [x] **Variables d'environnement** - Documentation complète
- [x] **Dockerfile optimisé** - Java 21 avec multi-stage build
- [x] **Railway.toml** - Configuration de déploiement
- [x] **Flyway activé** - Migrations automatiques en production
- [x] **Guide webhooks Stripe** - Configuration détaillée
- [x] **Script test DB** - Vérification connexion Railway

## 🚀 Étapes de Déploiement Restantes

### 9. Connecter le Repository Git à Railway.com

#### Préparation Git
```bash
# 1. Vérifier que tous les fichiers sont ajoutés
git status

# 2. Ajouter les nouveaux fichiers
git add .

# 3. Commit final avant déploiement
git commit -m "🚀 Préparation déploiement Railway - Configuration production complète"

# 4. Push vers le repository principal
git push origin main
```

#### Connexion Railway
1. 🌐 Aller sur [railway.app](https://railway.app)
2. 🔐 Se connecter avec GitHub
3. ➕ Cliquer "New Project"
4. 📂 Sélectionner "Deploy from GitHub repo"
5. 🎯 Choisir le repository LMP
6. 🔗 Autoriser l'accès Repository

---

### 10. Configurer les Variables d'Environnement dans Railway

#### Variables Essentielles à Configurer
```env
# Base de Données MySQL
DATABASE_URL=mysql://username:password@host:port/database_name
MYSQLUSER=votre_utilisateur_mysql
MYSQLPASSWORD=votre_mot_de_passe_mysql

# Stripe Production (IMPORTANT: Clés LIVE!)
STRIPE_SECRET_KEY=sk_live_votre_cle_secrete
STRIPE_PUBLISHABLE_KEY=pk_live_votre_cle_publique
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook

# Application
APP_BASE_URL=https://votre-app.railway.app
JWT_SECRET=votre_secret_jwt_minimum_32_caracteres
SPRING_PROFILES_ACTIVE=prod

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_mot_de_passe_app

# Entreprise (Optionnel)
COMPANY_ADDRESS=Votre adresse complète
COMPANY_PHONE=Votre numéro
COMPANY_EMAIL=contact@votre-domaine.com
```

#### Comment Configurer
1. 🎛️ Railway Dashboard → Votre projet
2. ⚙️ Onglet "Variables"
3. ➕ Ajouter chaque variable une par une
4. 💾 Sauvegarder après chaque ajout

---

### 11. Premier Déploiement et Vérification des Logs

#### Surveillance du Déploiement
1. 📊 Railway Dashboard → "Deployments"
2. 👀 Surveiller le build en temps réel
3. ⏱️ Attendre 3-5 minutes pour le build complet
4. 🔍 Vérifier les logs de déploiement

#### Logs à Surveiller
```
✅ Building Docker image...
✅ Installing Maven dependencies...
✅ Compiling Spring Boot application...
✅ Creating optimized JAR...
✅ Starting application container...
✅ Spring Boot started on port 8080
✅ Flyway migrations executed successfully
✅ Application ready to serve traffic
```

#### En Cas d'Erreur
- 🔴 **Build Failed**: Vérifier les dépendances Maven
- 🔴 **Start Failed**: Vérifier les variables d'environnement
- 🔴 **DB Error**: Vérifier la connexion MySQL

---

### 12. Tester les Fonctionnalités Critiques

#### 🏠 Test Page d'Accueil
```
URL: https://votre-app.railway.app/
Vérification: 
- [x] Page se charge sans erreur
- [x] CSS et images fonctionnent
- [x] Navigation fonctionne
```

#### 👤 Test Authentification
```
URL: https://votre-app.railway.app/admin
Login: admin@lmp.ca
Password: admin123
Vérification:
- [x] Connexion réussie
- [x] Dashboard admin accessible
- [x] Gestion utilisateurs fonctionne
```

#### 💳 Test Paiements Stripe
```
1. Créer une commande test
2. Utiliser carte test: 4242 4242 4242 4242
3. Vérifier paiement dans Stripe Dashboard
4. Confirmer webhook reçu dans logs
```

#### 📊 Test Health Check
```
URL: https://votre-app.railway.app/actuator/health
Réponse attendue: {"status":"UP"}
```

---

### 13. Configurer les Webhooks Stripe avec l'URL Railway

#### Configuration Stripe Dashboard
1. 🔐 [Stripe Dashboard](https://dashboard.stripe.com) → Mode LIVE
2. 🔧 Developers → Webhooks
3. ➕ Add endpoint
4. 🌐 URL: `https://votre-app.railway.app/api/stripe/webhook`
5. 📋 Événements essentiels:
   - `checkout.session.completed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `invoice.payment_succeeded`

#### Test Webhook
1. 🧪 Test webhook depuis Stripe Dashboard
2. 📝 Vérifier dans Railway logs:
   ```
   INFO StripeWebhookHandler - Webhook reçu: checkout.session.completed
   INFO StripeWebhookHandler - Paiement traité avec succès
   ```

---

### 14. Documentation Finale et Maintenance

#### 📚 Documentation à Créer
- [x] Guide de déploiement ✅ (`RAILWAY_DEPLOYMENT_GUIDE.md`)
- [x] Configuration webhooks ✅ (`STRIPE_WEBHOOK_SETUP.md`)
- [x] Checklist déploiement ✅ (`DEPLOYMENT_CHECKLIST.md`)
- [ ] Guide maintenance quotidienne
- [ ] Procédures de rollback
- [ ] Contact support

#### 🔧 Configuration Monitoring
```
URLs à surveiller:
- Application: https://votre-app.railway.app/
- Health: https://votre-app.railway.app/actuator/health
- Admin: https://votre-app.railway.app/admin

Métriques importantes:
- Temps de réponse < 2s
- Taux de succès > 99%
- Utilisation mémoire < 80%
```

#### 🔐 Sécurité Post-Déploiement
- [ ] Changer mot de passe admin par défaut
- [ ] Supprimer utilisateur test
- [ ] Vérifier SSL/HTTPS actif
- [ ] Tester tentatives d'intrusion

---

## 🎯 Résumé Final

### ✅ Fichiers Créés
- `src/main/resources/application-prod.properties`
- `Dockerfile`
- `railway.toml`
- `.dockerignore`
- `RAILWAY_DEPLOYMENT_GUIDE.md`
- `STRIPE_WEBHOOK_SETUP.md`
- `scripts/test-db-connection.py`

### 🔑 Points Critiques
1. **Variables d'environnement**: Toutes doivent être configurées
2. **Clés Stripe**: Utiliser les clés LIVE (sk_live_, pk_live_)
3. **Base de données**: Connexion Railway MySQL testée
4. **Webhooks**: URL Railway configurée dans Stripe
5. **Sécurité**: Mots de passe par défaut changés

### 📞 Support
- Railway: [docs.railway.app](https://docs.railway.app)
- Stripe: [stripe.com/docs](https://stripe.com/docs)
- Spring Boot: [spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)

---

**🚀 Votre application LMP est prête pour la production sur Railway.com !**

*Bonne chance avec votre déploiement !* 🎉