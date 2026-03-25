# 🚀 LMP Digital Services - Application Spring Boot

Une application web complète pour la gestion des services digitaux incluant la prise de commande, l'authentification et un panneau d'administration.

## 📋 Fonctionnalités

- 🔐 **Authentification sécurisée** avec Spring Security (Local, Google, Microsoft)
- 💳 **Paiements Stripe** intégrés (Checkout + Webhooks)
- 👥 **Gestion utilisateurs** (Clients, Admins, Super Admins)
- 📊 **Dashboard administrateur** pour piloter l'activité
- 🏪 **Catalogue de services** géré dynamiquement
- 📄 **Génération de factures** PDF et confirmations de commandes
- 📧 **Notifications email** avec templates Thymeleaf
- 🔄 **Migrations de schéma** avec Flyway

## 🛠️ Technologies

- **Backend**: Spring Boot 3.3+, Java 17
- **Base de données**: MySQL (Prod) / H2 (Tests)
- **Paiements**: Stripe API
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript (Vanilla/DaisyUI)
- **Sécurité**: Spring Security, OAuth2, BCrypt, Vérification Email
- **Build**: Maven 3.9+
- **Déploiement**: Docker, Coolify

## 💻 Développement Local

### Prérequis
- Java 21+
- Maven 3.9+
- MySQL 8.0+

### Installation Rapide

```bash
# 1. Cloner le repository
git clone https://github.com/votre-organisation/lmp.git
cd lmp

# 2. Configurer la base de données MySQL
mysql -u root -p
CREATE DATABASE lmp_db;
CREATE USER 'lmp_dev'@'localhost' IDENTIFIED BY 'votre_mot_de_passe_local';
GRANT ALL PRIVILEGES ON lmp_db.* TO 'lmp_dev'@'localhost';

# 3. Configurer les variables locales
cp src/main/resources/application-secrets.properties.sample src/main/resources/application-secrets.properties
# Éditer application-secrets.properties avec vos identifiants locaux (BDD, Stripe Test, OAuth, Remember-Me)

# 4. Démarrer l'application (Profil Dev)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

L'application sera accessible sur `http://localhost:8080`.

## 🚀 Déploiement (Production)

L'application est conteneurisée et optimisée pour un déploiement continu via **Coolify** (ou tout autre orchestrateur Docker).

### Variables d'Environnement (Production)
Ne stockez jamais de secrets dans le code source. Sur votre serveur de production, vous devez définir les variables d'environnement suivantes :

```env
# Base de Données
SPRING_DATASOURCE_URL=jdbc:mysql://votre-hote:3306/lmp_db
SPRING_DATASOURCE_USERNAME=user_prod
SPRING_DATASOURCE_PASSWORD=secret_prod

# Clés Publiques et Secrètes Stripe (Mode Live)
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Authentification Sociale (OAuth2)
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_SECRET=...
```

*Le profil `prod` (activé via la directive du `Dockerfile`) gère l'optimisation des requêtes, le cache et le pool de connexions HikariCP.*

## 🏗️ Architecture

```text
src/
├── main/
│   ├── java/com/lmp/
│   │   ├── config/          # Configuration Spring, Sécurité, Maintenance
│   │   ├── controller/      # Contrôleurs Web (Frontend)
│   │   ├── domain/          # Entités JPA et DTOs
│   │   ├── repository/      # Interfaces Spring Data JPA
│   │   ├── service/         # Logique métier et implémentations
│   │   └── web/             # Contrôleurs REST API
│   └── resources/
│       ├── db/migration/    # Scripts SQL Flyway
│       ├── static/          # CSS dist, JS, Images (Assets)
│       └── templates/       # Vues Thymeleaf et templates Emails
```

## 🧪 Tests

Des tests unitaires et d'intégration couvrent les fonctionnalités critiques du back-end.

```bash
# Lancer la suite de tests
./mvnw test
```

## 🤝 Contribution

1. Créez une branche feature (`git checkout -b feature/nom-de-la-feature`)
2. Commitez vos changements (`git commit -m 'feat: ajout de...'`)
3. Poussez vers la branche (`git push origin feature/nom-de-la-feature`)
4. Ouvrez une Pull Request sur GitHub.

---
© LMP Digital Services - Tous droits réservés.