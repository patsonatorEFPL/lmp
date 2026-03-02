package com.lmp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.OfferBenefit;

@Repository
public interface OfferBenefitRepository extends JpaRepository<OfferBenefit, Long> {
    List<OfferBenefit> findByOfferIdOrderByDisplayOrderAsc(Long offerId);
    void deleteByOfferId(Long offerId);
}
