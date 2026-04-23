package com.lmp.billing.repository;

import com.lmp.billing.domain.InstallmentStatus;
import com.lmp.billing.domain.OrderInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les échéances de paiement.
 */
@Repository
public interface OrderInstallmentRepository extends JpaRepository<OrderInstallment, UUID> {

    List<OrderInstallment> findByOrderIdOrderByInstallmentNumberAsc(UUID orderId);

    Optional<OrderInstallment> findByOrderIdAndInstallmentNumber(UUID orderId, Integer installmentNumber);

    Optional<OrderInstallment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<OrderInstallment> findByExternalPaymentId(String externalPaymentId);

    List<OrderInstallment> findByStatusAndDueDateLessThanEqual(InstallmentStatus status, LocalDate date);

    long countByOrderIdAndStatus(UUID orderId, InstallmentStatus status);
}
