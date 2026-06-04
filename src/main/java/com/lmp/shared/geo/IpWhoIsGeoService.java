package com.lmp.shared.geo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.lmp.shared.web.InetRoutability;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Géolocalisation IP via <a href="https://ipwho.is">ipwho.is</a>.
 *
 * <h3>Caractéristiques</h3>
 * <ul>
 *   <li>Gratuit, sans clé API</li>
 *   <li>HTTPS natif</li>
 *   <li>1 req/sec max (fair use) — usage commercial non autorisé sur le plan gratuit</li>
 *   <li><b>ATTENTION :</b> Le plan gratuit retourne le pays mais <b>PAS la devise</b>.
 *       La devise (currency) n'est disponible que sur le plan Premium payant.</li>
 * </ul>
 *
 * <p>Les résultats sont mis en cache en mémoire pendant {@value CACHE_TTL_SECONDS} secondes
 * pour éviter les appels répétés pour la même IP.
 */
@Service
public class IpWhoIsGeoService {

    private static final Logger logger = LoggerFactory.getLogger(IpWhoIsGeoService.class);

    private static final String API_URL = "https://ipwho.is/%s";
    private static final long CACHE_TTL_SECONDS = 3600;

    private final RestTemplate restTemplate;
    private final ApiHealthRecorder healthRecorder;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public IpWhoIsGeoService(ApiHealthRecorder healthRecorder) {
        this.healthRecorder = healthRecorder;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Résout le pays et la devise pour une adresse IP.
     *
     * @return {@link GeoResolution} ou vide si l'API est indisponible / IP privée.
     */
    public Optional<GeoResolution> lookup(String ip) {
        if (ip == null || ip.isBlank() || InetRoutability.isPrivateOrNonRoutable(ip)) {
            return Optional.empty();
        }

        CachedResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        long t0 = System.currentTimeMillis();
        try {
            String url = String.format(API_URL, ip);
            IpWhoIsResponse response = restTemplate.getForObject(url, IpWhoIsResponse.class);
            long latency = System.currentTimeMillis() - t0;

            if (response != null && Boolean.TRUE.equals(response.success)
                    && response.countryCode != null && !response.countryCode.isBlank()) {

                healthRecorder.record("ipwho.is", latency, true, null);
                String currency = (response.currency != null && response.currency.code != null
                        && !response.currency.code.isBlank())
                        ? response.currency.code.trim().toUpperCase()
                        : null;

                GeoResolution result = new GeoResolution(
                        response.countryCode.trim().toUpperCase(), currency);
                Optional<GeoResolution> opt = Optional.of(result);
                cache.put(ip, new CachedResult(opt));
                logger.debug("[GeoIP] ipwho.is resolved {} -> country={} currency={}",
                        ip, result.countryCode(), result.currencyCode());
                return opt;
            }
        } catch (Exception e) {
            healthRecorder.record("ipwho.is", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.debug("[GeoIP] ipwho.is unavailable for {}: {}", ip, e.getMessage());
        }

        cache.put(ip, new CachedResult(Optional.empty()));
        return Optional.empty();
    }

    // -------------------------------------------------------------------------

    private static final class CachedResult {
        final Optional<GeoResolution> value;
        final Instant expiresAt;

        CachedResult(Optional<GeoResolution> value) {
            this.value = value;
            this.expiresAt = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IpWhoIsResponse {
        @JsonProperty("success")
        Boolean success;
        @JsonProperty("country_code")
        String countryCode;
        @JsonProperty("currency")
        CurrencyField currency;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CurrencyField {
        @JsonProperty("code")
        String code;
    }
}
