package com.lmp.integration.sync.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.ExternalResponse;
import com.lmp.integration.sync.ExternalSystemClient;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final ObjectMapper objectMapper;
    private final boolean autoSubmit;

    public RestExternalClient(SyncProperties syncProperties, EntityTypeMapping entityTypeMapping,
                              ObjectMapper objectMapper) {
        this.entityTypeMapping = entityTypeMapping;
        this.objectMapper = objectMapper;
        this.autoSubmit = syncProperties.getExternal().isAutoSubmit();
        this.restClient = RestClient.builder()
                .baseUrl(syncProperties.getExternal().getBaseUrl())
                .defaultHeader("Authorization",
                        "token " + syncProperties.getExternal().getApiKey()
                                + ":" + syncProperties.getExternal().getApiSecret())
                .requestInitializer(request -> {
                    // Propagation du correlation ID pour le tracing distribué
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-Id", correlationId);
                    }
                })
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
            // Idempotence : check-before-create via lmp_idempotency_key
            String idempotencyKey = data != null ? (String) data.get("lmp_idempotency_key") : null;
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                ExternalResponse existing = findByIdempotencyKey(type, docType, idempotencyKey);
                if (existing != null) {
                    return existing;
                }
            }

            // Injecter le doctype dans le payload (requis par external CRM.client.insert)
            Map<String, Object> doc = new LinkedHashMap<>(data);
            doc.put("doctype", docType);

            Map<String, Object> response = postexternal CRMClientInsert(doc);

            String externalId = extractIdFromMethodResponse(response);
            log.info("✅ [SYNC] Created {} → externalId={}", type, externalId);

            // Auto-submit pour les documents soumissibles (Draft → Submitted)
            if (autoSubmit && SUBMITTABLE_TYPES.contains(type) && externalId != null) {
                submitDocument(docType, externalId);
            }

            return ExternalResponse.created(externalId, response);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("❌ [SYNC] Failed to create {}: {}", type, errorMsg);
            return ExternalResponse.failure(errorMsg, extractHttpStatus(e));
        }
    }

    /**
     * Vérifie si un document avec la même clé d'idempotence existe déjà.
     * Évite les doublons lors des retries après timeout réseau.
     *
     * @return ExternalResponse si trouvé, null sinon
     */
    @SuppressWarnings("unchecked")
    private ExternalResponse findByIdempotencyKey(SyncEntityType type, String docType, String idempotencyKey) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri -> uri
                            .path("/api/resource/{docType}")
                            .queryParam("filters", "[[\"lmp_idempotency_key\",\"=\",\"" + idempotencyKey + "\"]]")
                            .queryParam("fields", "[\"name\"]")
                            .queryParam("limit_page_length", 1)
                            .build(docType))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("data");
                if (results != null && !results.isEmpty()) {
                    String existingId = results.get(0).get("name").toString();
                    log.info("🔁 [SYNC IDEMPOTENT] {} already exists with key {} → reusing {}",
                            type, idempotencyKey, existingId);
                    // Fetch full document for the response data
                    ExternalResponse fullDoc = getEntity(type, existingId);
                    return ExternalResponse.created(existingId, fullDoc.data());
                }
            }
        } catch (Exception e) {
            // Si la vérification échoue (ex: custom field pas encore créé),
            // on continue avec le POST normal — mieux vaut risquer un doublon
            // détectable que de bloquer le sync.
            log.debug("⚠️ [SYNC IDEMPOTENT] Check failed for {} (key={}): {} — proceeding with create",
                    type, idempotencyKey, e.getMessage());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExternalResponse updateEntity(SyncEntityType type, String externalId, Map<String, Object> data) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            // external CRM.client.save : injecter doctype + name pour identifier le document
            Map<String, Object> doc = new LinkedHashMap<>(data);
            doc.put("doctype", docType);
            doc.put("name", externalId);

            Map<String, Object> response = postexternal CRMClientSave(doc);

            log.info("✅ [SYNC] Updated {} {}", type, externalId);
            return ExternalResponse.success(externalId, response);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("❌ [SYNC] Failed to update {} {}: {}", type, externalId, errorMsg);
            return ExternalResponse.failure(errorMsg, extractHttpStatus(e));
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
            SyncEntityType.PAYMENT,
            SyncEntityType.QUOTATION
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
     * Utilise external CRM.client.submit pour garantir le calcul correct des totaux.
     */
    private void submitDocument(String docType, String externalId) {
        try {
            // Récupérer le document complet pour éviter TimestampMismatchError
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = restClient.get()
                    .uri("/api/resource/{docType}/{name}", docType, externalId)
                    .retrieve()
                    .body(Map.class);
            if (doc != null && doc.containsKey("data")) {
                doc = (Map<String, Object>) doc.get("data");
            }
            if (doc == null) {
                log.warn("⚠️ [SYNC] Could not fetch {} '{}' for submit — document remains as Draft", docType, externalId);
                return;
            }
            doc.put("docstatus", 1);
            postexternal CRMClientMethod("external CRM.client.submit", "doc", doc);
            log.info("📋 [SYNC] Submitted {} '{}'", docType, externalId);
        } catch (Exception e) {
            log.warn("⚠️ [SYNC] Failed to submit {} '{}': {} — document remains as Draft",
                    docType, externalId, extractErrorMessage(e));
        }
    }

    /**
     * Annule un document soumis (docstatus = 1 → 2) avant suppression.
     * Utilise external CRM.client.cancel pour respecter les hooks external ERP.
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
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add("doctype", docType);
                    formData.add("name", externalId);
                    restClient.post()
                            .uri("/api/method/external CRM.client.cancel")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formData)
                            .retrieve()
                            .toBodilessEntity();
                    log.info("↩️ [SYNC] Cancelled submitted {} '{}' before deletion", docType, externalId);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [SYNC] Could not check/cancel {} '{}': {} — proceeding with delete",
                    docType, externalId, extractErrorMessage(e));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.math.BigDecimal fetchAggregatedTotal(SyncEntityType type, String sumField,
                                                      String dateField, String startDate, String endDate) {
        String docType = entityTypeMapping.toExternalDocType(type);
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri -> uri
                            .path("/api/resource/{docType}")
                            .queryParam("filters", "[[\"docstatus\",\"=\",1]," +
                                    "[\"" + dateField + "\",\">=\",\"" + startDate + "\"]," +
                                    "[\"" + dateField + "\",\"<\",\"" + endDate + "\"]]")
                            .queryParam("fields", "[\"sum(" + sumField + ") as total\"]")
                            .queryParam("limit_page_length", 1)
                            .build(docType))
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (data != null && !data.isEmpty()) {
                    Object total = data.get(0).get("total");
                    if (total != null) {
                        return new java.math.BigDecimal(total.toString());
                    }
                }
            }
            return java.math.BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("⚠️ [SYNC] Failed to fetch aggregated {} for {}: {}", sumField, type, e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExternalResponse callMethod(String method, Map<String, Object> args) {
        try {
            // Envoyer chaque argument comme champ form-data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            if (args != null) {
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof String s) {
                        formData.add(entry.getKey(), s);
                    } else {
                        formData.add(entry.getKey(), toJson(val));
                    }
                }
            }

            Map<String, Object> response = restClient.post()
                    .uri("/api/method/{method}", method)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);

            String externalId = extractIdFromMethodResponse(response);
            log.info("✅ [SYNC] Called method {} → result={}", method, externalId);
            return ExternalResponse.success(externalId, response);
        } catch (Exception e) {
            String errorMsg = extractErrorMessage(e);
            log.error("❌ [SYNC] Failed to call method {}: {}", method, errorMsg);
            return ExternalResponse.failure(errorMsg, extractHttpStatus(e));
        }
    }

    // ==================== external CRM Client API helpers ====================

    /**
     * Crée un document via {@code external CRM.client.insert} (form-data).
     * Cette méthode exécute les hooks de validation et calcule les totaux
     * automatiquement — contourne le bug base_grand_total de external ERP v17-dev.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postexternal CRMClientInsert(Map<String, Object> doc) {
        return postexternal CRMClientMethod("external CRM.client.insert", "doc", doc);
    }

    /**
     * Met à jour un document via {@code external CRM.client.save} (form-data).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postexternal CRMClientSave(Map<String, Object> doc) {
        return postexternal CRMClientMethod("external CRM.client.save", "doc", doc);
    }

    /**
     * Appelle une méthode external CRM avec un paramètre JSON en form-data.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> postexternal CRMClientMethod(String method, String paramName, Object paramValue) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(paramName, toJson(paramValue));

        return restClient.post()
                .uri("/api/method/{method}", method)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);
    }

    // ==================== Response extraction ====================

    /**
     * Extrait l'ID depuis une réponse {@code /api/method/} (format: {@code {"message": {"name": "..."}}} ).
     */
    @SuppressWarnings("unchecked")
    private String extractIdFromMethodResponse(Map<String, Object> response) {
        if (response == null) return null;
        // /api/method/ retourne {"message": {...}}
        Object message = response.get("message");
        if (message instanceof Map<?, ?> msgMap) {
            Object name = msgMap.get("name");
            return name != null ? name.toString() : null;
        }
        // Fallback : /api/resource/ retourne {"data": {...}}
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object name = dataMap.get("name");
            return name != null ? name.toString() : null;
        }
        return null;
    }

    // ==================== Error handling ====================

    /**
     * Extrait un message d'erreur lisible depuis une exception REST.
     * Pour les {@link RestClientResponseException}, parse le corps JSON de la réponse
     * pour extraire le message external ERP (champ {@code _server_messages}).
     */
    private String extractErrorMessage(Exception e) {
        if (e instanceof RestClientResponseException restEx) {
            String body = restEx.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errMap = objectMapper.readValue(body, Map.class);
                    // external ERP renvoie _server_messages comme JSON-encoded string array
                    Object serverMessages = errMap.get("_server_messages");
                    if (serverMessages instanceof String sMsg) {
                        return "external ERP: " + sMsg.substring(0, Math.min(sMsg.length(), 500));
                    }
                    Object excType = errMap.get("exc_type");
                    Object exception = errMap.get("exception");
                    if (exception != null) {
                        return "external ERP " + excType + ": " + exception.toString()
                                .substring(0, Math.min(exception.toString().length(), 300));
                    }
                } catch (Exception ignored) {
                    // Fallback to raw body
                    return body.substring(0, Math.min(body.length(), 500));
                }
            }
            return restEx.getStatusCode() + " " + restEx.getStatusText();
        }
        return e.getMessage();
    }

    /**
     * Extrait le code HTTP depuis une exception REST, ou 500 par défaut.
     */
    private int extractHttpStatus(Exception e) {
        if (e instanceof RestClientResponseException restEx) {
            return restEx.getStatusCode().value();
        }
        return 500;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ [SYNC] Failed to serialize to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
