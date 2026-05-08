#!/usr/bin/env bash
# ============================================================================
# LMP Backend Autoscaler — Docker Swarm + Prometheus
# ============================================================================
# Run on Oracle host as root via cron (every 30s).
#
# Rules :
#   CPU avg(30s) > 80% AND replicas < MAX → scale +1 (cooldown 60s)
#   CPU avg(30 min) < 30% AND replicas > MIN → scale -1 (cooldown 30 min)
#
# Why these thresholds (industry asymmetric pattern, AWS/Netflix) :
#   80%/30% gap (50 pts) prevents flapping
#   30s up : reactive to traffic burst
#   30 min down : avoids premature scale-down right before next peak
#
# Bounds : MIN=1, MAX=4 — au-delà de 4 replicas sur 2 vCPU host = thrashing CPU.
#
# Crontab :
#   * * * * * /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1
#   * * * * * sleep 30 && /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1
# ============================================================================
set -uo pipefail

# ===== CONFIG =====
SERVICE_NAME="${SERVICE_NAME:-lmp-back-staging-wb2l6c}"
# Prometheus container — autoscaler interroge via docker exec (pas besoin
# d'exposer Prom sur le host, fonctionne même quand IP overlay change).
PROM_CONTAINER="${PROM_CONTAINER:-lmp-menkeps-observability-siwy2b-prometheus-1}"
# Dokploy API — sans update DB Dokploy, son reconcile loop reset les replicas
# à la valeur stockée → annule scale via swarm. Update Dokploy + swarm =
# atomique.
DOKPLOY_URL="${DOKPLOY_URL:-https://dokploy.lmp-services.ca}"
DOKPLOY_API_KEY="${DOKPLOY_API_KEY:-claudeYWyDldFiCtEmOiXTbDGRNqCJvRvELvrfNAVQrowSiOASqpIDIAbprGzHrJxPwaYK}"
DOKPLOY_APP_ID="${DOKPLOY_APP_ID:-XaZn5qdN7QZzT3POl8ow-}"
MIN_REPLICAS=1
MAX_REPLICAS=4
SCALE_UP_THRESHOLD=80    # CPU%
SCALE_DOWN_THRESHOLD=30  # CPU% (50 pts hysteresis vs 80)
SCALE_UP_WINDOW="1m"   # cAdvisor scrape 30s+, besoin de >= 2 échantillons pour rate()
SCALE_DOWN_WINDOW="30m"
COOLDOWN_FILE="/var/run/lmp-autoscaler.cooldown"
SCALE_UP_COOLDOWN_S=60       # 1 min entre 2 scale-ups
SCALE_DOWN_COOLDOWN_S=1800   # 30 min entre 2 scale-downs

# ===== LOG =====
log() { echo "$(date -Iseconds) [autoscaler] $*"; }

# ===== COOLDOWN =====
in_cooldown() {
  local now action; now=$(date +%s); action="$1"
  [ ! -f "$COOLDOWN_FILE" ] && return 1
  local last_action last_ts
  read -r last_action last_ts < "$COOLDOWN_FILE" 2>/dev/null || return 1
  local cooldown=$SCALE_UP_COOLDOWN_S
  [ "$last_action" = "scale-down" ] && cooldown=$SCALE_DOWN_COOLDOWN_S
  local elapsed=$((now - last_ts))
  [ $elapsed -lt $cooldown ]
}

set_cooldown() {
  echo "$1 $(date +%s)" > "$COOLDOWN_FILE"
}

# ===== PROMETHEUS QUERY =====
# Returns avg CPU% (0-200% on 2-vCPU host) of all backend containers over $window.
# Uses container_cpu_usage_seconds_total via cAdvisor (already running per memory).
# Query via docker exec dans le container Prom (pas besoin de port bind sur host).
prom_avg_cpu() {
  local window="$1"
  # cAdvisor publish container name (not docker swarm label) — match les tasks
  # via regex sur le service name dans le container name (Docker swarm pattern :
  # <service>.<replica>.<task_id>).
  local query="avg(rate(container_cpu_usage_seconds_total{name=~\"$SERVICE_NAME.*\"}[$window])) * 100"
  local encoded resp value
  encoded=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$query")
  resp=$(docker exec "$PROM_CONTAINER" wget -qO- --timeout=5 \
    "http://localhost:9090/api/v1/query?query=$encoded" 2>/dev/null) || { echo "ERR"; return 1; }
  value=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); r=d.get('data',{}).get('result',[]); print(r[0]['value'][1] if r else 'NaN')" 2>/dev/null)
  echo "$value"
}

# ===== DOCKER SWARM =====
get_replicas() {
  docker service inspect "$SERVICE_NAME" --format "{{.Spec.Mode.Replicated.Replicas}}" 2>/dev/null
}

scale() {
  local target="$1"
  log "scaling $SERVICE_NAME → $target replicas"
  # Update Dokploy DB d'abord (sinon son reconcile loop reset à la valeur
  # stockée). API trpc accepte applicationId + replicas via application.update.
  curl -sS --max-time 10 -X POST -H "x-api-key: $DOKPLOY_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"json\":{\"applicationId\":\"$DOKPLOY_APP_ID\",\"replicas\":$target}}" \
    "$DOKPLOY_URL/api/trpc/application.update" >/dev/null 2>&1 || \
    log "WARN: Dokploy API update failed — Dokploy may revert replicas count"
  # Update swarm pour effet immédiat (sinon update DB seul = pas d'effet runtime)
  docker service scale "$SERVICE_NAME=$target" --detach 2>&1 | tail -1
}

# ===== MAIN =====
main() {
  if ! command -v docker >/dev/null; then
    log "ERROR: docker not found"
    exit 1
  fi

  local replicas; replicas=$(get_replicas)
  if [ -z "$replicas" ]; then
    log "ERROR: cannot get replicas count for $SERVICE_NAME"
    exit 1
  fi

  local cpu_short cpu_long
  cpu_short=$(prom_avg_cpu "$SCALE_UP_WINDOW")
  cpu_long=$(prom_avg_cpu "$SCALE_DOWN_WINDOW")

  log "replicas=$replicas cpu_${SCALE_UP_WINDOW}=${cpu_short}% cpu_${SCALE_DOWN_WINDOW}=${cpu_long}%"

  # SCALE UP : CPU > 80% sustained $SCALE_UP_WINDOW (skip si data NaN)
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

  # SCALE DOWN : CPU < 30% sustained $SCALE_DOWN_WINDOW (indépendant de cpu_short)
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
