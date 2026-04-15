package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service centralisé de calcul de la TVA.
 *
 * <p>Les prix catalogue ({@code ServiceOffer.price}) sont stockés <strong>HT</strong>.
 * Ce service applique (ou non) la TVA selon le statut d'autoliquidation du client
 * et le pays de facturation.
 *
 * <h3>Résolution du taux</h3>
 * <ul>
 *   <li>Surcharges avec {@code countryCode} → taux résolu via {@link VatRateLookupService}</li>
 *   <li>Surcharges sans {@code countryCode} → taux par défaut ({@code pricing.vat.rate})</li>
 *   <li>Client reverse-charge (B2B intra-UE) → TVA 0 %</li>
 * </ul>
 *
 * <p>Le taux par défaut est configurable via {@code pricing.vat.rate} (défaut 0.20 = 20 %).
 */
@Service
public class VatCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(VatCalculationService.class);

    private final BigDecimal vatRate;
    private final VatRateLookupService vatRateLookupService;

    public VatCalculationService(
            @Value("${pricing.vat.rate:0.20}") BigDecimal vatRate,
            VatRateLookupService vatRateLookupService) {
        this.vatRate = vatRate;
        this.vatRateLookupService = vatRateLookupService;
        logger.info("VatCalculationService initialized — default VAT rate: {}%",
                vatRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Taux
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * @return le taux de TVA par défaut configuré (ex: 0.20 pour 20 %).
     */
    public BigDecimal getVatRate() {
        return vatRate;
    }

    /**
     * Retourne le taux de TVA pour un pays donné.
     *
     * @param countryCode Code ISO 3166-1 alpha-2 (ex: "DE"). Peut être null → taux par défaut.
     * @return taux décimal (ex: 0.19 pour l'Allemagne).
     */
    public BigDecimal getVatRate(String countryCode) {
        return vatRateLookupService.getRate(countryCode);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Application TVA — avec pays
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Applique la TVA sur un montant HT en fonction du pays.
     *
     * @param amountHt      montant hors taxe
     * @param reverseCharge {@code true} si autoliquidation → pas de TVA ajoutée
     * @param countryCode   code pays ISO (ex: "DE") — null → taux par défaut
     * @return montant TTC (ou HT si reverse-charge)
     */
    public BigDecimal applyVat(BigDecimal amountHt, boolean reverseCharge, String countryCode) {
        if (amountHt == null) return null;
        if (reverseCharge) return amountHt;
        BigDecimal rate = vatRateLookupService.getRate(countryCode);
        return amountHt.multiply(BigDecimal.ONE.add(rate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant de TVA pour un montant HT, par pays.
     */
    public BigDecimal vatAmount(BigDecimal amountHt, boolean reverseCharge, String countryCode) {
        if (amountHt == null) return BigDecimal.ZERO;
        if (reverseCharge) return BigDecimal.ZERO;
        BigDecimal rate = vatRateLookupService.getRate(countryCode);
        return amountHt.multiply(rate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Application TVA — sans pays (rétrocompatibilité, taux par défaut)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Applique la TVA sur un montant HT (taux par défaut).
     *
     * @param amountHt      montant hors taxe
     * @param reverseCharge {@code true} si autoliquidation → pas de TVA ajoutée
     * @return montant TTC (ou HT si reverse-charge)
     */
    public BigDecimal applyVat(BigDecimal amountHt, boolean reverseCharge) {
        if (amountHt == null) return null;
        if (reverseCharge) return amountHt;
        return amountHt.multiply(BigDecimal.ONE.add(vatRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant de TVA pour un montant HT (taux par défaut).
     */
    public BigDecimal vatAmount(BigDecimal amountHt, boolean reverseCharge) {
        if (amountHt == null) return BigDecimal.ZERO;
        if (reverseCharge) return BigDecimal.ZERO;
        return amountHt.multiply(vatRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Extraction HT depuis TTC
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Extrait le montant HT d'un montant TTC (taux par défaut).
     */
    public BigDecimal extractHt(BigDecimal amountTtc) {
        if (amountTtc == null) return null;
        return amountTtc.divide(BigDecimal.ONE.add(vatRate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Extrait le montant HT d'un montant TTC, avec le taux du pays.
     */
    public BigDecimal extractHt(BigDecimal amountTtc, String countryCode) {
        if (amountTtc == null) return null;
        BigDecimal rate = vatRateLookupService.getRate(countryCode);
        return amountTtc.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Extrait le montant HT d'un montant TTC, avec un taux explicite.
     */
    public BigDecimal extractHt(BigDecimal amountTtc, BigDecimal explicitRate) {
        if (amountTtc == null) return null;
        BigDecimal rate = explicitRate != null ? explicitRate : vatRate;
        return amountTtc.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Extrait le montant de TVA d'un montant TTC (taux par défaut).
     */
    public BigDecimal extractVatFromTtc(BigDecimal amountTtc) {
        if (amountTtc == null) return BigDecimal.ZERO;
        return amountTtc.subtract(extractHt(amountTtc));
    }

    /**
     * Extrait le montant de TVA d'un montant TTC, avec un taux explicite.
     */
    public BigDecimal extractVatFromTtc(BigDecimal amountTtc, BigDecimal explicitRate) {
        if (amountTtc == null) return BigDecimal.ZERO;
        return amountTtc.subtract(extractHt(amountTtc, explicitRate));
    }
}
