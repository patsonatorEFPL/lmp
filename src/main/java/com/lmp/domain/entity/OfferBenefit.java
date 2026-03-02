package com.lmp.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "offer_benefits")
public class OfferBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private ServiceOffer offer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String benefit;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Constructors
    public OfferBenefit() {}

    public OfferBenefit(ServiceOffer offer, String benefit) {
        this.offer = offer;
        this.benefit = benefit;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ServiceOffer getOffer() { return offer; }
    public void setOffer(ServiceOffer offer) { this.offer = offer; }

    public String getBenefit() { return benefit; }
    public void setBenefit(String benefit) { this.benefit = benefit; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
