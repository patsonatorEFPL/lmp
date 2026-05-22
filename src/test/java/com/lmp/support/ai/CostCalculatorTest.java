package com.lmp.support.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostCalculatorTest {

    private final CostCalculator calc = new CostCalculator();

    @Test
    void opusPricingScalesWithTokens() {
        // 1M input @ $15 + 1M output @ $75 = $90.00
        BigDecimal cost = calc.compute(1_000_000, 1_000_000, "claude-opus-4-7");
        assertThat(cost).isEqualByComparingTo(new BigDecimal("90.000000"));
    }

    @Test
    void sonnetPricingIsLowerThanOpus() {
        BigDecimal opus = calc.compute(10_000, 1_000, "claude-opus-4-7");
        BigDecimal sonnet = calc.compute(10_000, 1_000, "claude-sonnet-4-6");
        assertThat(sonnet).isLessThan(opus);
    }

    @Test
    void haikuCheapest() {
        BigDecimal haiku = calc.compute(10_000, 1_000, "claude-haiku-4-5-20251001");
        BigDecimal sonnet = calc.compute(10_000, 1_000, "claude-sonnet-4-6");
        assertThat(haiku).isLessThan(sonnet);
    }

    @Test
    void zeroTokensCostsZero() {
        assertThat(calc.compute(0, 0, "claude-opus-4-7"))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void unknownModelFallsBackToSonnetTier() {
        BigDecimal unknown = calc.compute(10_000, 1_000, "claude-future-x");
        BigDecimal sonnet = calc.compute(10_000, 1_000, "claude-sonnet-4-6");
        assertThat(unknown).isEqualByComparingTo(sonnet);
    }

    @Test
    void negativeTokensRejected() {
        assertThatThrownBy(() -> calc.compute(-1, 0, "claude-opus-4-7"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.compute(0, -1, "claude-opus-4-7"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullModelRejected() {
        assertThatThrownBy(() -> calc.compute(1, 1, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void smallUsageRoundedToMicrocents() {
        // 100 input @ opus $15/M = 0.0015 USD → rounds to 0.001500
        BigDecimal cost = calc.compute(100, 0, "claude-opus-4-7");
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.001500"));
    }
}
