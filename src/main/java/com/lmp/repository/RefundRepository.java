package com.lmp.repository;

import com.lmp.domain.entity.Refund;
import com.lmp.domain.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des remboursements.
 * Fournit des méthodes de recherche et d'agrégation pour l'administration.
 */
import java.util.UUID;

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
     * Trouve tous les remboursements par statut
     */
    Page<Refund> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    /**
     * Trouve les remboursements en attente
     */
    List<Refund> findByStatusOrderByCreatedAtAsc(String status);

    /**
     * Trouve les remboursements créés dans une période
     */
    @Query("SELECT r FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    Page<Refund> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate, 
                                Pageable pageable);

    /**
     * Trouve les remboursements traités par un administrateur
     */
    Page<Refund> findByProcessedByOrderByCreatedAtDesc(String processedBy, Pageable pageable);

    /**
     * Calcule le montant total remboursé pour une commande
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.order.id = :orderId AND r.status = 'succeeded'")
    BigDecimal getTotalRefundedAmountByOrderId(@Param("orderId") UUID orderId);

    /**
     * Calcule le montant total remboursé sur une période
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = 'succeeded' AND r.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRefundedInPeriod(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Compte les remboursements par statut
     */
    @Query("SELECT r.status, COUNT(r) FROM Refund r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * Trouve les remboursements échoués récents pour retry
     */
    @Query("SELECT r FROM Refund r WHERE r.status = 'failed' AND r.createdAt > :cutoffDate ORDER BY r.createdAt DESC")
    List<Refund> findFailedRefundsAfterDate(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Trouve les plus gros remboursements
     */
    @Query("SELECT r FROM Refund r WHERE r.status = 'succeeded' ORDER BY r.amount DESC")
    Page<Refund> findTopRefundsByAmount(Pageable pageable);

    /**
     * Statistiques de remboursement par mois
     */
    @Query("SELECT YEAR(r.processedAt), MONTH(r.processedAt), COUNT(r), SUM(r.amount) " +
           "FROM Refund r WHERE r.status = 'succeeded' " +
           "GROUP BY YEAR(r.processedAt), MONTH(r.processedAt) " +
           "ORDER BY YEAR(r.processedAt) DESC, MONTH(r.processedAt) DESC")
    List<Object[]> getMonthlyRefundStats();

    /**
     * Recherche de remboursements avec critères multiples
     */
    @Query("SELECT r FROM Refund r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:orderId IS NULL OR r.order.id = :orderId) " +
           "AND (:processedBy IS NULL OR r.processedBy = :processedBy) " +
           "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.createdAt <= :endDate) " +
           "AND (:minAmount IS NULL OR r.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR r.amount <= :maxAmount) " +
           "ORDER BY r.createdAt DESC")
    Page<Refund> searchRefunds(@Param("status") String status,
                              @Param("orderId") UUID orderId,
                              @Param("processedBy") String processedBy,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("minAmount") BigDecimal minAmount,
                              @Param("maxAmount") BigDecimal maxAmount,
                              Pageable pageable);

    /**
     * Vérifie si une commande a des remboursements
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Compte le nombre de remboursements pour une commande
     */
    long countByOrderId(UUID orderId);

    /**
     * Trouve les remboursements partiels (basé sur le montant vs montant total de la commande)
     */
    @Query("SELECT r FROM Refund r WHERE r.amount < r.order.totalAmount ORDER BY r.createdAt DESC")
    Page<Refund> findPartialRefunds(Pageable pageable);

    /**
     * Trouve les remboursements complets (basé sur le montant vs montant total de la commande)
     */
    @Query("SELECT r FROM Refund r WHERE r.amount >= r.order.totalAmount ORDER BY r.createdAt DESC")
    Page<Refund> findFullRefunds(Pageable pageable);

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

    /**
     * Trouve les remboursements nécessitant une attention (échecs, en attente depuis longtemps)
     */
    @Query("SELECT r FROM Refund r WHERE " +
           "(r.status = 'failed') OR " +
           "(r.status = 'pending' AND r.createdAt < :oldPendingCutoff) " +
           "ORDER BY r.createdAt ASC")
    List<Refund> findRefundsNeedingAttention(@Param("oldPendingCutoff") LocalDateTime oldPendingCutoff);
    /**
     * Recherche avancée de remboursements (alias pour compatibilité)
     */
    default Page<Refund> searchRefundsAdvanced(String status, String reason,
                                             LocalDateTime startDate, LocalDateTime endDate,
                                             BigDecimal minAmount, BigDecimal maxAmount,
                                             Pageable pageable) {
        return searchRefunds(status, null, null, startDate, endDate, minAmount, maxAmount, pageable);
    }
    
    /**
     * Statistiques par statut (alias pour compatibilité)
     */
    default List<Object[]> getRefundStatsByStatus() {
        return countByStatus();
    }
    
    /**
     * Statistiques mensuelles avec date de début (surcharge)
     */
    @Query("SELECT YEAR(r.processedAt), MONTH(r.processedAt), COUNT(r), SUM(r.amount) " +
           "FROM Refund r WHERE r.status = 'succeeded' AND r.processedAt >= :startDate " +
           "GROUP BY YEAR(r.processedAt), MONTH(r.processedAt) " +
           "ORDER BY YEAR(r.processedAt) DESC, MONTH(r.processedAt) DESC")
    List<Object[]> getMonthlyRefundStats(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Top des raisons de remboursement
     */
    @Query("SELECT r.reason, COUNT(r), SUM(r.amount) FROM Refund r " +
           "WHERE r.processedAt >= :startDate AND r.status = 'succeeded' " +
           "GROUP BY r.reason ORDER BY COUNT(r) DESC")
    List<Object[]> getTopRefundReasons(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    /**
     * Total remboursé pour une commande (alias pour compatibilité)
     */
    default BigDecimal getTotalRefundedByOrder(UUID orderId) {
        return getTotalRefundedAmountByOrderId(orderId);
    }
}