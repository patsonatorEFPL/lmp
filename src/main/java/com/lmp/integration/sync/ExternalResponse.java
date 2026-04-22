package com.lmp.integration.sync;

import java.util.Map;

/**
 * Réponse d'une opération CRUD sur le système externe.
 * Immutable — utilisé pour communiquer le résultat au {@link service.SyncOutboundService}.
 */
public record ExternalResponse(
        boolean success,
        String externalId,
        Map<String, Object> data,
        String errorMessage,
        int httpStatus
) {

    public static ExternalResponse success(String externalId, Map<String, Object> data) {
        return new ExternalResponse(true, externalId, data, null, 200);
    }

    public static ExternalResponse created(String externalId, Map<String, Object> data) {
        return new ExternalResponse(true, externalId, data, null, 201);
    }

    public static ExternalResponse failure(String errorMessage, int httpStatus) {
        return new ExternalResponse(false, null, null, errorMessage, httpStatus);
    }

    public static ExternalResponse deleted(String externalId) {
        return new ExternalResponse(true, externalId, null, null, 200);
    }

    public static ExternalResponse unavailable() {
        return new ExternalResponse(false, null, null, "External system unavailable", 503);
    }
}
