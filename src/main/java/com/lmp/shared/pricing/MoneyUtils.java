package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Constantes et utilitaires de précision monétaire centralisés.
 * <p>
 * Aligné sur la précision external ERP (Currency precision = 2, HALF_UP).
 * Toutes les opérations monétaires de LMP doivent utiliser ces constantes
 * pour garantir des résultats identiques entre LMP et l'ERP.
 *
 * <h3>Précisions par type</h3>
 * <table>
 *   <tr><td>Montants monétaires</td><td>2 décimales, HALF_UP</td></tr>
 *   <tr><td>Pourcentages</td><td>2 décimales, DOWN (pour répartition)</td></tr>
 *   <tr><td>Taux de TVA</td><td>4 décimales (stockage)</td></tr>
 *   <tr><td>Taux de change</td><td>6 décimales</td></tr>
 * </table>
 */
public final class MoneyUtils {

    // ── Précisions ──────────────────────────────────────────────────────────

    /** Précision des montants monétaires (EUR, USD, etc.) — external ERP Currency field. */
    public static final int CURRENCY_SCALE = 2;

    /** Précision des pourcentages (invoice_portion, etc.) — external ERP Percent field. */
    public static final int PERCENT_SCALE = 2;

    /** Précision des taux de TVA stockés (ex: 0.2100). */
    public static final int VAT_RATE_SCALE = 4;

    /** Précision des taux de change. */
    public static final int FX_RATE_SCALE = 6;

    // ── Mode d'arrondi par défaut ───────────────────────────────────────────

    /** Arrondi standard pour les montants monétaires — identique à flt() dans external ERP. */
    public static final RoundingMode CURRENCY_ROUNDING = RoundingMode.HALF_UP;

    /** Arrondi pour les répartitions de pourcentages (N-1 premières parts). */
    public static final RoundingMode PERCENT_ROUNDING = RoundingMode.DOWN;

    // ── Constantes ──────────────────────────────────────────────────────────

    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(CURRENCY_SCALE);

    private MoneyUtils() {}

    // ── Opérations monétaires centralisées ───────────────────────────────────

    /**
     * Arrondit un montant à la précision monétaire standard (2 décimales, HALF_UP).
     * Équivalent de {@code flt(value, precision("payment_amount"))} dans external ERP.
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) return ZERO;
        return amount.setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
    }

    /**
     * Multiplie et arrondit au centime.
     * Ex: TVA, conversion, pourcentage.
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return ZERO;
        return a.multiply(b).setScale(CURRENCY_SCALE, CURRENCY_ROUNDING);
    }

    /**
     * Divise et arrondit au centime.
     * Ex: extraction HT depuis TTC, répartition.
     */
    public static BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return ZERO;
        }
        return numerator.divide(denominator, CURRENCY_SCALE, CURRENCY_ROUNDING);
    }

    /**
     * Calcule un pourcentage d'un montant — formule external ERP exacte.
     * {@code flt(grandTotal * flt(portion) / 100, precision("payment_amount"))}
     *
     * @param amount  montant de base (ex: grand_total)
     * @param percent pourcentage (ex: 33.33)
     * @return montant arrondi
     */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null) return ZERO;
        return amount.multiply(percent)
                .divide(HUNDRED, CURRENCY_SCALE, CURRENCY_ROUNDING);
    }

    /**
     * Applique un taux multiplicatif sur un montant.
     * Ex: {@code applyRate(100, 0.21) = 21.00} (montant TVA)
     *     {@code applyRate(100, 1.21) = 121.00} (montant TTC)
     */
    public static BigDecimal applyRate(BigDecimal amount, BigDecimal rate) {
        return multiply(amount, rate);
    }

    /**
     * Extrait le montant HT d'un montant TTC.
     * {@code ttc / (1 + vatRate)}
     */
    public static BigDecimal extractHt(BigDecimal amountTtc, BigDecimal vatRate) {
        if (amountTtc == null) return ZERO;
        BigDecimal divisor = BigDecimal.ONE.add(vatRate != null ? vatRate : BigDecimal.ZERO);
        return amountTtc.divide(divisor, CURRENCY_SCALE, CURRENCY_ROUNDING);
    }

    /**
     * Calcule le montant de TVA d'un montant HT.
     * {@code ht × vatRate}
     */
    public static BigDecimal vatAmount(BigDecimal amountHt, BigDecimal vatRate) {
        if (amountHt == null || vatRate == null) return ZERO;
        return multiply(amountHt, vatRate);
    }
}
