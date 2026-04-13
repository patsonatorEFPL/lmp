package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * Cache unifié des taux de change EUR → X.
 *
 * <h3>Chaîne de chargement (exécutée dans cet ordre)</h3>
 * <ol>
 *   <li><b>Taux statiques config</b> ({@code pricing.region.*.eur-rate}) — dernier garde-fou</li>
 *   <li><b>Fawaz Ahmed Currency API</b> — 200+ devises dont toute l'Afrique, l'Asie, etc.
 *       CDN jsDelivr, gratuit, sans clé, avec URL de secours</li>
 *   <li><b>Frankfurter (BCE)</b> — 33 devises majeures. Exécuté en dernier :
 *       <em>écrase</em> les taux Fawaz pour ces devises (ECB = source de référence)</li>
 * </ol>
 *
 * <p>Le rafraîchissement automatique se déclenche chaque jour à 07h00 UTC
 * si {@code pricing.fx.auto-refresh=true}.
 *
 * <p>Chaque devise stocke sa propre source ({@code "frankfurter"}, {@code "fawaz"},
 * {@code "static"}) pour traçabilité dans {@link PricingContext}.
 */
@Service
public class FxRateCacheService {

    private static final Logger logger = LoggerFactory.getLogger(FxRateCacheService.class);

    private static final String BASE_CURRENCY = "EUR";

    // Frankfurter (BCE — 33 devises majeures, source de référence)
    private static final String FRANKFURTER_URL =
            "https://api.frankfurter.app/latest?from=EUR";

    // Fawaz Ahmed Currency API — 200+ devises, CDN jsDelivr
    private static final String FAWAZ_PRIMARY_URL =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/eur.min.json";
    private static final String FAWAZ_BACKUP_URL =
            "https://latest.currency-api.pages.dev/v1/currencies/eur.min.json";

    private final RestTemplate restTemplate;
    private final RegionalPricingProperties properties;
    private final ApiHealthRecorder healthRecorder;
    private final boolean enabled;

    /** Taux bruts EUR → devise. */
    private final Map<String, BigDecimal> ratesCache = new ConcurrentHashMap<>();
    /** Source par devise : "frankfurter", "fawaz" ou "static". */
    private final Map<String, String> rateSources = new ConcurrentHashMap<>();

    private volatile Instant lastRefreshed;

