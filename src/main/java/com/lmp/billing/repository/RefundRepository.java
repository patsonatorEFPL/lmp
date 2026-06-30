package com.lmp.billing.repository;

import com.lmp.billing.domain.Refund;
import com.lmp.billing.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour la gestion des remboursements.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    /**
     * Trouve tous les remboursements d'une commande
     */
    List<Refund> findByOrderOrderByCreatedAtDesc(Order order);

    /**
     * Trouve tous les remboursements d'une commande par ID
     */
    List<Refund> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Trouve un remboursement par son ID Stripe
     */
    Optional<Refund> findByStripeRefundId(String stripeRefundId);

    /**
     * Calcule le montant total remboursé pour une commande
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.id = :orderId AND r.status = 'succeeded'")
    BigDecimal getTotalRefundedAmountByOrderId(@Param("orderId") UUID orderId);

    /**
     * Vérifie si une commande a des remboursements
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Statistiques pour le dashboard admin
     */
    @Query("SELECT " +
           "COUNT(r) as totalRefunds, " +
           "COALESCE(SUM(r.amount), 0) as totalAmount, " +
           "COUNT(CASE WHEN r.status = 'pending' THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN r.status = 'succeeded' THEN 1 END) as succeededCount, " +
           "COUNT(CASE WHEN r.status = 'failed' THEN 1 END) as failedCount " +
           "FROM Refund r " +
           "WHERE r.createdAt >= :startDate")
    Object[] getRefundStatsSince(@Param("startDate") LocalDateTime startDate);
}
