package com.lmp.shared.web.api;

import com.lmp.integration.event.LmpBusinessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Iter41 fix Bug #7 (extracted depuis AdminRestController iter37) — listener
 * d'invalidation cache stats placé hors classe @PreAuthorize.
 *
 * <p>Avant : @EventListener {@code invalidateStatsCacheOnBusinessEvent} défini
 * dans {@link AdminRestController} qui a {@code @PreAuthorize("hasRole('ADMIN')")}
 * class-level. Quand event fired par anonymous context (ex: registration user),
 * Spring AOP intercepte ET applique le PreAuthorize → AuthorizationDeniedException.
 * Cascade : {@code AuthRestController.register} catch (Exception e) → log error
 * + retour 500.</p>
 *
 * <p>Fix : @Component séparé sans @PreAuthorize. Délègue au controller via
 * setter direct sur le champ {@code statsCache}.</p>
 */
@Component
public class AdminStatsCacheInvalidator {

    private final AdminRestController adminRestController;

    public AdminStatsCacheInvalidator(AdminRestController adminRestController) {
        this.adminRestController = adminRestController;
    }

    @EventListener
    public void onBusinessEvent(LmpBusinessEvent event) {
        switch (event.type()) {
            case USER_REGISTERED, USER_VERIFIED, USER_UPDATED, USER_DELETED,
                 ORDER_CREATED, ORDER_CONFIRMED, ORDER_UPDATED, ORDER_CANCELLED, ORDER_DELETED,
                 PAYMENT_RECEIVED, PAYMENT_FAILED, REFUND_PROCESSED,
                 APPOINTMENT_CREATED, APPOINTMENT_CONFIRMED, APPOINTMENT_UPDATED,
                 APPOINTMENT_CANCELLED, APPOINTMENT_DELETED ->
                adminRestController.invalidateStatsCache();
            default -> {
            }
        }
    }
}
