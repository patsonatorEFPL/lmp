package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Arrondi psychologique des prix de présentation.
 *
 * Règle appliquée sur les montants < 10 000 :
 *   - .00  → se termine par .99  (ex. 25.00 → 24.99)
 *   - .50  → se termine par .49  (ex. 25.50 → 25.49)
 *   - sinon → pas d'arrondi psychologique, HALF_UP à 2 décimales
 *
 * Les grands montants (≥ 10 000) sont arrondis au 5 supérieur (ex. 12 347 → 12 350).
 * Les devises sans décimale (JPY, etc.) sont gérées séparément.
 */
public final class PsychologicalRounder {

    private static final BigDecimal THRESHOLD = new BigDecimal("10000");

    /** Devises dont l'unité monétaire n'a pas de sous-unité (ex. yen). */
    private static final Set<String> ZERO_DECIMAL = Set.of("JPY", "KRW", "VND", "BIF", "CLP", "GNF",
            "MGA", "PYG", "RWF", "UGX", "XAF", "XOF", "XPF");

    private PsychologicalRounder() {}

    public static BigDecimal round(BigDecimal amount, String currency) {
        if (amount == null) {
            return null;
        }
        if (ZERO_DECIMAL.contains(currency.toUpperCase())) {
            return amount.setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal base = amount.setScale(2, RoundingMode.HALF_UP);
        if (base.compareTo(THRESHOLD) >= 0) {
            // Arrondi au 5 CAD/USD supérieur : rend 12 347 → 12 350
            BigDecimal five = new BigDecimal("5");
            BigDecimal divided = base.divide(five, 0, RoundingMode.CEILING);
            return divided.multiply(five).setScale(2, RoundingMode.UNNECESSARY);
        }
        // Terminaison .99 / .49
        int cents = base.scaleByPowerOfTen(2).setScale(0, RoundingMode.HALF_UP).intValue();
        int lastTwo = cents % 100;
        if (lastTwo == 0) {
            return base.subtract(new BigDecimal("0.01"));
        }
        if (lastTwo == 50) {
            return base.subtract(new BigDecimal("0.01"));
        }
        return base;
    }
}
