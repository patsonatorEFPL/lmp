package com.lmp.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class VatIdentifierUtilsTest {

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "   ", "\t", "\n " })
    void normalize_nullOrBlank_returnsEmpty(String raw) {
        assertEquals("", VatIdentifierUtils.normalize(raw));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "fr12345678901|FR12345678901",
            "FR12345678901|FR12345678901",
            "be 0123.456.789|BE0123456789",
            "DE 123-456-789|DE123456789",
            "at u12345678|ATU12345678",
            "  nl 123456789b01  |NL123456789B01",
    })
    void normalize_stripsSeparatorsAndUppercases(String raw, String expected) {
        assertEquals(expected, VatIdentifierUtils.normalize(raw));
    }

    @Test
    void isPlausibleEuVatFormat_null_returnsFalse() {
        assertFalse(VatIdentifierUtils.isPlausibleEuVatFormat(null));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "F", "FR", "FR1", "1", "1234", "F1", "X" })
    void isPlausibleEuVatFormat_tooShortOrInvalidPrefix_returnsFalse(String normalized) {
        assertFalse(VatIdentifierUtils.isPlausibleEuVatFormat(normalized));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FR12",
            "FR12345678901",
            "BE0123456789",
            "DE123456789",
            "ATU12345678",
            "NL123456789B01",
            "EL123456789",
    })
    void isPlausibleEuVatFormat_validSamples_returnsTrue(String normalized) {
        assertTrue(VatIdentifierUtils.isPlausibleEuVatFormat(normalized));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "F12345678901",
            "123456789012",
            "FR",
            "FRX",
            "fr12345678901",
            "FR 12",
            "FR12@34",
    })
    void isPlausibleEuVatFormat_invalid_returnsFalse(String normalized) {
        assertFalse(VatIdentifierUtils.isPlausibleEuVatFormat(normalized));
    }

    @Test
    void isPlausibleEuVatFormat_nationalPartTooLong_returnsFalse() {
        String national28 = "A".repeat(28);
        assertTrue(VatIdentifierUtils.isPlausibleEuVatFormat("FR" + national28));

        String national29 = "A".repeat(29);
        assertFalse(VatIdentifierUtils.isPlausibleEuVatFormat("FR" + national29));
    }

    @Test
    void normalize_then_isPlausibleEuVatFormat_acceptsTypicalInput() {
        String raw = " be 0.123.456.789 ";
        String n = VatIdentifierUtils.normalize(raw);
        assertEquals("BE0123456789", n);
        assertTrue(VatIdentifierUtils.isPlausibleEuVatFormat(n));
    }
}
