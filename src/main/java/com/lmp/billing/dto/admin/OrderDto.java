package com.lmp.billing.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour l'affichage et la manipulation des commandes dans l'administration.
 * Contient toutes les informations nécessaires pour la gestion complète des commandes.
 */
public class OrderDto {

    private java.util.UUID id;
    private String orderNumber; // Numéro de commande généré (ex: LMP-2024-001)
    
    // Informations client
    private java.util.UUID customerId;
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
    private String customerCompany;
    
    // Informations de commande
    private String serviceName;
    private BigDecimal amount;
    private String currency;
    private OrderStatus status;
    
    // Informations de paiement Stripe
    private String stripePaymentIntentId;
    private String stripeCustomerId;
    private String paymentMethod;
    private String paymentStatus;
    private String stripeChargeId;
    
    // Informations d'adresse
    private String billingAddress;
    private String billingCity;
    private String billingPostalCode;
    private String billingCountry;
    
    // Dates et timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paidAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime shippedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deliveredAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;
    
    // Informations de remboursement
    private boolean hasRefunds;
    private BigDecimal totalRefunded;
    private List<RefundDto> refunds;
    
    // Informations de litige
    private boolean isDisputed;
    private String disputeReason;
    private String disputeStatus;
    
    // Historique des statuts
    private List<OrderStatusHistoryDto> statusHistory;
    
    // Métadonnées
    private String notes; // Notes administratives
    private String tags; // Tags pour catégorisation
    private Integer priority; // Priorité (1=urgent, 5=normal, 10=basse)
    
    // Champs supplémentaires pour l'administration
    private String adminNotes; // Notes administratives
    private String customerName; // Nom complet du client
    private String stripeSessionId; // ID de session Stripe
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastModifiedAt; // Dernière modification
    private String cancellationReason; // Raison d'annulation
    private String processingNotes; // Notes de traitement
    private Long paymentDelayHours; // Délai de paiement en heures
    private Integer progressPercentage; // Avancement 0-100
    private String progressStatus;      // Libellé étape en cours

    // Informations anti-fraude
    private String billingName;
    private String customerVatNumber;
    private String vatCompanyName;
    private Boolean vatReverseCharge;
    private String ipCountry;
    private String ipAddress;
    private BigDecimal vpnScore;
    private String vpnSources;
    private String browserTimezone;
    private String geoCountry;
    private String cardCountry;
    private Integer fraudScore;
    private String fraudFlags;

    // Informations calculées
    private Long daysSinceCreated;
    private Long daysSinceLastUpdate;
    private String statusDisplayName;
    private String statusColor; // Pour l'affichage coloré dans l'UI
    
    // Constructeurs
    public OrderDto() {}
    
    public OrderDto(Order order) {
        this.id = order.getId();
        this.orderNumber = generateOrderNumber(order);
        this.customerId = order.getUser().getId();
        this.customerEmail = order.getUser().getEmail();
        this.customerFirstName = order.getUser().getFirstName();
        this.customerLastName = order.getUser().getLastName();
        this.customerPhone = order.getUser().getPhone();
        this.customerCompany = order.getUser().getCompanyName();
        this.serviceName = order.getServiceName();
        this.amount = order.getTotalAmount();
        this.currency = order.getCurrency();
        this.status = order.getStatus();
        this.stripePaymentIntentId = order.getStripePaymentIntentId();
        this.stripeCustomerId = order.getStripeCustomerId();
        this.paymentMethod = order.getPaymentMethod();
        this.paymentStatus = order.getPaymentStatus();
        this.stripeChargeId = order.getStripeChargeId();
        this.billingAddress = order.getBillingAddress();
        this.billingCity = order.getBillingCity();
        this.billingPostalCode = order.getBillingPostalCode();
        this.billingCountry = order.getBillingCountry();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.paidAt = order.getPaidAt();
        this.shippedAt = order.getShippedAt();
        this.deliveredAt = order.getDeliveredAt();
        this.cancelledAt = order.getCancelledAt();
        this.notes = order.getNotes();
        this.tags = order.getTags();
        this.priority = order.getPriority();
        this.progressPercentage = order.getProgressPercentage() != null ? order.getProgressPercentage() : 0;
        this.progressStatus = order.getProgressStatus();

        // Anti-fraude
        this.billingName = order.getBillingName();
        this.customerVatNumber = order.getCustomerVatNumber();
        this.vatCompanyName = order.getVatCompanyName();
        this.vatReverseCharge = order.getVatReverseCharge();
        this.ipCountry = order.getIpCountry();
        this.ipAddress = order.getIpAddress();
        this.vpnScore = order.getVpnScore();
        this.vpnSources = order.getVpnSources();
        this.browserTimezone = order.getBrowserTimezone();
        this.geoCountry = order.getGeoCountry();
        this.cardCountry = order.getCardCountry();
        this.fraudScore = order.getFraudScore();
        this.fraudFlags = order.getFraudFlags();

        // Calculs
        this.daysSinceCreated = calculateDaysSince(order.getCreatedAt());
        this.daysSinceLastUpdate = calculateDaysSince(order.getUpdatedAt());
        this.statusDisplayName = getStatusDisplayName(order.getStatus());
        this.statusColor = getStatusColor(order.getStatus());
    }
    
