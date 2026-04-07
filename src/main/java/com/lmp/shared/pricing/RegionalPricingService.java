package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Résolution pays → devise → taux → prix final (marge FX + arrondi psychologique).
 *
 * <h3>Résolution de la devise</h3>
 * <ol>
 *   <li>{@code pricing.region.{pays}.currency} — override explicite en config</li>
 *   <li>Devise renvoyée par l'API géo (ipwho.is ou ip-api.com) via {@link GeoResolution}</li>
 *   <li>Devise de base ({@code pricing.base-currency}, défaut EUR)</li>
 * </ol>
 *
 * <h3>Résolution du taux EUR → devise</h3>
 * <ol>
 *   <li>Cache Frankfurter (BCE, rafraîchi à 07h UTC)</li>
 *   <li>Taux fixe légal intégré : XOF/XAF = 655.957, KMF = 491.968, CVE = 110.265</li>
 *   <li>Taux statique {@code pricing.region.{pays}.eur-rate}</li>
 *   <li>Taux 1.0 (retour à la devise de base)</li>
 * </ol>
 */
@Service
public class RegionalPricingService {

    private static final Logger logger = LoggerFactory.getLogger(RegionalPricingService.class);

    /**
     * Devises acceptées pour un paiement Stripe.
     * Si la devise détectée n'est pas dans cette liste, le paiement bascule sur EUR.
     */
    public static final Set<String> STRIPE_PAYMENT_CURRENCIES = Set.of(
            "CAD", "USD", "EUR", "GBP", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK");

    /**
     * Taux fixes légaux EUR → devise (parités officielles immuables ou quasi-stables).
     * Utilisés quand Frankfurter ne couvre pas la devise ET qu'aucun taux config n'existe.
     */
    private static final Map<String, BigDecimal> FIXED_EUR_RATES = Map.of(
            "XOF", new BigDecimal("655.957"),   // Franc CFA Ouest-Africain (UEMOA)
            "XAF", new BigDecimal("655.957"),   // Franc CFA Centre-Africain (CEMAC)
            "KMF", new BigDecimal("491.968"),   // Franc comorien
            "CVE", new BigDecimal("110.265")    // Escudo cap-verdien
    );

    private final GeoCountryLookupService geoCountryLookupService;
    private final RegionalPricingProperties properties;
    private final FxRateCacheService fxRateCacheService;
    private final boolean psychologicalRounding;

    public RegionalPricingService(
            GeoCountryLookupService geoCountryLookupService,
            RegionalPricingProperties properties,
            FxRateCacheService fxRateCacheService,
            @Value("${pricing.psychological-rounding:true}") boolean psychologicalRounding) {
        this.geoCountryLookupService = geoCountryLookupService;
        this.properties = properties;
        this.fxRateCacheService = fxRateCacheService;
        this.psychologicalRounding = psychologicalRounding;
    }

    /**
     * Résout le contexte de tarification depuis la requête HTTP.
     * La devise est fournie par l'API géo quand disponible (ipwho.is / ip-api.com).
     */
    public PricingContext resolve(HttpServletRequest request) {
        GeoResolution geo = geoCountryLookupService.resolve(request)
                .orElseGet(() -> GeoResolution.countryOnly(
                        properties.getFallbackCountry().toUpperCase(Locale.ROOT)));
        return contextForCountry(geo.countryCode(), geo.currencyCode());
    }

    /**
     * Résout le contexte pour un code pays ISO donné, sans hint devise (fallback legacy).
     */
    public PricingContext contextForCountry(String iso2Country) {
        return contextForCountry(iso2Country, null);
    }

    /**
     * Résout le contexte pour un code pays ISO donné avec un hint devise optionnel.
     *
     * @param iso2Country  Code pays ISO 3166-1 alpha-2.
     * @param currencyHint Devise renvoyée directement par l'API géo (peut être null).
     */
    public PricingContext contextForCountry(String iso2Country, String currencyHint) {
        if (iso2Country == null || iso2Country.isBlank()) {
            return eurContext("XX");
        }
        String key = iso2Country.trim().toUpperCase(Locale.ROOT);

        // ── 1. Devise ─────────────────────────────────────────────────────────
        RegionalPricingProperties.RegionRate regionRate = properties.getRegion().get(key);
        String currency;
        BigDecimal configStaticRate = null;

        if (regionRate != null && regionRate.getCurrency() != null
                && !regionRate.getCurrency().isBlank()) {
            // Config explicite toujours prioritaire (permet de corriger une API incorrecte)
            currency = regionRate.getCurrency().trim().toUpperCase(Locale.ROOT);
            configStaticRate = regionRate.getEurRate();
        } else if (currencyHint != null && !currencyHint.isBlank()) {
            // Devise fournie par l'API géo (ipwho.is / ip-api.com)
            currency = currencyHint.trim().toUpperCase(Locale.ROOT);
        } else {
            // Aucune source disponible → devise de base
            return eurContext(key);
        }

        // ── 2. Taux EUR → devise ──────────────────────────────────────────────
        // 2a. Frankfurter (cache quotidien)
        var fxOpt = fxRateCacheService.getRate(currency);
        if (fxOpt.isPresent()) {
            var fxRate = fxOpt.get();
            return new PricingContext(key, currency,
                    fxRate.effectiveRate(), fxRate.rawRate(), fxRate.margin(), fxRate.source());
        }

        // 2b. Taux fixe légal (XOF, XAF, KMF, CVE — non couverts par Frankfurter/BCE)
        BigDecimal fixedRate = FIXED_EUR_RATES.get(currency);
        if (fixedRate != null) {
            return new PricingContext(key, currency, fixedRate, fixedRate, BigDecimal.ZERO, "fixed");
        }

        // 2c. Taux statique depuis la config (pricing.region.XX.eur-rate)
        if (configStaticRate != null && configStaticRate.compareTo(BigDecimal.ZERO) > 0) {
            return new PricingContext(key, currency,
                    configStaticRate, configStaticRate, BigDecimal.ZERO, "static");
        }

        // 2d. Aucun taux connu → retour à la devise de base
        logger.debug("[Pricing] No FX rate for {} (country {}) — falling back to base currency", currency, key);
        return eurContext(key);
    }

    /**
     * Retourne un contexte adapté au paiement Stripe.
     *
     * <p>Si la devise du contexte d'affichage n'est pas dans {@link #STRIPE_PAYMENT_CURRENCIES},
     * le paiement est effectué dans la devise de base (EUR) pour éviter un rejet Stripe.
     */
    public PricingContext resolveForPayment(PricingContext displayCtx) {
        if (STRIPE_PAYMENT_CURRENCIES.contains(displayCtx.currency())) {
            return displayCtx;
        }
        logger.info("[Pricing] Devise {} non supportée par Stripe (pays {}) — facturation en {}",
                displayCtx.currency(), displayCtx.countryCode(),
                properties.getBaseCurrency() != null ? properties.getBaseCurrency() : "EUR");
        return eurContext(displayCtx.countryCode());
    }

    /**
     * Convertit un montant EUR en devise cible, avec arrondi psychologique si activé.
     */
    public BigDecimal convertFromEur(BigDecimal amountEur, PricingContext ctx) {
        if (amountEur == null) {
            return null;
        }
        BigDecimal converted = amountEur.multiply(ctx.eurToTargetRate()).setScale(2, RoundingMode.HALF_UP);
        if (psychologicalRounding) {
            return PsychologicalRounder.round(converted, ctx.currency());
        }
        return converted;
    }

    /**
     * Retourne le montant EUR de base depuis un montant en devise locale (déconversion).
     */
    public BigDecimal toBaseEur(BigDecimal amountLocal, PricingContext ctx) {
        if (amountLocal == null || ctx.eurToTargetRate().compareTo(BigDecimal.ZERO) == 0) {
            return amountLocal;
        }
        return amountLocal.divide(ctx.eurToTargetRate(), 2, RoundingMode.HALF_UP);
    }

    private PricingContext eurContext(String countryCode) {
        String base = properties.getBaseCurrency() != null
                ? properties.getBaseCurrency().trim().toUpperCase(Locale.ROOT)
                : "EUR";
        return new PricingContext(countryCode, base, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, "static");
    }
}
