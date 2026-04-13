package com.lmp.shared.geo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.lmp.shared.web.InetRoutability;

/**
 * Détection VPN/proxy via <a href="https://getipintel.net">GetIPIntel</a>.
 *
 * <p>Retourne un score probabiliste entre 0 et 1 indiquant la probabilité
 * que l'IP soit un proxy/VPN. Valeurs &gt; 0.95 → très probable.
 *
 * <p>Limites plan gratuit : 500 req/jour, 15 req/min.
 * Nécessite un email de contact valide ({@code getipintel.contact-email}).
 */
@Service
public class GetIPIntelService {

    private static final Logger logger = LoggerFactory.getLogger(GetIPIntelService.class);

    private static final String API_URL = "http://check.getipintel.net/check.php?ip=%s&contact=%s&format=json&flags=b";
    private static final long CACHE_TTL_SECONDS = 3600;

    private final RestTemplate restTemplate;
    private final String contactEmail;
    private final ApiHealthRecorder healthRecorder;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public GetIPIntelService(@Value("${getipintel.contact-email:}") String contactEmail,
                             ApiHealthRecorder healthRecorder) {
        this.contactEmail = (contactEmail != null) ? contactEmail.trim() : "";
        this.healthRecorder = healthRecorder;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();

        if (this.contactEmail.isBlank()) {
            logger.warn("[VPN-Detection] GetIPIntel : aucun email de contact configuré. "
                    + "Configurez 'getipintel.contact-email' pour activer ce service.");
        } else {
            logger.info("[VPN-Detection] GetIPIntel : activé avec contact={}", this.contactEmail);
        }
    }

    /**
     * Retourne le score VPN/proxy pour une IP (0 = clean, 1 = VPN/proxy certain).
     *
     * @return score entre 0 et 1, ou vide si le service est indisponible / non configuré.
     */
    public Optional<Double> lookupVpnScore(String ip) {
        if (contactEmail.isBlank()) {
            logger.debug("[FRAUD-DEBUG] GetIPIntel skipped: no contact email configured");
            return Optional.empty();
        }
        if (ip == null || ip.isBlank() || InetRoutability.isPrivateOrNonRoutable(ip)) {
            logger.debug("[FRAUD-DEBUG] GetIPIntel skipped: IP is null/private ({})", ip);
            return Optional.empty();
        }

        CachedResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            logger.debug("[FRAUD-DEBUG] GetIPIntel cache hit for {} → score={}", ip, cached.value.orElse(null));
            return cached.value;
        }

        long t0 = System.currentTimeMillis();
        try {
            String url = String.format(API_URL, ip, contactEmail);
            logger.debug("[FRAUD-DEBUG] GetIPIntel → calling for IP {}", ip);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            long latency = System.currentTimeMillis() - t0;

            if (response != null) {
                Object resultObj = response.get("result");
                if (resultObj != null) {
                    double score = Double.parseDouble(resultObj.toString());
                    if (score >= 0) {
                        healthRecorder.record("GetIPIntel", latency, true, null);
                        Optional<Double> opt = Optional.of(score);
                        cache.put(ip, new CachedResult(opt));
                        logger.debug("[FRAUD-DEBUG] GetIPIntel resolved {} → score={}", ip, score);
                        return opt;
                    } else {
                        // Negative values are error codes
                        healthRecorder.record("GetIPIntel", latency, false, "error code: " + score);
                        logger.warn("[FRAUD-DEBUG] GetIPIntel error code for {}: {}", ip, score);
                    }
                }
                Object statusObj = response.get("status");
                logger.debug("[FRAUD-DEBUG] GetIPIntel raw response for {}: status={}, result={}",
                        ip, statusObj, resultObj);
            }
        } catch (Exception e) {
            healthRecorder.record("GetIPIntel", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.warn("[FRAUD-DEBUG] GetIPIntel unavailable for {}: {}", ip, e.getMessage());
        }

        cache.put(ip, new CachedResult(Optional.empty()));
        return Optional.empty();
    }

    // -------------------------------------------------------------------------

    private static final class CachedResult {
        final Optional<Double> value;
        final Instant expiresAt;

        CachedResult(Optional<Double> value) {
            this.value = value;
            this.expiresAt = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
