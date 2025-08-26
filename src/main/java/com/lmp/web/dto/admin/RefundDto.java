package com.lmp.web.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les informations de remboursement d'une commande.
 */
public class RefundDto {

    private Long id;
    private Long orderId;
    private String stripeRefundId;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String status; // pending, succeeded, failed, canceled
    private String adminNotes;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
    
    private String createdByAdmin;
    private boolean isPartialRefund;
    
    // Champs supplémentaires pour l'administration
    private String failureReason;
    private String processedBy;
    private String cancellationReason;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String metadata;
    private String orderServiceName;
    private String customerEmail;
    private String customerName;
    
    // Constructeurs
    public RefundDto() {}
    
    public RefundDto(Long orderId, BigDecimal amount, String reason) {
        this.orderId = orderId;
        this.amount = amount;
        this.reason = reason;
        this.currency = "CAD";
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getStripeRefundId() {
        return stripeRefundId;
    }
    
    public void setStripeRefundId(String stripeRefundId) {
        this.stripeRefundId = stripeRefundId;
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getCreatedByAdmin() {
        return createdByAdmin;
    }
    
    public void setCreatedByAdmin(String createdByAdmin) {
        this.createdByAdmin = createdByAdmin;
    }
    
    public boolean isPartialRefund() {
        return isPartialRefund;
    }
    
    public void setPartialRefund(boolean partialRefund) {
        isPartialRefund = partialRefund;
    }
    
    // Getters et Setters pour les nouveaux champs
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public String getOrderServiceName() { return orderServiceName; }
    public void setOrderServiceName(String orderServiceName) { this.orderServiceName = orderServiceName; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    /**
     * Retourne le statut d'affichage en français
     */
    public String getStatusDisplayName() {
        if (status == null) return "Inconnu";
        
        switch (status.toLowerCase()) {
            case "pending": return "En attente";
            case "succeeded": return "Réussi";
            case "failed": return "Échoué";
            case "canceled": return "Annulé";
            default: return status;
        }
    }
    
    /**
     * Retourne la couleur du statut pour l'affichage
     */
    public String getStatusColor() {
        if (status == null) return "secondary";
        
        switch (status.toLowerCase()) {
            case "pending": return "warning";
            case "succeeded": return "success";
            case "failed": return "danger";
            case "canceled": return "secondary";
            default: return "secondary";
        }
    }
    
    /**
     * Vérifie si le remboursement est en cours de traitement
     */
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }
    
    /**
     * Vérifie si le remboursement a réussi
     */
    public boolean isSucceeded() {
        return "succeeded".equalsIgnoreCase(status);
    }
    
    @Override
    public String toString() {
        return "RefundDto{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}