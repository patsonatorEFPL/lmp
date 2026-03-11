package com.lmp.billing.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.billing.domain.OrderStatusHistory;
import com.lmp.billing.domain.OrderStatus;

/**
 * Repository pour l'historique des changements de statut des commandes.
 */
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    /**
     * Trouve l'historique d'une commande par ordre chronologique décroissant
     */
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(UUID orderId);

    /**
     * Trouve l'historique d'une commande avec pagination
     */
    Page<OrderStatusHistory> findByOrderId(UUID orderId, Pageable pageable);

    /**
     * Trouve l'historique par commande et statut de destination
     */
    List<OrderStatusHistory> findByOrderIdAndToStatus(UUID orderId, OrderStatus toStatus);

    /**
     * Trouve l'historique par plage de dates
     */
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.changedAt BETWEEN :startDate AND :endDate ORDER BY h.changedAt DESC")
    Page<OrderStatusHistory> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * Recherche avancée avec filtres
     */
    @Query("SELECT h FROM OrderStatusHistory h WHERE " +
           "(:startDate IS NULL OR h.changedAt >= :startDate) AND " +
           "(:endDate IS NULL OR h.changedAt <= :endDate) AND " +
           "(:fromStatus IS NULL OR h.fromStatus = :fromStatus) AND " +
           "(:toStatus IS NULL OR h.toStatus = :toStatus) " +
           "ORDER BY h.changedAt DESC")
    Page<OrderStatusHistory> findHistoryWithFilters(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("fromStatus") OrderStatus fromStatus,
                                                   @Param("toStatus") OrderStatus toStatus,
                                                   Pageable pageable);

    /**
     * Statistiques des changements de statut
     */
    @Query("SELECT h.fromStatus, h.toStatus, COUNT(h) FROM OrderStatusHistory h " +
           "WHERE h.changedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY h.fromStatus, h.toStatus " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> getStatusChangeStats(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Temps moyen entre les transitions de statut
     */
    @Query("SELECT " +
           "h1.toStatus, " +
           "h2.toStatus, " +
           "AVG(TIMESTAMPDIFF(HOUR, h1.changedAt, h2.changedAt)) as avgHours " +
           "FROM OrderStatusHistory h1 " +
           "JOIN OrderStatusHistory h2 ON h1.order.id = h2.order.id " +
           "WHERE h1.changedAt < h2.changedAt " +
           "AND h1.changedAt >= :startDate " +
           "AND h2.changedAt <= :endDate " +
           "AND NOT EXISTS (" +
           "  SELECT h3 FROM OrderStatusHistory h3 " +
           "  WHERE h3.order.id = h1.order.id " +
           "  AND h3.changedAt > h1.changedAt " +
           "  AND h3.changedAt < h2.changedAt" +
           ") " +
           "GROUP BY h1.toStatus, h2.toStatus " +
           "ORDER BY avgHours DESC")
    List<Object[]> getAverageTransitionTimes(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Nombre de changements par commande
     */
    @Query("SELECT h.order.id, COUNT(h) FROM OrderStatusHistory h GROUP BY h.order.id ORDER BY COUNT(h) DESC")
    List<Object[]> getChangeCountByOrder();

    /**
     * Changements récents
     */
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.changedAt >= :since ORDER BY h.changedAt DESC")
    List<OrderStatusHistory> findRecentChanges(@Param("since") LocalDateTime since);

    /**
     * Changements par utilisateur administrateur
     */
    @Query("SELECT h.changedBy, COUNT(h), h.toStatus FROM OrderStatusHistory h " +
           "WHERE h.changedBy IS NOT NULL " +
           "AND h.changedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY h.changedBy, h.toStatus " +
           "ORDER BY h.changedBy, COUNT(h) DESC")
    List<Object[]> getChangesByAdmin(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Dernière modification par commande
     */
    @Query("SELECT h FROM OrderStatusHistory h WHERE h.changedAt = (" +
           "SELECT MAX(h2.changedAt) FROM OrderStatusHistory h2 WHERE h2.order.id = h.order.id" +
           ") AND h.order.id = :orderId")
    OrderStatusHistory findLatestByOrderId(@Param("orderId") UUID orderId);

    /**
     * Commandes avec des changements multiples
     */
    @Query("SELECT h.order.id, COUNT(h) FROM OrderStatusHistory h " +
           "GROUP BY h.order.id " +
           "HAVING COUNT(h) > :minChanges " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findOrdersWithMultipleChanges(@Param("minChanges") Long minChanges);

    /**
     * Annulations dans la période
     */
    @Query("SELECT h FROM OrderStatusHistory h " +
           "WHERE h.toStatus = 'CANCELLED' " +
           "AND h.changedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.changedAt DESC")
    List<OrderStatusHistory> findCancellationsInPeriod(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Remboursements dans la période
     */
    @Query("SELECT h FROM OrderStatusHistory h " +
           "WHERE h.toStatus = 'REFUNDED' " +
           "AND h.changedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY h.changedAt DESC")
    List<OrderStatusHistory> findRefundsInPeriod(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * Délai moyen de traitement par statut
     */
    @Query("SELECT " +
           "h.toStatus, " +
           "AVG(TIMESTAMPDIFF(HOUR, o.createdAt, h.changedAt)) as avgProcessingHours " +
           "FROM OrderStatusHistory h " +
           "JOIN h.order o " +
           "WHERE h.changedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY h.toStatus " +
           "ORDER BY avgProcessingHours")
    List<Object[]> getAverageProcessingTimeByStatus(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
}
