# LMP — Stack observabilité (Prometheus + Grafana)

Self-hostée sur Oracle ARM via Dokploy (Compose). Scrape **prod** (interne réseau Docker) et **dev** (laptop via tunnel Cloudflare). Zéro SaaS payant.

## Architecture

```
                  ┌────────────────────────────────────┐
                  │   Oracle ARM — Dokploy + Traefik   │
                  │                                    │
  internet ──┐    │  ┌──────────┐    ┌──────────────┐  │
             ├────┼─►│ Grafana  │◄──►│  Prometheus  │  │ scrape
             │ 443│  │ (public) │    │   (privé)    │──┼──┐
             │    │  └──────────┘    └──────┬───────┘  │  │
             │    │                         │ scrape   │  │
             │    │   ┌─────────────┐       ▼          │  │
             │    │   │  lmpback    │◄──────┘ HTTP     │  │
             │    │   │ (prod APP)  │  Basic Auth      │  │
             │    │   └─────────────┘                  │  │
             │    │   ┌─────────────────────┐          │  │
             │    │   │ node/cadvisor/pgexp │          │  │
             │    │   └─────────────────────┘          │  │
             │    └────────────────────────────────────┘  │
             │                                            │
             │    ┌──────────────┐                        │
             └───►│ dev.lmp-     │  HTTPS + Basic Auth ◄──┘
                  │ services.ca  │  (via Cloudflare tunnel)
                  │ → laptop:8080│
                  └──────────────┘
```

## Composants

| Service | Image | Port | Exposé public |
|---------|-------|------|---------------|
| Prometheus | `prom/prometheus:v2.55.1` | 9090 | ❌ Réseau interne uniquement |
| Grafana | `grafana/grafana:11.3.0` | 3000 | ✅ `grafana.lmp-services.ca` |
| node-exporter | `prom/node-exporter:v1.8.2` | 9100 | ❌ |
| cAdvisor | `gcr.io/cadvisor/cadvisor:v0.49.1` | 8080 | ❌ |
| postgres-exporter | `prometheuscommunity/postgres-exporter:v0.16.0` | 9187 | ❌ |

## Déploiement Dokploy (étapes)

### 1. Pré-requis backend LMP

Le backend LMP doit avoir l'endpoint Prometheus exposé (déjà fait dans le code main) :

```properties
# application-prod.properties (déjà configuré)
management.endpoints.web.exposure.include=health,prometheus
lmp.metrics.basic-auth.username=${PROMETHEUS_BASIC_USER:prometheus}
lmp.metrics.basic-auth.password=${PROMETHEUS_BASIC_PASS:}
```

Sur Dokploy → projet `lmpback` → Environment Variables :

```
PROMETHEUS_BASIC_USER=prometheus
PROMETHEUS_BASIC_PASS=<générer un mot de passe fort>
```

Redémarrer l'application (Deploy, pas Rebuild — cf mémoire `dokploy-rebuild-vs-deploy-env-quirk`).

Vérifier (depuis Oracle host) :
```bash
docker exec lmpback curl -u prometheus:PASS http://localhost:8080/actuator/prometheus | head
```

### 2. Idem pour dev (laptop)

Ajouter dans `src/main/resources/application-secrets.properties` (gitignored) :

```properties
PROMETHEUS_BASIC_USER=prometheus
PROMETHEUS_BASIC_PASS=<mot de passe différent de la prod>
```

Ou en variable d'environnement de la session dev.

### 3. DNS Cloudflare

Ajouter A record `grafana.lmp-services.ca` → IP Oracle ARM (`140.238.216.231`), proxy DNS-only ou orange-cloud selon ton choix.

### 4. Créer le projet Dokploy "Compose"

- Dokploy → New > Compose
- Provider : Git (sélectionner ce repo, branch `develop`)
- Compose Path : `infra/observability/docker-compose.yml`
- Environment Variables : recopier depuis `.env.example`, remplir les vraies valeurs
- Deploy

### 5. Initialiser les fichiers de mots de passe Prometheus

Sur l'Oracle host, après le premier déploiement (qui crée le mount path) :