    public FxRateCacheService(RegionalPricingProperties properties, ApiHealthRecorder healthRecorder) {
        this.properties = properties;
        this.healthRecorder = healthRecorder;
        this.enabled = properties.isFxAutoRefresh();
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    void init() {
        loadStaticRates();
        if (enabled) {
            refreshAll();
        }
    }

    /** Rafraîchissement quotidien à 07h00 UTC. */
    @Scheduled(cron = "0 0 7 * * *")
    public void scheduledRefresh() {
        if (enabled) {
            refreshAll();
        }
    }

    /**
     * Taux EUR → {currency} avec marge FX appliquée.
     *
     * @param targetCurrency Code ISO 4217 (insensible à la casse).
     * @return Taux ou vide si la devise n'est pas connue d'aucune source.
     */
    public Optional<FxRate> getRate(String targetCurrency) {
        if (targetCurrency == null || targetCurrency.isBlank()) {
            return Optional.empty();
        }
        String code = targetCurrency.trim().toUpperCase();
        if (BASE_CURRENCY.equals(code)) {
            return Optional.of(new FxRate(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, "static"));
        }
        BigDecimal raw = ratesCache.get(code);
        if (raw == null) {
            return Optional.empty();
        }
        BigDecimal margin = properties.getFxMargin() != null
                ? properties.getFxMargin() : BigDecimal.ZERO;
        BigDecimal effective = raw.multiply(BigDecimal.ONE.add(margin))
                .setScale(6, RoundingMode.HALF_UP);
        String source = rateSources.getOrDefault(code, "static");
        return Optional.of(new FxRate(raw, effective, margin, source));
    }

    public Instant getLastRefreshed() {
        return lastRefreshed;
    }

    /** Résumé des sources actives dans le cache. */
    public String getCoverageSummary() {
        long frankfurterCount = rateSources.values().stream()
                .filter("frankfurter"::equals).count();
        long fawazCount = rateSources.values().stream()
                .filter("fawaz"::equals).count();
        long staticCount = rateSources.values().stream()
                .filter("static"::equals).count();
        return String.format("total=%d (frankfurter=%d, fawaz=%d, static=%d)",
                ratesCache.size(), frankfurterCount, fawazCount, staticCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshAll() {
        // 1. Fawaz en premier (200+ devises)
        refreshFromFawaz();
        // 2. Frankfurter en second : écrase ses 33 devises → source de référence ECB
        refreshFromFrankfurter();
        lastRefreshed = Instant.now();
        logger.info("[FX] Refresh complete — {}", getCoverageSummary());
    }

    private void refreshFromFrankfurter() {
        long t0 = System.currentTimeMillis();
        try {
            FrankfurterResponse resp = restTemplate.getForObject(
                    FRANKFURTER_URL, FrankfurterResponse.class);
            long latency = System.currentTimeMillis() - t0;
            if (resp != null && resp.rates != null && !resp.rates.isEmpty()) {
                healthRecorder.record("FX Rates", latency, true, null);
                resp.rates.forEach((k, v) -> {
                    String code = k.toUpperCase();
                    ratesCache.put(code, v);
                    rateSources.put(code, "frankfurter");
                });
                logger.info("[FX] Frankfurter: {} devises chargées", resp.rates.size());
            } else {
                healthRecorder.record("FX Rates", latency, false, "empty response");
                logger.warn("[FX] Frankfurter: réponse vide — taux Fawaz conservés");
            }
        } catch (Exception e) {
            healthRecorder.record("FX Rates", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.warn("[FX] Frankfurter indisponible: {} — taux Fawaz conservés", e.getMessage());
        }
    }

    private void refreshFromFawaz() {
        // Essaie l'URL principale, puis l'URL de secours
        if (!tryFawazUrl(FAWAZ_PRIMARY_URL)) {
            logger.warn("[FX] Fawaz CDN principal indisponible — tentative URL de secours");
            if (!tryFawazUrl(FAWAZ_BACKUP_URL)) {
                logger.warn("[FX] Fawaz indisponible (primaire + secours) — taux statiques conservés");
            }
        }
    }

    private boolean tryFawazUrl(String url) {
        try {
            FawazResponse resp = restTemplate.getForObject(url, FawazResponse.class);
            if (resp != null && resp.eur != null && !resp.eur.isEmpty()) {
                resp.eur.forEach((k, v) -> {
                    String code = k.toUpperCase();
                    // putIfAbsent ici : Frankfurter (appelé après) écrasera ses devises
                    ratesCache.putIfAbsent(code, v);
                    rateSources.putIfAbsent(code, "fawaz");
                });
                logger.info("[FX] Fawaz: {} devises chargées depuis {}", resp.eur.size(), url);
                return true;
            }
        } catch (Exception e) {
            logger.debug("[FX] Fawaz URL {} indisponible: {}", url, e.getMessage());
        }
        return false;
    }

    private void loadStaticRates() {
        properties.getRegion().forEach((country, rate) -> {
            if (rate.getCurrency() != null && rate.getEurRate() != null) {
                String code = rate.getCurrency().toUpperCase();
                ratesCache.putIfAbsent(code, rate.getEurRate());
                rateSources.putIfAbsent(code, "static");
            }
        });
        lastRefreshed = Instant.now();
        logger.info("[FX] Taux statiques config chargés ({} entrées)", ratesCache.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Value objects / DTOs
    // ─────────────────────────────────────────────────────────────────────────

    /** Résultat d'un taux : brut, effectif (avec marge), marge, source. */
    public record FxRate(
            BigDecimal rawRate,
            BigDecimal effectiveRate,
            BigDecimal margin,
            String source) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FrankfurterResponse {
        @JsonProperty("rates")
        Map<String, BigDecimal> rates;
    }

    /**
     * Réponse Fawaz Ahmed Currency API.
     * Structure : {@code {"date":"...","eur":{"ngn":1587.61,"mad":10.83,...}}}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FawazResponse {
        @JsonProperty("eur")
        Map<String, BigDecimal> eur;
    }
}
