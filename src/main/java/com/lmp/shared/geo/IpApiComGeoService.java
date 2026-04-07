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

import com.lmp.shared.web.InetRoutability;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Géolocalisation IP via <a href="https://ip-api.com">ip-api.com</a>.
 *
 * <h3>Limites et niveaux de service</h3>
 * <table>
 *   <tr><th>Plan</th><th>Protocole</th><th>Limite</th><th>Clé</th></tr>
 *   <tr><td>Gratuit</td><td><strong>HTTP seulement</strong></td><td>45 req/min</td><td>Non</td></tr>
 *   <tr><td>Pro</td><td>HTTPS</td><td>15 000 req/min</td><td>Oui ({@code ipapi.com.key})</td></tr>
 * </table>
 *
 * <p>Configurer {@code ipapi.com.key} dans les propriétés pour activer le plan Pro (HTTPS).
 * Sans clé, le service utilise le plan gratuit HTTP — acceptable en fallback derrière ipwho.is
 * (HTTPS), mais à ne pas utiliser en source principale en production.
 *
 * <p>Retourne {@link GeoResolution} avec le pays <strong>et</strong> la devise
 * ({@code "currency"} directement dans la réponse JSON).
 *
 * <p>Les résultats sont mis en cache en mémoire pendant {@value CACHE_TTL_SECONDS} secondes.
 */
@Service
public class IpApiComGeoService {

    private static final Logger logger = LoggerFactory.getLogger(IpApiComGeoService.class);

    private static final long CACHE_TTL_SECONDS = 3600;

    /**
     * Plan gratuit — HTTP uniquement.
     * Champs demandés : status, countryCode, currency.
     */
    private static final String FREE_URL = "http://ip-api.com/json/%s?fields=status,countryCode,currency";

    /**
     * Plan Pro — HTTPS.
     * Champs demandés : status, countryCode, currency.
     */
    private static final String PRO_URL = "https://pro.ip-api.com/json/%s?key=%s&fields=status,countryCode,currency";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public IpApiComGeoService(@Value("${ipapi.com.key:}") String apiKey) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();

        if (this.apiKey.isBlank()) {
            logger.info("[GeoIP] ip-api.com : plan gratuit (HTTP). "
                    + "Configurez 'ipapi.com.key' pour activer le plan Pro (HTTPS).");
        } else {
            logger.info("[GeoIP] ip-api.com : plan Pro (HTTPS) activé.");
        }
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

        try {
            String url = apiKey.isBlank()
                    ? String.format(FREE_URL, ip)
                    : String.format(PRO_URL, ip, apiKey);

            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

            if (response != null && "success".equalsIgnoreCase(response.status)
                    && response.countryCode != null && !response.countryCode.isBlank()) {

                String currency = (response.currency != null && !response.currency.isBlank())
                        ? response.currency.trim().toUpperCase()
                        : null;
                GeoResolution result = new GeoResolution(
                        response.countryCode.trim().toUpperCase(), currency);
                Optional<GeoResolution> opt = Optional.of(result);
                cache.put(ip, new CachedResult(opt));
                logger.debug("[GeoIP] ip-api.com resolved {} → country={} currency={}",
                        ip, result.countryCode(), result.currencyCode());
                return opt;
            }
        } catch (Exception e) {
            logger.debug("[GeoIP] ip-api.com unavailable for {}: {}", ip, e.getMessage());
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
    static class IpApiResponse {
        @JsonProperty("status")
        String status;
        @JsonProperty("countryCode")
        String countryCode;
        @JsonProperty("currency")
        String currency;
    }
}
