package com.lmp.shared.geo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour {@link FraudScoringService}.
 *
 * <p>Vérifie chaque règle de scoring individuellement ainsi que les
 * scénarios d'intégration (tout match / tout mismatch).
 *
 * <p>Aucune dépendance réseau — tests purement algorithmiques.
 */
class FraudScoringServiceTest {

    private FraudScoringService service;

    @BeforeEach
    void setUp() {
        service = new FraudScoringService();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Crée des signaux "clean" : tout cohérent, pas de VPN, pas de TVA. */
    private FraudScoringService.FraudSignals cleanSignals() {
        return new FraudScoringService.FraudSignals(
                "FR", 0.0, "Europe/Paris", "FR", "FR", null,
                false, null, null, null, false);
    }

    /** Crée des signaux minimaux avec uniquement ipCountry. */
    private FraudScoringService.FraudSignals minimalSignals(String ipCountry) {
        return new FraudScoringService.FraudSignals(
                ipCountry, 0.0, null, null, null, null,
                false, null, null, null, false);
    }

    // =========================================================================
    // 1. VPN Detection
    // =========================================================================

    @Nested
    @DisplayName("Règle 1 — VPN Detection")
    class VpnDetection {

        @Test
        void vpnScore_above085_penaltyHigh() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.90, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VPN_DETECTED_HIGH"));
        }

        @Test
        void vpnScore_above060_penaltyMedium() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.70, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VPN_DETECTED_MEDIUM"));
            assertFalse(result.flags().contains("VPN_DETECTED_HIGH"));
        }

