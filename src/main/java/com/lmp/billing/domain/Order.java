package com.lmp.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
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
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "currency", length = 3)
    private String currency = "EUR";
    
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
    
    @Column(name = "billing_name")
    private String billingName;

    @Column(name = "billing_address")
    private String billingAddress;
    
    @Column(name = "billing_city")
    private String billingCity;
    
    @Column(name = "billing_postal_code")
    private String billingPostalCode;
    
    @Column(name = "billing_country")
    private String billingCountry;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "tags")
    private String tags;
    
    @Column(name = "priority")
    private Integer priority = 5;
    
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
    
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "progress_status")
    private String progressStatus;

    /** Copie au moment de la commande : déclaration autoliquidation côté client. */
    @Column(name = "vat_reverse_charge", nullable = false)
    private Boolean vatReverseCharge = false;

    @Column(name = "customer_vat_number", length = 64)
    private String customerVatNumber;

    /** Nom de société saisi par le client lors du checkout (reverse charge). */
    @Column(name = "vat_company_name")
    private String vatCompanyName;

    /** Token opaque pour lien de paiement invité (commande sans user jusqu'à inscription). */
    @Column(name = "checkout_token", length = 64)
    private String checkoutToken;

    /** Taux de TVA appliqué au moment de la commande (ex: 0.2100 pour 21 %). */
    @Column(name = "applied_vat_rate", precision = 5, scale = 4)
    private BigDecimal appliedVatRate;

    /** Snapshot FX : montant en devise de base (EUR) au moment de la création de la commande. */
    @Column(name = "amount_base_eur", precision = 10, scale = 2)
    private BigDecimal amountBaseEur;

    /** Taux de change EUR → currency appliqué (taux effectif avec marge). */
    @Column(name = "fx_rate", precision = 18, scale = 6)
    private BigDecimal fxRate;

    /** Origine du taux : "frankfurter" ou "static". */
    @Column(name = "fx_source", length = 32)
    private String fxSource;

    // ── Fraud scoring fields ──────────────────────────────────────────────
    /** Pays détecté par IP au moment du checkout. */
    @Column(name = "ip_country", length = 2)
    private String ipCountry;

    /** Adresse IP du client au moment du checkout. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Score VPN normalisé (0.0–1.0) consensus multi-sources. */
    @Column(name = "vpn_score", precision = 5, scale = 3)
    private java.math.BigDecimal vpnScore;

    /** Sources VPN consultées avec résultats (ex: "ip-api:true,getipintel:0.99,iphub:1"). */
    @Column(name = "vpn_sources", length = 255)
    private String vpnSources;

    /** Timezone du navigateur (ex: "Europe/Paris"). */
    @Column(name = "browser_timezone", length = 64)
    private String browserTimezone;

    /** Pays via géolocalisation navigateur (reverse geocode). */
    @Column(name = "geo_country", length = 2)
    private String geoCountry;

    /** Pays de la carte bancaire (post-paiement, depuis Stripe PaymentMethod). */
    @Column(name = "card_country", length = 2)
    private String cardCountry;

    /** Score de fiabilité anti-fraude 0–100 (100 = fiable). */
    @Column(name = "fraud_score")
    private Integer fraudScore;

    /** Flags anti-fraude déclenchés (JSON array). */
    @Column(name = "fraud_flags", columnDefinition = "TEXT")
    private String fraudFlags;

    public Order() {}
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    public Set<OrderItem> getItems() { return items; }
    public void setItems(Set<OrderItem> items) { this.items = items; }
    public Set<PaymentTransaction> getPaymentTransactions() { return paymentTransactions; }
    public void setPaymentTransactions(Set<PaymentTransaction> paymentTransactions) { this.paymentTransactions = paymentTransactions; }
    public Set<Invoice> getInvoices() { return invoices; }
    public void setInvoices(Set<Invoice> invoices) { this.invoices = invoices; }
    public Set<Review> getReviews() { return reviews; }
    public void setReviews(Set<Review> reviews) { this.reviews = reviews; }
    public Set<OrderStatusHistory> getStatusHistories() { return statusHistories; }
    public void setStatusHistories(Set<OrderStatusHistory> statusHistories) { this.statusHistories = statusHistories; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }
    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }
    public String getBillingPostalCode() { return billingPostalCode; }
    public void setBillingPostalCode(String billingPostalCode) { this.billingPostalCode = billingPostalCode; }
    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Set<Refund> getRefunds() { return refunds; }
    public void setRefunds(Set<Refund> refunds) { this.refunds = refunds; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public String getProcessingNotes() { return processingNotes; }
    public void setProcessingNotes(String processingNotes) { this.processingNotes = processingNotes; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public String getProgressStatus() { return progressStatus; }
    public void setProgressStatus(String progressStatus) { this.progressStatus = progressStatus; }

    public Boolean getVatReverseCharge() { return vatReverseCharge; }
    public void setVatReverseCharge(Boolean vatReverseCharge) { this.vatReverseCharge = vatReverseCharge; }

    public String getCustomerVatNumber() { return customerVatNumber; }
    public void setCustomerVatNumber(String customerVatNumber) { this.customerVatNumber = customerVatNumber; }

    public String getVatCompanyName() { return vatCompanyName; }
    public void setVatCompanyName(String vatCompanyName) { this.vatCompanyName = vatCompanyName; }

    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String checkoutToken) { this.checkoutToken = checkoutToken; }

    public BigDecimal getAppliedVatRate() { return appliedVatRate; }
    public void setAppliedVatRate(BigDecimal appliedVatRate) { this.appliedVatRate = appliedVatRate; }

    public BigDecimal getAmountBaseEur() { return amountBaseEur; }
    public void setAmountBaseEur(BigDecimal amountBaseEur) { this.amountBaseEur = amountBaseEur; }

    public BigDecimal getFxRate() { return fxRate; }
    public void setFxRate(BigDecimal fxRate) { this.fxRate = fxRate; }

    public String getFxSource() { return fxSource; }
    public void setFxSource(String fxSource) { this.fxSource = fxSource; }

    public String getIpCountry() { return ipCountry; }
    public void setIpCountry(String ipCountry) { this.ipCountry = ipCountry; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public java.math.BigDecimal getVpnScore() { return vpnScore; }
    public void setVpnScore(java.math.BigDecimal vpnScore) { this.vpnScore = vpnScore; }

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

    // --- Paiement en plusieurs fois (installments) ---

    /** Nombre d'échéances choisi par le client (2, 3, 4) — null = paiement unique. */
    @Column(name = "installment_count")
    private Integer installmentCount;

    /** Nom du Payment Terms Template ERPNext associé (ex: "Paiement en 3x"). */
    @Column(name = "payment_terms_template", length = 140)
    private String paymentTermsTemplate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @jakarta.persistence.OrderBy("installmentNumber ASC")
    private Set<OrderInstallment> installments;

    // --- Lien vers le devis d'origine ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    // --- Champs de liaison système externe (agnostique ERP) ---

    @Column(name = "external_order_id", length = 140)
    private String externalOrderId;

    @Column(name = "external_invoice_id", length = 140)
    private String externalInvoiceId;

    @Column(name = "external_payment_id", length = 140)
    private String externalPaymentId;

    public Integer getInstallmentCount() { return installmentCount; }
    public void setInstallmentCount(Integer installmentCount) { this.installmentCount = installmentCount; }

    public String getPaymentTermsTemplate() { return paymentTermsTemplate; }
    public void setPaymentTermsTemplate(String paymentTermsTemplate) { this.paymentTermsTemplate = paymentTermsTemplate; }

    public Set<OrderInstallment> getInstallments() { return installments; }
    public void setInstallments(Set<OrderInstallment> installments) { this.installments = installments; }

    /** Retourne true si la commande est un paiement en plusieurs fois. */
    public boolean isInstallmentOrder() {
        return installmentCount != null && installmentCount > 1;
    }

    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }

    public String getExternalOrderId() { return externalOrderId; }
    public void setExternalOrderId(String externalOrderId) { this.externalOrderId = externalOrderId; }

    public String getExternalInvoiceId() { return externalInvoiceId; }
    public void setExternalInvoiceId(String externalInvoiceId) { this.externalInvoiceId = externalInvoiceId; }

    public String getExternalPaymentId() { return externalPaymentId; }
    public void setExternalPaymentId(String externalPaymentId) { this.externalPaymentId = externalPaymentId; }
}
