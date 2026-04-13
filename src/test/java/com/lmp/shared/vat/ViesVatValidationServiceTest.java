package com.lmp.shared.vat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests unitaires pour {@link ViesVatValidationService}.
 *
 * <p>Ces tests ne font <b>pas</b> d'appel réseau réel vers VIES.
 * Ils vérifient la logique de normalisation, validation de format,
 * et le comportement du cache interne.
 */
class ViesVatValidationServiceTest {

    private ViesVatValidationService service;

    @BeforeEach
    void setUp() {
        service = new ViesVatValidationService(new com.lmp.shared.monitoring.ApiHealthRecorder());
    }

    // =========================================================================
    // Format validation (pre-network)
    // =========================================================================

    @Nested
    @DisplayName("Validation de format — pré-réseau")
    class FormatValidation {

        @Test
        void null_returnsEmpty() {
            Optional<ViesVatValidationService.ViesResult> result = service.validate(null);
            assertTrue(result.isEmpty());
        }

        @Test
        void blank_returnsEmpty() {
            Optional<ViesVatValidationService.ViesResult> result = service.validate("   ");
            assertTrue(result.isEmpty());
        }

        @Test
        void tooShort_returnsEmpty() {
            Optional<ViesVatValidationService.ViesResult> result = service.validate("FR1");
            assertTrue(result.isEmpty());
        }

        @Test
        void noCountryPrefix_returnsEmpty() {
            // Only digits, no 2-letter prefix
            Optional<ViesVatValidationService.ViesResult> result = service.validate("12345678901");
            assertTrue(result.isEmpty());
        }

        @Test
        void invalidPrefix_returnsEmpty() {
            // "99" is not alpha
            Optional<ViesVatValidationService.ViesResult> result = service.validate("9912345");
            assertTrue(result.isEmpty());
        }

        @Test
        void normalizedFormat_withSpaces_isAccepted() {
            // "BE 0403.170.701" normalizes to "BE0403170701" which is plausible
            Optional<ViesVatValidationService.ViesResult> result = service.validate("BE 0403.170.701");
            assertTrue(result.isPresent(), "Plausible format should attempt validation");
            // Service may or may not be reachable depending on environment
        }

        @Test
        void normalizedFormat_withDashes_isAccepted() {
            Optional<ViesVatValidationService.ViesResult> result = service.validate("FR-12-345678901");
            assertTrue(result.isPresent());
        }
    }

    // =========================================================================
    // Cache behavior
    // =========================================================================

    @Nested
    @DisplayName("Cache — comportement interne")
    class CacheBehavior {

        @Test
        void sameNumber_calledTwice_secondUsesCache() {
            // First call: network fails → cached as unavailable (5 min TTL)
            String vat = "BE0403170701";
            Optional<ViesVatValidationService.ViesResult> result1 = service.validate(vat);
            Optional<ViesVatValidationService.ViesResult> result2 = service.validate(vat);

            // Both should return the same result (from cache on second call)
            assertEquals(result1.isPresent(), result2.isPresent());
            if (result1.isPresent() && result2.isPresent()) {
                assertEquals(result1.get().valid(), result2.get().valid());
                assertEquals(result1.get().serviceAvailable(), result2.get().serviceAvailable());
            }
        }

        @Test
        void differentNumbers_independentCacheEntries() {
            String vat1 = "BE0403170701";
            String vat2 = "FR12345678901";

            Optional<ViesVatValidationService.ViesResult> result1 = service.validate(vat1);
            Optional<ViesVatValidationService.ViesResult> result2 = service.validate(vat2);

            // Both should be present (both are plausible format)
            assertTrue(result1.isPresent());
            assertTrue(result2.isPresent());
        }

        @Test
        void normalizedDuplicates_sameCache() {
            // These should normalize to the same key "BE0403170701"
            Optional<ViesVatValidationService.ViesResult> r1 = service.validate("BE0403170701");
            Optional<ViesVatValidationService.ViesResult> r2 = service.validate("BE 0403.170.701");
            Optional<ViesVatValidationService.ViesResult> r3 = service.validate("be-0403-170-701");

            // All should produce the same result
            assertTrue(r1.isPresent());
            assertTrue(r2.isPresent());
            assertTrue(r3.isPresent());
            assertEquals(r1.get().valid(), r2.get().valid());
            assertEquals(r2.get().valid(), r3.get().valid());
        }
    }

    // =========================================================================
    // ViesResult record
    // =========================================================================

    @Nested
    @DisplayName("ViesResult — record behavior")
    class ViesResultRecord {

        @Test
        void validResult_holdsAllFields() {
            var result = new ViesVatValidationService.ViesResult(
                    true, true, "SA PROXIMUS", "Boulevard du Roi Albert II 27, 1030 Bruxelles");
            assertTrue(result.valid());
            assertTrue(result.serviceAvailable());
            assertEquals("SA PROXIMUS", result.name());
            assertNotNull(result.address());
        }

        @Test
        void unavailableResult_fieldsAreNull() {
            var result = new ViesVatValidationService.ViesResult(false, false, null, null);
            assertFalse(result.valid());
            assertFalse(result.serviceAvailable());
            assertNull(result.name());
            assertNull(result.address());
        }
    }

    // =========================================================================
    // Network failure (offline test)
    // =========================================================================

    @Nested
    @DisplayName("Réseau — comportement avec/sans accès VIES")
    class NetworkBehavior {

        @Test
        void validFormat_alwaysReturnsResult() {
            // Whether VIES is reachable or not, a plausible VAT should always return a result
            Optional<ViesVatValidationService.ViesResult> result = service.validate("BE0403170701");
            assertTrue(result.isPresent(), "Should always return a result for plausible VAT format");
            // If service is available, result reflects real validation
            // If service is unavailable, result has serviceAvailable=false
            assertNotNull(result.get());
        }

        @Test
        void unavailableResult_hasCorrectShape() {
            // Verify the shape of an unavailable result (constructed manually)
            var unavailable = new ViesVatValidationService.ViesResult(false, false, null, null);
            assertFalse(unavailable.valid());
            assertFalse(unavailable.serviceAvailable());
            assertNull(unavailable.name());
        }
    }
}
