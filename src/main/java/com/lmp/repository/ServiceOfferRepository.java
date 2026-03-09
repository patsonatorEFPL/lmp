package com.lmp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.ServiceOffer;

@Repository
public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, UUID> {
    Optional<ServiceOffer> findByIdAndActiveTrue(UUID id);
    List<ServiceOffer> findByServiceIdAndActiveTrue(UUID serviceId);
    List<ServiceOffer> findByServiceId(UUID serviceId);
}
