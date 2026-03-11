package com.lmp.auth.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO pour stocker temporairement l'intention de paiement d'un utilisateur
 * avant qu'il ne soit authentifié.
 */
public class PurchaseIntent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String serviceName;
    private BigDecimal amount;
    private String currency;
    private long timestamp;
    
    public PurchaseIntent() {
        this.timestamp = System.currentTimeMillis();
        this.currency = "EUR"; // Valeur par défaut
    }
    
    public PurchaseIntent(String serviceName, BigDecimal amount, String currency) {
        this();
        this.serviceName = serviceName;
        this.amount = amount;
        this.currency = currency;
    }
    
    // Getters et Setters
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Vérifie si l'intention de paiement est expirée (30 minutes)
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > (30 * 60 * 1000); // 30 minutes
    }
    
    /**
     * Vérifie si l'intention de paiement est valide
     */
    public boolean isValid() {
        return serviceName != null && !serviceName.trim().isEmpty() &&
               amount != null && amount.compareTo(BigDecimal.ZERO) > 0 &&
               currency != null && !currency.isEmpty() &&
               !isExpired();
    }
    
    @Override
    public String toString() {
        return "PurchaseIntent{" +
                "serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}