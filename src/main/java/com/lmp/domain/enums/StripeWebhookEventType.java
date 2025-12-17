package com.lmp.domain.enums;

/**
 * Énumération des types d'événements webhook Stripe supportés
 * Lie chaque événement Stripe aux statuts correspondants pour une gestion cohérente
 */
public enum StripeWebhookEventType {
    
    // Événements PaymentIntent
    PAYMENT_INTENT_SUCCEEDED("payment_intent.succeeded", 
                              "Paiement réussi", 
                              OrderStatus.CONFIRMED, 
                              PaymentStatus.COMPLETED),
    
    PAYMENT_INTENT_PAYMENT_FAILED("payment_intent.payment_failed", 
                                   "Paiement échoué", 
                                   OrderStatus.CANCELLED, 
                                   PaymentStatus.FAILED),
    
    PAYMENT_INTENT_REQUIRES_ACTION("payment_intent.requires_action", 
                                   "Paiement nécessite une action", 
                                   OrderStatus.PAYMENT_PENDING, 
                                   PaymentStatus.PENDING),
    
    // Événements Checkout Session
    CHECKOUT_SESSION_COMPLETED("checkout.session.completed", 
                               "Session de paiement complétée", 
                               OrderStatus.CONFIRMED, 
                               PaymentStatus.COMPLETED),
    
    CHECKOUT_SESSION_EXPIRED("checkout.session.expired", 
                             "Session de paiement expirée", 
                             OrderStatus.CANCELLED, 
                             PaymentStatus.FAILED),
    
    CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED("checkout.session.async_payment_succeeded", 
                                              "Paiement asynchrone réussi", 
                                              OrderStatus.CONFIRMED, 
                                              PaymentStatus.COMPLETED),
    
    CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED("checkout.session.async_payment_failed", 
                                           "Paiement asynchrone échoué", 
                                           OrderStatus.CANCELLED, 
                                           PaymentStatus.FAILED),
    
    // Événements Charge
    CHARGE_DISPUTE_CREATED("charge.dispute.created", 
                           "Litige créé", 
                           OrderStatus.UNDER_REVIEW, 
                           null),
    
    // Événements Refund
    REFUND_CREATED("refund.created", 
                   "Remboursement créé", 
                   OrderStatus.REFUNDED, 
                   PaymentStatus.REFUNDED),
    
    REFUND_UPDATED("refund.updated", 
                   "Remboursement mis à jour", 
                   OrderStatus.REFUNDED, 
                   PaymentStatus.REFUNDED),
    
    // Événements Invoice
    INVOICE_PAYMENT_SUCCEEDED("invoice.payment_succeeded", 
                              "Paiement de facture réussi", 
                              OrderStatus.CONFIRMED, 
                              PaymentStatus.COMPLETED),
    
    // Événements Customer/Subscription
    CUSTOMER_SUBSCRIPTION_UPDATED("customer.subscription.updated", 
                                   "Abonnement client mis à jour", 
                                   null, 
                                   null);
    
    private final String stripeEventType;
    private final String description;
    private final OrderStatus targetOrderStatus;
    private final PaymentStatus targetPaymentStatus;
    
    StripeWebhookEventType(String stripeEventType, String description, 
                          OrderStatus targetOrderStatus, PaymentStatus targetPaymentStatus) {
        this.stripeEventType = stripeEventType;
        this.description = description;
        this.targetOrderStatus = targetOrderStatus;
        this.targetPaymentStatus = targetPaymentStatus;
    }
    
    /**
     * Retourne le type d'événement Stripe (format string)
     */
    public String getStripeEventType() {
        return stripeEventType;
    }
    
    /**
     * Retourne la description lisible de l'événement
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Retourne le statut de commande cible pour cet événement
     */
    public OrderStatus getTargetOrderStatus() {
        return targetOrderStatus;
    }
    
    /**
     * Retourne le statut de paiement cible pour cet événement
     */
    public PaymentStatus getTargetPaymentStatus() {
        return targetPaymentStatus;
    }
    
    /**
     * Indique si cet événement doit mettre à jour le statut de la commande
     */
    public boolean shouldUpdateOrderStatus() {
        return targetOrderStatus != null;
    }
    
    /**
     * Indique si cet événement doit mettre à jour le statut du paiement
     */
    public boolean shouldUpdatePaymentStatus() {
        return targetPaymentStatus != null;
    }
    
    /**
     * Trouve un type d'événement webhook par sa string Stripe
     * 
     * @param stripeEventType Le type d'événement Stripe (ex: "payment_intent.succeeded")
     * @return L'énumération correspondante ou null si non trouvée
     */
    public static StripeWebhookEventType fromStripeEventType(String stripeEventType) {
        if (stripeEventType == null) {
            return null;
        }
        
        for (StripeWebhookEventType eventType : values()) {
            if (eventType.stripeEventType.equals(stripeEventType)) {
                return eventType;
            }
        }
        return null;
    }
    
    /**
     * Vérifie si un type d'événement Stripe est supporté
     * 
     * @param stripeEventType Le type d'événement Stripe
     * @return true si supporté, false sinon
     */
    public static boolean isSupported(String stripeEventType) {
        return fromStripeEventType(stripeEventType) != null;
    }
}