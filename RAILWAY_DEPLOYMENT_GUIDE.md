# Guide de Déploiement Railway.com - Application LMP

## 📋 Variables d'Environnement Requises

### 🗄️ Base de Données MySQL
```
DATABASE_URL=mysql://username:password@host:port/database_name
MYSQLUSER=votre_utilisateur_mysql
MYSQLPASSWORD=votre_mot_de_passe_mysql
```

### 💳 Stripe Production
```
STRIPE_SECRET_KEY=sk_live_votre_cle_secrete_stripe
STRIPE_PUBLISHABLE_KEY=pk_live_votre_cle_publique_stripe
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook_stripe
```

### 🌐 Configuration Application
```
APP_BASE_URL=https://votre-app.railway.app
JWT_SECRET=votre_secret_jwt_super_securise_minimum_32_caracteres
```

### 📧 Configuration Email
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_mot_de_passe_application_gmail
```

### 🏢 Informations Entreprise (Optionnel)
```
COMPANY_ADDRESS=123 Rue Principale, Ville, Province, Code Postal
COMPANY_PHONE=+1 (555) 123-4567
COMPANY_EMAIL=contact@lmp-digital.ca
```

## 🚀 Étapes de Déploiement

### 1. Préparation du Repository Git
```bash
# Assurez-vous que tous les fichiers sont commités
git add .
git commit -m "Préparation déploiement Railway"
git push origin main
```

### 2. Connexion à Railway.com
1. Connectez-vous sur [railway.app](https://railway.app)
2. Cliquez sur "New Project"
3. Sélectionnez "Deploy from GitHub repo"
4. Choisissez votre repository LMP

### 3. Configuration des Variables d'Environnement
1. Dans Railway Dashboard → Votre projet → Variables
2. Ajoutez TOUTES les variables listées ci-dessus
3. **IMPORTANT:** Remplacez les valeurs par vos vraies données

### 4. Configuration de la Base de Données
1. Railway devrait automatiquement détecter votre base MySQL existante
2. Vérifiez que `DATABASE_URL` pointe vers votre base Railway
3. Format attendu: `mysql://user:password@host:port/database`

### 5. Premier Déploiement
1. Railway va automatiquement builder et déployer
2. Surveillez les logs dans Railway Dashboard
3. Le déploiement prend généralement 3-5 minutes

## ✅ Vérifications Post-Déploiement

### 1. Santé de l'Application
- URL: `https://votre-app.railway.app/actuator/health`
- Statut attendu: `{"status":"UP"}`

### 2. Page d'Accueil
- URL: `https://votre-app.railway.app/`
- Doit afficher la page d'accueil LMP

### 3. Connexion Admin
- URL: `https://votre-app.railway.app/admin`
- Login: `admin@lmp.ca`
- Mot de passe: `admin123`

### 4. Test Paiement Stripe
- Créez une commande test
- Vérifiez les webhooks dans Stripe Dashboard

## 🔧 Configuration des Webhooks Stripe

### URL Webhook Production
```
https://votre-app.railway.app/api/stripe/webhook
```

### Événements à Configurer
- `checkout.session.completed`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `invoice.payment_succeeded`
- `invoice.payment_failed`

## 📊 Monitoring et Logs

### Accès aux Logs Railway
1. Railway Dashboard → Votre projet → Deployments
2. Cliquez sur le déploiement actif
3. Onglet "Logs" pour voir les logs en temps réel

### Points de Surveillance
- Temps de réponse des API
- Erreurs de base de données
- Échecs de paiement Stripe
- Utilisation mémoire/CPU

## 🚨 Troubleshooting

### Application ne démarre pas
1. Vérifiez les variables d'environnement
2. Consultez les logs de déploiement
3. Vérifiez la connexion à la base de données

### Erreurs Stripe
1. Vérifiez les clés de production
2. Confirmez la configuration des webhooks
3. Testez la connectivité API Stripe

### Problèmes de Base de Données
1. Vérifiez `DATABASE_URL`
2. Contrôlez les logs Flyway
3. Validez les permissions utilisateur MySQL

## 📱 URLs Importantes

### Production
- **Application:** `https://votre-app.railway.app`
- **Admin:** `https://votre-app.railway.app/admin`
- **API Health:** `https://votre-app.railway.app/actuator/health`
- **Webhook Stripe:** `https://votre-app.railway.app/api/stripe/webhook`

### Développement Local
- **Application:** `http://localhost:8080`
- **Admin:** `http://localhost:8080/admin`

## 🔐 Sécurité

### Recommandations
- [ ] Utilisez des mots de passe forts pour la base de données
- [ ] Générez un JWT secret unique et sécurisé
- [ ] Configurez SSL/HTTPS (Railway le fait automatiquement)
- [ ] Surveillez les logs pour détecter les tentatives d'intrusion
- [ ] Changez régulièrement les mots de passe admin

### Comptes par Défaut à Changer
```
Admin: admin@lmp.ca / admin123
User Test: user@lmp.ca / user123
```

**⚠️ IMPORTANT:** Changez ces mots de passe IMMÉDIATEMENT après le déploiement !

## 📞 Support

En cas de problème:
1. Consultez les logs Railway
2. Vérifiez la documentation Spring Boot
3. Testez en local d'abord
4. Contactez le support Railway si nécessaire

---
*Guide créé pour le déploiement de l'application LMP Digital Services sur Railway.com*