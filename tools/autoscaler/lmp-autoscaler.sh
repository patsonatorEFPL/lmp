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
SERVICE_NAME="${SERVICE_NAME:-lmp-test-lmptestback-6xvske}"
PROM_URL="${PROM_URL:-http://localhost:9090}"
PROM_USER="${PROM_USER:-prometheus}"
PROM_PASS="${PROM_PASS:-LDJLIrS6vLtYNQVVvkFOe244GSl26916}"
MIN_REPLICAS=1
MAX_REPLICAS=4
SCALE_UP_THRESHOLD=80    # CPU%
SCALE_DOWN_THRESHOLD=30  # CPU% (50 pts hysteresis vs 80)
SCALE_UP_WINDOW="30s"
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
prom_avg_cpu() {
  local window="$1"
  local query="avg(rate(container_cpu_usage_seconds_total{container_label_com_docker_swarm_service_name=\"$SERVICE_NAME\"}[$window])) * 100"
  local resp value
  resp=$(curl -sS --max-time 10 -u "$PROM_USER:$PROM_PASS" \
    --data-urlencode "query=$query" "$PROM_URL/api/v1/query") || { echo "ERR"; return 1; }
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

  local cpu_30s cpu_30m
  cpu_30s=$(prom_avg_cpu "$SCALE_UP_WINDOW")
  cpu_30m=$(prom_avg_cpu "$SCALE_DOWN_WINDOW")

  log "replicas=$replicas cpu_30s=${cpu_30s}% cpu_30m=${cpu_30m}%"

  # Skip if Prometheus query failed
  case "$cpu_30s" in NaN|ERR|"") log "skip: prometheus 30s data unavailable"; exit 0 ;; esac

  local cpu_30s_int
  cpu_30s_int=$(printf '%.0f' "$cpu_30s" 2>/dev/null) || cpu_30s_int=0

  # SCALE UP : CPU > 80% sustained 30s
  if [ "$cpu_30s_int" -gt $SCALE_UP_THRESHOLD ] && [ "$replicas" -lt $MAX_REPLICAS ]; then
    if in_cooldown scale-up; then
      log "scale-up needed (cpu_30s=${cpu_30s}%) but in cooldown"
      exit 0
    fi
    scale $((replicas + 1))
    set_cooldown scale-up
    exit 0
  fi

  # SCALE DOWN : CPU < 30% sustained 30 min
  case "$cpu_30m" in NaN|ERR|"") log "skip: prometheus 30m data unavailable for scale-down"; exit 0 ;; esac
  local cpu_30m_int
  cpu_30m_int=$(printf '%.0f' "$cpu_30m" 2>/dev/null) || cpu_30m_int=999

  if [ "$cpu_30m_int" -lt $SCALE_DOWN_THRESHOLD ] && [ "$replicas" -gt $MIN_REPLICAS ]; then
    if in_cooldown scale-down; then
      log "scale-down possible (cpu_30m=${cpu_30m}%) but in cooldown"
      exit 0
    fi
    scale $((replicas - 1))
    set_cooldown scale-down
    exit 0
  fi

  log "no action (within bounds)"
}

main "$@"
