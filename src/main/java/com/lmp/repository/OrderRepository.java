package com.lmp.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.OrderStatus;

/**
 * Repository étendu pour la gestion complète des commandes.
 * Inclut les fonctionnalités avancées pour l'administration.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // ========== Méthodes existantes ==========
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    
    // Nouvelles méthodes pour la gestion des sessions Stripe
    Optional<Order> findByStripeSessionId(String stripeSessionId);
    List<Order> findByUserAndStatusNotOrderByCreatedAtDesc(User user, OrderStatus status);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);

    /**
     * Commandes PAYMENT_PENDING avec un stripeSessionId, créées avant un seuil donné.
     * Utilisé par le service de réconciliation pour vérifier le statut auprès de Stripe.
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.stripeSessionId IS NOT NULL AND o.createdAt < :cutoff ORDER BY o.createdAt ASC")
    List<Order> findStaleOrdersWithStripeSession(@Param("status") OrderStatus status, @Param("cutoff") LocalDateTime cutoff);

    /**
     * Commandes CANCELLED qui ont un stripeSessionId et ont été annulées récemment.
     * Utilisé par le service de réconciliation pour récupérer les commandes annulées mais payées.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CANCELLED' AND o.stripeSessionId IS NOT NULL AND o.cancelledAt > :since ORDER BY o.cancelledAt DESC")
    List<Order> findRecentlyCancelledWithStripeSession(@Param("since") LocalDateTime since);

    // ========== Méthodes d'administration avancées ==========
    
    /**
     * Recherche avancée avec pagination et tri
     */
    @Query("SELECT o FROM Order o " +
           "WHERE (:searchTerm IS NULL OR " +
           "       LOWER(o.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(o.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(o.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       LOWER(o.serviceName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "       CAST(o.id AS string) LIKE CONCAT('%', :searchTerm, '%') OR " +
           "       LOWER(o.stripePaymentIntentId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:customerEmail IS NULL OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :customerEmail, '%'))) " +
           "AND (:serviceName IS NULL OR LOWER(o.serviceName) LIKE LOWER(CONCAT('%', :serviceName, '%'))) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:hasRefunds IS NULL OR " +
           "     (:hasRefunds = true AND EXISTS (SELECT r FROM Refund r WHERE r.order = o)) OR " +
           "     (:hasRefunds = false AND NOT EXISTS (SELECT r FROM Refund r WHERE r.order = o)))")
    Page<Order> searchOrdersAdvanced(@Param("searchTerm") String searchTerm,
                                    @Param("customerEmail") String customerEmail,
                                    @Param("serviceName") String serviceName,
                                    @Param("status") OrderStatus status,
                                    @Param("minAmount") BigDecimal minAmount,
                                    @Param("maxAmount") BigDecimal maxAmount,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    @Param("paymentStatus") String paymentStatus,
                                    @Param("hasRefunds") Boolean hasRefunds,
                                    Pageable pageable);

    /**
     * Trouve les commandes par plage de dates avec pagination
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByDateRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate,
                               Pageable pageable);

    /**
     * Trouve les commandes par client avec pagination
     */
    Page<Order> findByUserEmailContainingIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);

    /**
     * Trouve les commandes par service avec pagination
     */
    Page<Order> findByServiceNameContainingIgnoreCaseOrderByCreatedAtDesc(String serviceName, Pageable pageable);

    /**
     * Trouve les commandes par statut avec pagination
     */
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    /**
     * Trouve les commandes par Stripe Payment Intent ID
     */
    Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Statistiques globales des commandes
     */
    @Query("SELECT " +
           "COUNT(o) as totalOrders, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalRevenue, " +
           "COALESCE(AVG(o.totalAmount), 0) as averageOrderValue, " +
           "COUNT(DISTINCT o.user) as totalCustomers " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate")
    Object[] getOrderStatsSince(@Param("startDate") LocalDateTime startDate);

    /**
     * Statistiques par statut
     */
    @Query("SELECT o.status, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatsByStatus();

    /**
     * Statistiques mensuelles
     */
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt) DESC, MONTH(o.createdAt) DESC")
    List<Object[]> getMonthlyOrderStats(@Param("startDate") LocalDateTime startDate);

    /**
     * Statistiques quotidiennes pour graphiques
     */
    @Query("SELECT DATE(o.createdAt), COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(o.createdAt) " +
           "ORDER BY DATE(o.createdAt)")
    List<Object[]> getDailyOrderStats(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Top services par nombre de commandes
     */
    @Query("SELECT o.serviceName, COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate " +
           "GROUP BY o.serviceName " +
           "ORDER BY COUNT(o) DESC")
    List<Object[]> getTopServicesByOrderCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    /**
     * Top services par chiffre d'affaires
     */
    @Query("SELECT o.serviceName, COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate " +
           "GROUP BY o.serviceName " +
           "ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> getTopServicesByRevenue(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    /**
     * Commandes nécessitant une attention (anciennes, en attente, etc.)
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(o.status = 'PENDING' AND o.createdAt < :oldPendingCutoff) OR " +
           "(o.status = 'PAYMENT_PENDING' AND o.createdAt < :paymentPendingCutoff) OR " +
           "(o.paymentStatus = 'failed') " +
           "ORDER BY o.createdAt ASC")
    List<Order> findOrdersNeedingAttention(@Param("oldPendingCutoff") LocalDateTime oldPendingCutoff,
                                          @Param("paymentPendingCutoff") LocalDateTime paymentPendingCutoff);

    /**
     * Commandes récentes par priorité
     */
    @Query("SELECT o FROM Order o WHERE o.priority <= :maxPriority ORDER BY o.priority ASC, o.createdAt DESC")
    Page<Order> findRecentOrdersByPriority(@Param("maxPriority") Integer maxPriority, Pageable pageable);

    /**
     * Recherche de commandes par tags
     */
    @Query("SELECT o FROM Order o WHERE o.tags LIKE CONCAT('%', :tag, '%')")
    Page<Order> findByTagsContaining(@Param("tag") String tag, Pageable pageable);

    /**
     * Commandes avec remboursements
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.refunds r WHERE r.status = 'succeeded'")
    Page<Order> findOrdersWithRefunds(Pageable pageable);

    /**
     * Commandes sans remboursements
     */
    @Query("SELECT o FROM Order o WHERE NOT EXISTS (SELECT r FROM Refund r WHERE r.order = o)")
    Page<Order> findOrdersWithoutRefunds(Pageable pageable);

    /**
     * Commandes par période et statut de paiement
     */
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :paymentStatus AND o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByPaymentStatusAndDateRange(@Param("paymentStatus") String paymentStatus,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    /**
     * Calcul du montant total remboursé par commande
     */
    @Query("SELECT o.id, COALESCE(SUM(r.amount), 0) FROM Order o LEFT JOIN o.refunds r WHERE r.status = 'succeeded' GROUP BY o.id")
    List<Object[]> getTotalRefundedByOrder();

    /**
     * Temps moyen de traitement par statut
     */
    @Query("SELECT " +
           "AVG(CASE WHEN o.paidAt IS NOT NULL THEN TIMESTAMPDIFF(HOUR, o.createdAt, o.paidAt) END) as avgPaymentTime, " +
           "AVG(CASE WHEN o.shippedAt IS NOT NULL THEN TIMESTAMPDIFF(HOUR, o.createdAt, o.shippedAt) END) as avgShippingTime, " +
           "AVG(CASE WHEN o.deliveredAt IS NOT NULL THEN TIMESTAMPDIFF(HOUR, o.createdAt, o.deliveredAt) END) as avgDeliveryTime " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate")
    Object[] getAverageProcessingTimes(@Param("startDate") LocalDateTime startDate);

    /**
     * Nouveaux clients vs clients récurrents
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN (SELECT COUNT(o2) FROM Order o2 WHERE o2.user = o.user AND o2.createdAt < o.createdAt) = 0 THEN 1 END) as newCustomers, " +
           "COUNT(CASE WHEN (SELECT COUNT(o2) FROM Order o2 WHERE o2.user = o.user AND o2.createdAt < o.createdAt) > 0 THEN 1 END) as returningCustomers " +
           "FROM Order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Object[] getCustomerTypeStats(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Dashboard rapide - dernières commandes
     */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findLatestOrders(Pageable pageable);

    /**
     * Export de données pour rapports
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR o.createdAt <= :endDate) AND " +
           "(:status IS NULL OR o.status = :status) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findOrdersForExport(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("status") OrderStatus status);
}
