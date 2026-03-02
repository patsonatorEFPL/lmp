package com.lmp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.ServiceOffer;

@Repository
public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, Long> {

    Optional<ServiceOffer> findByIdAndActiveTrue(Long id);

    java.util.List<ServiceOffer> findByServiceIdAndActiveTrue(Long serviceId);

    java.util.List<ServiceOffer> findByServiceId(Long serviceId);
}
