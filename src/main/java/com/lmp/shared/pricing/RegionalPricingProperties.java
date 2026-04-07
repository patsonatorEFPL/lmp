package com.lmp.shared.pricing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Grille par pays : devise cible et taux depuis l'EUR (prix catalogue en base).
 * Les pays absents de {@link #region} utilisent {@link #baseCurrency} avec un taux de 1.
 */
@ConfigurationProperties(prefix = "pricing")
public class RegionalPricingProperties {

    /** Devise des montants en base (offres). */
    private String baseCurrency = "EUR";

    /** Pays utilisé si la géolocalisation échoue. */
    private String fallbackCountry = "FR";

    /**
     * Marge FX appliquée sur le taux brut pour couvrir les risques de change.
     * Ex : 0.015 = +1,5 %. null ou 0 = pas de marge.
     */
    private BigDecimal fxMargin = BigDecimal.ZERO;

    /**
     * Activer la mise à jour automatique des taux via Frankfurter (BCE).
     * Défaut : true. Mettre à false pour n'utiliser que les taux statiques de pricing.region.*.
     */
    private boolean fxAutoRefresh = true;

    private Map<String, RegionRate> region = new HashMap<>();

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getFallbackCountry() {
        return fallbackCountry;
    }

    public void setFallbackCountry(String fallbackCountry) {
        this.fallbackCountry = fallbackCountry;
    }

    public BigDecimal getFxMargin() {
        return fxMargin;
    }

    public void setFxMargin(BigDecimal fxMargin) {
        this.fxMargin = fxMargin;
    }

    public boolean isFxAutoRefresh() {
        return fxAutoRefresh;
    }

    public void setFxAutoRefresh(boolean fxAutoRefresh) {
        this.fxAutoRefresh = fxAutoRefresh;
    }

    public Map<String, RegionRate> getRegion() {
        return region;
    }

    public void setRegion(Map<String, RegionRate> region) {
        this.region = region;
    }

    public static class RegionRate {
        private String currency;
        private BigDecimal eurRate;

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getEurRate() {
            return eurRate;
        }

        public void setEurRate(BigDecimal eurRate) {
            this.eurRate = eurRate;
        }
    }
}
