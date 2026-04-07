package com.lmp.shared.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PsychologicalRounderTest {

    @Test
    void nullAmount_returnsNull() {
        assertNull(PsychologicalRounder.round(null, "EUR"));
    }

    @Test
    void jpy_roundsToInteger() {
        assertEquals(new BigDecimal("125"), PsychologicalRounder.round(new BigDecimal("124.7"), "JPY"));
    }

    @Test
    void endsWith00_becomes99() {
        assertEquals(new BigDecimal("24.99"), PsychologicalRounder.round(new BigDecimal("25.00"), "CAD"));
    }

    @Test
    void endsWith50_becomes49() {
        assertEquals(new BigDecimal("25.49"), PsychologicalRounder.round(new BigDecimal("25.50"), "CAD"));
    }

    @Test
    void largeAmount_roundsUpToMultipleOf5() {
        assertEquals(new BigDecimal("12350.00"), PsychologicalRounder.round(new BigDecimal("12347.00"), "USD"));
    }

    @Test
    void otherCents_unchangedAfterHalfUp() {
        assertEquals(new BigDecimal("25.33"), PsychologicalRounder.round(new BigDecimal("25.333"), "EUR"));
    }
}
