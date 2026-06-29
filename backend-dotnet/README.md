# LMP Backend — .NET 10 port

A parallel implementation of the LMP Digital Services backend on **.NET 10 /
ASP.NET Core**, built to be compared head-to-head with the production
**Spring Boot 4 / Java** backend.

## Goals & constraints

- **Drop-in for the existing Angular frontend.** The frontend is *not* changing,
  so this backend must honour the same OpenAPI contract (`lmp-frontend/openapi.json`):
  same routes, same JSON shapes (`ApiResponse` envelope, camelCase), same auth model.
- **Shared database.** EF Core maps onto the *existing* PostgreSQL schema, which
  **Flyway** (in the Java backend) continues to own. This backend never runs
  migrations — so both backends can run against the very same database, making the
  comparison apples-to-apples.
- **Faithful business logic.** Domain rules (offer selection, regional pricing,
  psychological rounding, money precision) are ported 1:1 from the Java sources,
  not re-invented.

## Tech stack

| Concern | Choice |
|---|---|
| Runtime | .NET 10, C# 14 |
| Web | ASP.NET Core (controllers) |
| Data | EF Core 10 + Npgsql, snake_case mapping (`EFCore.NamingConventions`) |
| Packages | Central Package Management (`Directory.Packages.props`) |

## Layout (modular monolith, mirrors the Java domains)

```
backend-dotnet/
├── Directory.Build.props          # shared TFM / language / analysis settings
├── Directory.Packages.props       # central NuGet versions
├── Lmp.sln
└── src/
    ├── Lmp.Domain/                 # entities + domain logic (per module)
    ├── Lmp.Application/            # DTOs, service contracts, pricing primitives
    ├── Lmp.Infrastructure/         # EF Core DbContext, integrations, service impls
    └── Lmp.Api/                    # ASP.NET Core host, controllers, DI wiring
```

## Status

Incremental port. Implemented so far:

- Foundation: solution, central packages, DbContext mapped to the live schema,
  `ApiResponse` envelope, JSON contract (camelCase + per-DTO null omission).
- **Catalog** module — `GET /api/v1/services`, `/featured`, `/search`, `/{slug}`
  with faithful current-offer selection and regional pricing.

Pending (tracked against the 170-endpoint OpenAPI surface): auth/session + OAuth2,
billing/orders/Stripe, cart, appointments (CRM), notifications, admin, portal/config,
integration/webhooks. The live FX refresh (Frankfurter), response pre-compression
caches, and the GeoLite2 lookup are noted in-code as follow-ups.

## Run

```bash
# 1. Start the shared dev PostgreSQL (from repo root)
./bin/dev-up.sh           # or: docker compose -f docker-compose.dev.yml up -d

# 2. Run the API (expects the Flyway-migrated schema to exist)
cd backend-dotnet
dotnet run --project src/Lmp.Api
```

Configuration uses the standard ASP.NET Core providers; the database connection is
`ConnectionStrings:LmpDb` (env: `ConnectionStrings__LmpDb`).

## Test it yourself

The API listens on **`:8080`** — the same port the Angular frontend proxies to —
and maps onto the **existing Flyway-migrated schema** (it never runs migrations,
so point it at a database the Java backend has already migrated, e.g. your dev
or TEST database).

### Option A — Docker (recommended)

```bash
cd backend-dotnet
docker build -t lmp-dotnet .
docker run --rm -p 8080:8080 \
  -e ConnectionStrings__LmpDb="Host=<db-host>;Port=5432;Database=lmp_db;Username=<user>;Password=<pass>" \
  lmp-dotnet
```

### Option B — dotnet CLI

```bash
cd backend-dotnet
ConnectionStrings__LmpDb="Host=<db-host>;Port=5432;Database=lmp_db;Username=<user>;Password=<pass>" \
  dotnet run --project src/Lmp.Api
```

### Smoke test

```bash
curl http://localhost:8080/api/v1/config
curl http://localhost:8080/api/v1/services
curl -i -X POST http://localhost:8080/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"Administrator","password":"Admin@LMP-ChangeMe2026!"}'
```

### Option C — Deploy to the VPS via Dokploy (no local machine needed)

The repo branch ships a `backend-dotnet/Dockerfile`, so Dokploy can build it
straight from Git:

1. **Dokploy → Create Application** (own it on a throwaway/TEST scope, not prod).
2. **Source**: this Git repo, branch `claude/vps-access-lhgroc`.
3. **Build**: Dockerfile · Build context/path `backend-dotnet` · Dockerfile `Dockerfile`.
4. **Environment**: `ConnectionStrings__LmpDb=Host=<db>;Port=5432;Database=lmp_db;Username=<u>;Password=<p>`
   — point it at the **TEST** database (already Flyway-migrated) so prod is untouched.
5. **Port**: container `8080`. Add a domain (e.g. `dotnet-test.lmp-services.ca`) with Let's Encrypt.
6. **Deploy**, then smoke-test the domain with the curls above.

> The image build is standard multi-stage; it has not been built inside this
> sandbox (no Docker daemon), only the app itself was run via `dotnet run`.

### Point the Angular frontend at it

Run this backend on `:8080` instead of the Java one (stop the Java backend, or
run this on a different host/port and update the frontend proxy). The frontend
needs **no changes** — same routes, same `ApiResponse` envelope, same session
cookie + `X-XSRF-TOKEN` model.

> Implemented modules (verifiable now): catalog, config, SEO, auth (login/register/
> verify), cart, orders read, appointments, notifications, dashboard, and the admin
> surface (stats, users/orders lists, appointments/catalogue/orders/users management).
> Not yet wired: Stripe checkout/webhooks, transactional email (contact, password
> reset), invoice PDF, OAuth2 server + social login — these need external
> credentials and are seamed but inactive.
