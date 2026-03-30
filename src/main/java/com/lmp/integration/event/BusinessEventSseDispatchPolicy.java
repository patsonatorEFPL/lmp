package com.lmp.integration.event;

import java.util.Map;

import com.lmp.integration.event.LmpBusinessEvent.EventType;

/**
 * Règles de diffusion secondaire (ex. ping dashboard admin) indépendantes du routage utilisateur.
 */
public final class BusinessEventSseDispatchPolicy {

    private BusinessEventSseDispatchPolicy() {
    }

    /**
     * @return {@code true} si un événement {@code admin-dashboard} doit être émis après le flux unifié admin.
     */
    public static boolean includeDashboardPing(EventType type, Map<String, Object> payload) {
        if (payload.containsKey(BusinessEventPayloadKeys.SSE_DASHBOARD_PING)) {
            return Boolean.TRUE.equals(payload.get(BusinessEventPayloadKeys.SSE_DASHBOARD_PING));
        }
        if (type == EventType.INVOICE_GENERATED) {
            return false;
        }
        return true;
    }
}
