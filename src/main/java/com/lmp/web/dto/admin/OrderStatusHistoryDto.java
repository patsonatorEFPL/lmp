package com.lmp.web.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmp.domain.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * DTO pour l'historique des changements de statut d'une commande.
 * Permet de tracer tous les changements effectués sur une commande avec audit complet.
 */
public class OrderStatusHistoryDto {

    private java.util.UUID id;
    private java.util.UUID orderId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String adminNotes;
    private String changedByAdmin;
    private String changedByAdminEmail;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
    
    private boolean isSystemChange; // true si changement automatique (webhook), false si manuel
    private String sourceType; // ADMIN, WEBHOOK, SYSTEM
    private String ipAddress;
    private String userAgent;
    
    // Champs supplémentaires pour la compatibilité avec le service
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String note;
    private String changedBy;
    private String orderServiceName;
    private String customerEmail;
    
    // Constructeurs
    public OrderStatusHistoryDto() {}
    
    public OrderStatusHistoryDto(java.util.UUID orderId, String previousStatus, String newStatus, String reason) {
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
        this.isSystemChange = false;
        this.sourceType = "ADMIN";
    }
    
    // Getters et Setters
    
    public java.util.UUID getId() {
        return id;
    }
    
    public void setId(java.util.UUID id) {
        this.id = id;
    }
    
    public java.util.UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(java.util.UUID orderId) {
        this.orderId = orderId;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getChangedByAdmin() {
        return changedByAdmin;
    }
    
    public void setChangedByAdmin(String changedByAdmin) {
        this.changedByAdmin = changedByAdmin;
    }
    
    public String getChangedByAdminEmail() {
        return changedByAdminEmail;
    }
    
    public void setChangedByAdminEmail(String changedByAdminEmail) {
        this.changedByAdminEmail = changedByAdminEmail;
    }
    
    public LocalDateTime getChangedAt() {
        return changedAt;
    }
    
    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
    
    public boolean isSystemChange() {
        return isSystemChange;
    }
    
    public void setSystemChange(boolean systemChange) {
        isSystemChange = systemChange;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // Getters et Setters supplémentaires pour compatibilité
    public OrderStatus getFromStatus() {
        return fromStatus;
    }
    
    public void setFromStatus(OrderStatus fromStatus) {
        this.fromStatus = fromStatus;
        this.previousStatus = fromStatus != null ? fromStatus.toString() : null;
    }
    
    public OrderStatus getToStatus() {
        return toStatus;
    }
    
    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
        this.newStatus = toStatus != null ? toStatus.toString() : null;
    }
    
    public String getNote() {
        return note != null ? note : reason;
    }
    
    public void setNote(String note) {
        this.note = note;
        this.reason = note;
    }
    
    public String getChangedBy() {
        return changedBy != null ? changedBy : changedByAdmin;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
        this.changedByAdmin = changedBy;
    }
    
    public String getOrderServiceName() {
        return orderServiceName;
    }
    
    public void setOrderServiceName(String orderServiceName) {
        this.orderServiceName = orderServiceName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne le nom d'affichage pour le statut précédent
     */
    public String getPreviousStatusDisplayName() {
        return getStatusDisplayName(previousStatus);
    }
    
    /**
     * Retourne le nom d'affichage pour le nouveau statut
     */
    public String getNewStatusDisplayName() {
        return getStatusDisplayName(newStatus);
    }
    
    /**
     * Retourne le nom d'affichage pour un statut donné
     */
    private String getStatusDisplayName(String status) {
        if (status == null) return "Inconnu";
        
        switch (status.toUpperCase()) {
            case "PENDING": return "En attente";
            case "CONFIRMED": return "Confirmée";
            case "PROCESSING": return "En préparation";
            case "SHIPPED": return "Expédiée";
            case "DELIVERED": return "Livrée";
            case "CANCELLED": return "Annulée";
            case "REFUNDED": return "Remboursée";
            default: return status;
        }
    }
    
    /**
     * Retourne la couleur associée au nouveau statut
     */
    public String getNewStatusColor() {
        return getStatusColor(newStatus);
    }
    
    /**
     * Retourne la couleur associée à un statut
     */
    private String getStatusColor(String status) {
        if (status == null) return "secondary";
        
        switch (status.toUpperCase()) {
            case "PENDING": return "warning";
            case "CONFIRMED": return "info";
            case "PROCESSING": return "primary";
            case "SHIPPED": return "success";
            case "DELIVERED": return "success";
            case "CANCELLED": return "danger";
            case "REFUNDED": return "secondary";
            default: return "secondary";
        }
    }
    
    /**
     * Retourne l'icône associée au type de source
     */
    public String getSourceIcon() {
        if (sourceType == null) return "fas fa-question";
        
        switch (sourceType.toUpperCase()) {
            case "ADMIN": return "fas fa-user-shield";
            case "WEBHOOK": return "fas fa-cloud";
            case "SYSTEM": return "fas fa-cog";
            default: return "fas fa-question";
        }
    }
    
    /**
     * Retourne une description du changement
     */
    public String getChangeDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (isSystemChange) {
            desc.append("Changement automatique");
        } else {
            desc.append("Changement manuel");
        }
        
        if (changedByAdmin != null) {
            desc.append(" par ").append(changedByAdmin);
        }
        
        if (previousStatus != null && newStatus != null) {
            desc.append(" : ").append(getPreviousStatusDisplayName())
                .append(" → ").append(getNewStatusDisplayName());
        }
        
        return desc.toString();
    }
    
    /**
     * Retourne le temps écoulé depuis le changement
     */
    public String getTimeAgo() {
        if (changedAt == null) return "Inconnu";
        
        java.time.Duration duration = java.time.Duration.between(changedAt, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long days = duration.toDays();
        
        if (days > 0) {
            return days + " jour" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " heure" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return "À l'instant";
        }
    }
    
    @Override
    public String toString() {
        return "OrderStatusHistoryDto{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", previousStatus='" + previousStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", reason='" + reason + '\'' +
                ", changedByAdmin='" + changedByAdmin + '\'' +
                ", changedAt=" + changedAt +
                ", sourceType='" + sourceType + '\'' +
                '}';
    }
}