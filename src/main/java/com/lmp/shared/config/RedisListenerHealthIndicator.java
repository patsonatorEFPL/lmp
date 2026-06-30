package com.lmp.shared.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Expose l'état du pub/sub Redis sur {@code /actuator/health/redisListener}.
 *
 * <p>Contrepartie obligatoire du démarrage différé ({@link RedisListenerStarter}) :
 * sans cet indicateur, un abandon définitif du démarrage du listener serait
 * invisible (l'app sert le HTTP, mais le SSE realtime et la sync dispatcher
 * cross-pod sont morts). DOWN = listener pas encore démarré ou abandonné.</p>
 *
 * <p>Note : ne fait PAS partie du groupe {@code liveness} (cf. Dockerfile
 * healthcheck) — un listener pas-encore-démarré pendant la fenêtre de boot
 * ne doit pas déclencher un restart Docker.</p>
 */
@Component("redisListener")
public class RedisListenerHealthIndicator implements HealthIndicator {

    private final RedisListenerStarter starter;

    public RedisListenerHealthIndicator(RedisListenerStarter starter) {
        this.starter = starter;
    }

    @Override
    public Health health() {
        if (starter.isStarted()) {
            return Health.up()
                    .withDetail("attempts", starter.attempts())
                    .build();
        }
        return Health.down()
                .withDetail("attempts", starter.attempts())
                .withDetail("gaveUp", starter.hasGivenUp())
                .withDetail("reason", starter.hasGivenUp()
                        ? "pub/sub listener gave up starting — realtime disabled until restart"
                        : "pub/sub listener not started yet")
                .build();
    }
}
