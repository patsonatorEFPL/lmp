package com.lmp.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.OfferBenefit;

@Repository
public interface OfferBenefitRepository extends JpaRepository<OfferBenefit, UUID> {
    List<OfferBenefit> findByOfferIdOrderByDisplayOrderAsc(UUID offerId);
    void deleteByOfferId(UUID offerId);
}
