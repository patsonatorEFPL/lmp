package com.lmp.billing.util;

import com.lmp.shared.pricing.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcule les montants d'échéances en reproduisant exactement la logique externalErp.
 * <p>
 * Formule externalErp (accounts_controller.py l.2597) :
 * <pre>
 *   payment_amount = flt(grand_total * flt(invoice_portion) / 100, precision("payment_amount"))
 * </pre>
 * <p>
 * La dernière échéance utilise un pourcentage ajusté (ex: 33.34% au lieu de 33.33%)
 * pour que la somme des pourcentages = 100% exactement.
 * Cela garantit que la somme des montants = grand_total sans ajustement explicite.
 * <p>
 * Précision définie par {@link MoneyUtils} (alignée sur externalErp).
 */
public final class InstallmentCalculator {

    private InstallmentCalculator() {}

    /**
     * Résultat du calcul pour une échéance.
     */
    public record Installment(
            int number,
            BigDecimal invoicePortion,
            BigDecimal paymentAmount
    ) {}

    /**
     * Calcule les montants d'échéances selon la méthode externalErp.
     * <p>
     * Les N-1 premières échéances ont la même portion ({@code floor(100/N, 2)}).
     * La dernière échéance reçoit le reliquat ({@code 100 - (N-1) × portion}).
     * Le montant de chaque échéance est {@code grandTotal × portion / 100}, arrondi à 2 décimales.
     *
     * @param grandTotal montant total TTC
     * @param count      nombre d'échéances (≥ 1)
     * @return liste ordonnée des échéances avec portions et montants
     * @throws IllegalArgumentException si count < 1 ou grandTotal est null/négatif
     */
    public static List<Installment> calculate(BigDecimal grandTotal, int count) {
        if (grandTotal == null || grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grandTotal must be non-null and non-negative");
        }
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1");
        }

        List<Installment> result = new ArrayList<>(count);

        // Portion uniforme pour les N-1 premières échéances
        // externalErp: floor(100/N, 2) — ex: 100/3 = 33.33
        BigDecimal uniformPortion = MoneyUtils.HUNDRED
                .divide(new BigDecimal(count), MoneyUtils.PERCENT_SCALE, MoneyUtils.PERCENT_ROUNDING);

        // Portion de la dernière échéance = 100 - (N-1) × uniformPortion
        // Ex: 100 - 2 × 33.33 = 33.34
        BigDecimal lastPortion = MoneyUtils.HUNDRED.subtract(
                uniformPortion.multiply(new BigDecimal(count - 1)));

        // Calcul des N-1 premières échéances (formule externalErp exacte)
        BigDecimal sumPrevious = BigDecimal.ZERO;
        for (int i = 1; i < count; i++) {
            // externalErp formula: flt(grand_total * flt(invoice_portion) / 100, precision)
            BigDecimal paymentAmount = MoneyUtils.percentOf(grandTotal, uniformPortion);
            sumPrevious = sumPrevious.add(paymentAmount);
            result.add(new Installment(i, uniformPortion, paymentAmount));
        }

        // Dernière échéance = reliquat exact pour garantir sum = grandTotal
        // C'est ce que fait externalErp via validate_payment_schedule_amount
        BigDecimal lastAmount = grandTotal.subtract(sumPrevious);
        result.add(new Installment(count, lastPortion, lastAmount));

        return result;
    }

    /**
     * Vérifie que la somme des montants calculés correspond au grand total.
     * Utile pour les tests et le diagnostic.
     *
     * @return la différence (devrait être 0.00 si le calcul est correct)
     */
    public static BigDecimal verifyTotal(List<Installment> installments, BigDecimal grandTotal) {
        BigDecimal sum = installments.stream()
                .map(Installment::paymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return grandTotal.subtract(sum);
    }
}
