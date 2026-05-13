package com.lmp.billing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que InstallmentCalculator produit exactement les mêmes montants qu'externalErp.
 * Valeurs de référence extraites de l'API externalErp sur SINV-2026-00029 (1200 € en 3x).
 */
class InstallmentCalculatorTest {

    @Test
    void threeInstallments_1200_matchesexternalErp() {
        // Valeurs réelles externalErp pour "Paiement en 3x" sur 1200 €
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(
                new BigDecimal("1200.00"), 3);

        assertEquals(3, result.size());

        // 1ère échéance: 33.33% → 399.96 (externalErp: 399.96)
        assertEquals(new BigDecimal("33.33"), result.get(0).invoicePortion());
        assertEquals(new BigDecimal("399.96"), result.get(0).paymentAmount());

        // 2ème échéance: 33.33% → 399.96 (externalErp: 399.96)
        assertEquals(new BigDecimal("33.33"), result.get(1).invoicePortion());
        assertEquals(new BigDecimal("399.96"), result.get(1).paymentAmount());

        // 3ème échéance: 33.34% → 400.08 (externalErp: 400.08)
        assertEquals(new BigDecimal("33.34"), result.get(2).invoicePortion());
        assertEquals(new BigDecimal("400.08"), result.get(2).paymentAmount());

        // Total = 1200.00 exactement
        assertEquals(BigDecimal.ZERO.setScale(2), InstallmentCalculator.verifyTotal(result, new BigDecimal("1200.00")));
    }

    @Test
    void twoInstallments_1200() {
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(
                new BigDecimal("1200.00"), 2);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("50.00"), result.get(0).invoicePortion());
        assertEquals(new BigDecimal("600.00"), result.get(0).paymentAmount());
        assertEquals(new BigDecimal("50.00"), result.get(1).invoicePortion());
        assertEquals(new BigDecimal("600.00"), result.get(1).paymentAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), InstallmentCalculator.verifyTotal(result, new BigDecimal("1200.00")));
    }

    @Test
    void fourInstallments_1000() {
        // 100/4 = 25.00 per installment — no rounding issue
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(
                new BigDecimal("1000.00"), 4);

        assertEquals(4, result.size());
        for (var inst : result) {
            assertEquals(new BigDecimal("25.00"), inst.invoicePortion());
            assertEquals(new BigDecimal("250.00"), inst.paymentAmount());
        }
        assertEquals(BigDecimal.ZERO.setScale(2), InstallmentCalculator.verifyTotal(result, new BigDecimal("1000.00")));
    }

    @Test
    void threeInstallments_100_erpRounding() {
        // 100 € / 3 → externalErp: 33.33% × 100 = 33.33, 33.33, 33.34
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(
                new BigDecimal("100.00"), 3);

        assertEquals(new BigDecimal("33.33"), result.get(0).paymentAmount());
        assertEquals(new BigDecimal("33.33"), result.get(1).paymentAmount());
        assertEquals(new BigDecimal("33.34"), result.get(2).paymentAmount());
        assertEquals(BigDecimal.ZERO.setScale(2), InstallmentCalculator.verifyTotal(result, new BigDecimal("100.00")));
    }

    @ParameterizedTest
    @CsvSource({
            "500.00, 3",
            "999.99, 3",
            "1.00, 3",
            "7777.77, 4",
            "123.45, 2",
            "10000.00, 3",
    })
    void totalAlwaysMatchesGrandTotal(String totalStr, int count) {
        BigDecimal total = new BigDecimal(totalStr);
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(total, count);
        assertEquals(count, result.size());
        assertEquals(0, InstallmentCalculator.verifyTotal(result, total).compareTo(BigDecimal.ZERO),
                "Sum of installments must equal grand total for " + totalStr + " / " + count);
    }

    @Test
    void singleInstallment() {
        List<InstallmentCalculator.Installment> result = InstallmentCalculator.calculate(
                new BigDecimal("1200.00"), 1);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).invoicePortion());
        assertEquals(new BigDecimal("1200.00"), result.get(0).paymentAmount());
    }
}
