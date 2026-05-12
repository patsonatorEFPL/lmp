#!/usr/bin/env bash
# ============================================================================
# LMP Backend Autoscaler — Docker Compose + Prometheus (Dokploy stack)
# ============================================================================
# Run on Oracle host as root via /etc/cron.d/lmp-autoscaler (every 30s, dual
# cron entries with a +30s sleep on one of them).
#
# Mode : LMP backend tourne en Docker Compose (pas Swarm) sous Dokploy. Scale
# via `docker compose --scale`, NOT `docker service scale`.
#
# Rules :
#   CPU avg(2m) > 80% AND replicas < MAX → scale +1 (cooldown 60s up)
#   CPU avg(30m) < 30% AND replicas > MIN → scale -1 (cooldown 30m down)
#
# Anti-flap : direction-aware cooldown (cf. set_cooldown / in_cooldown).
#
# Crontab (already deployed) :
#   * * * * * root /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1
#   * * * * * root sleep 30 && /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1
# ============================================================================
set -uo pipefail

# ===== CONFIG =====
# Dokploy stack Compose : container name pattern = lmp-back-sm5uvg-backend-N.
# Labels com.docker.compose.project + com.docker.compose.service used for
# filtering (more robust than name regex).
COMPOSE_PROJECT="${COMPOSE_PROJECT:-lmp-back-sm5uvg}"
COMPOSE_SERVICE="${COMPOSE_SERVICE:-backend}"
COMPOSE_FILE="${COMPOSE_FILE:-/etc/dokploy/compose/lmp-back-sm5uvg/code/docker-compose.yml}"

# Prometheus via docker exec — pas besoin d'exposer port host. cAdvisor scrape
# tous les containers via container name regex (LMP backend = compose
# container `lmp-back-sm5uvg-backend-N`).
PROM_CONTAINER="${PROM_CONTAINER:-lmp-menkeps-observability-siwy2b-prometheus-1}"

MIN_REPLICAS=2
MAX_REPLICAS=5
SCALE_UP_THRESHOLD=80    # CPU%
SCALE_DOWN_THRESHOLD=30  # CPU% (50 pts hysteresis vs 80)
SCALE_UP_WINDOW="2m"     # cAdvisor scrape ~60s : 1m donne 1 sample (rate() empty),
                         # 2m garantit >=2 samples pour rate().
SCALE_DOWN_WINDOW="30m"
COOLDOWN_FILE="/var/run/lmp-autoscaler.cooldown"
SCALE_UP_COOLDOWN_S=60       # 1 min entre 2 scale-ups
SCALE_DOWN_COOLDOWN_S=1800   # 30 min entre 2 scale-downs

# ===== LOG =====
log() { echo "$(date -Iseconds) [autoscaler] $*"; }

# ===== COOLDOWN =====
# Direction-aware anti-flap :
#   scale-up : blocks scale-up 60s + scale-down 30min (rolling avg pollué par idle)
#   scale-down : blocks scale-down 30min + scale-up 60s
in_cooldown() {
  local now action; now=$(date +%s); action="$1"
  [ ! -f "$COOLDOWN_FILE" ] && return 1
  local last_action last_ts
  read -r last_action last_ts < "$COOLDOWN_FILE" 2>/dev/null || return 1
  local elapsed=$((now - last_ts))
  case "$last_action:$action" in
    scale-up:scale-up)     [ $elapsed -lt $SCALE_UP_COOLDOWN_S ] ;;
    scale-up:scale-down)   [ $elapsed -lt $SCALE_DOWN_COOLDOWN_S ] ;;
    scale-down:scale-up)   [ $elapsed -lt $SCALE_UP_COOLDOWN_S ] ;;
    scale-down:scale-down) [ $elapsed -lt $SCALE_DOWN_COOLDOWN_S ] ;;
    *) return 1 ;;
  esac
}

set_cooldown() {
  echo "$1 $(date +%s)" > "$COOLDOWN_FILE"
}

