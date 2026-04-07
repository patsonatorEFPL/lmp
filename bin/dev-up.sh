#!/usr/bin/env bash
# Démarre PostgreSQL local (docker-compose.dev.yml) et attend que la base accepte les connexions.
# Usage : depuis la racine du dépôt — ./bin/dev-up.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE_FILE="$ROOT/docker-compose.dev.yml"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Fichier introuvable : $COMPOSE_FILE" >&2
  exit 1
fi

cd "$ROOT"
echo "Démarrage de PostgreSQL (docker compose)..."
docker compose -f "$COMPOSE_FILE" up -d

ready=false
for _ in $(seq 1 45); do
  if docker compose -f "$COMPOSE_FILE" exec -T lmp-dev-db pg_isready -U lmp_dev -d lmp_db >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 2
done

if [[ "$ready" != "true" ]]; then
  echo "PostgreSQL n'est pas devenu prêt à temps. Vérifiez : docker compose -f docker-compose.dev.yml logs lmp-dev-db" >&2
  exit 1
fi

echo "PostgreSQL est prêt (volume persistant lmp-dev-pgdata)."
echo ""
echo "Lancer le backend (profil dev) :"
echo "  SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run"
echo "Ou exécuter la classe com.lmp.LmpApplication depuis l'IDE."
echo ""
echo "Mot de passe BDD par défaut (aligner application-secrets) : lmp_dev_local"
