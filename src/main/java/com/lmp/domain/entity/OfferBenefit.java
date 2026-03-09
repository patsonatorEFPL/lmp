package com.lmp.domain.entity;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "offer_benefits")
public class OfferBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private ServiceOffer offer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String benefit;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public OfferBenefit() {}

    public OfferBenefit(ServiceOffer offer, String benefit) {
        this.offer = offer;
        this.benefit = benefit;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ServiceOffer getOffer() { return offer; }
    public void setOffer(ServiceOffer offer) { this.offer = offer; }
    public String getBenefit() { return benefit; }
    public void setBenefit(String benefit) { this.benefit = benefit; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