# ===== PROMETHEUS QUERY =====
# Returns avg CPU% (0..N*100% sur N vCPU) de tous les backend containers sur $window.
prom_avg_cpu() {
  local window="$1"
  local query="avg(rate(container_cpu_usage_seconds_total{name=~\"${COMPOSE_PROJECT}-${COMPOSE_SERVICE}.*\"}[$window])) * 100"
  local encoded resp value
  encoded=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$query")
  resp=$(docker exec "$PROM_CONTAINER" wget -qO- --timeout=5 \
    "http://localhost:9090/api/v1/query?query=$encoded" 2>/dev/null) || { echo "ERR"; return 1; }
  value=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); r=d.get('data',{}).get('result',[]); print(r[0]['value'][1] if r else 'NaN')" 2>/dev/null)
  echo "$value"
}

# ===== DOCKER COMPOSE =====
get_replicas() {
  # Count running containers matching the compose project/service labels.
  # Plus robuste que name regex (immune à renaming/numbering quirks).
  docker ps --filter "label=com.docker.compose.project=${COMPOSE_PROJECT}" \
            --filter "label=com.docker.compose.service=${COMPOSE_SERVICE}" \
            --filter "status=running" -q | wc -l
}

scale() {
  local target="$1"
  log "scaling ${COMPOSE_PROJECT}/${COMPOSE_SERVICE} → $target replicas"
  # --no-recreate : ne pas restart les containers existants
  # --no-build    : skip rebuild image (already deployed)
  # --no-deps     : ne touche pas aux dépendances (redis-cache, etc.)
  if [ ! -f "$COMPOSE_FILE" ]; then
    log "ERROR: compose file not found at $COMPOSE_FILE"
    return 1
  fi
  docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" \
    up -d --scale "${COMPOSE_SERVICE}=${target}" --no-recreate --no-build --no-deps \
    "${COMPOSE_SERVICE}" 2>&1 | tail -3
}

# ===== MAIN =====
main() {
  if ! command -v docker >/dev/null; then
    log "ERROR: docker not found"
    exit 1
  fi

  local replicas; replicas=$(get_replicas)
  if [ -z "$replicas" ] || [ "$replicas" = "0" ]; then
    log "ERROR: no running backend containers found for ${COMPOSE_PROJECT}/${COMPOSE_SERVICE}"
    exit 1
  fi

  local cpu_short cpu_long
  cpu_short=$(prom_avg_cpu "$SCALE_UP_WINDOW")
  cpu_long=$(prom_avg_cpu "$SCALE_DOWN_WINDOW")

  log "replicas=$replicas cpu_${SCALE_UP_WINDOW}=${cpu_short}% cpu_${SCALE_DOWN_WINDOW}=${cpu_long}%"

  # SCALE UP : CPU > 80% sustained $SCALE_UP_WINDOW
  case "$cpu_short" in
    NaN|ERR|"") log "scale-up branch: ${SCALE_UP_WINDOW} data unavailable, skipping" ;;
    *)
      local cpu_short_int
      cpu_short_int=$(printf '%.0f' "$cpu_short" 2>/dev/null) || cpu_short_int=0
      if [ "$cpu_short_int" -gt $SCALE_UP_THRESHOLD ] && [ "$replicas" -lt $MAX_REPLICAS ]; then
        if in_cooldown scale-up; then
          log "scale-up needed (cpu=${cpu_short}%) but in cooldown"
          exit 0
        fi
        scale $((replicas + 1))
        set_cooldown scale-up
        exit 0
      fi
      ;;
  esac

  # SCALE DOWN : CPU < 30% sustained $SCALE_DOWN_WINDOW
  case "$cpu_long" in
    NaN|ERR|"") log "scale-down branch: ${SCALE_DOWN_WINDOW} data unavailable, skipping" ;;
    *)
      local cpu_long_int
      cpu_long_int=$(printf '%.0f' "$cpu_long" 2>/dev/null) || cpu_long_int=999
      if [ "$cpu_long_int" -lt $SCALE_DOWN_THRESHOLD ] && [ "$replicas" -gt $MIN_REPLICAS ]; then
        if in_cooldown scale-down; then
          log "scale-down possible (cpu=${cpu_long}%) but in cooldown"
          exit 0
        fi
        scale $((replicas - 1))
        set_cooldown scale-down
        exit 0
      fi
      ;;
  esac

  log "no action (within bounds)"
}

main "$@"
