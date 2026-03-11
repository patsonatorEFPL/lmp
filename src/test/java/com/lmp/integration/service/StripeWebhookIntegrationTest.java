package com.lmp.integration.service;

import com.lmp.integration.domain.StripeWebhookEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour valider la refactorisation des webhooks Stripe
 * Simule des scénarios réels d'événements webhook
 */
class StripeWebhookIntegrationTest {

    @Test
    @DisplayName("Test du mapping complet des événements webhook critiques")
    void testCriticalWebhookEventMapping() {
        // Scénario de paiement réussi
        StripeWebhookEventType paymentSuccess = StripeWebhookEventType.fromStripeEventType("payment_intent.succeeded");
        assertNotNull(paymentSuccess);
        assertTrue(paymentSuccess.shouldUpdateOrderStatus());
        assertTrue(paymentSuccess.shouldUpdatePaymentStatus());
        
        // Scénario de checkout complété
        StripeWebhookEventType checkoutComplete = StripeWebhookEventType.fromStripeEventType("checkout.session.completed");
        assertNotNull(checkoutComplete);
        assertTrue(checkoutComplete.shouldUpdateOrderStatus());
        assertTrue(checkoutComplete.shouldUpdatePaymentStatus());
        
        // Scénario d'expiration de session
        StripeWebhookEventType sessionExpired = StripeWebhookEventType.fromStripeEventType("checkout.session.expired");
        assertNotNull(sessionExpired);
        assertTrue(sessionExpired.shouldUpdateOrderStatus());
        assertTrue(sessionExpired.shouldUpdatePaymentStatus());
    }

    @Test
    @DisplayName("Test de la gestion des événements de remboursement")
    void testRefundEventHandling() {
        // Création de remboursement
        StripeWebhookEventType refundCreated = StripeWebhookEventType.fromStripeEventType("refund.created");
        assertNotNull(refundCreated);
        assertEquals("Remboursement créé", refundCreated.getDescription());
        assertTrue(refundCreated.shouldUpdateOrderStatus());
        assertTrue(refundCreated.shouldUpdatePaymentStatus());
        
        // Mise à jour de remboursement
        StripeWebhookEventType refundUpdated = StripeWebhookEventType.fromStripeEventType("refund.updated");
        assertNotNull(refundUpdated);
        assertEquals("Remboursement mis à jour", refundUpdated.getDescription());
        assertTrue(refundUpdated.shouldUpdateOrderStatus());
        assertTrue(refundUpdated.shouldUpdatePaymentStatus());
    }

    @Test
    @DisplayName("Test de la gestion des paiements asynchrones")
    void testAsyncPaymentHandling() {
        // Paiement asynchrone réussi
        StripeWebhookEventType asyncSuccess = StripeWebhookEventType.fromStripeEventType("checkout.session.async_payment_succeeded");
        assertNotNull(asyncSuccess);
        assertEquals("Paiement asynchrone réussi", asyncSuccess.getDescription());
        assertTrue(asyncSuccess.shouldUpdateOrderStatus());
        assertTrue(asyncSuccess.shouldUpdatePaymentStatus());
        
        // Paiement asynchrone échoué
        StripeWebhookEventType asyncFailed = StripeWebhookEventType.fromStripeEventType("checkout.session.async_payment_failed");
        assertNotNull(asyncFailed);
        assertEquals("Paiement asynchrone échoué", asyncFailed.getDescription());
        assertTrue(asyncFailed.shouldUpdateOrderStatus());
        assertTrue(asyncFailed.shouldUpdatePaymentStatus());
    }

    @Test
    @DisplayName("Test de résistance aux événements non supportés")
    void testUnsupportedEventResilience() {
        // Événements qui n'existent pas ou ne sont pas supportés
        String[] unsupportedEvents = {
            "unknown.event.type",
            "account.updated", 
            "customer.created",
            "payment_method.attached",
            "setup_intent.succeeded"
        };
        
        for (String eventType : unsupportedEvents) {
            StripeWebhookEventType result = StripeWebhookEventType.fromStripeEventType(eventType);
            assertNull(result, "L'événement non supporté " + eventType + " ne doit pas être mappé");
        }
    }

    @Test
    @DisplayName("Test de cohérence bidirectionnelle du mapping")
    void testBidirectionalMappingConsistency() {
        // Vérifier que chaque enum peut être converti vers Stripe et vice-versa
        for (StripeWebhookEventType eventType : StripeWebhookEventType.values()) {
            String stripeType = eventType.getStripeEventType();
            StripeWebhookEventType backConverted = StripeWebhookEventType.fromStripeEventType(stripeType);
            
            assertEquals(eventType, backConverted, 
                "La conversion bidirectionnelle doit être cohérente pour " + eventType);
        }
    }

    @Test
    @DisplayName("Test de validation des statuts cibles")
    void testTargetStatusValidation() {
        for (StripeWebhookEventType eventType : StripeWebhookEventType.values()) {
            // Si un événement dit qu'il met à jour les statuts, il doit avoir des statuts cibles
            if (eventType.shouldUpdateOrderStatus()) {
                assertNotNull(eventType.getTargetOrderStatus(), 
                    "L'événement " + eventType + " qui met à jour les commandes doit avoir un statut de commande cible");
            }
            
            if (eventType.shouldUpdatePaymentStatus()) {
                assertNotNull(eventType.getTargetPaymentStatus(), 
                    "L'événement " + eventType + " qui met à jour les paiements doit avoir un statut de paiement cible");
            }
        }
    }

    @Test
    @DisplayName("Test des scénarios de workflow complets")
    void testCompleteWorkflowScenarios() {
        // Workflow réussi : checkout -> paiement -> confirmation
        StripeWebhookEventType[] successWorkflow = {
            StripeWebhookEventType.fromStripeEventType("checkout.session.completed"),
            StripeWebhookEventType.fromStripeEventType("payment_intent.succeeded")
        };
        
        for (StripeWebhookEventType event : successWorkflow) {
            assertNotNull(event);
            assertTrue(event.shouldUpdateOrderStatus());
            assertTrue(event.shouldUpdatePaymentStatus());
        }
        
        // Workflow d'échec : checkout -> expiration -> annulation
        StripeWebhookEventType[] failureWorkflow = {
            StripeWebhookEventType.fromStripeEventType("checkout.session.expired"),
            StripeWebhookEventType.fromStripeEventType("payment_intent.payment_failed")
        };
        
        for (StripeWebhookEventType event : failureWorkflow) {
            assertNotNull(event);
            assertTrue(event.shouldUpdateOrderStatus());
            assertTrue(event.shouldUpdatePaymentStatus());
        }
    }

    @Test
    @DisplayName("Test de performance du mapping")
    void testMappingPerformance() {
        String[] commonEvents = {
            "payment_intent.succeeded",
            "checkout.session.completed", 
            "payment_intent.payment_failed",
            "checkout.session.expired"
        };
        
        // Test de performance simple - 1000 conversions
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            for (String eventType : commonEvents) {
                StripeWebhookEventType.fromStripeEventType(eventType);
            }
        }
        long endTime = System.nanoTime();
        
        // La conversion doit être rapide (moins de 10ms pour 4000 conversions)
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 10, 
            "Le mapping des événements doit être performant. Durée: " + durationMs + "ms");
    }
}