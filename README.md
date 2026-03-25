# LMP Digital Services

Application fullstack de gestion des services digitaux — marketing local, référencement SEO, création web et campagnes publicitaires.

## Stack technique

| Couche | Technologie |
|---|---|
| Backend | Spring Boot 3.5.4, Java 21 |
| Frontend | Angular 21 (SSR), Tailwind CSS v4 |
| UI Components | Spartan UI / Helm, Lucide Angular |
| Base de données | MySQL 8 (prod) / H2 (tests) |
| Paiements | Stripe API (Checkout + Webhooks) |
| Auth | Spring Security, OAuth2 (Google, Microsoft), BCrypt |
| Emails | Thymeleaf templates |
| Migrations | Flyway |
| Build | Maven 3.9+, Angular CLI 21 |
| Déploiement | Docker, Coolify |

## Fonctionnalités

- Authentification locale, Google et Microsoft (OAuth2)
- Paiements Stripe avec gestion des webhooks
- Catalogue de services géré dynamiquement via l'API
- Prise de rendez-vous en ligne
- Gestion des commandes et factures PDF
- Notifications email HTML (confirmation, shipping, annulation…)
- Dashboard administrateur complet (utilisateurs, commandes, RDV, services)
- Dashboard client (historique commandes, rendez-vous, paramètres)
- Landing page Angular SSR optimisée SEO
- Support multilingue (FR, EN, ES, DE, IT, NL, PT, LB)
- Mode clair / sombre

## Démarrage local

### Prérequis

- Java 21+
- Maven 3.9+
- Node.js 20+ et npm 10+
- MySQL 8.0+

### Backend

```bash
# 1. Cloner le repository
git clone https://github.com/votre-organisation/lmp.git
cd lmp

# 2. Créer la base de données
mysql -u root -p
CREATE DATABASE lmp_db;
CREATE USER 'lmp_dev'@'localhost' IDENTIFIED BY 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON lmp_db.* TO 'lmp_dev'@'localhost';

# 3. Configurer les secrets
cp src/main/resources/application-secrets.properties.sample \
   src/main/resources/application-secrets.properties
# Remplir les valeurs : BDD, Stripe Test, OAuth, Remember-Me

# 4. Lancer l'application (profil dev)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

API accessible sur `http://localhost:8080`.

### Frontend

```bash
cd lmp-frontend

# Installer les dépendances
npm install

# Lancer le serveur de développement
npm start
```

Application accessible sur `http://localhost:4200` (proxy vers le backend sur `:8080`).

## Architecture

```
lmp/
├── src/main/java/com/lmp/
│   ├── config/          # Spring Security, Stripe, CORS, maintenance
│   ├── controller/      # Contrôleurs REST API
│   ├── domain/          # Entités JPA, DTOs, enums
│   ├── repository/      # Interfaces Spring Data JPA
│   ├── service/         # Logique métier (commandes, paiements, emails…)
│   └── shared/          # Utilitaires, exceptions globales
├── src/main/resources/
│   ├── db/migration/    # Scripts Flyway
│   ├── static/          # Vérification Google Search Console
│   └── templates/       # Emails HTML Thymeleaf
└── lmp-frontend/        # SPA Angular (SSR)
    ├── src/app/
    │   ├── core/        # Services, guards, interceptors
    │   ├── features/    # Pages (home, services, auth, dashboard, admin…)
    │   ├── shared/      # Layouts, modals, composants partagés
    │   └── libs/ui/     # Composants Spartan/Helm (button, card, input…)
    └── public/          # manifest, robots.txt, sitemap
```

## Déploiement (production)

L'application est conteneurisée. Le `Dockerfile` du frontend génère un build Angular SSR servi par Express, et le `Dockerfile` du backend produit un JAR Spring Boot optimisé.

### Variables d'environnement requises

```env
# Base de données
SPRING_DATASOURCE_URL=jdbc:mysql://hote:3306/lmp_db
SPRING_DATASOURCE_USERNAME=user_prod
SPRING_DATASOURCE_PASSWORD=secret_prod

# Stripe (clés Live)
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# OAuth2
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_SECRET=...
```

## Compte administrateur par défaut

Au premier démarrage, Flyway initialise un compte admin :

| Champ | Valeur |
|---|---|
| Email | `admin@lmp.ca` |
| Mot de passe | `Admin@LMP-ChangeMe2026!` |

> Changer ce mot de passe immédiatement après la première connexion via **Profil → Sécurité**.

## Tests

```bash
# Tests backend
./mvnw test

# Tests frontend
cd lmp-frontend && npm test
```

## Contribution

1. Créer une branche : `git checkout -b feature/nom-de-la-feature`
2. Committer : `git commit -m 'feat: description'`
3. Pousser : `git push origin feature/nom-de-la-feature`
4. Ouvrir une Pull Request

---

© 2026 LMP Digital Services — Tous droits réservés.
