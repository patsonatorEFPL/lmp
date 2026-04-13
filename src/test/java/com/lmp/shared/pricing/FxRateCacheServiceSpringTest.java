package com.lmp.shared.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Charge {@link FxRateCacheService} avec des taux statiques uniquement (pas d'appel Frankfurter).
 */
@SpringBootTest(classes = { FxRateCacheService.class, com.lmp.shared.monitoring.ApiHealthRecorder.class })
@EnableConfigurationProperties(RegionalPricingProperties.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "pricing.fx-auto-refresh=false",
        "pricing.fx-margin=0.015",
        "pricing.region.CA.currency=CAD",
        "pricing.region.CA.eur-rate=1.48"
})
class FxRateCacheServiceSpringTest {

    @Autowired
    private FxRateCacheService fxRateCacheService;

    @Test
    void loadsStaticCadRateWithMargin() {
        Optional<FxRateCacheService.FxRate> opt = fxRateCacheService.getRate("CAD");
        assertTrue(opt.isPresent());
        FxRateCacheService.FxRate r = opt.get();
        assertEquals(new BigDecimal("1.48"), r.rawRate());
        assertEquals(new BigDecimal("1.502200"), r.effectiveRate());
        // source peut être "static" ou "fawaz" selon disponibilité réseau en test
        assertTrue(r.source() != null && !r.source().isBlank());
    }

    @Test
    void eurReturnsOne() {
        Optional<FxRateCacheService.FxRate> opt = fxRateCacheService.getRate("EUR");
        assertTrue(opt.isPresent());
        assertEquals(BigDecimal.ONE, opt.get().effectiveRate());
    }

    @Test
    void coverageSummaryNotEmpty() {
        String summary = fxRateCacheService.getCoverageSummary();
        assertTrue(summary.contains("total="));
    }
}
