package com.lmp.integration.sync.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.integration.sync.InboundSyncPayload;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.integration.sync.service.SyncInboundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Endpoint générique pour recevoir les webhooks du système externe.
 * <p>
 * Validation HMAC-SHA256 obligatoire via header {@code X-Webhook-Signature}.
 * Pas de référence à un ERP spécifique — payload générique.
 */
@RestController
@RequestMapping("/api/v1/webhooks/sync")
public class InboundWebhookController {

    private static final Logger log = LoggerFactory.getLogger(InboundWebhookController.class);

    private final SyncInboundService syncInboundService;
    private final SyncProperties syncProperties;
    private final ObjectMapper objectMapper;

    public InboundWebhookController(SyncInboundService syncInboundService,
                                     SyncProperties syncProperties,
                                     ObjectMapper objectMapper) {
        this.syncInboundService = syncInboundService;
        this.syncProperties = syncProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        // Validate HMAC signature
        if (!validateHmac(rawBody, signature)) {
            log.warn("[WEBHOOK] Invalid HMAC signature from incoming webhook");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
        }

        // Parse payload
        InboundSyncPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, InboundSyncPayload.class);
        } catch (Exception e) {
            log.warn("[WEBHOOK] Failed to parse payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload format"));
        }

        log.info("[WEBHOOK] Received: entityType={}, event={}, entityId={}",
                payload.entityType(), payload.event(), payload.entityId());

        syncInboundService.processInbound(payload);

        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    private boolean validateHmac(String body, String receivedSignature) {
        String secret = syncProperties.getWebhook().getHmacSecret();

        // Si pas de secret configuré, rejeter tout
        if (secret == null || secret.isBlank()) {
            log.warn("[WEBHOOK] No HMAC secret configured — rejecting all webhooks");
            return false;
        }

        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(receivedSignature);
        } catch (Exception e) {
            log.error("[WEBHOOK] HMAC validation error: {}", e.getMessage());
            return false;
        }
    }
}
