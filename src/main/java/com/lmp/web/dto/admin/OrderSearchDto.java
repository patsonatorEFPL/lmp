package com.lmp.web.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les critères de recherche et filtrage des commandes dans l'administration.
 * Permet une recherche avancée multi-critères avec pagination.
 */
public class OrderSearchDto {

    // Critères de recherche texte
    private String searchTerm; // Recherche globale dans ID, email client, nom service
    private String customerEmail;
    private String customerName;
    private String serviceName;
    private String orderId;
    private String stripePaymentIntentId;

    // Critères de filtrage par statut
    private String status; // PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED

    // Critères de filtrage par montant
    @PositiveOrZero(message = "Le montant minimum doit être positif ou zéro")
    private BigDecimal minAmount;

    @PositiveOrZero(message = "Le montant maximum doit être positif ou zéro")
    private BigDecimal maxAmount;

    private String currency = "EUR";

    // Critères de filtrage par date
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    // Critères de tri
    private String sortBy = "createdAt"; // createdAt, amount, status, customerEmail
    private String sortDirection = "DESC"; // ASC, DESC

    // Pagination
    @Min(value = 0, message = "Le numéro de page doit être positif ou zéro")
    private int page = 0;

    @Min(value = 1, message = "La taille de page doit être au minimum 1")
    private int size = 20;

    // Filtres avancés
    private Boolean hasRefunds; // Null = tous, true = avec remboursements, false = sans remboursements
    private Boolean isDisputed; // Null = tous, true = en litige, false = pas en litige
    private Integer daysSinceCreated; // Commandes créées dans les X derniers jours

    // Filtres par type de paiement
    private String paymentMethod; // card, bank_transfer, etc.
    private String paymentStatus; // succeeded, failed, pending, etc.

    // Export options
    private String exportFormat; // csv, excel, pdf
    private boolean includeCustomerDetails = false;
    private boolean includePaymentDetails = false;

    // Constructeurs
    public OrderSearchDto() {}

    // Getters et Setters

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Boolean getHasRefunds() {
        return hasRefunds;
    }

    public void setHasRefunds(Boolean hasRefunds) {
        this.hasRefunds = hasRefunds;
    }

    public Boolean getIsDisputed() {
        return isDisputed;
    }

    public void setIsDisputed(Boolean isDisputed) {
        this.isDisputed = isDisputed;
    }

    public Integer getDaysSinceCreated() {
        return daysSinceCreated;
    }

    public void setDaysSinceCreated(Integer daysSinceCreated) {
        this.daysSinceCreated = daysSinceCreated;
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

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }

    public boolean isIncludeCustomerDetails() {
        return includeCustomerDetails;
    }

    public void setIncludeCustomerDetails(boolean includeCustomerDetails) {
        this.includeCustomerDetails = includeCustomerDetails;
    }

    public boolean isIncludePaymentDetails() {
        return includePaymentDetails;
    }

    public void setIncludePaymentDetails(boolean includePaymentDetails) {
        this.includePaymentDetails = includePaymentDetails;
    }

    /**
     * Vérifie si la recherche a des critères actifs
     */
    public boolean hasSearchCriteria() {
        return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
               (customerEmail != null && !customerEmail.trim().isEmpty()) ||
               (customerName != null && !customerName.trim().isEmpty()) ||
               (serviceName != null && !serviceName.trim().isEmpty()) ||
               (orderId != null && !orderId.trim().isEmpty()) ||
               (stripePaymentIntentId != null && !stripePaymentIntentId.trim().isEmpty()) ||
               (status != null && !status.trim().isEmpty()) ||
               minAmount != null || maxAmount != null ||
               startDate != null || endDate != null ||
               hasRefunds != null || isDisputed != null ||
               daysSinceCreated != null ||
               (paymentMethod != null && !paymentMethod.trim().isEmpty()) ||
               (paymentStatus != null && !paymentStatus.trim().isEmpty());
    }

    /**
     * Réinitialise tous les critères de recherche
     */
    public void reset() {
        this.searchTerm = null;
        this.customerEmail = null;
        this.customerName = null;
        this.serviceName = null;
        this.orderId = null;
        this.stripePaymentIntentId = null;
        this.status = null;
        this.minAmount = null;
        this.maxAmount = null;
        this.startDate = null;
        this.endDate = null;
        this.hasRefunds = null;
        this.isDisputed = null;
        this.daysSinceCreated = null;
        this.paymentMethod = null;
        this.paymentStatus = null;
        this.page = 0;
    }

    @Override
    public String toString() {
        return "OrderSearchDto{" +
                "searchTerm='" + searchTerm + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", status='" + status + '\'' +
                ", minAmount=" + minAmount +
                ", maxAmount=" + maxAmount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                ", page=" + page +
                ", size=" + size +
                '}';
    }
}