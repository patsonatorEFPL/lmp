package com.lmp.shared.web.debug;

import com.lmp.billing.service.OrderCleanupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur de test pour la fonctionnalité de nettoyage des commandes annulées
 * Permet de tester la fonctionnalité de suppression automatique des commandes CANCELLED
 */
@RestController
@RequestMapping("/api/test/order-cleanup")
public class TestOrderCleanupController {

        private final OrderCleanupService orderCleanupService;


    public TestOrderCleanupController(OrderCleanupService orderCleanupService) {
        this.orderCleanupService = orderCleanupService;
    }

    /**
     * Endpoint pour déclencher manuellement le nettoyage des commandes CANCELLED
     * @param retentionDays Nombre de jours de rétention à utiliser pour le test
     * @return Résultat de l'opération
     */
    @PostMapping("/cleanup-cancelled")
    public ResponseEntity<Map<String, Object>> cleanupCancelledOrders(
            @RequestParam(defaultValue = "5") int retentionDays) {
        
        try {
            // Exécuter le nettoyage manuellement
            orderCleanupService.cleanupCancelledOrders(retentionDays);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Nettoyage des commandes CANCELLED terminé avec succès (rétention: %d jours)", retentionDays),
                "retentionDays", retentionDays
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur lors du nettoyage des commandes CANCELLED: " + e.getMessage()
            ));
        }
    }

    /**
     * Endpoint pour déclencher manuellement le nettoyage des commandes PAYMENT_PENDING
     * @param timeoutMinutes Délai d'expiration en minutes
     * @return Résultat de l'opération
     */
    @PostMapping("/cleanup-pending")
    public ResponseEntity<Map<String, Object>> cleanupPaymentPendingOrders(
            @RequestParam(defaultValue = "30") int timeoutMinutes) {
        
        try {
            // Exécuter le nettoyage manuellement
            orderCleanupService.cleanupStalePaymentPendingOrders(timeoutMinutes);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Nettoyage des commandes PAYMENT_PENDING terminé avec succès (timeout: %d minutes)", timeoutMinutes),
                "timeoutMinutes", timeoutMinutes
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur lors du nettoyage des commandes PAYMENT_PENDING: " + e.getMessage()
            ));
        }
    }
}