# LMP — Load tests (k6)

Tests de charge HTTP sur le backend LMP. À lancer depuis laptop ou CI, **jamais** depuis l'host cible (fausserait les résultats CPU).

## Pré-requis

- k6 installé : `choco install k6` (Windows) / `brew install k6` (mac) / `apt install k6` (Linux)
- Stack observabilité Grafana up — pour watch les métriques côté serveur pendant le test

## Scripts

| Script | Profil | But |
|--------|--------|-----|
| `k6-smoke.js` | 1 VU × 30s | Sanity check, vérifie que tout répond |
| `k6-load.js` | 0→50 VUs × 8m | Charge nominale, perf attendue en prod |
| `k6-stress.js` | 0→500 VUs × 10m | Trouve le point de rupture |

## Cibles

| Env | URL | Quand |
|-----|-----|-------|
| Dev/Staging | `https://dev.lmp-services.ca` (défaut) | Tests systématiques avant merge |
| Prod | `https://lmp-services.ca` | Hors heures de pointe seulement, avec accord |

## Lancer

```bash
# Smoke (default = dev URL)
k6 run tools/loadtest/k6-smoke.js

# Load avec URL custom
k6 run -e BASE=https://lmp-services.ca tools/loadtest/k6-load.js

# Stress + dump JSON
k6 run --out json=stress-results.json tools/loadtest/k6-stress.js
```

## Watch côté Grafana pendant le test

Ouvrir `https://grafana.lmp-services.ca` → dashboards :

| Dashboard | Quoi surveiller |
|-----------|-----------------|
| JVM (Micrometer) — 4701 | Heap (fuite mémoire ?), threads, GC pauses |
| Spring Boot APM — 12900 | HTTP rate, errors, duration p95/p99 |
| PostgreSQL — 9628 | Connexions HikariCP, transactions, lock contention |
| Node Exporter Full — 1860 | CPU host, RAM, network saturation |
| Docker — 893 | RAM/CPU par conteneur (saturation lmp-back ?) |

## Métriques clés à observer

| Métrique | Sain | Alerte |
|----------|------|--------|
| `http_req_duration` p95 | < 800ms | > 2s |
| `http_req_failed` rate | < 1% | > 5% |
| HikariCP `connections_active` | < 30/50 | = pool max |
| JVM heap | stable | montée monotone (fuite) |
| GC pause | < 100ms | > 500ms |
| CPU host | < 70% | > 90% |

## Workflow type

1. Lancer Grafana, ouvrir dashboards en split-screen
2. `k6 run k6-smoke.js` → vérifier 0 erreur
3. `k6 run k6-load.js` → watch métriques en live, noter p95
4. Si p95 dépasse seuil ou erreurs : investigate (logs, JVM thread dump, slow queries)
5. `k6 run k6-stress.js` → trouver point de rupture (VUs où erreurs > 10%)

## Pièges

- **Tester depuis le serveur** = fausse les résultats (CPU partagé). Toujours depuis machine externe.
- **Tunnel Cloudflare dev OFF** = `dev.lmp-services.ca` route vers staging Dokploy (lmp-test-back). C'est OK pour test charge sur staging.
- **Tunnel ON** = route vers laptop, pas représentatif de la prod.
- **Stripe / SMTP webhooks** = ne pas activer en load test, sinon factures de test partout.
- **Endpoints auth-protégés** = nécessitent setup login + cookie persistence (TODO si besoin).

## TODO ultérieur

- [ ] Scripts k6 pour parcours user authentifié (login → dashboard → order)
- [ ] Scripts k6 pour webhook Stripe (mock signing)
- [ ] Output Prometheus remote-write pour voir métriques k6 dans Grafana
- [ ] Intégration GitHub Actions (run smoke sur chaque PR)
