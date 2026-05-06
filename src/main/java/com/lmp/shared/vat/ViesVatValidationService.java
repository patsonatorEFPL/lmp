package com.lmp.shared.vat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.lmp.shared.util.VatIdentifierUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Validation TVA via l'API REST VIES (VAT Information Exchange System).
 *
 * <p>Interroge le service officiel de la Commission européenne pour vérifier
 * la validité d'un numéro de TVA intracommunautaire et récupérer le nom/adresse
 * de l'entreprise enregistrée.
 *
 * <p>Cache en mémoire : 1 h pour les résultats normaux, 5 min si VIES indisponible.
 */
@Service
public class ViesVatValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ViesVatValidationService.class);

    private static final String VIES_URL =
            "https://ec.europa.eu/taxation_customs/vies/rest-api/check-vat-number";

    private static final long CACHE_TTL_SECONDS = 3600;
    private static final long CACHE_TTL_UNAVAILABLE_SECONDS = 300;

    private final RestTemplate restTemplate;
    private final ApiHealthRecorder healthRecorder;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Résultat de la validation VIES.
     *
     * @param valid            {@code true} si le numéro est valide selon VIES
     * @param serviceAvailable {@code true} si VIES a répondu correctement
     * @param name             nom de l'entreprise enregistrée (peut être null ou "---")
     * @param address          adresse enregistrée (peut être null ou "---")
     */
    public record ViesResult(boolean valid, boolean serviceAvailable, String name, String address) {}

    public ViesVatValidationService(ApiHealthRecorder healthRecorder) {
        this.healthRecorder = healthRecorder;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
        logger.info("[VIES] Service de validation TVA initialisé.");
    }

    /**
     * Valide un numéro de TVA via VIES.
     *
     * @param rawVatNumber numéro brut (ex: "BE 0123.456.789")
     * @return résultat de validation, ou vide si le format est invalide
     */
    public Optional<ViesResult> validate(String rawVatNumber) {
        if (rawVatNumber == null || rawVatNumber.isBlank()) {
            return Optional.empty();
        }

        String normalized = VatIdentifierUtils.normalize(rawVatNumber);
        if (!VatIdentifierUtils.isPlausibleEuVatFormat(normalized)) {
            logger.debug("[VIES] Format invalide après normalisation : {}", normalized);
            return Optional.empty();
        }

        // Extraire le préfixe pays (2 lettres) et la partie nationale
        String countryCode = normalized.substring(0, 2);
        String vatNumber = normalized.substring(2);

        // Cache lookup
        CachedResult cached = cache.get(normalized);
        if (cached != null && !cached.isExpired()) {
            logger.debug("[VIES] Cache hit pour {} → valid={}", normalized,
                    cached.value.map(ViesResult::valid).orElse(null));
            return cached.value;
        }

        long t0 = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of(
                    "countryCode", countryCode,
                    "vatNumber", vatNumber
            );

            logger.debug("[VIES] Appel API pour countryCode={} vatNumber={}", countryCode, vatNumber);

            ViesResponse response = restTemplate.postForObject(
                    VIES_URL, new HttpEntity<>(body, headers), ViesResponse.class);
            long latency = System.currentTimeMillis() - t0;

            if (response != null) {
                healthRecorder.record("VIES", latency, true, null);
                String name = sanitizeViesField(response.name);
                String address = sanitizeViesField(response.address);

                ViesResult result = new ViesResult(
                        Boolean.TRUE.equals(response.valid),
                        true,
                        name,
                        address
                );

                Optional<ViesResult> opt = Optional.of(result);
                cache.put(normalized, new CachedResult(opt, CACHE_TTL_SECONDS));

                logger.info("[VIES] {} → valid={} name=\"{}\"",
                        normalized, result.valid(), result.name());
                return opt;
            }
        } catch (Exception e) {
            healthRecorder.record("VIES", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.warn("[VIES] Service indisponible pour {} : {}", normalized, e.getMessage());
        }

        // VIES indisponible → résultat spécial avec serviceAvailable=false
        ViesResult unavailable = new ViesResult(false, false, null, null);
        Optional<ViesResult> opt = Optional.of(unavailable);
        cache.put(normalized, new CachedResult(opt, CACHE_TTL_UNAVAILABLE_SECONDS));
        return opt;
    }

    /**
     * Nettoie un champ VIES : retourne null si vide ou "---".
     */
    private String sanitizeViesField(String value) {
        if (value == null || value.isBlank() || "---".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }

    // -------------------------------------------------------------------------

    private static final class CachedResult {
        final Optional<ViesResult> value;
        final Instant expiresAt;

        CachedResult(Optional<ViesResult> value, long ttlSeconds) {
            this.value = value;
            this.expiresAt = Instant.now().plusSeconds(ttlSeconds);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ViesResponse {
        @JsonProperty("valid")
        Boolean valid;
        @JsonProperty("name")
        String name;
        @JsonProperty("address")
        String address;
    }
}
