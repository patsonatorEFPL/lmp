# LMP Autoscaler — Docker Swarm + Prometheus

Scale automatique du service backend selon CPU monitoré via Prometheus/cAdvisor.

## Installation Oracle host

```bash
# 1. Copy script
sudo mkdir -p /opt/lmp-autoscaler
sudo cp lmp-autoscaler.sh /opt/lmp-autoscaler/
sudo chmod +x /opt/lmp-autoscaler/lmp-autoscaler.sh

# 2. Test once
sudo /opt/lmp-autoscaler/lmp-autoscaler.sh

# 3. Cron setup (every 30s = 2 calls/min)
echo '* * * * * root /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1
* * * * * root sleep 30 && /opt/lmp-autoscaler/lmp-autoscaler.sh >> /var/log/lmp-autoscaler.log 2>&1' | sudo tee /etc/cron.d/lmp-autoscaler

# 4. Verify cron loaded
sudo systemctl reload cron
sudo tail -f /var/log/lmp-autoscaler.log
```

## Behaviour

| Condition | Action | Cooldown |
|---|---|---|
| CPU avg(30s) > 80% AND replicas < 4 | scale +1 | 1 min |
| CPU avg(30 min) < 30% AND replicas > 1 | scale -1 | 30 min |

**Hysteresis** : 80% up vs 30% down (50 pts gap) prevents flapping.

**Async scaling pattern** asymmetric : up agressif (1 min), down conservateur (30 min) — perte de capacité = incident, gain = juste $.

**Bounds** : MIN=1 / MAX=4 sur 2 vCPU host (au-delà = thrashing).

## Configuration

Variables d'env (override defaults) :

```bash
SERVICE_NAME=lmp-test-lmptestback-6xvske  # service swarm à scaler
PROM_URL=http://localhost:9090            # prometheus endpoint
PROM_USER=prometheus
PROM_PASS=...                              # auth basic Grafana scrape
```

Pour ajuster les thresholds → édite `SCALE_UP_THRESHOLD` / `SCALE_DOWN_THRESHOLD` directement dans le script.

## Monitoring

```bash
# Live log
sudo tail -f /var/log/lmp-autoscaler.log

# Replicas count over time (last 24h)
journalctl -u cron --since "24h ago" | grep "scaling" | tail -20
```

## Disable

```bash
sudo rm /etc/cron.d/lmp-autoscaler
sudo systemctl reload cron
# Manual reset to 1 replica
sudo docker service scale lmp-test-lmptestback-6xvske=1
```
