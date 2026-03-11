package com.lmp.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO pour les requêtes de remboursement
 * Contient toutes les informations nécessaires pour traiter un remboursement
 */
public class RefundRequestDto {
    
    @NotNull(message = "Le montant du remboursement est requis")
    @DecimalMin(value = "0.01", message = "Le montant du remboursement doit être supérieur à 0")
    private BigDecimal amount;
    
    @Size(max = 500, message = "La raison du remboursement ne peut pas dépasser 500 caractères")
    private String reason;
    
    // Indique si c'est un remboursement partiel ou total
    private boolean isPartialRefund = false;
    
    // Métadonnées additionnelles pour le remboursement
    private Map<String, Object> metadata;
    
    // Indique si le remboursement doit être traité immédiatement ou différé
    private boolean immediate = true;
    
    // Référence interne pour le suivi du remboursement
    private String internalReference;
    
    // Notification par email au client
    private boolean notifyCustomer = true;
    
    // Instructions spéciales pour le fournisseur de paiement
    private String instructions;
    
    // Constructeurs
    public RefundRequestDto() {}
    
    public RefundRequestDto(BigDecimal amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }
    
    public RefundRequestDto(BigDecimal amount, String reason, boolean isPartialRefund) {
        this.amount = amount;
        this.reason = reason;
        this.isPartialRefund = isPartialRefund;
    }
    
    // Getters et Setters
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public boolean isPartialRefund() {
        return isPartialRefund;
    }
    
    public void setPartialRefund(boolean partialRefund) {
        isPartialRefund = partialRefund;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public boolean isImmediate() {
        return immediate;
    }
    
    public void setImmediate(boolean immediate) {
        this.immediate = immediate;
    }
    
    public String getInternalReference() {
        return internalReference;
    }
    
    public void setInternalReference(String internalReference) {
        this.internalReference = internalReference;
    }
    
    public boolean isNotifyCustomer() {
        return notifyCustomer;
    }
    
    public void setNotifyCustomer(boolean notifyCustomer) {
        this.notifyCustomer = notifyCustomer;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    @Override
    public String toString() {
        return "RefundRequestDto{" +
                "amount=" + amount +
                ", reason='" + reason + '\'' +
                ", isPartialRefund=" + isPartialRefund +
                ", immediate=" + immediate +
                ", notifyCustomer=" + notifyCustomer +
                '}';
    }
}