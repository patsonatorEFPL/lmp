package com.lmp.shared.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Scénarios de tarification affichée selon le pays du visiteur.
 *
 * <p>Simule ce que l'API ipwho.is renverrait pour chaque pays,
 * et montre le prix affiché au catalogue + le montant facturé via Stripe.
 *
 * <p>Prix catalogue de référence : <strong>49,99 EUR</strong>.
 */
@ExtendWith(MockitoExtension.class)
class PricingDisplayScenarioTest {

    private static final Logger log = LoggerFactory.getLogger(PricingDisplayScenarioTest.class);

    private static final BigDecimal CATALOG_EUR = new BigDecimal("49.99");

    @Mock GeoCountryLookupService geo;
    @Mock FxRateCacheService fx;
    @Mock HttpServletRequest request;

    // Arrondi psychologique activé (comme en prod)
    private RegionalPricingService service;

    @BeforeEach
    void setUp() {
        RegionalPricingProperties props = new RegionalPricingProperties();
        props.setBaseCurrency("EUR");
        props.setFallbackCountry("FR");

        // Seuls CA, US, GB sont dans la config explicite
        addRegion(props, "CA", "CAD", "1.50");
        addRegion(props, "US", "USD", "1.09");
        addRegion(props, "GB", "GBP", "0.85");

        service = new RegionalPricingService(geo, props, fx, true); // arrondi actif
    }

    // ── Sénégal (SN) ─────────────────────────────────────────────────────────

    @Test
    void senegal_xof_affiche_et_paiement_eur() {
        // ipwho.is renvoie : SN + XOF
        mockGeo("SN", "XOF");
        // XOF absent de Frankfurter → taux fixe légal 655,957 dans RegionalPricingService
        when(fx.getRate("XOF")).thenReturn(Optional.empty());

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        PricingContext payment = service.resolveForPayment(display);
        BigDecimal pricePayment = service.convertFromEur(CATALOG_EUR, payment);

        print("Sénégal (SN)", display, priceDisplay, payment, pricePayment);

        assertEquals("SN",            display.countryCode());
        assertEquals("XOF",           display.currency());
        // XOF est une devise sans décimales (comme JPY) : setScale(0, HALF_UP)
        // 49,99 × 655,957 = 32 791,29 → arrondi entier = 32 791
        assertEquals(new BigDecimal("32791"), priceDisplay);
        assertEquals("fixed",         display.rateSource());

        assertEquals("EUR",           payment.currency());        // XOF hors Stripe → EUR
        assertEquals(new BigDecimal("49.99"), pricePayment);      // .99 inchangé
    }

    // ── Côte d'Ivoire (CI) — même zone XOF ───────────────────────────────────

    @Test
    void coteDivoire_xof_memeZoneCfa() {
        mockGeo("CI", "XOF");
        when(fx.getRate("XOF")).thenReturn(Optional.empty());

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        print("Côte d'Ivoire (CI)", display, priceDisplay,
                service.resolveForPayment(display),
                service.convertFromEur(CATALOG_EUR, service.resolveForPayment(display)));

        assertEquals("XOF", display.currency());
        assertEquals(new BigDecimal("32791"), priceDisplay);
    }

    // ── Maroc (MA) — MAD, couvert par Frankfurter ────────────────────────────

    @Test
    void maroc_mad_frankfurter() {
        mockGeo("MA", "MAD");
        when(fx.getRate("MAD")).thenReturn(Optional.of(fxRate("10.80", "10.97", "0.015", "frankfurter")));

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        PricingContext payment = service.resolveForPayment(display);
        BigDecimal pricePayment = service.convertFromEur(CATALOG_EUR, payment);

        print("Maroc (MA)", display, priceDisplay, payment, pricePayment);

        assertEquals("MAD", display.currency());
        // 49,99 × 10,97 = 548,39 → lastTwo=39, pas 0 ni 50 → inchangé
        assertEquals(new BigDecimal("548.39"), priceDisplay);
        assertEquals("EUR", payment.currency()); // MAD hors Stripe → EUR
    }

    // ── Nigeria (NG) — NGN, couvert par Frankfurter ──────────────────────────

