package com.lmp.shared.web.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Holder du cache stats admin — séparé de {@link AdminRestController} pour
 * éviter que les invalidations event-driven traversent le proxy AOP de la
 * classe @PreAuthorize("hasRole('ADMIN')") (cf. Bug #7 iter41/iter42).
 *
 * <p>Pattern : {@code AdminStatsCacheInvalidator} appelle
 * {@link #invalidate()} sans risquer un AuthorizationDeniedException quand
 * l'event source est un context anonymous (registration utilisateur).
 * {@link AdminRestController} lit + écrit via {@link #get()} et
 * {@link #put(Map)}.</p>
 */
@Component
public class StatsCacheHolder {

    public static final Duration TTL = Duration.ofSeconds(30);

    private record Entry(Map<String, Object> data, Instant expiresAt) {}

    private volatile Entry current;

    public Map<String, Object> get() {
        Entry c = current;
        if (c == null || Instant.now().isAfter(c.expiresAt())) {
            return null;
        }
        return c.data();
    }

    public void put(Map<String, Object> data) {
        current = new Entry(data, Instant.now().plus(TTL));
    }

    public void invalidate() {
        current = null;
    }
}
