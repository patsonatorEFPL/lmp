# 🚀 LMP Digital Services - Application Spring Boot

Une application web complète pour la gestion de services digitaux avec paiements Stripe et dashboard administrateur.

## 📋 Fonctionnalités

- 🔐 **Authentification sécurisée** avec Spring Security
- 💳 **Paiements Stripe** intégrés (Checkout + Webhooks)
- 👥 **Gestion utilisateurs** avec rôles admin/user
- 📊 **Dashboard administrateur** complet
- 🏪 **Catalogue de services** dynamique
- 📄 **Génération de factures** PDF
- 📧 **Notifications email** automatiques
- 🔄 **Migrations base de données** avec Flyway

## 🛠️ Technologies

- **Backend**: Spring Boot 3.5.4, Java 21
- **Base de données**: MySQL 8.0+
- **Paiements**: Stripe API
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript
- **Sécurité**: Spring Security, BCrypt
- **Build**: Maven 3.9+
- **Déploiement**: Docker, Railway.com

## 🚀 Déploiement sur Railway.com

### Déploiement en 1-Click
1. **Fork ce repository**
2. **Connectez-vous sur [Railway.app](https://railway.app)**
3. **Deploy from GitHub** → Sélectionnez votre fork
4. **Configurez les variables d'environnement** (voir ci-dessous)
5. **Déployez !** 🎉

### Variables d'Environnement Requises

```env
# Base de Données MySQL Railway
DATABASE_URL=mysql://username:password@host:port/database
MYSQLUSER=votre_utilisateur
MYSQLPASSWORD=votre_mot_de_passe

# Stripe Production
STRIPE_SECRET_KEY=sk_live_votre_cle_secrete
STRIPE_PUBLISHABLE_KEY=pk_live_votre_cle_publique
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook

# Application
APP_BASE_URL=https://votre-app.railway.app
JWT_SECRET=votre_secret_jwt_minimum_32_caracteres

# Email (Gmail)
MAIL_USERNAME=votre_email@gmail.com
MAIL_PASSWORD=votre_mot_de_passe_app
```

### 📚 Guides Détaillés
- 📖 [**Guide de Déploiement Complet**](RAILWAY_DEPLOYMENT_GUIDE.md)
- ✅ [**Checklist de Déploiement**](DEPLOYMENT_CHECKLIST.md)
- 🔗 [**Configuration Webhooks Stripe**](STRIPE_WEBHOOK_SETUP.md)

## 💻 Développement Local

### Prérequis
- Java 21+
- Maven 3.9+
- MySQL 8.0+
- Compte Stripe (mode test)

### Installation Rapide

```bash
# 1. Cloner le repository
git clone https://github.com/votre-username/lmp.git
cd lmp

# 2. Configurer la base de données MySQL
mysql -u root -p
CREATE DATABASE lmp;
CREATE USER 'lmp_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON lmp.* TO 'lmp_user'@'localhost';

# 3. Configurer application.properties
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Éditer avec vos paramètres locaux

# 4. Installer les dépendances et démarrer
./mvnw spring-boot:run
```

L'application sera accessible sur `http://localhost:8080`

### 🧪 Comptes de Test

```
Admin: admin@lmp.ca / admin123
User:  user@lmp.ca / user123
```

**⚠️ Changez ces mots de passe en production !**

## 🏗️ Architecture

```
src/
├── main/
│   ├── java/com/lmp/
│   │   ├── config/          # Configuration Spring
│   │   ├── controller/      # Contrôleurs web
│   │   ├── domain/          # Entités et enums
│   │   ├── repository/      # Repositories JPA
│   │   ├── service/         # Services métier
│   │   └── web/             # Contrôleurs REST API
│   └── resources/
│       ├── db/migration/    # Scripts Flyway
│       ├── static/          # CSS, JS, Images
│       └── templates/       # Templates Thymeleaf
```

## 🧪 Tests

```bash
# Tests unitaires
./mvnw test

# Tests d'intégration
./mvnw verify

# Test de connexion DB Railway
python3 scripts/test-db-connection.py
```

## 📊 API Endpoints

### Public
- `GET /` - Page d'accueil
- `GET /services` - Catalogue de services
- `POST /contact` - Formulaire de contact

### Authentification
- `GET /login` - Connexion
- `GET /register` - Inscription
- `POST /logout` - Déconnexion

### Admin
- `GET /admin` - Dashboard admin
- `GET /admin/users` - Gestion utilisateurs
- `GET /admin/orders` - Gestion commandes

### API Stripe
- `POST /api/stripe/create-checkout-session` - Créer session paiement
- `POST /api/stripe/webhook` - Webhooks Stripe
- `GET /stripe/checkout/success` - Succès paiement
- `GET /stripe/checkout/cancel` - Annulation paiement

### Monitoring
- `GET /actuator/health` - Santé de l'application
- `GET /actuator/info` - Informations application

## 🔧 Configuration Stripe

### Mode Test (Développement)
```properties
stripe.secret.key=sk_test_...
stripe.publishable.key=pk_test_...
stripe.webhook.secret=whsec_...
```

### Mode Live (Production)
```properties
stripe.secret.key=sk_live_...
stripe.publishable.key=pk_live_...
stripe.webhook.secret=whsec_...
```

### Webhooks à Configurer
- `checkout.session.completed`
- `payment_intent.succeeded`
- `payment_intent.payment_failed`
- `invoice.payment_succeeded`

## 📱 URLs de Production

Une fois déployé sur Railway :

- **Application**: `https://votre-app.railway.app`
- **Admin**: `https://votre-app.railway.app/admin`
- **API Health**: `https://votre-app.railway.app/actuator/health`
- **Webhook Stripe**: `https://votre-app.railway.app/api/stripe/webhook`

## 🔐 Sécurité

- ✅ HTTPS forcé en production
- ✅ Hachage des mots de passe avec BCrypt
- ✅ Protection CSRF
- ✅ Sessions sécurisées
- ✅ Validation des entrées utilisateur
- ✅ Vérification signatures Stripe webhooks

## 📈 Monitoring

### Logs à Surveiller
- Erreurs d'authentification
- Échecs de paiement Stripe
- Erreurs de base de données
- Performance des requêtes

### Métriques Importantes
- Temps de réponse < 2s
- Taux de succès > 99%
- Taux de conversion paiements
- Utilisation ressources serveur

## 🆘 Dépannage

### Problèmes Communs

**Application ne démarre pas**
- Vérifiez la connexion base de données
- Contrôlez les variables d'environnement
- Consultez les logs Railway

**Erreurs Stripe**
- Vérifiez les clés API (test vs live)
- Contrôlez la configuration webhooks
- Testez avec cartes de test Stripe

**Problèmes de connexion**
- Vérifiez les paramètres MySQL
- Contrôlez les permissions utilisateur
- Testez avec le script `test-db-connection.py`

## 📞 Support

- 📧 Email: support@lmp-digital.ca
- 📖 Documentation: [Guides de déploiement](RAILWAY_DEPLOYMENT_GUIDE.md)
- 🐛 Issues: [GitHub Issues](https://github.com/votre-username/lmp/issues)

## 📄 Licence

Ce projet est sous licence MIT. Voir [LICENSE](LICENSE) pour plus de détails.

## 🤝 Contribution

Les contributions sont les bienvenues ! Veuillez :

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/amelioration`)
3. Commit vos changements (`git commit -m 'Ajouter une amélioration'`)
4. Push vers la branche (`git push origin feature/amelioration`)
5. Ouvrir une Pull Request

---

**Développé avec ❤️ pour LMP Digital Services**

*Déployé facilement sur Railway.com en quelques minutes !* 🚀