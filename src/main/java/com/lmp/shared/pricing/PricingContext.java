package com.lmp.shared.pricing;

import java.math.BigDecimal;

/**
 * Contexte de tarification résolu pour une requête donnée.
 *
 * @param countryCode      Code pays ISO 3166-1 alpha-2 (ex. CA, FR).
 * @param currency         Code devise ISO 4217 (ex. CAD, EUR).
 * @param eurToTargetRate  Taux effectif EUR → {currency} (taux brut × (1 + marge)).
 * @param rawRate          Taux brut depuis Frankfurter (ou statique), sans marge.
 * @param fxMargin         Marge appliquée (0 si aucune).
 * @param rateSource       "frankfurter" ou "static".
 */
public record PricingContext(
        String countryCode,
        String currency,
        BigDecimal eurToTargetRate,
        BigDecimal rawRate,
        BigDecimal fxMargin,
        String rateSource
) {
    /** Rétrocompatibilité : constructeur sans metadata FX. */
    public PricingContext(String countryCode, String currency, BigDecimal eurToTargetRate) {
        this(countryCode, currency, eurToTargetRate, eurToTargetRate, BigDecimal.ZERO, "static");
    }
}
