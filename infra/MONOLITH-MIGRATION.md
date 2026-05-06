# Migration Monolith — Checklist Dokploy

Refacto code committed (commit `bcce8b9`). Steps infra restants à exécuter manuellement via Dokploy UI + SSH.

## Phase 1 — Push develop

```bash
cd lmp
git push origin develop
```

(Hook bloque Claude — manuel)

## Phase 2 — Deploy TEST

### 2.1 Dokploy redeploy `lmp-test-lmptestback-6xvske`

Dokploy UI → Application → **Redeploy** (Git pull + rebuild Dockerfile monolith).

Vérifie logs :
- `Started LmpApplication in NN seconds`
- `Migrated schema "public" to version "37"` (Flyway)
- Aucun `OidcHostGuardFilter configured for use` (filter supprimé ✓)

### 2.2 Update env vars TEST

Dokploy UI → Environment → modifier :

| Key | Avant | Après |
|---|---|---|
| `OAUTH2_ISSUER_URI` | (dérivé `https://auth-dev.lmp-services.ca`) | **supprimer** (dérivation new = `SITE_URL`) |
| `APP_FRONTEND_URL` | `https://dev.lmp-services.ca` | (supprimer — monolith) |
| `CORS_ALLOWED_ORIGINS` | `https://dev.lmp-services.ca,...,auth-dev.*` | **supprimer** (dérivé sans `auth.`) |
| `ERP_OAUTH2_REDIRECT_URI` | `http://localhost:8069/...` | `https://crm.lmp-services.ca/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso` |

Click **Deploy** (pas Rebuild — env vars seulement).

### 2.3 Flip Traefik TEST

```bash
ssh ubuntu@140.238.216.231
sudo cp /etc/dokploy/traefik/dynamic/lmp-test-lmptestback-6xvske.yml{,.bak}
# Copie le YAML monolith depuis le repo local vers le serveur
```

Sur laptop :
```bash
scp infra/traefik/lmp-test-lmptestback-6xvske.monolith.yml \
    ubuntu@140.238.216.231:/tmp/test.yml
ssh ubuntu@140.238.216.231 \
    "sudo mv /tmp/test.yml /etc/dokploy/traefik/dynamic/lmp-test-lmptestback-6xvske.yml"
```

Traefik recharge auto (file provider watch).

### 2.4 Stop frontend TEST

Dokploy UI → `lmp-test-lmptestfront-g8m81a` → **Stop Application**.

### 2.5 Verify TEST

```bash
curl -o /dev/null -w "%{http_code}\n" https://dev.lmp-services.ca/                    # 200
curl -o /dev/null -w "%{http_code}\n" https://dev.lmp-services.ca/login               # 200
curl -o /dev/null -w "%{http_code}\n" https://dev.lmp-services.ca/api/v1/config       # 200
curl -o /dev/null -w "%{http_code}\n" https://dev.lmp-services.ca/oauth2/jwks         # 200
curl -s https://dev.lmp-services.ca/api/v1/config | grep authBaseUrl
# attendu : "authBaseUrl":"https://dev.lmp-services.ca"  (== baseUrl)
```

E2E browser : connexion Administrator → dashboard.

## Phase 3 — DNS Cleanup

### Cloudflare DNS

Supprimer records (UI Cloudflare ou API) :
- `auth-dev.lmp-services.ca` (record ID `6092fb3189ec8b2c1ea90a15e660b215`)
- `auth.lmp-services.ca` (si existe)

Garde `dev.*` et apex `lmp-services.ca`.

## Phase 4 — Deploy PROD (après TEST validé)

### 4.1 Backup PROD DB

```bash
ssh ubuntu@140.238.216.231
sudo docker exec lmpdb-hmf1lz.1.<task-id> \
    pg_dump -U lmp_prod lmp_db | gzip > /tmp/lmp-prod-backup-$(date +%Y%m%d).sql.gz
```

### 4.2 Update env vars PROD

Dokploy UI → `lmp-menkeps-lmpback-bekrag` → Environment :

| Key | Avant | Après |
|---|---|---|
| `OAUTH2_ISSUER_URI` | `https://dev.lmp-services.ca` | (supprimer — dérivé `https://lmp-services.ca`) |
| `APP_FRONTEND_URL` | `https://dev.lmp-services.ca` | (supprimer) |
| `APP_BASE_URL` | `https://dev.lmp-services.ca` | (supprimer — dérivé de `SITE_URL`) |
| `CORS_ALLOWED_ORIGINS` | `https://dev.lmp-services.ca,...` | (supprimer — dérivé) |
| `SITE_URL` | absent | `https://lmp-services.ca` ⚠️ **set if missing** |
| `ERP_OAUTH2_CLIENT_ID` | `erpnext-client` | `frappe-erp-client` |
| `ERP_OAUTH2_CLIENT_SECRET` | `erpnext-secret` | `<rotate — match Frappe Social Login Key>` |
| `ERP_OAUTH2_REDIRECT_URI` | `http://localhost:8069/...` | `https://crm.lmp-services.ca/api/method/frappe.integrations.oauth2_logins.custom/lmp_sso` |

Click **Deploy** (env vars only).

### 4.3 Redeploy PROD code

Dokploy UI → **Redeploy** (Git pull + rebuild — applique 28 migrations Flyway V10→V37).

⚠️ Watch logs : si Flyway échoue → restore backup.

### 4.4 Flip Traefik PROD

```bash
scp infra/traefik/lmp-menkeps-lmpback-bekrag.monolith.yml \
    ubuntu@140.238.216.231:/tmp/prod.yml
ssh ubuntu@140.238.216.231 "
    sudo cp /etc/dokploy/traefik/dynamic/lmp-menkeps-lmpback-bekrag.yml{,.bak}
    sudo mv /tmp/prod.yml /etc/dokploy/traefik/dynamic/lmp-menkeps-lmpback-bekrag.yml
"
```

### 4.5 Stop frontend PROD

Dokploy UI → `lmpfront-zr8wpb` → **Stop Application**.

### 4.6 Verify PROD

Mêmes curls que Phase 2.5 mais sur `lmp-services.ca`.

E2E browser complet : login user/admin → dashboard → checkout → paiement Stripe test.

## Phase 5 — Migration env → DB (Frappe-style, optionnel)

Voir `infra/ENV-TO-DB-MIGRATION.md` (à venir).

## Rollback

Chaque step a un backup `.bak`. Sequence reverse :

1. Restore `.bak` Traefik YAML → `sudo mv .bak original.yml`
2. Re-start frontend Dokploy → Start Application
3. Restore env vars (Dokploy UI conserve historique)
4. Redeploy backend pour code revert si besoin

## Checklist court

- [ ] Push develop
- [ ] Phase 2 — TEST (5 sous-étapes)
- [ ] Phase 3 — DNS cleanup
- [ ] Phase 4 — PROD (6 sous-étapes)
- [ ] E2E browser PROD complet
- [ ] Phase 5 — env→DB (à planifier)
