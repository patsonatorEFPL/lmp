package com.lmp.billing.service;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour la gestion et le nettoyage des commandes
 * Nettoie les commandes PAYMENT_PENDING qui n'ont pas été finalisées dans un délai raisonnable
 */
@Service
@Transactional
public class OrderCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(OrderCleanupService.class);

    private final OrderRepository orderRepository;

    private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;

    private final StripeClient stripeClient;

    public OrderCleanupService(OrderRepository orderRepository,
            OrderRealtimeEventPublisher orderRealtimeEventPublisher,
            StripeClient stripeClient) {
        this.orderRepository = orderRepository;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
        this.stripeClient = stripeClient;
    }

    /**
     * Durée maximale en minutes pour laisser une commande PAYMENT_PENDING avant nettoyage
     * La valeur par défaut est 30 minutes, configurable via application.properties
     */
    @Value("${lmp.order.payment.pending.timeout.minutes:30}")
    private int paymentPendingTimeoutMinutes;

    /**
     * Durée maximale en jours pour conserver une commande CANCELLED avant suppression
     * La valeur par défaut est 5 jours, configurable via application.properties
     */
    @Value("${lmp.order.cancelled.retention.days:5}")
    private int cancelledRetentionDays;

    /**
     * Tâche planifiée pour nettoyer les commandes PAYMENT_PENDING qui n'ont pas été finalisées
     * Exécutée toutes les heures pour nettoyer les commandes abandonnées
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    public void cleanupStalePaymentPendingOrders() {
        logger.info("Démarrage du nettoyage des commandes PAYMENT_PENDING non finalisées");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(paymentPendingTimeoutMinutes);
            
            // Trouver les commandes PAYMENT_PENDING plus anciennes que le seuil
            List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoffTime);
            
            if (staleOrders.isEmpty()) {
                logger.info("Aucune commande PAYMENT_PENDING à nettoyer. Fin du nettoyage.");
                return;
            }

            logger.info("Trouvé {} commandes PAYMENT_PENDING à nettoyer (cutoff: {})", 
                       staleOrders.size(), cutoffTime);

            int cancelledCount = 0;
            int errorCount = 0;

            for (Order order : staleOrders) {
                try {
                    // Vérifier auprès de Stripe avant d'annuler
                    if (isActuallyPaidOnStripe(order)) {
                        // Le paiement a été effectué — ne pas annuler, confirmer la commande
                        OrderStatus previous = order.getStatus();
                        order.setStatus(OrderStatus.CONFIRMED);
                        order.setPaymentStatus("succeeded");
                        order.setPaidAt(LocalDateTime.now());
                        order.setUpdatedAt(LocalDateTime.now());
                        order.setPaymentMethod("stripe_checkout");
                        OrderProgressSync.applyMinimumForStatus(order);
                        orderRepository.save(order);
                        orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, previous,
                                OrderStatus.CONFIRMED);

                        logger.info("✅ Commande {} confirmée par vérification Stripe lors du nettoyage (créée le: {})",
                                   order.getId(), order.getCreatedAt());
                        continue; // Ne pas compter comme annulée
                    }

                    // Annuler la commande car le délai de paiement est expiré et non payée
                    OrderStatus previous = order.getStatus();
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancellationReason("Paiement non finalisé dans le délai imparti (" + paymentPendingTimeoutMinutes + " minutes)");
                    order.setCancelledAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());
                    OrderProgressSync.applyMinimumForStatus(order);
                    orderRepository.save(order);
                    orderRealtimeEventPublisher.publishOrderUpdated(order, previous, OrderStatus.CANCELLED);
                    cancelledCount++;
                    
                    logger.info("Commande {} annulée car Paiement non finalisé (créée le: {})", 
                               order.getId(), order.getCreatedAt());
                    
                } catch (Exception e) {
                    logger.error("Erreur lors de l'annulation de la commande {} : {}", order.getId(), e.getMessage(), e);
                    errorCount++;
                }
            }

            logger.info("Nettoyage terminé - {} commandes annulées, {} erreurs", cancelledCount, errorCount);

        } catch (Exception e) {
            logger.error("Erreur critique lors du nettoyage des commandes PAYMENT_PENDING : {}", e.getMessage(), e);
        }
    }

    /**
     * Méthode pour nettoyer manuellement les commandes PAYMENT_PENDING selon un seuil personnalisé
     * Utilisé pour des opérations ponctuelles ou de maintenance
     */
    @Transactional
    public void cleanupStalePaymentPendingOrders(int timeoutMinutes) {
        logger.info("Nettoyage manuel des commandes PAYMENT_PENDING avec timeout de {} minutes", timeoutMinutes);

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoffTime);
        
        if (staleOrders.isEmpty()) {
            logger.info("Aucune commande PAYMENT_PENDING à nettoyer avec le timeout de {} minutes", timeoutMinutes);
            return;
        }

        int cancelledCount = 0;
        int errorCount = 0;

        for (Order order : staleOrders) {
            try {
                OrderStatus previous = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancellationReason("Paiement non finalisé dans le délai imparti (" + timeoutMinutes + " minutes)");
                order.setCancelledAt(LocalDateTime.now());
                order.setUpdatedAt(LocalDateTime.now());
                OrderProgressSync.applyMinimumForStatus(order);
                orderRepository.save(order);
                orderRealtimeEventPublisher.publishOrderUpdated(order, previous, OrderStatus.CANCELLED);
                cancelledCount++;
                
                logger.info("Commande {} annulée manuellement car Paiement non finalisé", order.getId());
                
            } catch (Exception e) {
                logger.error("Erreur lors de l'annulation manuelle de la commande {} : {}", order.getId(), e.getMessage(), e);
                errorCount++;
            }
        }

        logger.info("Nettoyage manuel terminé - {} commandes annulées, {} erreurs", cancelledCount, errorCount);
    }

    /**
     * Tâche planifiée pour supprimer les commandes CANCELLED qui ont dépassé la période de rétention
     * Exécutée tous les 5 jours à minuit pour nettoyer les commandes annulées
     */
    @Scheduled(cron = "0 0 0 */5 * *") // Tous les 5 jours à minuit
    public void cleanupCancelledOrders() {
        logger.info("Démarrage du nettoyage des commandes CANCELLED expirées (rétention: {} jours)", cancelledRetentionDays);

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(cancelledRetentionDays);
            
            // Trouver les commandes CANCELLED plus anciennes que le seuil
            List<Order> cancelledOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.CANCELLED, cutoffTime);
            
            if (cancelledOrders.isEmpty()) {
                logger.info("Aucune commande CANCELLED à supprimer. Fin du nettoyage.");
                return;
            }

            logger.info("Trouvé {} commandes CANCELLED à supprimer (cutoff: {})",
                       cancelledOrders.size(), cutoffTime);

            int deletedCount = 0;
            int errorCount = 0;

            for (Order order : cancelledOrders) {
                try {
                    // Suppression de la commande en respectant les contraintes de clés étrangères
                    // La cascade est gérée par JPA mais nous supprimons explicitement pour plus de sécurité
                    orderRepository.delete(order);
                    deletedCount++;
                    
                    logger.info("Commande {} supprimée car annulée et périmée (créée le: {}, annulée le: {})",
                               order.getId(), order.getCreatedAt(), order.getCancelledAt());
                    
                } catch (Exception e) {
                    logger.error("Erreur lors de la suppression de la commande {} : {}", order.getId(), e.getMessage(), e);
                    errorCount++;
                }
            }

            logger.info("Nettoyage des commandes CANCELLED terminé - {} commandes supprimées, {} erreurs", deletedCount, errorCount);

        } catch (Exception e) {
            logger.error("Erreur critique lors du nettoyage des commandes CANCELLED : {}", e.getMessage(), e);
        }
    }

    /**
     * Méthode pour supprimer manuellement les commandes CANCELLED selon un seuil personnalisé
     * Utilisé pour des opérations ponctuelles ou de maintenance
     */
    @Transactional
    public void cleanupCancelledOrders(int retentionDays) {
        logger.info("Nettoyage manuel des commandes CANCELLED avec rétention de {} jours", retentionDays);

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        List<Order> cancelledOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.CANCELLED, cutoffTime);
        
        if (cancelledOrders.isEmpty()) {
            logger.info("Aucune commande CANCELLED à supprimer avec la rétention de {} jours", retentionDays);
            return;
        }

        int deletedCount = 0;
        int errorCount = 0;

        for (Order order : cancelledOrders) {
            try {
                // Suppression de la commande en respectant les contraintes de clés étrangères
                // La cascade est gérée par JPA mais nous supprimons explicitement pour plus de sécurité
                orderRepository.delete(order);
                deletedCount++;
                
                logger.info("Commande {} supprimée manuellement car annulée et périmée (créée le: {}, annulée le: {})",
                           order.getId(), order.getCreatedAt(), order.getCancelledAt());
                
            } catch (Exception e) {
                logger.error("Erreur lors de la suppression manuelle de la commande {} : {}", order.getId(), e.getMessage(), e);
                errorCount++;
            }
        }

        logger.info("Nettoyage manuel des commandes CANCELLED terminé - {} commandes supprimées, {} erreurs", deletedCount, errorCount);
    }

    /**
     * Méthode pour obtenir des statistiques sur les commandes PAYMENT_PENDING
     */
    @Transactional(readOnly = true)
    public OrderCleanupStats getPaymentPendingStats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(paymentPendingTimeoutMinutes);
        
        List<Order> allPaymentPendingOrders = orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING);
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoffTime);
        
        OrderCleanupStats stats = new OrderCleanupStats();
        stats.setTotalPaymentPendingOrders(allPaymentPendingOrders.size());
        stats.setStalePaymentPendingOrders(staleOrders.size());
        stats.setCutoffTime(cutoffTime);
        stats.setCleanupThresholdMinutes(paymentPendingTimeoutMinutes);
        
        return stats;
    }

    /**
     * Vérifie auprès de Stripe si une commande a réellement été payée.
     * Retourne true si le paiement est confirmé, false sinon.
     */
    private boolean isActuallyPaidOnStripe(Order order) {
        String sessionId = order.getStripeSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            return false;
        }

        try {
            Session session = stripeClient.checkout().sessions().retrieve(sessionId);
            boolean paid = "paid".equals(session.getPaymentStatus());

            if (paid) {
                logger.info("🔍 VÉRIFICATION STRIPE - Commande {} : paiement confirmé (session {})",
                           order.getId(), sessionId);
            }

            return paid;
        } catch (Exception e) {
            logger.warn("⚠️ VÉRIFICATION STRIPE - Impossible de vérifier la commande {} (session {}) : {}",
                       order.getId(), sessionId, e.getMessage());
            // En cas d'erreur Stripe, ne pas annuler la commande pour éviter de perdre un paiement
            // Retourner true par précaution — le service de réconciliation s'en chargera
            return true;
        }
    }

    /**
     * Classe pour les statistiques de nettoyage
     */
    public static class OrderCleanupStats {
        private int totalPaymentPendingOrders;
        private int stalePaymentPendingOrders;
        private LocalDateTime cutoffTime;
        private int cleanupThresholdMinutes;

        // Getters and setters
        public int getTotalPaymentPendingOrders() { return totalPaymentPendingOrders; }
        public void setTotalPaymentPendingOrders(int totalPaymentPendingOrders) { this.totalPaymentPendingOrders = totalPaymentPendingOrders; }

        public int getStalePaymentPendingOrders() { return stalePaymentPendingOrders; }
        public void setStalePaymentPendingOrders(int stalePaymentPendingOrders) { this.stalePaymentPendingOrders = stalePaymentPendingOrders; }

        public LocalDateTime getCutoffTime() { return cutoffTime; }
        public void setCutoffTime(LocalDateTime cutoffTime) { this.cutoffTime = cutoffTime; }

        public int getCleanupThresholdMinutes() { return cleanupThresholdMinutes; }
        public void setCleanupThresholdMinutes(int cleanupThresholdMinutes) { this.cleanupThresholdMinutes = cleanupThresholdMinutes; }
    }
}