        @Test
        void vpnScore_above040_suspected() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.50, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VPN_SUSPECTED"));
            assertFalse(result.flags().contains("VPN_DETECTED_MEDIUM"));
        }

        @Test
        void vpnScore_below040_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.20, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VPN_SUSPECTED"));
            assertFalse(result.flags().contains("VPN_DETECTED_MEDIUM"));
            assertFalse(result.flags().contains("VPN_DETECTED_HIGH"));
        }
    }

    // =========================================================================
    // 2. Timezone Mismatch
    // =========================================================================

    @Nested
    @DisplayName("Règle 2 — Timezone Mismatch")
    class TimezoneMismatch {

        @Test
        void tz_matches_ipCountry_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, "Europe/Paris", null, null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("TZ_MISMATCH"));
        }

        @Test
        void tz_doesNotMatch_ipCountry_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, "America/New_York", null, null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("TZ_MISMATCH"));
        }

        @Test
        void tz_null_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("TZ_MISMATCH"));
        }

        @Test
        void tz_unmappedCountry_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "ZZ", 0.0, "Asia/Tokyo", null, null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("TZ_MISMATCH"));
        }
    }

    // =========================================================================
    // 3. Billing country vs IP country
    // =========================================================================

    @Nested
    @DisplayName("Règle 3 — Billing vs IP Country")
    class BillingIpMismatch {

        @Test
        void billing_matches_ip_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("BILLING_IP_MISMATCH"));
        }

        @Test
        void billing_differs_from_ip_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "DE", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("BILLING_IP_MISMATCH"));
        }

        @Test
        void billing_null_noFlag() {
            var signals = minimalSignals("FR");
            var result = service.score(signals);
            assertFalse(result.flags().contains("BILLING_IP_MISMATCH"));
        }
    }

    // =========================================================================
    // 4. Geolocation vs Billing
    // =========================================================================

    @Nested
    @DisplayName("Règle 4 — Geo vs Billing Country")
    class GeoBillingMismatch {

        @Test
        void geo_matches_billing_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("GEO_BILLING_MISMATCH"));
        }

        @Test
        void geo_differs_from_billing_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, "BE", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("GEO_BILLING_MISMATCH"));
        }
    }

    // =========================================================================
    // 5. Geolocation vs IP
    // =========================================================================

    @Nested
    @DisplayName("Règle 5 — Geo vs IP Country")
    class GeoIpMismatch {

        @Test
        void geo_matches_ip_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, "FR", null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("GEO_IP_MISMATCH"));
        }

        @Test
        void geo_differs_from_ip_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, "US", null, null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("GEO_IP_MISMATCH"));
        }
    }

    // =========================================================================
    // 6. Geolocation denied
    // =========================================================================

    @Nested
    @DisplayName("Règle 6 — Geolocation Denied")
    class GeolocationDenied {

        @Test
        void geoDenied_withVpn_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.50, null, null, "FR", null,
                    true, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("GEO_DENIED_WITH_VPN"));
        }

        @Test
        void geoDenied_withMismatch_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "DE", null,
                    true, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("GEO_DENIED_WITH_MISMATCH"));
        }

        @Test
        void geoDenied_noVpn_noMismatch_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", null,
                    true, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("GEO_DENIED_WITH_VPN"));
            assertFalse(result.flags().contains("GEO_DENIED_WITH_MISMATCH"));
        }

        @Test
        void geoNotDenied_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.50, null, null, "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("GEO_DENIED_WITH_VPN"));
        }
    }

    // =========================================================================
    // 7. Card country vs Billing country
    // =========================================================================

    @Nested
    @DisplayName("Règle 7 — Card vs Billing Country")
    class CardBillingMismatch {

        @Test
        void card_matches_billing_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", "FR",
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("CARD_BILLING_MISMATCH"));
        }

        @Test
        void card_differs_from_billing_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", "DE",
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("CARD_BILLING_MISMATCH"));
        }

        @Test
        void card_null_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("CARD_BILLING_MISMATCH"));
        }
    }

    // =========================================================================
    // 8. VAT country vs Billing country
    // =========================================================================

    @Nested
    @DisplayName("Règle 8 — VAT vs Billing Country")
    class VatBillingMismatch {

        @Test
        void vatCountry_matches_billing_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VAT_BILLING_COUNTRY_MISMATCH"));
        }

        @Test
        void vatCountry_differs_from_billing_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "FR", null,
                    false, "BE", null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VAT_BILLING_COUNTRY_MISMATCH"));
        }
    }

    // =========================================================================
    // 9. VAT country vs IP country
    // =========================================================================

    @Nested
    @DisplayName("Règle 9 — VAT vs IP Country")
    class VatIpMismatch {

        @Test
        void vatCountry_matches_ip_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, null, null,
                    false, "BE", null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VAT_IP_COUNTRY_MISMATCH"));
        }

        @Test
        void vatCountry_differs_from_ip_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, null, null,
                    false, "BE", null, null, false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VAT_IP_COUNTRY_MISMATCH"));
        }
    }

    // =========================================================================
    // 10. VIES company name vs billing name (Jaccard)
    // =========================================================================

    @Nested
    @DisplayName("Règle 10 — VAT Name Matching (Jaccard)")
    class VatNameMatching {

        @Test
        void exactMatch_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", "SA PROXIMUS", "PROXIMUS", false);
            var result = service.score(signals);
            // "PROXIMUS" tokens match after stripping "SA" → Jaccard = 1.0
            assertFalse(result.flags().contains("VAT_NAME_MISMATCH"));
        }

        @Test
        void totalMismatch_strongPenalty() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", "SA PROXIMUS", "Jean Dupont", false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VAT_NAME_MISMATCH"));
        }

        @Test
        void partialMatch_commonToken_reducedPenalty() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", "SA PROXIMUS", "DPU Proximus Group International", false);
            var result = service.score(signals);
            // Jaccard < 0.4 but common token "PROXIMUS" exists
            assertTrue(result.flags().contains("VAT_NAME_PARTIAL_MATCH"));
            assertFalse(result.flags().contains("VAT_NAME_MISMATCH"));
        }

        @Test
        void weakMatch_moderateSimilarity() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", "PROXIMUS GROUP", "PROXIMUS CONSULTING", false);
            var result = service.score(signals);
            // Jaccard ~0.33–0.5 depending on tokens, but common token exists
            boolean hasWeakOrPartial = result.flags().contains("VAT_NAME_WEAK_MATCH")
                    || result.flags().contains("VAT_NAME_PARTIAL_MATCH");
            assertTrue(hasWeakOrPartial);
        }

        @Test
        void bothNull_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", null, null, false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VAT_NAME_MISMATCH"));
        }

        @Test
        void viesNameNull_billingNamePresent_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", null, "Proximus", false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VAT_NAME_MISMATCH"));
        }
    }

    // =========================================================================
    // 11. VIES unavailable
    // =========================================================================

    @Nested
    @DisplayName("Règle 11 — VIES Unavailable")
    class ViesUnavailable {

        @Test
        void viesUnavailable_withVat_flag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", null, null, true);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VIES_UNAVAILABLE"));
        }

        @Test
        void viesUnavailable_withoutVat_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.0, null, null, "FR", null,
                    false, null, null, null, true);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VIES_UNAVAILABLE"));
        }

        @Test
        void viesAvailable_noFlag() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, null, null, "BE", null,
                    false, "BE", "PROXIMUS", "PROXIMUS", false);
            var result = service.score(signals);
            assertFalse(result.flags().contains("VIES_UNAVAILABLE"));
        }
    }

    // =========================================================================
    // Bonus rules
    // =========================================================================

    @Nested
    @DisplayName("Bonus — Countries Match + Low VPN")
    class Bonus {

        @Test
        void allCountriesMatch_lowVpn_scoreAbove100Clamped() {
            var signals = cleanSignals();
            var result = service.score(signals);
            // 100 + 5 (all match) + 5 (no vpn + tz match) → capped at 100
            assertEquals(100, result.score());
            assertFalse(result.alert());
        }

        @Test
        void allCountriesMatch_highVpn_noBonus() {
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.90, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            // VPN_DETECTED_HIGH = -30, no bonus because vpnScore > 0.3
            assertTrue(result.score() <= 70);
        }
    }

    // =========================================================================
    // Score boundaries & alert
    // =========================================================================

    @Nested
    @DisplayName("Score — Boundaries & Alert Threshold")
    class ScoreBoundaries {

        @Test
        void cleanSignals_perfectScore() {
            var result = service.score(cleanSignals());
            assertEquals(100, result.score());
            assertFalse(result.alert());
            assertTrue(result.flags().isEmpty());
        }

        @Test
        void score_neverBelowZero() {
            // Maximum penalties: VPN_HIGH(-30) + TZ(-10) + BILLING_IP(-15) +
            // GEO_BILLING(-20) + GEO_IP(-10) + GEO_DENIED_VPN(-15) +
            // CARD_BILLING(-15) + VAT_BILLING(-15) + VAT_IP(-10) +
            // VAT_NAME_MISMATCH(-20) + VIES_UNAVAILABLE(-10) = -170
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.95, "America/New_York", "US", "DE", "JP",
                    true, "BE", "ACME CORP", "Jean Dupont", true);
            var result = service.score(signals);
            assertTrue(result.score() >= 0);
            assertTrue(result.alert());
        }

        @Test
        void score_neverAbove100() {
            var result = service.score(cleanSignals());
            assertTrue(result.score() <= 100);
        }

        @Test
        void alertThreshold_below60_isAlert() {
            // VPN_HIGH(-30) + BILLING_IP(-15) = -45 → score=55 < 60
            var signals = new FraudScoringService.FraudSignals(
                    "FR", 0.90, "Europe/Paris", null, "DE", null,
                    false, null, null, null, false);
            var result = service.score(signals);
            assertTrue(result.score() < FraudScoringService.ALERT_THRESHOLD);
            assertTrue(result.alert());
        }
    }

    // =========================================================================
    // Jaccard helper methods (package-private static)
    // =========================================================================

    @Nested
    @DisplayName("Helpers — Jaccard & Token matching")
    class JaccardHelpers {

        @Test
        void jaccardSimilarity_identical_returnsOne() {
            assertEquals(1.0, FraudScoringService.jaccardTokenSimilarity("PROXIMUS", "PROXIMUS"));
        }

        @Test
        void jaccardSimilarity_noOverlap_returnsZero() {
            assertEquals(0.0, FraudScoringService.jaccardTokenSimilarity("ALPHA", "BRAVO"));
        }

        @Test
        void jaccardSimilarity_legalSuffixIgnored() {
            // "SA" is stripped → both reduce to {"PROXIMUS"} → 1.0
            assertEquals(1.0, FraudScoringService.jaccardTokenSimilarity("SA PROXIMUS", "PROXIMUS"));
        }

        @Test
        void jaccardSimilarity_accentsNormalized() {
            double sim = FraudScoringService.jaccardTokenSimilarity("SOCIÉTÉ GÉNÉRALE", "SOCIETE GENERALE");
            assertEquals(1.0, sim);
        }

        @Test
        void jaccardSimilarity_null_returnsZero() {
            assertEquals(0.0, FraudScoringService.jaccardTokenSimilarity(null, "TEST"));
            assertEquals(0.0, FraudScoringService.jaccardTokenSimilarity("TEST", null));
        }

        @Test
        void hasAnyCommonToken_withCommon_returnsTrue() {
            assertTrue(FraudScoringService.hasAnyCommonToken("SA DPU PROXIMUS", "PROXIMUS GROUP"));
        }

        @Test
        void hasAnyCommonToken_noCommon_returnsFalse() {
            assertFalse(FraudScoringService.hasAnyCommonToken("ALPHA BETA", "GAMMA DELTA"));
        }

        @Test
        void hasAnyCommonToken_onlyLegalSuffix_returnsFalse() {
            // "SA" is legal suffix → stripped from both → no common token
            assertFalse(FraudScoringService.hasAnyCommonToken("SA", "SA"));
        }

        @Test
        void hasAnyCommonToken_null_returnsFalse() {
            assertFalse(FraudScoringService.hasAnyCommonToken(null, "TEST"));
        }
    }

    // =========================================================================
    // recalculateWithCardCountry
    // =========================================================================

    @Nested
    @DisplayName("recalculateWithCardCountry")
    class Recalculate {

        @Test
        void recalculate_addsCardCountry_andDetectsMismatch() {
            var original = new FraudScoringService.FraudSignals(
                    "FR", 0.0, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);

            var result = service.recalculateWithCardCountry(original, "DE");
            assertTrue(result.flags().contains("CARD_BILLING_MISMATCH"));
        }

        @Test
        void recalculate_cardMatchesBilling_noNewFlag() {
            var original = new FraudScoringService.FraudSignals(
                    "FR", 0.0, "Europe/Paris", "FR", "FR", null,
                    false, null, null, null, false);

            var result = service.recalculateWithCardCountry(original, "FR");
            assertFalse(result.flags().contains("CARD_BILLING_MISMATCH"));
        }
    }

    // =========================================================================
    // Integration scenarios
    // =========================================================================

    @Nested
    @DisplayName("Scénarios intégration")
    class IntegrationScenarios {

        @Test
        void scenario_legitimateBelgianCompany_highScore() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.05, "Europe/Brussels", "BE", "BE", "BE",
                    false, "BE", "SA PROXIMUS", "PROXIMUS", false);
            var result = service.score(signals);
            assertTrue(result.score() >= 90, "Legitimate company should score >= 90, got " + result.score());
            assertFalse(result.alert());
        }

        @Test
        void scenario_vpnFromDifferentCountry_lowScore() {
            var signals = new FraudScoringService.FraudSignals(
                    "US", 0.90, "Europe/Paris", null, "FR", null,
                    true, null, null, null, false);
            var result = service.score(signals);
            // VPN_HIGH(-30) + TZ(-10? US has multiple) + BILLING_IP(-15) + GEO_DENIED_WITH_VPN(-15)
            assertTrue(result.score() < FraudScoringService.ALERT_THRESHOLD,
                    "Suspicious signals should trigger alert, got score=" + result.score());
            assertTrue(result.alert());
        }

        @Test
        void scenario_stolenVatNumber_nameMismatch() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, "Europe/Brussels", "BE", "BE", null,
                    false, "BE", "SA PROXIMUS", "Jean Dupont Consulting", false);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VAT_NAME_MISMATCH"));
            // -20 for name mismatch, +5 all countries match, +5 no vpn+tz → score 90
            assertTrue(result.score() <= 95);
            assertTrue(result.score() < 100, "Name mismatch should reduce score from perfect");
        }

        @Test
        void scenario_viesDown_penalizedButNotCritical() {
            var signals = new FraudScoringService.FraudSignals(
                    "BE", 0.0, "Europe/Brussels", "BE", "BE", null,
                    false, "BE", null, "PROXIMUS", true);
            var result = service.score(signals);
            assertTrue(result.flags().contains("VIES_UNAVAILABLE"));
            // Only -10, rest is clean → should still be above threshold
            assertTrue(result.score() >= FraudScoringService.ALERT_THRESHOLD);
        }
    }
}
