package com.lmp.shared.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Charge {@link FxRateCacheService} avec des taux statiques uniquement (pas d'appel Frankfurter).
 *
 * SB 4 / Spring Framework 7 : @SpringBootTest(classes = {...}) déclenche
 * ImportsContextCustomizer qui tente de charger la classe test depuis le
 * classpath (échec). Refactor en @SpringJUnitConfig + @Configuration locale
 * pour bypasser le bootstrapper Spring Boot tout en gardant l'injection.
 */
@SpringJUnitConfig(classes = FxRateCacheServiceSpringTest.TestContext.class)
@TestPropertySource(properties = {
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

    @Configuration
    @EnableConfigurationProperties(RegionalPricingProperties.class)
    static class TestContext {

        @Bean
        FxRateCacheService fxRateCacheService(RegionalPricingProperties props,
                                              com.lmp.shared.monitoring.ApiHealthRecorder recorder) {
            return new FxRateCacheService(props, recorder);
        }

        @Bean
        com.lmp.shared.monitoring.ApiHealthRecorder apiHealthRecorder() {
            return new com.lmp.shared.monitoring.ApiHealthRecorder(apiHealthRecordRepository());
        }

        @Bean
        com.lmp.shared.monitoring.ApiHealthRecordRepository apiHealthRecordRepository() {
            return mock(com.lmp.shared.monitoring.ApiHealthRecordRepository.class);
        }
    }
}
