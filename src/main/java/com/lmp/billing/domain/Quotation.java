package com.lmp.billing.domain;

import com.lmp.auth.domain.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Devis LMP — point d'entrée commercial avant commande.
 * <p>
 * Cycle de vie : DRAFT → SENT → ACCEPTED/REJECTED/EXPIRED.
 * Un devis ACCEPTED peut être converti en {@link Order} via {@code QuotationService.convertToOrder()}.
 * <p>
 * Synchronisé vers le système externe (externalErp Quotation) quand le statut passe à SENT.
 */
@Entity
@Table(name = "quotations")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationItem> items = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.DRAFT;

    /** Montant total TTC (tax-inclusive). */
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Taux de TVA appliqué (ex: 0.2100 pour 21%). */
    @Column(name = "applied_vat_rate", precision = 6, scale = 4)
    private BigDecimal appliedVatRate;

    /** Autoliquidation UE (reverse charge). */
    @Column(name = "vat_reverse_charge")
    private Boolean vatReverseCharge = false;

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    /** Date limite de validité du devis. */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** Date d'acceptation par le client. */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /** Nom de facturation (entreprise ou particulier). */
    @Column(name = "billing_name")
    private String billingName;

    /** Notes internes (admin). */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    /** Notes client (visibles sur le devis). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Sync fields ---

    /** Identifiant externe du Quotation côté système externe (externalErp). */
    @Column(name = "external_quotation_id", length = 140)
    private String externalQuotationId;

    // --- Conversion vers Order ---

    /** Commande issue de la conversion de ce devis (null tant que non converti). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_order_id")
    private Order convertedOrder;

    // --- Timestamps ---

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Quotation() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Helper methods ---

    public void addItem(QuotationItem item) {
        items.add(item);
        item.setQuotation(this);
    }

    public void removeItem(QuotationItem item) {
        items.remove(item);
        item.setQuotation(null);
    }

    public boolean isExpired() {
        return validUntil != null && LocalDateTime.now().isAfter(validUntil);
    }

    public boolean isInstallmentOrder() {
        return false; // Devis = paiement unique pour l'instant
    }

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<QuotationItem> getItems() { return items; }
    public void setItems(List<QuotationItem> items) { this.items = items; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getAppliedVatRate() { return appliedVatRate; }
    public void setAppliedVatRate(BigDecimal appliedVatRate) { this.appliedVatRate = appliedVatRate; }

    public Boolean getVatReverseCharge() { return vatReverseCharge; }
    public void setVatReverseCharge(Boolean vatReverseCharge) { this.vatReverseCharge = vatReverseCharge; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getExternalQuotationId() { return externalQuotationId; }
    public void setExternalQuotationId(String externalQuotationId) { this.externalQuotationId = externalQuotationId; }

    public Order getConvertedOrder() { return convertedOrder; }
    public void setConvertedOrder(Order convertedOrder) { this.convertedOrder = convertedOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
