package com.lmp.shared.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ligne unique (id = 1) : adresse et ville/province affichées sur les factures PDF.
 */
@Entity
@Table(name = "company_profile")
public class CompanyProfile {

    public static final short SINGLETON_ID = 1;

    @Id
    @Column(nullable = false)
    private Short id = SINGLETON_ID;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "city_region", nullable = false, length = 200)
    private String cityRegion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getCityRegion() {
        return cityRegion;
    }

    public void setCityRegion(String cityRegion) {
        this.cityRegion = cityRegion;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
