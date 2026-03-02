package com.lmp.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import com.lmp.domain.entity.enums.DurationType;

@Entity
@Table(name = "service_offers")
public class ServiceOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false)
    private DurationType durationType; // ONE_TIME, MONTHLY, YEARLY

    /**
     * Colonne legacy conservée en base de données de production.
     * Synchronisée automatiquement avec durationType via @PrePersist/@PreUpdate.
     */
    @Column(name = "duration")
    private String duration;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OfferBenefit> benefits;

    // Constructors
    public ServiceOffer() {
    }

    public ServiceOffer(Service service, String name, BigDecimal price, BigDecimal originalPrice,
            DurationType durationType,
            Boolean isDefault) {
        this.service = service;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.durationType = durationType;
        this.duration = (durationType != null) ? durationType.name() : "ONE_TIME";
        this.isDefault = isDefault;
    }

    @PrePersist
    @PreUpdate
    private void syncDurationField() {
        this.duration = (durationType != null) ? durationType.name() : "ONE_TIME";
    }

    // Transient method to evaluate validity strictly
    @Transient
    public boolean isCurrentlyValid() {
        if (!active)
            return false;
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom))
            return false;
        if (validTo != null && now.isAfter(validTo))
            return false;
        return true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public DurationType getDurationType() {
        return durationType;
    }

    public void setDurationType(DurationType durationType) {
        this.durationType = durationType;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<OfferBenefit> getBenefits() {
        return benefits;
    }

    public void setBenefits(Set<OfferBenefit> benefits) {
        this.benefits = benefits;
    }
}