    // Getters et Setters
    
    public java.util.UUID getId() {
        return id;
    }
    
    public void setId(java.util.UUID id) {
        this.id = id;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public java.util.UUID getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(java.util.UUID customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerFirstName() {
        return customerFirstName;
    }
    
    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }
    
    public String getCustomerLastName() {
        return customerLastName;
    }
    
    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getCustomerCompany() {
        return customerCompany;
    }
    
    public void setCustomerCompany(String customerCompany) {
        this.customerCompany = customerCompany;
    }
    
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
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.statusDisplayName = getStatusDisplayName(status);
        this.statusColor = getStatusColor(status);
    }
    
    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }
    
    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }
    
    public String getStripeCustomerId() {
        return stripeCustomerId;
    }
    
    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getStripeChargeId() {
        return stripeChargeId;
    }
    
    public void setStripeChargeId(String stripeChargeId) {
        this.stripeChargeId = stripeChargeId;
    }
    
    public String getBillingAddress() {
        return billingAddress;
    }
    
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    
    public String getBillingCity() {
        return billingCity;
    }
    
    public void setBillingCity(String billingCity) {
        this.billingCity = billingCity;
    }
    
    public String getBillingPostalCode() {
        return billingPostalCode;
    }
    
    public void setBillingPostalCode(String billingPostalCode) {
        this.billingPostalCode = billingPostalCode;
    }
    
    public String getBillingCountry() {
        return billingCountry;
    }
    
    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        this.daysSinceCreated = calculateDaysSince(createdAt);
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        this.daysSinceLastUpdate = calculateDaysSince(updatedAt);
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
    
    public LocalDateTime getShippedAt() {
        return shippedAt;
    }
    
    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }
    
    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
    
    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public boolean isHasRefunds() {
        return hasRefunds;
    }
    
    public void setHasRefunds(boolean hasRefunds) {
        this.hasRefunds = hasRefunds;
    }
    
    public BigDecimal getTotalRefunded() {
        return totalRefunded;
    }
    
    public void setTotalRefunded(BigDecimal totalRefunded) {
        this.totalRefunded = totalRefunded;
    }
    
    public List<RefundDto> getRefunds() {
        return refunds;
    }
    
    public void setRefunds(List<RefundDto> refunds) {
        this.refunds = refunds;
    }
    
    public boolean isDisputed() {
        return isDisputed;
    }
    
    public void setDisputed(boolean disputed) {
        isDisputed = disputed;
    }
    
    public String getDisputeReason() {
        return disputeReason;
    }
    
    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }
    
    public String getDisputeStatus() {
        return disputeStatus;
    }
    
    public void setDisputeStatus(String disputeStatus) {
        this.disputeStatus = disputeStatus;
    }
    
    public List<OrderStatusHistoryDto> getStatusHistory() {
        return statusHistory;
    }
    
    public void setStatusHistory(List<OrderStatusHistoryDto> statusHistory) {
        this.statusHistory = statusHistory;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Long getDaysSinceCreated() {
        return daysSinceCreated;
    }
    
    public void setDaysSinceCreated(Long daysSinceCreated) {
        this.daysSinceCreated = daysSinceCreated;
    }
    
    public Long getDaysSinceLastUpdate() {
        return daysSinceLastUpdate;
    }
    
    public void setDaysSinceLastUpdate(Long daysSinceLastUpdate) {
        this.daysSinceLastUpdate = daysSinceLastUpdate;
    }
    
    public String getStatusDisplayName() {
        return statusDisplayName;
    }
    
    public void setStatusDisplayName(String statusDisplayName) {
        this.statusDisplayName = statusDisplayName;
    }
    
    public String getStatusColor() {
        return statusColor;
    }
    
    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
    }
    
    // Getters et Setters pour les nouveaux champs
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getStripeSessionId() {
        return stripeSessionId;
    }
    
    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }
    
    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }
    
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public String getProcessingNotes() {
        return processingNotes;
    }
    
    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }
    
    public Long getPaymentDelayHours() {
        return paymentDelayHours;
    }
    
    public void setPaymentDelayHours(Long paymentDelayHours) {
        this.paymentDelayHours = paymentDelayHours;
    }

    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getProgressStatus() { return progressStatus; }
    public void setProgressStatus(String progressStatus) { this.progressStatus = progressStatus; }

    // Anti-fraude getters/setters
    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }
    public String getCustomerVatNumber() { return customerVatNumber; }
    public void setCustomerVatNumber(String customerVatNumber) { this.customerVatNumber = customerVatNumber; }
    public String getVatCompanyName() { return vatCompanyName; }
    public void setVatCompanyName(String vatCompanyName) { this.vatCompanyName = vatCompanyName; }
    public Boolean getVatReverseCharge() { return vatReverseCharge; }
    public void setVatReverseCharge(Boolean vatReverseCharge) { this.vatReverseCharge = vatReverseCharge; }
    public String getIpCountry() { return ipCountry; }
    public void setIpCountry(String ipCountry) { this.ipCountry = ipCountry; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public BigDecimal getVpnScore() { return vpnScore; }
    public void setVpnScore(BigDecimal vpnScore) { this.vpnScore = vpnScore; }
    public String getVpnSources() { return vpnSources; }
    public void setVpnSources(String vpnSources) { this.vpnSources = vpnSources; }
    public String getBrowserTimezone() { return browserTimezone; }
    public void setBrowserTimezone(String browserTimezone) { this.browserTimezone = browserTimezone; }
    public String getGeoCountry() { return geoCountry; }
    public void setGeoCountry(String geoCountry) { this.geoCountry = geoCountry; }
    public String getCardCountry() { return cardCountry; }
    public void setCardCountry(String cardCountry) { this.cardCountry = cardCountry; }
    public Integer getFraudScore() { return fraudScore; }
    public void setFraudScore(Integer fraudScore) { this.fraudScore = fraudScore; }
    public String getFraudFlags() { return fraudFlags; }
    public void setFraudFlags(String fraudFlags) { this.fraudFlags = fraudFlags; }

    // Méthodes utilitaires
    
    /**
     * Génère un numéro de commande unique
     */
    private String generateOrderNumber(Order order) {
        if (order.getCreatedAt() != null) {
            int year = order.getCreatedAt().getYear();
            String shortId = order.getId().toString().substring(0, 8);
            return String.format("LMP-%d-%s", year, shortId);
        }
        return "LMP-" + order.getId().toString().substring(0, 8);
    }
    
    /**
     * Calcule le nombre de jours depuis une date
     */
    private Long calculateDaysSince(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return java.time.Duration.between(dateTime, LocalDateTime.now()).toDays();
    }
    
    /**
     * Retourne le nom d'affichage pour un statut
     */
    private String getStatusDisplayName(OrderStatus status) {
        if (status == null) return "Inconnu";
        
        switch (status) {
            case PENDING: return "En attente";
            case CONFIRMED: return "Confirmée";
            case PROCESSING: return "En préparation";
            case SHIPPED: return "Expédiée";
            case DELIVERED: return "Livrée";
            case CANCELLED: return "Annulée";
            case REFUNDED: return "Remboursée";
            default: return status.name();
        }
    }
    
    /**
     * Retourne la couleur associée à un statut
     */
    private String getStatusColor(OrderStatus status) {
        if (status == null) return "secondary";
        
        switch (status) {
            case PENDING: return "warning";
            case CONFIRMED: return "info";
            case PROCESSING: return "primary";
            case SHIPPED: return "success";
            case DELIVERED: return "success";
            case CANCELLED: return "danger";
            case REFUNDED: return "secondary";
            default: return "secondary";
        }
    }
    
    /**
     * Retourne le nom complet du client
     */
    public String getCustomerFullName() {
        StringBuilder name = new StringBuilder();
        if (customerFirstName != null) {
            name.append(customerFirstName);
        }
        if (customerLastName != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customerLastName);
        }
        return name.length() > 0 ? name.toString() : customerEmail;
    }
    
    /**
     * Retourne l'adresse complète de facturation
     */
    public String getFullBillingAddress() {
        StringBuilder address = new StringBuilder();
        if (billingAddress != null) address.append(billingAddress);
        if (billingCity != null) {
            if (address.length() > 0) address.append(", ");
            address.append(billingCity);
        }
        if (billingPostalCode != null) {
            if (address.length() > 0) address.append(" ");
            address.append(billingPostalCode);
        }
        if (billingCountry != null) {
            if (address.length() > 0) address.append(", ");
            address.append(billingCountry);
        }
        return address.toString();
    }
    
    /**
     * Vérifie si la commande peut être annulée
     */
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }
    
    /**
     * Vérifie si la commande peut être remboursée
     */
    public boolean canBeRefunded() {
        return (status == OrderStatus.CONFIRMED || status == OrderStatus.PROCESSING ||
                status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) &&
               stripePaymentIntentId != null && paymentStatus != null && 
               paymentStatus.equals("succeeded");
    }
    
    @Override
    public String toString() {
        return "OrderDto{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}