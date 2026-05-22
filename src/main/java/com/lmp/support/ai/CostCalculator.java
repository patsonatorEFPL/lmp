package com.lmp.support.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * USD cost for one Anthropic API call from token counts and the model tier.
 * Pricing per 1M tokens, snapshot 2026-05. Update when Anthropic publishes new
 * tiers. Source of truth is the Anthropic pricing page; values inline here so the
 * service has no runtime dependency on it.
 */
@Component
public class CostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    public BigDecimal compute(int inTokens, int outTokens, String model) {
        if (inTokens < 0 || outTokens < 0) {
            throw new IllegalArgumentException("token counts must be non-negative");
        }
        BigDecimal inputPrice;
        BigDecimal outputPrice;
        if (model == null) {
            throw new IllegalArgumentException("model required");
        }
        if (model.startsWith("claude-opus-4")) {
            inputPrice = new BigDecimal("15.00");
            outputPrice = new BigDecimal("75.00");
        } else if (model.startsWith("claude-sonnet-4")) {
            inputPrice = new BigDecimal("3.00");
            outputPrice = new BigDecimal("15.00");
        } else if (model.startsWith("claude-haiku-4")) {
            inputPrice = new BigDecimal("0.80");
            outputPrice = new BigDecimal("4.00");
        } else {
            // Unknown model: fall back to Sonnet pricing to avoid silent under-billing
            inputPrice = new BigDecimal("3.00");
            outputPrice = new BigDecimal("15.00");
        }
        return inputPrice.multiply(BigDecimal.valueOf(inTokens))
            .add(outputPrice.multiply(BigDecimal.valueOf(outTokens)))
            .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
    }
}
