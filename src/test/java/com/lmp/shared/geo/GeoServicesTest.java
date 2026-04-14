package com.lmp.shared.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests unitaires sur les services géo (sans appel réseau réel).
 *
 * <p>Les appels réseau sont testés via des IPs privées/loopback
 * qui retournent immédiatement {@code Optional.empty()} sans I/O.
 */
class GeoServicesTest {

    private final com.lmp.shared.monitoring.ApiHealthRecorder recorder = new com.lmp.shared.monitoring.ApiHealthRecorder(null);
    private final IpWhoIsGeoService ipWhoIs = new IpWhoIsGeoService(recorder);
    private final IpApiComGeoService ipApiCom = new IpApiComGeoService("", recorder);

    // ── IpWhoIsGeoService ─────────────────────────────────────────────────────

    @Test
    void ipWhoIs_loopback_returnsEmpty() {
        assertTrue(ipWhoIs.lookup("127.0.0.1").isEmpty());
    }

    @Test
    void ipWhoIs_privateRange_returnsEmpty() {
        assertTrue(ipWhoIs.lookup("192.168.1.100").isEmpty());
    }

    @Test
    void ipWhoIs_null_returnsEmpty() {
        assertTrue(ipWhoIs.lookup(null).isEmpty());
    }

    @Test
    void ipWhoIs_blank_returnsEmpty() {
        assertTrue(ipWhoIs.lookup("  ").isEmpty());
    }

    // ── IpApiComGeoService ────────────────────────────────────────────────────

    @Test
    void ipApiCom_loopback_returnsEmpty() {
        assertTrue(ipApiCom.lookup("127.0.0.1").isEmpty());
    }

    @Test
    void ipApiCom_private10_returnsEmpty() {
        assertTrue(ipApiCom.lookup("10.0.0.1").isEmpty());
    }

    @Test
    void ipApiCom_null_returnsEmpty() {
        assertTrue(ipApiCom.lookup(null).isEmpty());
    }

    // ── GeoResolution ─────────────────────────────────────────────────────────

    @Test
    void geoResolution_countryOnly_hasnullCurrency() {
        GeoResolution r = GeoResolution.countryOnly("SN");
        assertEquals("SN", r.countryCode());
        assertFalse(r.currencyCode() != null, "currencyCode should be null for countryOnly");
    }

    @Test
    void geoResolution_full_holdsBothFields() {
        GeoResolution r = new GeoResolution("NG", "NGN");
        assertEquals("NG", r.countryCode());
        assertEquals("NGN", r.currencyCode());
    }
}
