package com.lmp.billing.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.stripe.StripeClient;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;

/**
 * Résout le type de moyen de paiement Stripe en libellé lisible
 * pour l'affichage sur les factures et dans l'interface utilisateur.
 *
 * <p>Exemples : "card" → "Carte bancaire", "bancontact" → "Bancontact", etc.
 */
@Component
public class StripePaymentMethodResolver {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentMethodResolver.class);

    private final StripeClient stripeClient;

    /** Mapping Stripe payment method type → libellé français. */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("card", "Carte bancaire"),
            Map.entry("bancontact", "Bancontact"),
            Map.entry("klarna", "Klarna"),
            Map.entry("ideal", "iDEAL"),
            Map.entry("sepa_debit", "Prélèvement SEPA"),
            Map.entry("sofort", "Sofort"),
            Map.entry("giropay", "Giropay"),
            Map.entry("eps", "EPS"),
            Map.entry("p24", "Przelewy24"),
            Map.entry("amazon_pay", "Amazon Pay"),
            Map.entry("paypal", "PayPal"),
            Map.entry("link", "Link"),
            Map.entry("apple_pay", "Apple Pay"),
            Map.entry("google_pay", "Google Pay"),
            Map.entry("revolut_pay", "Revolut Pay"),
            Map.entry("multibanco", "Multibanco"),
            Map.entry("boleto", "Boleto"),
            Map.entry("oxxo", "OXXO"),
            Map.entry("wechat_pay", "WeChat Pay"),
            Map.entry("alipay", "Alipay"),
            Map.entry("affirm", "Affirm"),
            Map.entry("afterpay_clearpay", "Afterpay / Clearpay"),
            Map.entry("blik", "BLIK"),
            Map.entry("mobilepay", "MobilePay"),
            Map.entry("twint", "TWINT"),
            Map.entry("swish", "Swish")
    );

    public StripePaymentMethodResolver(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    /**
     * Résout le libellé du moyen de paiement à partir d'un PaymentIntent.
     *
     * @param pi le PaymentIntent Stripe (doit avoir un payment_method)
     * @return libellé lisible (ex. "Carte bancaire", "Bancontact") ou {@code null} si impossible
     */
    public String resolveFromPaymentIntent(PaymentIntent pi) {
        if (pi == null) return null;
        String pmId = pi.getPaymentMethod();
        if (pmId == null || pmId.isBlank()) return null;
        return resolveFromPaymentMethodId(pmId);
    }

    /**
     * Résout le libellé du moyen de paiement à partir d'un identifiant de PaymentIntent (pi_xxx).
     * Récupère le PI chez Stripe puis extrait le payment_method.
     *
     * @param paymentIntentId identifiant Stripe du PaymentIntent
     * @return libellé lisible ou {@code null} si impossible
     */
    public String resolveFromPaymentIntentId(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) return null;
        try {
            PaymentIntent pi = stripeClient.paymentIntents().retrieve(paymentIntentId);
            return resolveFromPaymentIntent(pi);
        } catch (Exception e) {
            logger.warn("Impossible de récupérer le PaymentIntent {} : {}", paymentIntentId, e.getMessage());
            return null;
        }
    }

    /**
     * Résout le libellé du moyen de paiement à partir de son identifiant Stripe (pm_xxx).
     *
     * @param paymentMethodId identifiant Stripe du PaymentMethod
     * @return libellé lisible ou {@code null} si impossible
     */
    public String resolveFromPaymentMethodId(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.isBlank()) return null;
        try {
            PaymentMethod pm = stripeClient.paymentMethods().retrieve(paymentMethodId);
            String type = pm.getType();
            if (type == null) return null;

            String label = LABELS.getOrDefault(type, type);
            logger.debug("Resolved Stripe PM {} → type='{}' → label='{}'", paymentMethodId, type, label);
            return label;
        } catch (Exception e) {
            logger.warn("Impossible de résoudre le moyen de paiement {} : {}", paymentMethodId, e.getMessage());
            return null;
        }
    }

    /**
     * Retourne le libellé français pour un type Stripe donné (ex. "card" → "Carte bancaire").
     *
     * @param stripeType type Stripe brut
     * @return libellé ou le type capitalisé si inconnu
     */
    public static String labelFor(String stripeType) {
        if (stripeType == null) return null;
        return LABELS.getOrDefault(stripeType, stripeType);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace('_', ' ');
    }
}
