package com.lmp.shared.geo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour {@link VpnDetectionService}.
 *
 * <p>Les 3 sources (ip-api.com, GetIPIntel, IPHub) sont mockées
 * pour tester la logique de consensus pondéré sans appels réseau.
 */
class VpnDetectionServiceTest {

    private IpApiComGeoService ipApiComService;
    private GetIPIntelService getIPIntelService;
    private IPHubService ipHubService;
    private VpnDetectionService service;

    @BeforeEach
    void setUp() {
        ipApiComService = mock(IpApiComGeoService.class);
        getIPIntelService = mock(GetIPIntelService.class);
        ipHubService = mock(IPHubService.class);
        service = new VpnDetectionService(ipApiComService, getIPIntelService, ipHubService);
    }

    /** Helper: crée un GeoResolution avec vpnDetected. */
    private GeoResolution geoWithVpn(boolean vpn) {
        return new GeoResolution("US", "USD", vpn);
    }

    /** Helper: crée un IPHubResult avec seulement le block flag. */
    private IPHubService.IPHubResult ipHubResult(int block) {
        return new IPHubService.IPHubResult(block, "US", "ISP", block == 1, false, false);
    }

    // =========================================================================
    // All sources agree
    // =========================================================================

    @Nested
    @DisplayName("Consensus — toutes les sources concordent")
    class AllSourcesAgree {

        @Test
        void allSources_clean_lowScore() {
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(false)));
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.05));
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(0)));

            var result = service.check("1.2.3.4");

            assertTrue(result.normalizedScore() < 0.1,
                    "All clean → score should be near 0, got " + result.normalizedScore());
            assertFalse(result.vpnDetected());
        }

        @Test
        void allSources_vpn_highScore() {
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(true)));
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.99));
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(1)));

            var result = service.check("1.2.3.4");

            assertTrue(result.normalizedScore() > 0.9,
                    "All VPN → score should be near 1.0, got " + result.normalizedScore());
            assertTrue(result.vpnDetected());
        }
    }

    // =========================================================================
    // Partial sources
    // =========================================================================

    @Nested
    @DisplayName("Sources partielles — certaines échouent")
    class PartialSources {

        @Test
        void onlyGetIPIntel_responds_usesItsScore() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.of(0.85));
            when(ipHubService.lookup(anyString())).thenReturn(Optional.empty());

            var result = service.check("1.2.3.4");

            // Only GetIPIntel with weight 0.40 → normalized = 0.85
            assertEquals(0.85, result.normalizedScore(), 0.01);
            assertTrue(result.vpnDetected());
        }

        @Test
        void onlyIpApi_responds_clean() {
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(false)));
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString())).thenReturn(Optional.empty());

            var result = service.check("1.2.3.4");

            assertEquals(0.0, result.normalizedScore(), 0.01);
            assertFalse(result.vpnDetected());
        }

        @Test
        void noSources_respond_scoreIsZero() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString())).thenReturn(Optional.empty());

            var result = service.check("1.2.3.4");

            assertEquals(0.0, result.normalizedScore());
            assertFalse(result.vpnDetected());
        }
    }

    // =========================================================================
    // Weight distribution
    // =========================================================================

    @Nested
    @DisplayName("Pondération — poids des sources")
    class WeightDistribution {

        @Test
        void ipApi_dilutes_vpnScore_when2of3_clean() {
            // ip-api says VPN (weight 0.25), GetIPIntel says clean (0.40), IPHub says clean (0.35)
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(true)));
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.1));
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(0)));

            var result = service.check("1.2.3.4");

            // Weighted: (1.0*0.25 + 0.1*0.40 + 0.0*0.35) / 1.0 = 0.29
            assertTrue(result.normalizedScore() < 0.4,
                    "ip-api alone shouldn't make it VPN, got " + result.normalizedScore());
            assertFalse(result.vpnDetected());
        }

        @Test
        void getIPIntel_and_ipHub_agree_vpn_despite_ipApi_clean() {
            // ip-api says clean (0.25), GetIPIntel says VPN 0.95 (0.40), IPHub says VPN block=1 (0.35)
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(false)));
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.95));
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(1)));

            var result = service.check("1.2.3.4");

            // Weighted: (0.0*0.25 + 0.95*0.40 + 1.0*0.35) / 1.0 = 0.73
            assertTrue(result.normalizedScore() > 0.6,
                    "2/3 VPN sources should trigger detection, got " + result.normalizedScore());
            assertTrue(result.vpnDetected());
        }
    }

    // =========================================================================
    // IPHub block values
    // =========================================================================

    @Nested
    @DisplayName("IPHub — valeurs du flag block")
    class IPHubBlockValues {

        @Test
        void block0_residential_scoreZero() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(0)));

            var result = service.check("1.2.3.4");
            assertEquals(0.0, result.normalizedScore(), 0.01);
        }

        @Test
        void block1_nonResidential_scoreOne() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(1)));

            var result = service.check("1.2.3.4");
            assertEquals(1.0, result.normalizedScore(), 0.01);
        }

        @Test
        void block2_warning_scoreSixty() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(2)));

            var result = service.check("1.2.3.4");
            assertEquals(0.6, result.normalizedScore(), 0.01);
        }
    }

    // =========================================================================
    // Sources string
    // =========================================================================

    @Nested
    @DisplayName("Sources — chaîne d'audit")
    class SourcesString {

        @Test
        void allSources_respond_sourcesContainsAll() {
            when(ipApiComService.lookup(anyString()))
                    .thenReturn(Optional.of(geoWithVpn(false)));
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.1));
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(0)));

            var result = service.check("1.2.3.4");

            assertTrue(result.sources().contains("ip-api:"));
            assertTrue(result.sources().contains("getipintel:"));
            assertTrue(result.sources().contains("iphub:"));
        }

        @Test
        void noSources_respond_sourcesEmpty() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString())).thenReturn(Optional.empty());

            var result = service.check("1.2.3.4");
            assertTrue(result.sources().isEmpty());
        }
    }

    // =========================================================================
    // VPN detection threshold
    // =========================================================================

    @Nested
    @DisplayName("Seuil — vpnDetected à 0.6")
    class DetectionThreshold {

        @Test
        void scoreExactly060_notDetected() {
            // Only IPHub block=2 → score=0.6 → NOT detected (> 0.6, not >=)
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString())).thenReturn(Optional.empty());
            when(ipHubService.lookup(anyString()))
                    .thenReturn(Optional.of(ipHubResult(2)));

            var result = service.check("1.2.3.4");
            // score = 0.6 exactly → vpnDetected is false (> 0.6, not >=)
            assertFalse(result.vpnDetected());
        }

        @Test
        void scoreAbove060_detected() {
            when(ipApiComService.lookup(anyString())).thenReturn(Optional.empty());
            when(getIPIntelService.lookupVpnScore(anyString()))
                    .thenReturn(Optional.of(0.80));
            when(ipHubService.lookup(anyString())).thenReturn(Optional.empty());

            var result = service.check("1.2.3.4");
            assertTrue(result.vpnDetected());
        }
    }
}