```bash
# Trouver le path du compose Dokploy
APP_DIR=/etc/dokploy/compose/<nom-app>
cd "$APP_DIR/infra/observability/prometheus/secrets"

# Écrire les mots de passe sans newline final
printf '%s' "PASS_PROD_VALUE" | sudo tee prom-prod-pass > /dev/null
printf '%s' "PASS_DEV_VALUE"  | sudo tee prom-dev-pass  > /dev/null
sudo chmod 0400 prom-*-pass
```

Reload Prometheus (sans restart du conteneur) :
```bash
docker exec <prom-container> wget -qO- --post-data= http://localhost:9090/-/reload
```

### 6. Vérifier dans Grafana

- Aller sur `https://grafana.lmp-services.ca`
- Login admin / `${GRAFANA_ADMIN_PASSWORD}`
- Configuration > Data sources : Prometheus doit être listé "Working"
- Explore : `up{}` — tous les jobs doivent retourner `1`
  - `lmp-backend-prod` = 1
  - `lmp-backend-dev` = 1 (si laptop ON + tunnel actif), sinon 0
  - `node`, `cadvisor`, `postgres-prod` = 1

### 7. Importer dashboards (UI Grafana)

Dashboards > New > Import (par ID dashboard.com) :

| ID | Nom | Source |
|----|-----|--------|
| 4701 | JVM (Micrometer) | métriques Spring Boot |
| 12900 | Spring Boot 2.x Statistics | request rate / latency |
| 1860 | Node Exporter Full | host CPU/RAM/disk |
| 9628 | PostgreSQL Database | DB stats |
| 893 | Docker (cAdvisor) | par-conteneur |

Pour chaque : sélectionner datasource Prometheus, Import.

## Sécurité — points clés

- **Endpoint `/actuator/prometheus` = Basic Auth obligatoire** (cf `MetricsSecurityConfig.java`)
- **Prometheus jamais exposé via Traefik** — seul le réseau Docker interne le voit
- **Grafana** : login admin requis, no anonymous, no signup
- **Mots de passe** : env vars Dokploy (jamais en git) + fichiers `secrets/` (gitignored)
- **TLS** : Let's Encrypt auto via Traefik pour Grafana
- **Cardinality** : tags Micrometer globaux uniquement (`application`, `env`) — pas de tag tenant/user (sinon TSDB explose)

## Test charge — intégration

Lancer k6 depuis laptop, output Prometheus remote-write :

```bash
k6 run \
  --out experimental-prometheus-rw=https://prom-rw.lmp-services.ca/api/v1/write \
  --tag testid=$(date +%s) \
  k6/loadtest-backend.js
```

(Nécessite d'exposer un endpoint remote-write sur Prometheus + auth — étape ultérieure si besoin.)

## Mainenance

### Recharger config Prometheus sans downtime

```bash
docker exec <prom-container> kill -HUP 1
# OU
curl -X POST http://localhost:9090/-/reload  # si --web.enable-lifecycle activé
```

### Backup TSDB

Volume `prometheus-data` — snapshot via :
```bash
curl -XPOST http://localhost:9090/api/v1/admin/tsdb/snapshot
docker cp <prom>:/prometheus/snapshots/<id> ./backup/
```

### Rotation rétention

Modifier `--storage.tsdb.retention.time` dans `docker-compose.yml`. Défaut : 30 jours / 5GB.

## Troubleshooting

**Prometheus target prod = DOWN**
- Vérifier `LMP_NETWORK_NAME` correspond au réseau Docker du backend
- `docker network inspect <network>` doit lister Prometheus ET lmpback
- Tester depuis le container : `docker exec <prom> wget -O- http://lmpback:8080/actuator/health`

**Prometheus target dev = DOWN**
- Tunnel Cloudflare actif ? `https://dev.lmp-services.ca/actuator/health` doit répondre
- Backend laptop expose `/actuator/prometheus` ? Vérifier `application-secrets.properties` charge bien `PROMETHEUS_BASIC_PASS`
- Curl test : `curl -u prometheus:PASS https://dev.lmp-services.ca/actuator/prometheus | head`

**Grafana 502 / Bad Gateway via Traefik**
- Vérifier label `traefik.docker.network=dokploy-network`
- Container Grafana sur le bon réseau ? `docker network inspect dokploy-network`
