package com.lmp.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.lmp.domain.enums.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // Permet l'anonymisation lors du hard delete
    private User user;
    
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PAYMENT_PENDING;
    
    @Column(name = "stripe_session_id")
    private String stripeSessionId;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Nouveaux champs pour la gestion complète des commandes
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    // Le montant principal est déjà défini par totalAmount
    // @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    // private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency = "CAD";
    
    // Informations Stripe étendues
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;
    
    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_status")
    private String paymentStatus;
    
    @Column(name = "stripe_charge_id")
    private String stripeChargeId;
    
    // Informations d'adresse de facturation
    @Column(name = "billing_address")
    private String billingAddress;
    
    @Column(name = "billing_city")
    private String billingCity;
    
    @Column(name = "billing_postal_code")
    private String billingPostalCode;
    
    @Column(name = "billing_country")
    private String billingCountry;
    
    // Dates importantes
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    // Métadonnées administratives
    @Column(name = "tags")
    private String tags;
    
    @Column(name = "priority")
    private Integer priority = 5; // 1=urgent, 5=normal, 10=basse
    
    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> items;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PaymentTransaction> paymentTransactions;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Invoice> invoices;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Review> reviews;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderStatusHistory> statusHistories;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Refund> refunds;
    
    // Champs supplémentaires pour l'administration
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes;
    
    // Constructors
    public Order() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getStripeSessionId() {
        return stripeSessionId;
    }
    
    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }
    
    public Set<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(Set<OrderItem> items) {
        this.items = items;
    }
    
    public Set<PaymentTransaction> getPaymentTransactions() {
        return paymentTransactions;
    }
    
    public void setPaymentTransactions(Set<PaymentTransaction> paymentTransactions) {
        this.paymentTransactions = paymentTransactions;
    }
    
    public Set<Invoice> getInvoices() {
        return invoices;
    }
    
    public void setInvoices(Set<Invoice> invoices) {
        this.invoices = invoices;
    }
    
    public Set<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }
    
    public Set<OrderStatusHistory> getStatusHistories() {
        return statusHistories;
    }
    
    public void setStatusHistories(Set<OrderStatusHistory> statusHistories) {
        this.statusHistories = statusHistories;
    }
    
    // Getters et Setters pour les nouveaux champs
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    // Méthodes pour amount supprimées - nous utilisons totalAmount
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
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
    
    public Set<Refund> getRefunds() {
        return refunds;
    }
    
    public void setRefunds(Set<Refund> refunds) {
        this.refunds = refunds;
    }
    
    // Getters et Setters pour les champs supplémentaires
    
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
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getProcessingNotes() {
        return processingNotes;
    }
    
    public void setProcessingNotes(String processingNotes) {
        this.processingNotes = processingNotes;
    }
}