    @Test
    void nigeria_ngn_frankfurter() {
        mockGeo("NG", "NGN");
        when(fx.getRate("NGN")).thenReturn(Optional.of(fxRate("1620", "1644.30", "0.015", "frankfurter")));

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        PricingContext payment = service.resolveForPayment(display);

        print("Nigeria (NG)", display, priceDisplay, payment,
                service.convertFromEur(CATALOG_EUR, payment));

        assertEquals("NGN", display.currency());
        // 49,99 × 1644,30 = 82 198,557 → arrondi au 5 sup. = 82 200,00
        assertEquals(new BigDecimal("82200.00"), priceDisplay);
        assertEquals("EUR", payment.currency()); // NGN hors Stripe → EUR
    }

    // ── Canada (CA) — CAD, dans Frankfurter ET dans Stripe ───────────────────

    @Test
    void canada_cad_paiement_direct_cad() {
        mockGeo("CA", "CAD");
        when(fx.getRate("CAD")).thenReturn(Optional.of(fxRate("1.48", "1.502", "0.015", "frankfurter")));

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        PricingContext payment = service.resolveForPayment(display);
        BigDecimal pricePayment = service.convertFromEur(CATALOG_EUR, payment);

        print("Canada (CA)", display, priceDisplay, payment, pricePayment);

        assertEquals("CAD", display.currency());
        // 49,99 × 1,502 = 75,085 → 75,08 → lastTwo=8, pas 0 ni 50 → inchangé
        assertEquals(new BigDecimal("75.08"), priceDisplay);
        assertEquals("CAD", payment.currency()); // CAD supporté par Stripe → pas de fallback
        assertEquals(priceDisplay, pricePayment); // affichage = paiement
    }

    // ── France (FR) — fallback pays, EUR ─────────────────────────────────────

    @Test
    void france_fallback_eur() {
        // geo ne retourne rien (CF hors ligne, API indisponible)
        when(geo.resolve(request)).thenReturn(Optional.empty());

        PricingContext display = service.resolve(request);
        BigDecimal priceDisplay = service.convertFromEur(CATALOG_EUR, display);

        print("France (FR – fallback)", display, priceDisplay,
                service.resolveForPayment(display), priceDisplay);

        assertEquals("EUR", display.currency());
        // 49,99 EUR → lastTwo=99, pas 0 ni 50 → inchangé
        assertEquals(new BigDecimal("49.99"), priceDisplay);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockGeo(String country, String currency) {
        when(geo.resolve(request)).thenReturn(Optional.of(new GeoResolution(country, currency)));
    }

    private static FxRateCacheService.FxRate fxRate(
            String raw, String effective, String margin, String source) {
        return new FxRateCacheService.FxRate(
                new BigDecimal(raw), new BigDecimal(effective),
                new BigDecimal(margin), source);
    }

    private static void addRegion(RegionalPricingProperties props,
            String country, String currency, String rate) {
        var r = new RegionalPricingProperties.RegionRate();
        r.setCurrency(currency);
        r.setEurRate(new BigDecimal(rate));
        props.getRegion().put(country, r);
    }

    private static void print(String label,
            PricingContext display, BigDecimal displayPrice,
            PricingContext payment, BigDecimal payPrice) {
        log.info("┌─ {} ─────────────────────────", label);
        log.info("│  Pays détecté  : {}", display.countryCode());
        log.info("│  Devise affichée : {} (source taux : {})", display.currency(), display.rateSource());
        log.info("│ Taux EUR->{} : {}", display.currency(), display.eurToTargetRate());
        log.info("│  Prix catalogue : {} EUR", "49.99");
        log.info("│  Prix affiché   : {} {}", displayPrice, display.currency());
        log.info("│  ─── Paiement Stripe ────────────────");
        log.info("│  Devise paiement : {}", payment.currency());
        log.info("│  Montant Stripe  : {} {}", payPrice, payment.currency());
        if (!display.currency().equals(payment.currency())) {
            log.info("│ Devise affichée ≠ devise facturée (Stripe ne supporte pas {})",
                    display.currency());
        }
        log.info("└─────────────────────────────────────");
    }
}
