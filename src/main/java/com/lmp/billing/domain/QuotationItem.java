package com.lmp.billing.domain;

import com.lmp.catalog.domain.Service;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ligne d'article d'un devis. Même structure que {@link OrderItem}.
 */
@Entity
@Table(name = "quotation_items")
public class QuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    private Integer quantity = 1;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    /** Description libre (optionnelle — override du titre service). */
    @Column(columnDefinition = "TEXT")
    private String description;

    public QuotationItem() {}

    // --- Getters / Setters ---

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Quotation getQuotation() { return quotation; }
    public void setQuotation(Quotation quotation) { this.quotation = quotation; }

    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
