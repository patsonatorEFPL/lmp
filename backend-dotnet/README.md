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
