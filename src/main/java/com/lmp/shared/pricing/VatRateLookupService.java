package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.lmp.shared.monitoring.ApiHealthRecorder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.PostConstruct;

/**
 * Cache des taux de TVA standard par pays (ISO 3166-1 alpha-2).
 *
 * <h3>Chaîne de résolution (refresh)</h3>
 * <ol>
 *   <li><b>Table statique</b> — seed initial 27 pays UE</li>
 *   <li><b>VATComply API</b> — primaire, gratuit, sans clé</li>
 *   <li><b>VATLayer</b> (apilayer {@code rate_list}) — si {@code pricing.vat.vatlayer.access-key}
 *       est renseigné <em>et</em> que VATComply a échoué</li>
 * </ol>
 *
 * <p>Refresh : {@code @PostConstruct} + cron mensuel (1er du mois, 06h UTC).
 * Chaque refresh met à jour la table statique en mémoire.
 *
 * <h3>Résolution du taux</h3>
 * <ul>
 *   <li>{@code null} / vide → taux par défaut ({@code pricing.vat.rate}, défaut 0.20 = FR domestique)</li>
 *   <li>Pays UE → taux du cache (API + seed)</li>
 *   <li>Pays hors-UE (code connu) → <strong>0 %</strong> (exonération export — hors champ TVA)</li>
 * </ul>
 */
@Service
public class VatRateLookupService {

    private static final Logger logger = LoggerFactory.getLogger(VatRateLookupService.class);

    /** VATComply — endpoint TVA (pas /rates qui est FX). */
    private static final String VATCOMPLY_URL = "https://api.vatcomply.com/vat_rates";

    /** VATLayer (apilayer) — liste des taux UE ; nécessite une clé d'accès. */
    private static final String VATLAYER_RATE_LIST_BASE = "https://apilayer.net/api/rate_list";

    /**
     * Jeu explicite des 27 pays membres de l'UE (ISO 3166-1 alpha-2).
     * Découplé du cache pour éviter toute ambiguïté en cas de bug API ou d'adhésion d'un nouveau membre.
     */
    private static final Set<String> EU_COUNTRY_CODES = Set.of(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI",
            "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT",
            "NL", "PL", "PT", "RO", "SE", "SI", "SK"
    );

