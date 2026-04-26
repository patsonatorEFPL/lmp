package com.lmp.billing.repository;

import com.lmp.billing.domain.Quotation;
import com.lmp.billing.domain.QuotationStatus;
import com.lmp.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    List<Quotation> findByUserOrderByCreatedAtDesc(User user);

    Page<Quotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status, Pageable pageable);

    Optional<Quotation> findByExternalQuotationId(String externalQuotationId);

    /**
     * Charge un devis avec son User et ses Items pour la synchronisation externe.
     * Évite les LazyInitializationException hors session Hibernate.
     */
    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.user LEFT JOIN FETCH q.items i LEFT JOIN FETCH i.service WHERE q.id = :id")
    Optional<Quotation> findByIdWithUserAndItems(@Param("id") UUID id);

    /**
     * Devis SENT expirés (validUntil dépassé).
     */
    @Query("SELECT q FROM Quotation q WHERE q.status = 'SENT' AND q.validUntil IS NOT NULL AND q.validUntil < :now")
    List<Quotation> findExpiredQuotations(@Param("now") LocalDateTime now);
}
