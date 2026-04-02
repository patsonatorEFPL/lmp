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
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

/**
 * Crée des PaymentIntents Stripe pour le Payment Element (sans session Checkout hébergée).
 */
@Service
public class StripePaymentIntentCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentIntentCheckoutService.class);

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "CAD", "USD", "EUR", "GBP", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK");

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

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

        Stripe.apiKey = stripeSecretKey;

        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", order.getId().toString());
        metadata.put("orderId", order.getId().toString());
        metadata.put("userId", user.getId().toString());
        metadata.put("userEmail", user.getEmail() != null ? user.getEmail() : "");
        metadata.put("webhook_version", "v3");
        metadata.put("creation_mode", creationMode != null ? creationMode : "payment_element");

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
            PaymentIntent pi = PaymentIntent.create(b.build());
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

    public record PaymentIntentResult(String clientSecret, String paymentIntentId) {}
}
