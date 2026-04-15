package com.lmp.shared.web.debug;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;
import com.lmp.shared.pricing.PricingContext;
import com.lmp.shared.pricing.RegionalPricingService;
import com.lmp.shared.pricing.VatCalculationService;
import com.lmp.shared.pricing.VatRateLookupService;
import com.lmp.shared.web.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * TEMPORARY diagnostic endpoint for debugging VAT resolution on dev/prod servers.
 * DELETE THIS FILE once the issue is resolved.
 *
 * Authentication required (any logged-in user).
 */
@RestController
@RequestMapping("/api/v1/payments/vat-diagnostic")
class VatDiagnosticController {

    private final GeoCountryLookupService geoCountryLookupService;
    private final RegionalPricingService regionalPricingService;
    private final VatCalculationService vatCalculationService;
    private final VatRateLookupService vatRateLookupService;

    VatDiagnosticController(GeoCountryLookupService geoCountryLookupService,
                            RegionalPricingService regionalPricingService,
                            VatCalculationService vatCalculationService,
                            VatRateLookupService vatRateLookupService) {
        this.geoCountryLookupService = geoCountryLookupService;
        this.regionalPricingService = regionalPricingService;
        this.vatCalculationService = vatCalculationService;
        this.vatRateLookupService = vatRateLookupService;
    }

    @GetMapping
    ResponseEntity<Map<String, Object>> diagnose(
            HttpServletRequest request,
            Authentication authentication,
            @RequestParam(defaultValue = "NL") String testCountry) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Raw IP
        String ip = ClientIpResolver.resolve(request);
        result.put("ip", ip);
        result.put("cf_ip_country", request.getHeader("CF-IPCountry"));

        // 2. GeoCountryLookupService resolution
        var geoOpt = geoCountryLookupService.resolve(request);
        Map<String, Object> geoMap = new LinkedHashMap<>();
        if (geoOpt.isPresent()) {
            GeoResolution geo = geoOpt.get();
            geoMap.put("countryCode", geo.countryCode());
            geoMap.put("currencyCode", geo.currencyCode());
        } else {
            geoMap.put("countryCode", null);
            geoMap.put("note", "resolve() returned empty — fallback-country will be used");
        }
        result.put("geo_lookup", geoMap);

        // 3. RegionalPricingService resolution
        PricingContext displayCtx = regionalPricingService.resolve(request);
        PricingContext payCtx = regionalPricingService.resolveForPayment(displayCtx);
        Map<String, Object> pricingMap = new LinkedHashMap<>();
        pricingMap.put("display_country", displayCtx.countryCode());
        pricingMap.put("display_currency", displayCtx.currency());
        pricingMap.put("pay_country", payCtx.countryCode());
        pricingMap.put("pay_currency", payCtx.currency());
        result.put("pricing_context", pricingMap);

        // 4. VAT rate for resolved country
        String resolvedCountry = payCtx.countryCode();
        Map<String, Object> vatResolved = new LinkedHashMap<>();
        vatResolved.put("country_used", resolvedCountry);
        vatResolved.put("vat_rate", vatCalculationService.getVatRate(resolvedCountry));
        vatResolved.put("is_eu", vatRateLookupService.isEuCountry(resolvedCountry));
        vatResolved.put("has_rate", vatRateLookupService.hasRate(resolvedCountry));
        result.put("vat_for_resolved_country", vatResolved);

        // 5. VAT rate for testCountry (explicit check)
        Map<String, Object> vatTest = new LinkedHashMap<>();
        vatTest.put("country", testCountry);
        vatTest.put("vat_rate", vatCalculationService.getVatRate(testCountry));
        vatTest.put("vat_rate_pct", vatCalculationService.getVatRate(testCountry)
                .multiply(new BigDecimal("100")).intValue());
        vatTest.put("is_eu", vatRateLookupService.isEuCountry(testCountry));
        vatTest.put("has_rate", vatRateLookupService.hasRate(testCountry));
        result.put("vat_for_test_country", vatTest);

        // 6. VAT cache info
        Map<String, Object> cacheMap = new LinkedHashMap<>();
        cacheMap.put("cache_size", vatRateLookupService.getCacheSize());
        cacheMap.put("last_refreshed", vatRateLookupService.getLastRefreshed());
        // Check specific countries
        for (String cc : new String[]{"NL", "FR", "DE", "BE", "IT", "ES"}) {
            cacheMap.put("rate_" + cc, vatCalculationService.getVatRate(cc));
        }
        result.put("vat_cache", cacheMap);

        result.put("_note", "TEMPORARY ENDPOINT — delete VatDiagnosticController.java after debugging");

        return ResponseEntity.ok(result);
    }
}
