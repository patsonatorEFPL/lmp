package com.lmp.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    private Boolean featured = false;

    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ServiceBenefit> benefits;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ServiceOffer> offers;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CartItem> cartItems;

    // Constructors
    public Service() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    // Helpers
    @Transient
    public ServiceOffer getCurrentOffer() {
        if (offers == null || offers.isEmpty())
            return null;

        // Priority 1: Currently valid promotional offer
        ServiceOffer validPromo = offers.stream()
                .filter(ServiceOffer::isCurrentlyValid)
                .filter(o -> !Boolean.TRUE.equals(o.getIsDefault()))
                .findFirst()
                .orElse(null);

        if (validPromo != null) {
            return validPromo;
        }

        // Priority 2: Default active offer
        return offers.stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .filter(o -> Boolean.TRUE.equals(o.getIsDefault()))
                .findFirst()
                .orElse(null);
    }

    public void addBenefit(ServiceBenefit benefit) {
        if (benefits == null) {
            benefits = new java.util.LinkedHashSet<>();
        }
        benefits.add(benefit);
        benefit.setService(this);
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public Set<ServiceBenefit> getBenefits() {
        return benefits;
    }

    public void setBenefits(Set<ServiceBenefit> benefits) {
        this.benefits = benefits;
    }

    public Set<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(Set<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Set<ServiceOffer> getOffers() {
        return offers;
    }

    public void setOffers(Set<ServiceOffer> offers) {
        this.offers = offers;
    }

    public Set<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(Set<CartItem> cartItems) {
        this.cartItems = cartItems;
    }
}
