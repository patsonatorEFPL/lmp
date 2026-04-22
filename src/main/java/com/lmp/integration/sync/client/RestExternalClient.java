package com.lmp.integration.sync.client;

import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client REST générique — appelle un système externe via son API REST.
 * <p>
 * Configuration via {@code lmp.sync.external.*} properties.
 * Actif uniquement quand {@code lmp.sync.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "lmp.sync.enabled", havingValue = "true")
public class RestExternalClient implements ExternalSystemClient {

    private static final Logger log = LoggerFactory.getLogger(RestExternalClient.class);

    private final RestClient restClient;
    private final EntityTypeMapping entityTypeMapping;
    private final boolean autoSubmit;

    public RestExternalClient(SyncProperties syncProperties, EntityTypeMapping entityTypeMapping) {
        this.entityTypeMapping = entityTypeMapping;
        this.autoSubmit = syncProperties.getExternal().isAutoSubmit();
        this.restClient = RestClient.builder()
                .baseUrl(syncProperties.getExternal().getBaseUrl())
                .defaultHeader("Authorization",
                        "token " + syncProperties.getExternal().getApiKey()
                                + ":" + syncProperties.getExternal().getApiSecret())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            restClient.get()
                    .uri("/api/method/external CRM.auth.get_logged_user")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("🔴 [SYNC] External system unavailable: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExternalResponse createEntity(SyncEntityType type, Map<String, Object> data) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/api/resource/{docType}", docType)
                    .body(data)
                    .retrieve()
                    .body(Map.class);

            String externalId = extractId(response);
            log.info("✅ [SYNC] Created {} → externalId={}", type, externalId);

            // Auto-submit pour les documents soumissibles (Draft → Submitted)
            if (autoSubmit && SUBMITTABLE_TYPES.contains(type) && externalId != null) {
                submitDocument(docType, externalId);
            }

            return ExternalResponse.created(externalId, response);
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to create {}: {}", type, e.getMessage());
            return ExternalResponse.failure(e.getMessage(), 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExternalResponse updateEntity(SyncEntityType type, String externalId, Map<String, Object> data) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.put()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .body(data)
                    .retrieve()
                    .body(Map.class);

            log.info("✅ [SYNC] Updated {} {}", type, externalId);
            return ExternalResponse.success(externalId, response);
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to update {} {}: {}", type, externalId, e.getMessage());
            return ExternalResponse.failure(e.getMessage(), 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExternalResponse getEntity(SyncEntityType type, String externalId) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .retrieve()
                    .body(Map.class);

            return ExternalResponse.success(externalId, response);
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to get {} {}: {}", type, externalId, e.getMessage());
            return ExternalResponse.failure(e.getMessage(), 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listEntities(SyncEntityType type, Instant modifiedSince) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri -> uri
                            .path("/api/resource/{docType}")
                            .queryParam("filters", "[[\"modified\",\">=\",\"" + modifiedSince + "\"]]")
                            .queryParam("limit_page_length", 100)
                            .build(docType))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                return (List<Map<String, Object>>) response.get("data");
            }
            return List.of();
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to list {}: {}", type, e.getMessage());
            return List.of();
        }
    }

    /**
     * Types de documents soumissibles (docstatus = 0 → Draft, 1 → Submitted).
     * Ces documents doivent être annulés (docstatus = 2) avant suppression.
     */
    private static final Set<SyncEntityType> SUBMITTABLE_TYPES = Set.of(
            SyncEntityType.SALES_ORDER,
            SyncEntityType.SALES_INVOICE,
            SyncEntityType.PAYMENT
    );

    @Override
    public ExternalResponse deleteEntity(SyncEntityType type, String externalId) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            // Pour les documents soumissibles, annuler avant de supprimer
            if (SUBMITTABLE_TYPES.contains(type)) {
                cancelIfSubmitted(docType, externalId);
            }

            restClient.delete()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("🗑️ [SYNC] Deleted {} '{}'", type, externalId);
            return ExternalResponse.deleted(externalId);
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to delete {} '{}': {}", type, externalId, e.getMessage());
            return ExternalResponse.failure(e.getMessage(), 500);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listLinkedEntityIds(SyncEntityType type, String parentField, String parentId) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri -> uri
                            .path("/api/resource/{docType}")
                            .queryParam("filters", "[[\"" + parentField + "\",\"=\",\"" + parentId + "\"]]")
                            .queryParam("fields", "[\"name\"]")
                            .queryParam("limit_page_length", 0)
                            .build(docType))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                List<String> ids = new ArrayList<>();
                for (Map<String, Object> entry : data) {
                    Object name = entry.get("name");
                    if (name != null) ids.add(name.toString());
                }
                log.debug("🔍 [SYNC] Found {} linked {} for {}={}", ids.size(), type, parentField, parentId);
                return ids;
            }
            return List.of();
        } catch (Exception e) {
            log.error("❌ [SYNC] Failed to list linked {} for {}={}: {}", type, parentField, parentId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Soumet un document Draft (docstatus = 0 → 1) après création.
     * Transforme le document en document comptable officiel.
     */
    private void submitDocument(String docType, String externalId) {
        try {
            restClient.put()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .body(Map.of("docstatus", 1))
                    .retrieve()
                    .toBodilessEntity();
            log.info("📋 [SYNC] Submitted {} '{}'", docType, externalId);
        } catch (Exception e) {
            log.warn("⚠️ [SYNC] Failed to submit {} '{}': {} — document remains as Draft",
                    docType, externalId, e.getMessage());
        }
    }

    /**
     * Annule un document soumis (docstatus = 1 → 2) avant suppression.
     * Si le document est en draft (docstatus = 0), ne fait rien.
     */
    @SuppressWarnings("unchecked")
    private void cancelIfSubmitted(String docType, String externalId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object docstatus = data.get("docstatus");
                int status = (docstatus instanceof Number) ? ((Number) docstatus).intValue() : 0;

                if (status == 1) {
                    // Amend to Cancel via PUT with docstatus = 2
                    restClient.put()
                            .uri("/api/resource/{docType}/{name}", docType, externalId)
                            .body(Map.of("docstatus", 2))
                            .retrieve()
                            .toBodilessEntity();
                    log.info("↩️ [SYNC] Cancelled submitted {} '{}' before deletion", docType, externalId);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [SYNC] Could not check/cancel {} '{}': {} — proceeding with delete",
                    docType, externalId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractId(Map<String, Object> response) {
        if (response == null) return null;
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return (String) dataMap.get("name");
        }
        return null;
    }
}
