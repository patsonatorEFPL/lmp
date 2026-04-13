package com.lmp.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.exception.PaymentProcessingException;
import com.lmp.billing.exception.PaymentProviderException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;

/**
 * Crée des PaymentIntents Stripe pour le Payment Element (sans session Checkout hébergée).
 */
@Service
public class StripePaymentIntentCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentIntentCheckoutService.class);

    private final StripeClient stripeClient;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "CAD", "USD", "EUR", "GBP", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK");

    /** Statuts Stripe pour lesquels on réutilise le même PI au lieu d’en créer un nouveau. */
    private static final Set<String> REUSABLE_PAYMENT_INTENT_STATUSES = Set.of(
            "requires_payment_method",
            "requires_confirmation",
            "requires_action",
            "processing",
            "requires_capture");

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    public StripePaymentIntentCheckoutService(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    public String getPublishableKey() {
        return stripePublishableKey;
    }

    /**
     * Crée ou remplace le PaymentIntent pour une commande en attente de paiement.
     *
     * @param creationMode valeur metadata (ex. payment_element, guest_checkout)
     */
    public PaymentIntentResult createOrRefreshPaymentIntent(Order order, User user, String customerIp,
            String creationMode) throws PaymentProcessingException {

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new PaymentProviderException("La commande n'est pas en attente de paiement", "stripe");
        }
        if (user == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            throw new PaymentProviderException("Utilisateur incompatible avec la commande", "stripe");
        }

        String currency = order.getCurrency() != null ? order.getCurrency().toLowerCase() : "eur";
        String currencyUpper = currency.toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(currencyUpper)) {
            throw new PaymentProviderException("Devise non supportée: " + currencyUpper, "stripe");
        }

        // Toujours arrondir à 2 décimales avant ×100 : évite ArithmeticException sur longValueExact()
        // (ex. montants issus de double / JSON avec échelle > 2).
        BigDecimal amountMajor = order.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        long amountCents = amountMajor.multiply(new BigDecimal("100")).longValueExact();
        if (amountCents <= 0) {
            throw new PaymentProviderException("Montant de commande invalide", "stripe");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", order.getId().toString());
        metadata.put("orderId", order.getId().toString());
        metadata.put("userId", user.getId().toString());
        metadata.put("userEmail", user.getEmail() != null ? user.getEmail() : "");
        metadata.put("webhook_version", "v3");
        metadata.put("creation_mode", creationMode != null ? creationMode : "payment_element");

        String existingPiId = order.getStripePaymentIntentId();
        if (existingPiId != null && !existingPiId.isBlank()) {
            try {
                PaymentIntent existing = stripeClient.paymentIntents().retrieve(existingPiId);
                String st = existing.getStatus();
                if ("succeeded".equals(st)) {
                    throw new PaymentProviderException(
                            "Le paiement a déjà été accepté par Stripe. Actualisez la liste des commandes.",
                            "ALREADY_PAID",
                            null,
                            "stripe",
                            null);
                }
                if (REUSABLE_PAYMENT_INTENT_STATUSES.contains(st)) {
                    logger.info("Réutilisation du PaymentIntent {} (status={}) pour commande {}",
                            existingPiId, st, order.getId());
                    return new PaymentIntentResult(existing.getClientSecret(), existing.getId());
                }
                logger.info("PaymentIntent {} en statut « {} » : création d’un nouveau pour commande {}",
                        existingPiId, st, order.getId());
            } catch (StripeException e) {
                logger.warn("Lecture du PaymentIntent {} impossible, création d’un nouveau : {}",
                        existingPiId, e.getMessage());
            }
        }

        PaymentIntentCreateParams.Builder b = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                // Pas de return_url ici : Stripe l’exige seulement avec confirm=true ; le Payment Element
                // passe return_url au confirm côté client (redirect PM) si besoin.
                .putAllMetadata(metadata);

        try {
            PaymentIntent pi = stripeClient.paymentIntents().create(b.build());
            logger.info("PaymentIntent créé {} pour commande {}", pi.getId(), order.getId());
            return new PaymentIntentResult(pi.getClientSecret(), pi.getId());
        } catch (StripeException e) {
            logger.error("Stripe PaymentIntent error: {}", e.getMessage());
            throw new PaymentProviderException(
                    "Échec création PaymentIntent: " + e.getMessage(),
                    "STRIPE_PI_ERROR",
                    e.getCode(),
                    "stripe",
                    null);
        }
    }

    /**
     * Met à jour le montant d'un PaymentIntent existant (avant confirmation).
     */
    public void updatePaymentIntentAmount(String paymentIntentId, BigDecimal newAmount, String currency)
            throws PaymentProcessingException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new PaymentProviderException("Aucun PaymentIntent à mettre à jour", "stripe");
        }

        BigDecimal amountMajor = newAmount.setScale(2, RoundingMode.HALF_UP);
        long amountCents = amountMajor.multiply(new BigDecimal("100")).longValueExact();

        try {
            PaymentIntentUpdateParams params = PaymentIntentUpdateParams.builder()
                    .setAmount(amountCents)
                    .build();
            PaymentIntent updated = stripeClient.paymentIntents().update(paymentIntentId, params);
            logger.info("PaymentIntent {} montant mis à jour → {} cents ({})",
                    paymentIntentId, amountCents, currency);
        } catch (StripeException e) {
            logger.error("Stripe PaymentIntent update error: {}", e.getMessage());
            throw new PaymentProviderException(
                    "Échec mise à jour PaymentIntent: " + e.getMessage(),
                    "STRIPE_PI_UPDATE_ERROR",
                    e.getCode(),
                    "stripe",
                    null);
        }
    }

    public record PaymentIntentResult(String clientSecret, String paymentIntentId) {}
}
