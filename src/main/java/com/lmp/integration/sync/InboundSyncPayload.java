package com.lmp.integration.sync;

import java.util.Map;

/**
 * Payload reçu d'un webhook du système externe.
 * Structure générique — pas de référence à un ERP.
 */
public record InboundSyncPayload(
        String entityType,
        String entityId,
        String event,
        Map<String, Object> data
) {}
