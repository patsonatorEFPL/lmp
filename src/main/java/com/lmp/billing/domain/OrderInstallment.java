package com.lmp.billing.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Représente une échéance de paiement pour une commande en plusieurs fois.
 * <p>
 * Aligné sur le modèle externalErp Payment Schedule :
 * chaque échéance correspond à un Payment Term avec un pourcentage du total,
 * une date d'échéance et un suivi du paiement (Stripe + ERP).
 */
@Entity
@Table(name = "order_installments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "installment_number"}))
public class OrderInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Numéro d'échéance (1, 2, 3…). */
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    /** Nom du Payment Term externalErp (ex: "1ère échéance"). */
    @Column(name = "payment_term", nullable = false, length = 140)
    private String paymentTerm;

    /** Pourcentage de la facture (ex: 33.33). */
    @Column(name = "invoice_portion", precision = 6, scale = 2, nullable = false)
    private BigDecimal invoicePortion;

    /** Montant calculé pour cette échéance. */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /** Date d'échéance prévue. */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /** Statut du paiement de cette échéance. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    /** Stripe PaymentIntent ID pour cette échéance. */
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    /** ID du Payment Entry créé dans le système externe. */
    @Column(name = "external_payment_id", length = 140)
    private String externalPaymentId;

    /** Date effective du paiement. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrderInstallment() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }

    public String getPaymentTerm() { return paymentTerm; }
    public void setPaymentTerm(String paymentTerm) { this.paymentTerm = paymentTerm; }

    public BigDecimal getInvoicePortion() { return invoicePortion; }
    public void setInvoicePortion(BigDecimal invoicePortion) { this.invoicePortion = invoicePortion; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }

    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) { this.stripePaymentIntentId = stripePaymentIntentId; }

    public String getExternalPaymentId() { return externalPaymentId; }
    public void setExternalPaymentId(String externalPaymentId) { this.externalPaymentId = externalPaymentId; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
