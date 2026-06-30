package com.lmp.shared.geo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.lmp.shared.web.InetRoutability;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Géolocalisation IP via <a href="https://ip-api.com">ip-api.com</a>.
 *
 * <p>Retourne {@link GeoResolution} avec le pays, la devise et le flag proxy/VPN.
 */
@Service
public class IpApiComGeoService {

    private static final Logger logger = LoggerFactory.getLogger(IpApiComGeoService.class);

    private static final long CACHE_TTL_SECONDS = 3600;

    /** Plan gratuit — HTTP uniquement. proxy + hosting pour meilleure détection VPN. */
    private static final String FREE_URL = "http://ip-api.com/json/%s?fields=status,countryCode,currency,proxy,hosting";

    /** Plan Pro — HTTPS. */
    private static final String PRO_URL = "https://pro.ip-api.com/json/%s?key=%s&fields=status,countryCode,currency,proxy,hosting";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final ApiHealthRecorder healthRecorder;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    public IpApiComGeoService(@Value("${ipapi.com.key:}") String apiKey, ApiHealthRecorder healthRecorder) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.healthRecorder = healthRecorder;
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
     * Résout le pays, la devise et le flag proxy pour une adresse IP.
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
            String url = apiKey.isBlank()
                    ? String.format(FREE_URL, ip)
                    : String.format(PRO_URL, ip, apiKey);

            logger.debug("[FRAUD-DEBUG] ip-api.com -> calling {} for IP {}", url.replaceAll("key=[^&]+", "key=***"), ip);

            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);
            long latency = System.currentTimeMillis() - t0;

            if (response != null && "success".equalsIgnoreCase(response.status)
                    && response.countryCode != null && !response.countryCode.isBlank()) {

                healthRecorder.record("ip-api.com", latency, true, null);
                String currency = (response.currency != null && !response.currency.isBlank())
                        ? response.currency.trim().toUpperCase()
                        : null;
                boolean proxy = Boolean.TRUE.equals(response.proxy);
                boolean hosting = Boolean.TRUE.equals(response.hosting);
                // proxy=true → VPN/proxy connu ; hosting=true → IP datacenter (VPN, hébergeur)
                boolean vpnDetected = proxy || hosting;
                GeoResolution result = new GeoResolution(
                        response.countryCode.trim().toUpperCase(), currency, vpnDetected);
                Optional<GeoResolution> opt = Optional.of(result);
                cache.put(ip, new CachedResult(opt));
                logger.debug("[FRAUD-DEBUG] ip-api.com resolved {} -> country={} currency={} proxy={} hosting={} vpn={}",
                        ip, result.countryCode(), result.currencyCode(), proxy, hosting, vpnDetected);
                return opt;
            } else {
                healthRecorder.record("ip-api.com", latency, false, "non-success status");
                logger.debug("[FRAUD-DEBUG] ip-api.com returned non-success for {}: status={}",
                        ip, response != null ? response.status : "null");
            }
        } catch (Exception e) {
            healthRecorder.record("ip-api.com", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.warn("[FRAUD-DEBUG] ip-api.com unavailable for {}: {}", ip, e.getMessage());
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
        @JsonProperty("proxy")
        Boolean proxy;
        @JsonProperty("hosting")
        Boolean hosting;
    }
}
