package com.lmp.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO pour les actions administratives sur les commandes.
 * Utilisé pour les changements de statut, remboursements, annulations, etc.
 */
public class OrderActionDto {

    @NotNull(message = "L'ID de la commande est obligatoire")
    private java.util.UUID orderId;
    
    @NotBlank(message = "Le type d'action est obligatoire")
    private String actionType; // CHANGE_STATUS, REFUND, CANCEL, ADD_NOTE, UPDATE_PRIORITY
    
    // Pour les changements de statut
    private String newStatus;
    private String statusReason;
    
    // Pour les remboursements
    @Positive(message = "Le montant du remboursement doit être positif")
    private BigDecimal refundAmount;
    private String refundReason;
    private boolean isPartialRefund = false;
    private boolean processRefund = false; // Pour traiter automatiquement le remboursement
    
    // Pour les notes et commentaires
    private String adminNotes;
    private String publicComment; // Visible par le client
    private String note; // Note générale pour l'action
    
    // Pour la priorité
    private Integer priority;
    
    // Pour les tags
    private String tags;
    
    // Pour les notifications
    private boolean sendNotification = true;
    private String notificationMessage;
    
    // Métadonnées
    private String adminEmail; // Automatiquement rempli par le système
    private String ipAddress; // Automatiquement rempli par le système
    private String userAgent; // Automatiquement rempli par le système
    
    // Constructeurs
    public OrderActionDto() {}
    
    public OrderActionDto(java.util.UUID orderId, String actionType) {
        this.orderId = orderId;
        this.actionType = actionType;
    }
    
    // Factory methods pour les actions courantes
    
    /**
     * Crée une action de changement de statut
     */
    public static OrderActionDto changeStatus(java.util.UUID orderId, String newStatus, String reason) {
        OrderActionDto action = new OrderActionDto(orderId, "CHANGE_STATUS");
        action.setNewStatus(newStatus);
        action.setStatusReason(reason);
        return action;
    }
    
    /**
     * Crée une action de remboursement complet
     */
    public static OrderActionDto fullRefund(java.util.UUID orderId, String reason) {
        OrderActionDto action = new OrderActionDto(orderId, "REFUND");
        action.setRefundReason(reason);
        action.setPartialRefund(false);
        return action;
    }
    
    /**
     * Crée une action de remboursement partiel
     */
    public static OrderActionDto partialRefund(java.util.UUID orderId, BigDecimal amount, String reason) {
        OrderActionDto action = new OrderActionDto(orderId, "REFUND");
        action.setRefundAmount(amount);
        action.setRefundReason(reason);
        action.setPartialRefund(true);
        return action;
    }
    
    /**
     * Crée une action d'annulation
     */
    public static OrderActionDto cancel(java.util.UUID orderId, String reason) {
        OrderActionDto action = new OrderActionDto(orderId, "CANCEL");
        action.setStatusReason(reason);
        action.setNewStatus("CANCELLED");
        return action;
    }
    
    /**
     * Crée une action d'ajout de note
     */
    public static OrderActionDto addNote(java.util.UUID orderId, String note) {
        OrderActionDto action = new OrderActionDto(orderId, "ADD_NOTE");
        action.setAdminNotes(note);
        return action;
    }
    
    // Getters et Setters
    
    public java.util.UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(java.util.UUID orderId) {
        this.orderId = orderId;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getNewStatus() {
        return newStatus;
    }
    
    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
    
    public String getStatusReason() {
        return statusReason;
    }
    
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
    
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
    
    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    public String getRefundReason() {
        return refundReason;
    }
    
    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
    
    public boolean isPartialRefund() {
        return isPartialRefund;
    }
    
    public void setPartialRefund(boolean partialRefund) {
        isPartialRefund = partialRefund;
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getPublicComment() {
        return publicComment;
    }
    
    public void setPublicComment(String publicComment) {
        this.publicComment = publicComment;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public boolean isSendNotification() {
        return sendNotification;
    }
    
    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }
    
    public String getNotificationMessage() {
        return notificationMessage;
    }
    
    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }
    
    public String getAdminEmail() {
        return adminEmail;
    }
    
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
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
    
    public boolean isProcessRefund() {
        return processRefund;
    }
    
    public void setProcessRefund(boolean processRefund) {
        this.processRefund = processRefund;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    // Méthodes utilitaires
    
    /**
     * Vérifie si l'action est valide
     */
    public boolean isValid() {
        if (orderId == null || actionType == null || actionType.trim().isEmpty()) {
            return false;
        }
        
        switch (actionType.toUpperCase()) {
            case "CHANGE_STATUS":
                return newStatus != null && !newStatus.trim().isEmpty();
            case "REFUND":
                return refundReason != null && !refundReason.trim().isEmpty() &&
                       (!isPartialRefund || (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0));
            case "CANCEL":
                return statusReason != null && !statusReason.trim().isEmpty();
            case "ADD_NOTE":
                return adminNotes != null && !adminNotes.trim().isEmpty();
            case "UPDATE_PRIORITY":
                return priority != null && priority >= 1 && priority <= 10;
            default:
                return true; // Autres actions futures
        }
    }
    
    /**
     * Vérifie si l'action nécessite une confirmation
     */
    public boolean requiresConfirmation() {
        return "REFUND".equals(actionType) || "CANCEL".equals(actionType) ||
               ("CHANGE_STATUS".equals(actionType) && "CANCELLED".equals(newStatus));
    }
    
    /**
     * Retourne une description de l'action pour l'audit
     */
    public String getActionDescription() {
        switch (actionType.toUpperCase()) {
            case "CHANGE_STATUS":
                return "Changement de statut vers : " + newStatus;
            case "REFUND":
                return isPartialRefund ? 
                    "Remboursement partiel de " + refundAmount + " CAD" :
                    "Remboursement complet";
            case "CANCEL":
                return "Annulation de la commande";
            case "ADD_NOTE":
                return "Ajout de note administrative";
            case "UPDATE_PRIORITY":
                return "Modification de la priorité : " + priority;
            default:
                return "Action : " + actionType;
        }
    }
    
    /**
     * Retourne le niveau de sévérité de l'action
     */
    public String getSeverityLevel() {
        switch (actionType.toUpperCase()) {
            case "REFUND":
            case "CANCEL":
                return "HIGH";
            case "CHANGE_STATUS":
                return "CANCELLED".equals(newStatus) ? "HIGH" : "MEDIUM";
            case "UPDATE_PRIORITY":
                return priority != null && priority <= 2 ? "MEDIUM" : "LOW";
            case "ADD_NOTE":
            default:
                return "LOW";
        }
    }
    
    @Override
    public String toString() {
        return "OrderActionDto{" +
                "orderId=" + orderId +
                ", actionType='" + actionType + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", refundAmount=" + refundAmount +
                ", isPartialRefund=" + isPartialRefund +
                ", sendNotification=" + sendNotification +
                '}';
    }
}