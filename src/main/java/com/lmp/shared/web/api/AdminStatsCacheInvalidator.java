package com.lmp.shared.web.api;

import com.lmp.integration.event.LmpBusinessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Iter42 (fix complémentaire Bug #7) — listener d'invalidation cache stats
 * découplé de AdminRestController.
 *
 * <p>Iter41 avait extrait ce listener de AdminRestController mais conservait
 * une dépendance directe sur lui (appel {@code adminRestController.invalidateStatsCache()}).
 * Comme AdminRestController est annoté {@code @PreAuthorize("hasRole('ADMIN')")}
 * class-level, le proxy AOP intercepte tout appel public — y compris depuis
 * un autre @Component — et déclenche le check de rôle. Quand l'event source
 * est un context anonymous (USER_REGISTERED depuis /api/v1/auth/register),
 * l'appel rebondit en AuthorizationDeniedException → register 500.</p>
 *
 * <p>Fix iter42 : injection de {@link StatsCacheHolder} (composant
 * non-@PreAuthorize) directement. Plus aucune traversée du proxy AOP.</p>
 */
@Component
public class AdminStatsCacheInvalidator {

    private final StatsCacheHolder statsCacheHolder;

    public AdminStatsCacheInvalidator(StatsCacheHolder statsCacheHolder) {
        this.statsCacheHolder = statsCacheHolder;
    }

    @EventListener
    public void onBusinessEvent(LmpBusinessEvent event) {
        switch (event.type()) {
            case USER_REGISTERED, USER_VERIFIED, USER_UPDATED, USER_DELETED,
                 ORDER_CREATED, ORDER_CONFIRMED, ORDER_UPDATED, ORDER_CANCELLED, ORDER_DELETED,
                 PAYMENT_RECEIVED, PAYMENT_FAILED, REFUND_PROCESSED,
                 APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED, APPOINTMENT_UPDATED,
                 APPOINTMENT_CANCELLED, APPOINTMENT_DELETED ->
                statsCacheHolder.invalidate();
            default -> {
            }
        }
    }
}
