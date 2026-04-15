package com.lmp.shared.web.debug;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;
import com.lmp.shared.pricing.FxRateCacheService;
import com.lmp.shared.pricing.PricingContext;
import com.lmp.shared.pricing.RegionalPricingService;
import com.lmp.shared.pricing.VatCalculationService;
import com.lmp.shared.pricing.VatRateLookupService;
import com.lmp.shared.web.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endpoint de test pour visualiser la détection pays / devise / taux en temps réel.
 *
 * <p>Disponible uniquement en profil {@code dev}.
 * URL : {@code GET /api/debug/pricing}
 * URL avec prix : {@code GET /api/debug/pricing?price=49.99}
 */
@RestController
@RequestMapping("/api/debug/pricing")
@Profile("dev")
class PricingDebugController {

    private final GeoCountryLookupService geoCountryLookupService;
    private final RegionalPricingService regionalPricingService;
    private final FxRateCacheService fxRateCacheService;
    private final VatCalculationService vatCalculationService;
    private final VatRateLookupService vatRateLookupService;

    PricingDebugController(GeoCountryLookupService geoCountryLookupService,
                           RegionalPricingService regionalPricingService,
                           FxRateCacheService fxRateCacheService,
                           VatCalculationService vatCalculationService,
                           VatRateLookupService vatRateLookupService) {
        this.geoCountryLookupService = geoCountryLookupService;
        this.regionalPricingService = regionalPricingService;
        this.fxRateCacheService = fxRateCacheService;
        this.vatCalculationService = vatCalculationService;
        this.vatRateLookupService = vatRateLookupService;
    }

    @GetMapping
    ResponseEntity<Map<String, Object>> pricingDebug(
            HttpServletRequest request,
            @RequestParam(defaultValue = "49.99") BigDecimal price) {

        Map<String, Object> result = new LinkedHashMap<>();

        // ── 1. IP client ─────────────────────────────────────────────────────
        String ip = ClientIpResolver.resolve(request);
        result.put("ip_detected", ip);
        result.put("cf_ip_country_header", request.getHeader("CF-IPCountry"));

        // ── 2. Résolution géographique ────────────────────────────────────────
        GeoResolution geo = geoCountryLookupService.resolve(request)
                .orElse(null);

        Map<String, Object> geoMap = new LinkedHashMap<>();
        if (geo != null) {
            geoMap.put("country_code", geo.countryCode());
            geoMap.put("currency_from_api", geo.currencyCode());
        } else {
            geoMap.put("country_code", null);
            geoMap.put("currency_from_api", null);
            geoMap.put("note", "Aucune source géo disponible — fallback-country utilisé");
        }
        result.put("geo", geoMap);

        // ── 3. Contexte tarifaire (affichage) ────────────────────────────────
        PricingContext displayCtx = regionalPricingService.resolve(request);
        BigDecimal displayPrice = regionalPricingService.convertFromEur(price, displayCtx);

        Map<String, Object> displayMap = new LinkedHashMap<>();
        displayMap.put("country_code", displayCtx.countryCode());
        displayMap.put("currency", displayCtx.currency());
        displayMap.put("rate_eur_to_currency", displayCtx.eurToTargetRate());
        displayMap.put("rate_raw", displayCtx.rawRate());
        displayMap.put("fx_margin", displayCtx.fxMargin());
        displayMap.put("rate_source", displayCtx.rateSource());
        displayMap.put("price_eur_input", price);
        displayMap.put("price_displayed", displayPrice);
        displayMap.put("price_displayed_formatted",
                displayPrice + " " + displayCtx.currency());
        result.put("display", displayMap);

        // ── 4. Contexte paiement Stripe ───────────────────────────────────────
        PricingContext payCtx = regionalPricingService.resolveForPayment(displayCtx);
        BigDecimal payPrice = regionalPricingService.convertFromEur(price, payCtx);

        Map<String, Object> payMap = new LinkedHashMap<>();
        payMap.put("currency", payCtx.currency());
        payMap.put("amount_stripe", payPrice);
        payMap.put("stripe_currency_supported",
                RegionalPricingService.STRIPE_PAYMENT_CURRENCIES.contains(displayCtx.currency()));
        if (!displayCtx.currency().equals(payCtx.currency())) {
            payMap.put("fallback_reason",
                    displayCtx.currency() + " non supportée par Stripe → " + payCtx.currency());
        }
        result.put("payment_stripe", payMap);

        // ── 5. TVA ────────────────────────────────────────────────────────────
        String vatCountry = payCtx.countryCode();
        BigDecimal vatRate = vatCalculationService.getVatRate(vatCountry);
        BigDecimal amountHt = payPrice;
        BigDecimal vatAmount = vatCalculationService.vatAmount(amountHt, false, vatCountry);
        BigDecimal amountTtc = vatCalculationService.applyVat(amountHt, false, vatCountry);
        Map<String, Object> vatMap = new LinkedHashMap<>();
        vatMap.put("country", vatCountry);
        vatMap.put("rate_decimal", vatRate);
        vatMap.put("rate_pct", vatRate.multiply(new BigDecimal("100")).intValue());
        vatMap.put("amount_ht", amountHt);
        vatMap.put("vat_amount", vatAmount);
        vatMap.put("amount_ttc", amountTtc);
        vatMap.put("cache_size", vatRateLookupService.getCacheSize());
        vatMap.put("last_refreshed", vatRateLookupService.getLastRefreshed());
        vatMap.put("has_rate_for_country", vatRateLookupService.hasRate(vatCountry));
        vatMap.put("is_eu_country", vatRateLookupService.isEuCountry(vatCountry));
        result.put("vat", vatMap);

        // ── 6. Couverture du cache FX ─────────────────────────────────────────
        result.put("fx_cache", fxRateCacheService.getCoverageSummary());
        result.put("fx_last_refreshed", fxRateCacheService.getLastRefreshed());

        // ── 6. Conseil pour tester d'autres pays ─────────────────────────────
        result.put("_tip", Map.of(
                "override_country", "Ajouter 'geoip.country-test-override=SN' dans application-dev.properties",
                "examples", "SN=Sénégal(XOF)  NG=Nigeria(NGN)  MA=Maroc(MAD)  CA=Canada(CAD)  JP=Japon(JPY)",
                "test_url", "http://localhost:8080/api/debug/pricing?price=49.99"
        ));

        return ResponseEntity.ok(result);
    }
}