    /** Table statique UE — seed initial, mise à jour en mémoire par les APIs. */
    private static final Map<String, BigDecimal> STATIC_EU_RATES = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("AT", new BigDecimal("0.20")),
            Map.entry("BE", new BigDecimal("0.21")),
            Map.entry("BG", new BigDecimal("0.20")),
            Map.entry("CY", new BigDecimal("0.19")),
            Map.entry("CZ", new BigDecimal("0.21")),
            Map.entry("DE", new BigDecimal("0.19")),
            Map.entry("DK", new BigDecimal("0.25")),
            Map.entry("EE", new BigDecimal("0.22")),
            Map.entry("ES", new BigDecimal("0.21")),
            Map.entry("FI", new BigDecimal("0.255")),
            Map.entry("FR", new BigDecimal("0.20")),
            Map.entry("GR", new BigDecimal("0.24")),
            Map.entry("HR", new BigDecimal("0.25")),
            Map.entry("HU", new BigDecimal("0.27")),
            Map.entry("IE", new BigDecimal("0.23")),
            Map.entry("IT", new BigDecimal("0.22")),
            Map.entry("LT", new BigDecimal("0.21")),
            Map.entry("LU", new BigDecimal("0.17")),
            Map.entry("LV", new BigDecimal("0.21")),
            Map.entry("MT", new BigDecimal("0.18")),
            Map.entry("NL", new BigDecimal("0.21")),
            Map.entry("PL", new BigDecimal("0.23")),
            Map.entry("PT", new BigDecimal("0.23")),
            Map.entry("RO", new BigDecimal("0.19")),
            Map.entry("SE", new BigDecimal("0.25")),
            Map.entry("SI", new BigDecimal("0.22")),
            Map.entry("SK", new BigDecimal("0.23"))
    ));

    private final RestTemplate restTemplate;
    private final ApiHealthRecorder healthRecorder;
    private final BigDecimal defaultRate;
    /** Vide = désactivé. */
    private final String vatLayerAccessKey;

    /** Cache principal : pays ISO → taux TVA décimal (ex: 0.21). */
    private final Map<String, BigDecimal> ratesCache = new ConcurrentHashMap<>();

    private volatile Instant lastRefreshed;

    public VatRateLookupService(
            ApiHealthRecorder healthRecorder,
            @Value("${pricing.vat.rate:0.20}") BigDecimal defaultRate,
            @Value("${pricing.vat.vatlayer.access-key:}") String vatLayerAccessKey) {
        this.healthRecorder = healthRecorder;
        this.defaultRate = defaultRate;
        this.vatLayerAccessKey = vatLayerAccessKey != null ? vatLayerAccessKey.trim() : "";
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    void init() {
        refreshAll();
    }

    /** Refresh mensuel : 1er du mois à 06h00 UTC. */
    @Scheduled(cron = "0 0 6 1 * *")
    public void scheduledRefresh() {
        refreshAll();
    }

    /**
     * Retourne le taux TVA standard pour un pays donné.
     *
     * <ul>
     *   <li>{@code null} / vide → taux par défaut (règle domestique FR)</li>
     *   <li>Pays UE → taux du cache (API + seed)</li>
     *   <li>Pays hors-UE → {@code 0} (exonération export, hors champ TVA)</li>
     * </ul>
     *
     * @param countryCode Code ISO 3166-1 alpha-2 (ex: "DE", "FR").
     * @return Taux TVA décimal (ex: 0.19 pour DE, 0 pour CA).
     */
    public BigDecimal getRate(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return defaultRate;
        }
        String key = normalizeCountryCode(countryCode);

        // Pays UE : taux du cache, fallback defaultRate si cache vide (ne devrait pas arriver)
        if (EU_COUNTRY_CODES.contains(key)) {
            BigDecimal rate = ratesCache.get(key);
            return rate != null ? rate : defaultRate;
        }

        // Pays hors-UE connu : exonération export → 0 %
        return BigDecimal.ZERO;
    }

    /** Vérifie si un pays est dans le cache (UE ou connu). */
    public boolean hasRate(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return false;
        String key = normalizeCountryCode(countryCode);
        return ratesCache.containsKey(key);
    }

    /** Vérifie si un pays fait partie de l'UE (jeu explicite, indépendant du cache). */
    public boolean isEuCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return false;
        return EU_COUNTRY_CODES.contains(normalizeCountryCode(countryCode));
    }

    /** Normalise un code pays : trim, uppercase, alias EL→GR. */
    private static String normalizeCountryCode(String countryCode) {
        String key = countryCode.trim().toUpperCase();
        if ("EL".equals(key)) {
            key = "GR";
        }
        return key;
    }

    public Instant getLastRefreshed() {
        return lastRefreshed;
    }

    public int getCacheSize() {
        return ratesCache.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh logic
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshAll() {
        // 1. Charger la table statique comme base
        ratesCache.putAll(STATIC_EU_RATES);
        logger.info("[VAT Rates] Table statique chargée ({} pays)", STATIC_EU_RATES.size());

        // 2. VATComply (primaire) — écrase les taux statiques
        boolean vatComplyOk = refreshFromVatComply();

        // 3. VATLayer — uniquement si VATComply a échoué et clé configurée
        boolean vatLayerOk = false;
        if (!vatComplyOk && !vatLayerAccessKey.isEmpty()) {
            vatLayerOk = refreshFromVatLayer();
            if (!vatLayerOk) {
                logger.warn("[VAT Rates] VATLayer indisponible ou invalide — taux statiques UE conservés");
            }
        }

        // 4. Mettre à jour la table statique avec les résultats API
        updateStaticTable();

        lastRefreshed = Instant.now();
        logger.info("[VAT Rates] Refresh terminé — {} pays en cache (VATComply={}, VATLayer={})",
                ratesCache.size(),
                vatComplyOk ? "OK" : "FAIL",
                vatLayerAccessKey.isEmpty() ? "skip(no-key)"
                        : (vatComplyOk ? "skip(primary-ok)" : (vatLayerOk ? "OK" : "FAIL")));
    }

    /**
     * VATComply API — GET /vat_rates retourne tous les taux TVA UE en un seul appel.
     * Réponse : tableau JSON [{country_code, standard_rate, ...}, ...]
     */
    private boolean refreshFromVatComply() {
        long t0 = System.currentTimeMillis();
        try {
            VatComplyEntry[] entries = restTemplate.getForObject(VATCOMPLY_URL, VatComplyEntry[].class);
            long latency = System.currentTimeMillis() - t0;

            if (entries == null || entries.length == 0) {
                healthRecorder.record("VATComply", latency, false, "empty response");
                logger.warn("[VAT Rates] VATComply: réponse vide");
                return false;
            }

            int updated = 0;
            for (VatComplyEntry entry : entries) {
                if (entry.countryCode != null && entry.standardRate != null
                        && entry.standardRate.compareTo(BigDecimal.ZERO) > 0) {
                    String country = entry.countryCode.trim().toUpperCase();
                    // Convertir pourcentage → décimal (19.0 → 0.19)
                    BigDecimal rate = entry.standardRate.movePointLeft(2);
                    ratesCache.put(country, rate);
                    updated++;
                }
            }

            healthRecorder.record("VATComply", latency, updated > 0, null);
            if (updated > 0) {
                logger.info("[VAT Rates] VATComply: {} pays mis à jour", updated);
            } else {
                logger.warn("[VAT Rates] VATComply: aucun taux récupéré");
            }
            return updated > 0;
        } catch (Exception e) {
            healthRecorder.record("VATComply", System.currentTimeMillis() - t0, false, e.getMessage());
            logger.warn("[VAT Rates] VATComply indisponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * VATLayer (apilayer) — GET rate_list, taux standard par pays UE.
     * N'appelé que si {@link #vatLayerAccessKey} est non vide et VATComply a échoué.
     */
    private boolean refreshFromVatLayer() {
        long t0 = System.currentTimeMillis();
        String url = UriComponentsBuilder.fromUriString(VATLAYER_RATE_LIST_BASE)
                .queryParam("access_key", vatLayerAccessKey)
                .build()
                .toUriString();
        try {
            VatLayerRateListResponse body = restTemplate.getForObject(url, VatLayerRateListResponse.class);
            long latency = System.currentTimeMillis() - t0;

            if (body == null || !Boolean.TRUE.equals(body.success) || body.rates == null || body.rates.isEmpty()) {
                healthRecorder.record("VATLayer", latency, false,
                        body == null ? "null body" : "success=false or empty rates");
                logger.warn("[VAT Rates] VATLayer: réponse vide ou success=false");
                return false;
            }

            int updated = 0;
            for (Map.Entry<String, VatLayerRateEntry> e : body.rates.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue().standardRate == null) {
                    continue;
                }
                String country = e.getKey().trim().toUpperCase();
                if (!EU_COUNTRY_CODES.contains(country)) {
                    continue;
                }
                BigDecimal pct = e.getValue().standardRate;
                if (pct.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal rate = pct.movePointLeft(2);
                ratesCache.put(country, rate);
                updated++;
            }

            healthRecorder.record("VATLayer", latency, updated > 0, null);
            if (updated > 0) {
                logger.info("[VAT Rates] VATLayer: {} pays UE mis à jour", updated);
            } else {
                logger.warn("[VAT Rates] VATLayer: aucun taux UE applicable dans la réponse");
            }
            return updated > 0;
        } catch (Exception ex) {
            healthRecorder.record("VATLayer", System.currentTimeMillis() - t0, false, ex.getMessage());
            logger.warn("[VAT Rates] VATLayer indisponible: {}", ex.getMessage());
            return false;
        }
    }

    /** Met à jour la table statique en mémoire pour survivre à un échec API. */
    private void updateStaticTable() {
        for (String country : STATIC_EU_RATES.keySet()) {
            BigDecimal cached = ratesCache.get(country);
            if (cached != null) {
                STATIC_EU_RATES.put(country, cached);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON DTO — VATComply
    // ─────────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VatComplyEntry {
        @JsonProperty("country_code")
        String countryCode;

        @JsonProperty("standard_rate")
        BigDecimal standardRate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON DTO — VATLayer (apilayer rate_list)
    // ─────────────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VatLayerRateListResponse {
        Boolean success;
        Map<String, VatLayerRateEntry> rates;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VatLayerRateEntry {
        @JsonProperty("standard_rate")
        BigDecimal standardRate;
    }

}
