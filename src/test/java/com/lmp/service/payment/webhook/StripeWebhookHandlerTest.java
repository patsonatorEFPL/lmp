package com.lmp.service.payment.webhook;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.domain.enums.PaymentStatus;
import com.lmp.domain.enums.StripeWebhookEventType;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.PaymentTransactionRepository;
import com.lmp.service.payment.dto.WebhookEventDto;
import com.lmp.service.payment.exception.PaymentProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour StripeWebhookHandler
 * Valide la refactorisation avec les enums StripeWebhookEventType
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookHandlerTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StripeWebhookHandler stripeWebhookHandler;

    private final String VALID_WEBHOOK_SECRET = "whsec_test123";
    private final String MOCK_PAYMENT_INTENT_ID = "pi_test123";
    private final String MOCK_SESSION_ID = "cs_test123";

    @BeforeEach
    void setUp() {
        // Configurer le secret webhook via reflection
        ReflectionTestUtils.setField(stripeWebhookHandler, "webhookSecret", VALID_WEBHOOK_SECRET);
    }

    @Test
    void testStripeWebhookEventTypeEnum() {
        // Test que l'enum StripeWebhookEventType fonctionne correctement
        
        // Test du mapping des événements Stripe
        StripeWebhookEventType paymentSucceeded = StripeWebhookEventType.fromStripeEventType("payment_intent.succeeded");
        assertNotNull(paymentSucceeded);
        assertEquals(StripeWebhookEventType.PAYMENT_INTENT_SUCCEEDED, paymentSucceeded);
        assertEquals("Paiement réussi", paymentSucceeded.getDescription());
        
        // Test des statuts cibles
        assertTrue(paymentSucceeded.shouldUpdateOrderStatus());
        assertTrue(paymentSucceeded.shouldUpdatePaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, paymentSucceeded.getTargetOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, paymentSucceeded.getTargetPaymentStatus());
    }

    @Test
    void testStripeWebhookEventTypeMapping() {
        // Test tous les événements supportés
        assertNotNull(StripeWebhookEventType.fromStripeEventType("payment_intent.succeeded"));
        assertNotNull(StripeWebhookEventType.fromStripeEventType("payment_intent.payment_failed"));
        assertNotNull(StripeWebhookEventType.fromStripeEventType("checkout.session.completed"));
        assertNotNull(StripeWebhookEventType.fromStripeEventType("checkout.session.expired"));
        assertNotNull(StripeWebhookEventType.fromStripeEventType("refund.created"));
        assertNotNull(StripeWebhookEventType.fromStripeEventType("refund.updated"));
        
        // Test événement non supporté
        assertNull(StripeWebhookEventType.fromStripeEventType("unknown.event.type"));
    }

    @Test
    void testPaymentIntentSucceededMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.PAYMENT_INTENT_SUCCEEDED;
        
        assertEquals("payment_intent.succeeded", eventType.getStripeEventType());
        assertEquals("Paiement réussi", eventType.getDescription());
        assertEquals(OrderStatus.CONFIRMED, eventType.getTargetOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, eventType.getTargetPaymentStatus());
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testPaymentIntentFailedMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.PAYMENT_INTENT_PAYMENT_FAILED;
        
        assertEquals("payment_intent.payment_failed", eventType.getStripeEventType());
        assertEquals("Paiement échoué", eventType.getDescription()); // Corriger la description réelle
        assertEquals(OrderStatus.CANCELLED, eventType.getTargetOrderStatus());
        assertEquals(PaymentStatus.FAILED, eventType.getTargetPaymentStatus());
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testCheckoutSessionCompletedMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.CHECKOUT_SESSION_COMPLETED;
        
        assertEquals("checkout.session.completed", eventType.getStripeEventType());
        assertEquals("Session de paiement complétée", eventType.getDescription());
        assertEquals(OrderStatus.CONFIRMED, eventType.getTargetOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, eventType.getTargetPaymentStatus());
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testCheckoutSessionExpiredMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.CHECKOUT_SESSION_EXPIRED;
        
        assertEquals("checkout.session.expired", eventType.getStripeEventType());
        assertEquals("Session de paiement expirée", eventType.getDescription());
        assertEquals(OrderStatus.CANCELLED, eventType.getTargetOrderStatus());
        assertEquals(PaymentStatus.FAILED, eventType.getTargetPaymentStatus());
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testRefundCreatedMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.REFUND_CREATED;
        
        assertEquals("refund.created", eventType.getStripeEventType());
        assertEquals("Remboursement créé", eventType.getDescription());
        assertEquals(OrderStatus.REFUNDED, eventType.getTargetOrderStatus()); // En réalité met à jour la commande
        assertEquals(PaymentStatus.REFUNDED, eventType.getTargetPaymentStatus()); // En réalité met à jour le paiement
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testRefundUpdatedMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.REFUND_UPDATED;
        
        assertEquals("refund.updated", eventType.getStripeEventType());
        assertEquals("Remboursement mis à jour", eventType.getDescription());
        assertEquals(OrderStatus.REFUNDED, eventType.getTargetOrderStatus()); // En réalité met à jour la commande
        assertEquals(PaymentStatus.REFUNDED, eventType.getTargetPaymentStatus());
        assertTrue(eventType.shouldUpdateOrderStatus()); // En réalité met à jour la commande
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testAsyncPaymentMapping() {
        // Test paiement asynchrone réussi
        StripeWebhookEventType asyncSucceeded = StripeWebhookEventType.CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED;
        assertEquals("checkout.session.async_payment_succeeded", asyncSucceeded.getStripeEventType());
        assertEquals(OrderStatus.CONFIRMED, asyncSucceeded.getTargetOrderStatus());
        assertEquals(PaymentStatus.COMPLETED, asyncSucceeded.getTargetPaymentStatus());
        
        // Test paiement asynchrone échoué
        StripeWebhookEventType asyncFailed = StripeWebhookEventType.CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED;
        assertEquals("checkout.session.async_payment_failed", asyncFailed.getStripeEventType());
        assertEquals(OrderStatus.CANCELLED, asyncFailed.getTargetOrderStatus());
        assertEquals(PaymentStatus.FAILED, asyncFailed.getTargetPaymentStatus());
    }

    @Test
    void testInvoicePaymentMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.INVOICE_PAYMENT_SUCCEEDED;
        
        assertEquals("invoice.payment_succeeded", eventType.getStripeEventType());
        assertEquals("Paiement de facture réussi", eventType.getDescription());
        assertEquals(OrderStatus.CONFIRMED, eventType.getTargetOrderStatus()); // En réalité met à jour la commande
        assertEquals(PaymentStatus.COMPLETED, eventType.getTargetPaymentStatus()); // En réalité met à jour le paiement
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testSubscriptionMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.CUSTOMER_SUBSCRIPTION_UPDATED;
        
        assertEquals("customer.subscription.updated", eventType.getStripeEventType());
        assertEquals("Abonnement client mis à jour", eventType.getDescription());
        assertNull(eventType.getTargetOrderStatus()); // Pas de mise à jour de commande pour abonnements
        assertNull(eventType.getTargetPaymentStatus()); // Pas de mise à jour automatique
        assertFalse(eventType.shouldUpdateOrderStatus());
        assertFalse(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testChargeDisputeMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.CHARGE_DISPUTE_CREATED;
        
        assertEquals("charge.dispute.created", eventType.getStripeEventType());
        assertEquals("Litige créé", eventType.getDescription());
        assertEquals(OrderStatus.UNDER_REVIEW, eventType.getTargetOrderStatus()); // Utilise UNDER_REVIEW
        assertNull(eventType.getTargetPaymentStatus()); // Pas de mise à jour du paiement pour litige
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertFalse(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testPaymentIntentRequiresActionMapping() {
        StripeWebhookEventType eventType = StripeWebhookEventType.PAYMENT_INTENT_REQUIRES_ACTION;
        
        assertEquals("payment_intent.requires_action", eventType.getStripeEventType());
        assertEquals("Paiement nécessite une action", eventType.getDescription());
        assertEquals(OrderStatus.PAYMENT_PENDING, eventType.getTargetOrderStatus()); // Utilise PAYMENT_PENDING
        assertEquals(PaymentStatus.PENDING, eventType.getTargetPaymentStatus()); // Utilise PENDING
        assertTrue(eventType.shouldUpdateOrderStatus());
        assertTrue(eventType.shouldUpdatePaymentStatus());
    }

    @Test
    void testAllEventTypesHaveValidMappings() {
        // Vérifier que tous les événements enum ont des mappings Stripe valides
        for (StripeWebhookEventType eventType : StripeWebhookEventType.values()) {
            assertNotNull(eventType.getStripeEventType(), 
                "L'événement " + eventType + " doit avoir un type Stripe défini");
            assertNotNull(eventType.getDescription(), 
                "L'événement " + eventType + " doit avoir une description");
            
            // Test de la conversion bidirectionnelle
            assertEquals(eventType, StripeWebhookEventType.fromStripeEventType(eventType.getStripeEventType()),
                "La conversion bidirectionnelle doit fonctionner pour " + eventType);
        }
    }

    @Test
    void testEventTypeConsistency() {
        // Test que les événements qui mettent à jour les statuts ont des statuts cibles définis
        for (StripeWebhookEventType eventType : StripeWebhookEventType.values()) {
            if (eventType.shouldUpdateOrderStatus()) {
                assertNotNull(eventType.getTargetOrderStatus(), 
                    "L'événement " + eventType + " qui met à jour les commandes doit avoir un statut cible");
            }
            
            if (eventType.shouldUpdatePaymentStatus()) {
                assertNotNull(eventType.getTargetPaymentStatus(), 
                    "L'événement " + eventType + " qui met à jour les paiements doit avoir un statut cible");
            }
        }
    }

    /**
     * Test que la refactorisation n'a pas cassé la logique existante
     */
    @Test
    void testBackwardCompatibility() {
        // Vérifier que tous les types d'événements Stripe importants sont supportés
        String[] importantEventTypes = {
            "payment_intent.succeeded",
            "payment_intent.payment_failed", 
            "checkout.session.completed",
            "checkout.session.expired",
            "refund.created",
            "refund.updated"
        };
        
        for (String eventType : importantEventTypes) {
            assertNotNull(StripeWebhookEventType.fromStripeEventType(eventType),
                "L'événement important " + eventType + " doit être supporté");
        }
    }
}