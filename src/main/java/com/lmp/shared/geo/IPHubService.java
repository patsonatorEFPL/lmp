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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.web.InetRoutability;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Détection VPN/proxy via <a href="https://iphub.info">IPHub</a>.
 *
 * <p>Retourne un flag {@code block} : 0 = résidentiel (safe), 1 = non-résidentiel (VPN/proxy),
 * 2 = non-résidentiel + résidentiel (warning).
 *
 * <p>Plan gratuit Basic : 1000 req/jour. Nécessite une clé API ({@code iphub.api-key}).
 */
@Service
public class IPHubService {

    private static final Logger logger = LoggerFactory.getLogger(IPHubService.class);

    private static final String API_URL = "https://v2.api.iphub.info/ip/%s?v=2.2";
    private static final long CACHE_TTL_SECONDS = 3600;

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public IPHubService(@Value("${iphub.api-key:}") String apiKey) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();

        if (this.apiKey.isBlank()) {
            logger.warn("[VPN-Detection] IPHub : aucune clé API configurée. "
                    + "Configurez 'iphub.api-key' pour activer ce service.");
        } else {
            logger.info("[VPN-Detection] IPHub : activé.");
        }
    }

    /**
     * Résultat de la détection IPHub.
     *
     * @param block       0 = safe, 1 = VPN/proxy, 2 = warning
     * @param countryCode pays de l'IP
     * @param isp         FAI
     * @param proxy       true si proxy détecté (v2.2)
     * @param tor         true si nœud Tor (v2.2)
     * @param hosting     true si IP datacenter/hébergeur (v2.2)
     */
    public record IPHubResult(int block, String countryCode, String isp,
                              boolean proxy, boolean tor, boolean hosting) {}

    /**
     * Retourne le résultat de détection pour une IP.
     *
     * @return {@link IPHubResult} ou vide si le service est indisponible / non configuré.
     */
    public Optional<IPHubResult> lookup(String ip) {
        if (apiKey.isBlank()) {
            logger.debug("[FRAUD-DEBUG] IPHub skipped: no API key configured");
            return Optional.empty();
        }
        if (ip == null || ip.isBlank() || InetRoutability.isPrivateOrNonRoutable(ip)) {
            logger.debug("[FRAUD-DEBUG] IPHub skipped: IP is null/private ({})", ip);
            return Optional.empty();
        }

        CachedResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            logger.debug("[FRAUD-DEBUG] IPHub cache hit for {} → block={}", ip,
                    cached.value.map(r -> String.valueOf(r.block())).orElse("null"));
            return cached.value;
        }

        try {
            String url = String.format(API_URL, ip);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Key", apiKey);

            logger.debug("[FRAUD-DEBUG] IPHub → calling for IP {}", ip);

            ResponseEntity<IPHubResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), IPHubResponse.class);

            IPHubResponse response = responseEntity.getBody();

            if (response != null && response.ip != null) {
                boolean proxy = response.proxyType != null && Boolean.TRUE.equals(response.proxyType.proxy);
                boolean tor = response.proxyType != null && Boolean.TRUE.equals(response.proxyType.tor);
                boolean hosting = response.proxyType != null && Boolean.TRUE.equals(response.proxyType.hosting);
                IPHubResult result = new IPHubResult(
                        response.block != null ? response.block : 0,
                        response.countryCode,
                        response.isp,
                        proxy, tor, hosting);
                Optional<IPHubResult> opt = Optional.of(result);
                cache.put(ip, new CachedResult(opt));
                logger.debug("[FRAUD-DEBUG] IPHub resolved {} → block={} country={} isp={} proxy={} tor={} hosting={}",
                        ip, result.block(), result.countryCode(), result.isp(), proxy, tor, hosting);
                return opt;
            } else {
                logger.debug("[FRAUD-DEBUG] IPHub returned empty body for {}", ip);
            }
        } catch (Exception e) {
            logger.warn("[FRAUD-DEBUG] IPHub unavailable for {}: {}", ip, e.getMessage());
        }

        cache.put(ip, new CachedResult(Optional.empty()));
        return Optional.empty();
    }

    // -------------------------------------------------------------------------

    private static final class CachedResult {
        final Optional<IPHubResult> value;
        final Instant expiresAt;

        CachedResult(Optional<IPHubResult> value) {
            this.value = value;
            this.expiresAt = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IPHubResponse {
        @JsonProperty("ip")
        String ip;
        @JsonProperty("countryCode")
        String countryCode;
        @JsonProperty("isp")
        String isp;
        @JsonProperty("block")
        Integer block;
        @JsonProperty("proxyType")
        ProxyType proxyType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ProxyType {
        @JsonProperty("proxy")
        Boolean proxy;
        @JsonProperty("tor")
        Boolean tor;
        @JsonProperty("hosting")
        Boolean hosting;
        @JsonProperty("relay")
        Boolean relay;
        @JsonProperty("residentialProxy")
        Boolean residentialProxy;
    }
}
