package com.lmp.catalog.domain;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "service_benefits")
public class ServiceBenefit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String benefit;
    
    public ServiceBenefit() {}
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public String getBenefit() { return benefit; }
    public void setBenefit(String benefit) { this.benefit = benefit; }
}
