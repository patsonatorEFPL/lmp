package com.lmp.shared.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RegionalPricingServiceTest {

    @Mock
    private GeoCountryLookupService geoCountryLookupService;

    @Mock
    private FxRateCacheService fxRateCacheService;

    @Mock
    private HttpServletRequest request;

    private RegionalPricingProperties properties;
    private RegionalPricingService service;

    @BeforeEach
    void setUp() {
        properties = new RegionalPricingProperties();
        properties.setBaseCurrency("EUR");
        properties.setFallbackCountry("FR");
        properties.setFxMargin(new BigDecimal("0.10"));

        var ca = new RegionalPricingProperties.RegionRate();
        ca.setCurrency("CAD");
        ca.setEurRate(new BigDecimal("1.50"));
        properties.getRegion().put("CA", ca);

        service = new RegionalPricingService(geoCountryLookupService, properties, fxRateCacheService, false);
    }

    // ── Devise depuis config ──────────────────────────────────────────────────

    @Test
    void unknownCountry_noHint_returnsEurContext() {
        PricingContext ctx = service.contextForCountry("ZZ");
        assertEquals("EUR", ctx.currency());
        assertEquals(BigDecimal.ONE, ctx.eurToTargetRate());
    }

    @Test
    void canada_configCurrency_usesFxCacheWhenPresent() {
        when(fxRateCacheService.getRate("CAD"))
                .thenReturn(Optional.of(new FxRateCacheService.FxRate(
                        new BigDecimal("1.48"), new BigDecimal("1.628"),
                        new BigDecimal("0.10"), "frankfurter")));

        PricingContext ctx = service.contextForCountry("CA");
        assertEquals("CAD", ctx.currency());
        assertEquals(new BigDecimal("1.628"), ctx.eurToTargetRate());
        assertEquals("frankfurter", ctx.rateSource());
    }

    @Test
    void canada_configCurrency_fallsBackToStaticWhenFxEmpty() {
        when(fxRateCacheService.getRate("CAD")).thenReturn(Optional.empty());

        PricingContext ctx = service.contextForCountry("CA");
        assertEquals("CAD", ctx.currency());
        assertEquals(new BigDecimal("1.50"), ctx.eurToTargetRate());
        assertEquals("static", ctx.rateSource());
    }

    // ── Devise depuis l'API géo (currencyHint) ───────────────────────────────

    @Test
    void senegal_currencyHintXof_usesFixedRate() {
        // ipwho.is a renvoyé SN + XOF (pas dans pricing.region, pas dans Frankfurter)
        when(fxRateCacheService.getRate("XOF")).thenReturn(Optional.empty());

        PricingContext ctx = service.contextForCountry("SN", "XOF");
        assertEquals("SN", ctx.countryCode());
        assertEquals("XOF", ctx.currency());
        assertEquals(new BigDecimal("655.957"), ctx.eurToTargetRate());
        assertEquals("fixed", ctx.rateSource());
    }

    @Test
    void nigeria_currencyHintNgn_frankfurterRate() {
        when(fxRateCacheService.getRate("NGN"))
                .thenReturn(Optional.of(new FxRateCacheService.FxRate(
                        new BigDecimal("1600"), new BigDecimal("1640"),
                        new BigDecimal("0.025"), "frankfurter")));

        PricingContext ctx = service.contextForCountry("NG", "NGN");
        assertEquals("NGN", ctx.currency());
        assertEquals(new BigDecimal("1640"), ctx.eurToTargetRate());
        assertEquals("frankfurter", ctx.rateSource());
    }

    @Test
    void unknownCountry_withApiHint_usesHintCurrency() {
        // Pays inconnu mais l'API a fourni une devise
        when(fxRateCacheService.getRate("MGA")).thenReturn(Optional.empty());

        // MGA (Madagascar) — aucun taux fixe ni config, mais l'API a fourni la devise
        PricingContext ctx = service.contextForCountry("MG", "MGA");
        // Pas de taux disponible → fallback EUR
        assertEquals("EUR", ctx.currency());
    }

    @Test
    void configOverrides_apiHint() {
        // CA est dans pricing.region avec CAD — le hint "USD" de l'API doit être ignoré
        when(fxRateCacheService.getRate("CAD"))
                .thenReturn(Optional.of(new FxRateCacheService.FxRate(
                        new BigDecimal("1.48"), new BigDecimal("1.628"),
                        BigDecimal.ZERO, "frankfurter")));

        PricingContext ctx = service.contextForCountry("CA", "USD"); // hint ignoré
        assertEquals("CAD", ctx.currency()); // config gagne
    }

    // ── resolve() depuis requête HTTP ─────────────────────────────────────────

    @Test
    void resolve_usesGeoResolutionWithCurrency() {
        when(geoCountryLookupService.resolve(request))
                .thenReturn(Optional.of(new GeoResolution("SN", "XOF")));
        when(fxRateCacheService.getRate("XOF")).thenReturn(Optional.empty());

        PricingContext ctx = service.resolve(request);
        assertEquals("SN", ctx.countryCode());
        assertEquals("XOF", ctx.currency());
        assertEquals("fixed", ctx.rateSource());
    }

    @Test
    void resolve_fallbackCountryWhenGeoEmpty() {
        when(geoCountryLookupService.resolve(request)).thenReturn(Optional.empty());

        // fallback-country = FR, pas dans pricing.region, pas de hint → EUR
        PricingContext ctx = service.resolve(request);
        assertEquals("EUR", ctx.currency());
    }

    // ── resolveForPayment (Stripe fallback) ───────────────────────────────────

    @Test
    void resolveForPayment_supportedCurrency_unchanged() {
        PricingContext cadCtx = new PricingContext("CA", "CAD",
                new BigDecimal("1.48"), new BigDecimal("1.48"), BigDecimal.ZERO, "frankfurter");

        PricingContext pay = service.resolveForPayment(cadCtx);
        assertEquals("CAD", pay.currency());
    }

    @Test
    void resolveForPayment_xof_fallsBackToEur() {
        PricingContext xofCtx = new PricingContext("SN", "XOF",
                new BigDecimal("655.957"), new BigDecimal("655.957"), BigDecimal.ZERO, "fixed");

        PricingContext pay = service.resolveForPayment(xofCtx);
        assertEquals("EUR", pay.currency()); // Stripe ne supporte pas XOF
        assertEquals(BigDecimal.ONE, pay.eurToTargetRate());
    }

    @Test
    void resolveForPayment_ngn_fallsBackToEur() {
        PricingContext ngnCtx = new PricingContext("NG", "NGN",
                new BigDecimal("1640"), new BigDecimal("1640"), BigDecimal.ZERO, "frankfurter");

        PricingContext pay = service.resolveForPayment(ngnCtx);
        assertEquals("EUR", pay.currency());
    }

    // ── convertFromEur / toBaseEur ────────────────────────────────────────────

    @Test
    void convertFromEur_appliesRate() {
        when(fxRateCacheService.getRate("CAD"))
                .thenReturn(Optional.of(new FxRateCacheService.FxRate(
                        new BigDecimal("2"), new BigDecimal("2"), BigDecimal.ZERO, "static")));

        PricingContext ctx = service.contextForCountry("CA");
        assertEquals(new BigDecimal("200.00"), service.convertFromEur(new BigDecimal("100"), ctx));
    }

    @Test
    void toBaseEur_invertsRate() {
        when(fxRateCacheService.getRate("CAD"))
                .thenReturn(Optional.of(new FxRateCacheService.FxRate(
                        new BigDecimal("2"), new BigDecimal("2"), BigDecimal.ZERO, "static")));

        PricingContext ctx = service.contextForCountry("CA");
        assertEquals(new BigDecimal("50.00"), service.toBaseEur(new BigDecimal("100"), ctx));
    }
}
