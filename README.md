# LMP Digital Services

Full-stack application for managing digital services — local marketing, SEO, web development, and advertising campaigns.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 25 |
| Frontend | Angular 21 (SSR), Tailwind CSS v4 |
| UI Components | Spartan UI / Helm, Lucide Angular |
| Database | PostgreSQL (prod) / persistent local PostgreSQL via Docker in dev (`docker-compose.dev.yml`) |
| Payments | Stripe API (Checkout + Webhooks) — stripe-java 32 |
| Auth | Spring Security, OAuth2 (Google, Microsoft), BCrypt |
| Emails | Thymeleaf templates |
| Migrations | Flyway |
| Build | Maven 3.9+, Angular CLI 21 |
| Deployment | Docker, Dokploy |

## Features

- Local, Google, and Microsoft authentication (OAuth2)
- Stripe payments with webhook handling
- Service catalog managed dynamically through the API
- Online appointment booking
- Order management and PDF invoices
- HTML email notifications (confirmation, shipping, cancellation…)
- Full admin dashboard (users, orders, appointments, services)
- Customer dashboard (order history, appointments, settings)
- SEO-optimized Angular SSR landing page
- Multilingual support (FR, EN, ES, DE, IT, NL, PT, LB)
- Light / dark mode

## Local setup

### Prerequisites

- Java 25+
- Maven 3.9+
- Node.js 20+ and npm 10+
- Docker (recommended — for the development PostgreSQL with persistent data)

### Backend

```bash
# 1. Clone the repository
git clone https://github.com/your-organization/lmp.git
cd lmp

# 2. Configure secrets
cp src/main/resources/application-secrets.properties.sample \
   src/main/resources/application-secrets.properties
# Fill in the values: Stripe Test, OAuth, Remember-Me
# For the local database: DB_PASSWORD must match POSTGRES_PASSWORD (default below: lmp_dev_local)

# 3. Start PostgreSQL (data stored in a Docker volume — preserved across restarts)
#    Recommended script: waits for Postgres to be ready (pg_isready).
./bin/dev-up.sh
#    Windows PowerShell: .\bin\dev-up.ps1
#    Manual equivalent: docker compose -f docker-compose.dev.yml up -d

# 4. Start the application (dev profile by default)
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
#    Windows PowerShell: $env:SPRING_PROFILES_ACTIVE='dev'; mvn spring-boot:run
#    Or: run com.lmp.LmpApplication from your IDE
```

The API is available at `http://localhost:8080`.

**Without persistent PostgreSQL (CI / machine without Compose):** run with Testcontainers — `LMP_DEV_TESTCONTAINERS=true` then `./mvnw spring-boot:test-run` or run `TestLmpApplication` (ephemeral database, only for this mode).

**Troubleshooting — PostgreSQL authentication error (`28P01`):** the password on the Spring side must match the one in the container. Default: `lmp_dev_local` in `application-dev.properties` and in `docker-compose.dev.yml` (`POSTGRES_PASSWORD`). If `application-secrets.properties` defines a different `DB_PASSWORD`, align it or remove the line to fall back to the dev profile value.

### Frontend

```bash
cd lmp-frontend

# Install dependencies
npm install

# Start the development server
npm start
```

The app is available at `http://localhost:4200` (proxied to the backend on `:8080`).

## Architecture

The backend follows a **domain-modular architecture** (package-by-feature).

```
lmp/
├── src/main/java/com/lmp/
│   ├── auth/            # Authentication (OAuth2, JWT, BCrypt, sessions)
│   │   ├── config/      # Spring Security, OAuth2
│   │   ├── domain/      # User, Role, Token
│   │   ├── repository/
│   │   ├── service/
│   │   └── web/         # Auth + admin users API
│   ├── billing/         # Orders, Stripe payments, PDF invoices
│   │   ├── config/      # Stripe SDK
│   │   ├── domain/      # Order, Invoice, Payment
│   │   ├── service/     # Payment processors, webhooks
│   │   └── web/         # Orders + admin orders API
│   ├── catalog/         # Services & commercial offers
│   │   ├── domain/      # Service, Offer, Category
│   │   └── web/         # Catalog + admin services API
│   ├── crm/             # Appointments and contacts
│   │   ├── domain/      # Appointment, Contact
│   │   └── web/         # Appointments + admin appointments API
│   ├── integration/     # Inbound webhooks / external events
│   ├── notification/    # Thymeleaf emails, in-app notifications
│   ├── portal/          # Public endpoints (landing, SEO)
│   └── shared/          # Global config, shared DTOs, exceptions, utils
├── src/main/resources/
│   ├── db/migration/         # Flyway scripts (active)
│   ├── db/migration-mysql-legacy/ # Legacy MySQL history
│   ├── db/scripts/           # SQL utility scripts
│   ├── i18n/                 # Validation messages (fr, en)
│   ├── static/images/        # Static assets
│   └── templates/
│       ├── emails/           # Thymeleaf HTML templates (15+ models)
│       └── error/            # Error pages (403, 500)
└── lmp-frontend/             # Angular 21 SPA (SSR)
    └── src/app/
        ├── core/             # Guards, interceptors, domain services
        ├── features/         # Pages (home, services, auth, dashboard, admin…)
        ├── generated/        # Generated API clients (OpenAPI / ng-openapi-gen)
        ├── shared/           # Layouts, modals
        └── libs/ui/          # Spartan/Helm components (button, card, input…)
```

## Deployment (production)

The application is containerized and deployed through **Dokploy**. The frontend `Dockerfile` produces an Angular SSR build served by Express, and the backend `Dockerfile` produces an optimized Spring Boot JAR.

### Required environment variables

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/lmp_db
SPRING_DATASOURCE_USERNAME=prod_user
SPRING_DATASOURCE_PASSWORD=prod_secret

# Stripe (Live keys)
STRIPE_PUBLISHABLE_KEY=pk_live_...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# OAuth2
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MICROSOFT_CLIENT_SECRET=...
```

## Default administrator account

On the first run, Flyway seeds an admin account:

| Field | Value |
|---|---|
| Email | `admin@lmp.ca` |
| Password | `Admin@LMP-ChangeMe2026!` |

> Change this password immediately after the first login via **Profile → Security**.

## Tests

```bash
# Backend tests
./mvnw test

# Frontend tests
cd lmp-frontend && npm test
```

## Contributing

1. Create a branch: `git checkout -b feature/feature-name`
2. Commit: `git commit -m 'feat: description'`
3. Push: `git push origin feature/feature-name`
4. Open a Pull Request

---

© 2026 LMP Digital Services — All rights reserved.
