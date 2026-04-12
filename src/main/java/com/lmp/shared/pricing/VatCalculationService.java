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
 * Ce service applique (ou non) la TVA selon le statut d'autoliquidation du client.
 *
 * <ul>
 *   <li>Client normal → prix TTC = HT × (1 + taux TVA)</li>
 *   <li>Client reverse-charge (B2B intra-UE) → prix = HT (TVA 0 %)</li>
 * </ul>
 *
 * <p>Le taux est configurable via {@code pricing.vat.rate} (défaut 0.20 = 20 %).
 */
@Service
public class VatCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(VatCalculationService.class);

    private final BigDecimal vatRate;

    public VatCalculationService(
            @Value("${pricing.vat.rate:0.20}") BigDecimal vatRate) {
        this.vatRate = vatRate;
        logger.info("VatCalculationService initialized — VAT rate: {}%",
                vatRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString());
    }

    /**
     * @return le taux de TVA configuré (ex: 0.20 pour 20 %).
     */
    public BigDecimal getVatRate() {
        return vatRate;
    }

    /**
     * Applique la TVA sur un montant HT.
     *
     * @param amountHt      montant hors taxe
     * @param reverseCharge {@code true} si autoliquidation → pas de TVA ajoutée
     * @return montant TTC (ou HT si reverse-charge)
     */
    public BigDecimal applyVat(BigDecimal amountHt, boolean reverseCharge) {
        if (amountHt == null) return null;
        if (reverseCharge) {
            return amountHt;
        }
        return amountHt.multiply(BigDecimal.ONE.add(vatRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant de TVA pour un montant HT.
     *
     * @param amountHt      montant hors taxe
     * @param reverseCharge {@code true} si autoliquidation → TVA = 0
     * @return montant de TVA
     */
    public BigDecimal vatAmount(BigDecimal amountHt, boolean reverseCharge) {
        if (amountHt == null) return BigDecimal.ZERO;
        if (reverseCharge) {
            return BigDecimal.ZERO;
        }
        return amountHt.multiply(vatRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Extrait le montant HT d'un montant TTC.
     *
     * @param amountTtc montant toutes taxes comprises
     * @return montant hors taxe
     */
    public BigDecimal extractHt(BigDecimal amountTtc) {
        if (amountTtc == null) return null;
        return amountTtc.divide(BigDecimal.ONE.add(vatRate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Extrait le montant de TVA d'un montant TTC.
     *
     * @param amountTtc montant toutes taxes comprises
     * @return montant de TVA contenu dans le TTC
     */
    public BigDecimal extractVatFromTtc(BigDecimal amountTtc) {
        if (amountTtc == null) return BigDecimal.ZERO;
        return amountTtc.subtract(extractHt(amountTtc));
    }
}
