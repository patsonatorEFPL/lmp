package com.lmp.billing.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lmp.billing.domain.Order;
import com.stripe.StripeClient;
import com.stripe.model.Address;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.checkout.Session;

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
     * Récupère l'objet PaymentMethod Stripe complet (pour extraction card country, etc.).
     *
     * @param paymentMethodId identifiant Stripe du PaymentMethod (pm_xxx)
     * @return PaymentMethod ou {@code null} si impossible
     */
    public PaymentMethod retrievePaymentMethod(String paymentMethodId) {
        if (paymentMethodId == null || paymentMethodId.isBlank()) return null;
        try {
            return stripeClient.paymentMethods().retrieve(paymentMethodId);
        } catch (Exception e) {
            logger.warn("Impossible de récupérer le PaymentMethod {} : {}", paymentMethodId, e.getMessage());
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

    // =========================================================================
    // Billing address extraction
    // =========================================================================

    /**
     * Extrait l'adresse de facturation depuis une Checkout Session Stripe
     * et peuple les champs billing de la commande.
     *
     * <p>Source : {@code session.getCustomerDetails().getAddress()}.
     *
     * @param order   la commande à peupler
     * @param session la session Checkout Stripe
     */
    public static void extractBillingAddress(Order order, Session session) {
        if (order == null || session == null) return;
        try {
            Session.CustomerDetails cd = session.getCustomerDetails();
            if (cd == null) return;

            // Extraire le nom de facturation (billing_details.name)
            // Ne PAS écraser un nom personnalisé déjà renseigné par l'utilisateur (checkbox checkout)
            String name = cd.getName();
            if (name != null && !name.isBlank() && order.getBillingName() == null) {
                order.setBillingName(name);
                logger.debug("Billing name extracted from session {}: '{}'", session.getId(), name);
            }

            if (cd.getAddress() == null) return;
            Address addr = cd.getAddress();
            applyAddress(order, addr.getLine1(), addr.getLine2(), addr.getCity(),
                    addr.getPostalCode(), addr.getCountry());
        } catch (Exception e) {
            logger.warn("Impossible d'extraire l'adresse de facturation de la session {} : {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Extrait l'adresse de facturation depuis un PaymentIntent Stripe
     * via le PaymentMethod associé et peuple les champs billing de la commande.
     *
     * <p>Source : {@code paymentMethod.getBillingDetails().getAddress()}.
     *
     * @param order         la commande à peupler
     * @param paymentIntent le PaymentIntent Stripe
     */
    public void extractBillingAddress(Order order, PaymentIntent paymentIntent) {
        if (order == null || paymentIntent == null) return;
        String pmId = paymentIntent.getPaymentMethod();
        if (pmId == null || pmId.isBlank()) return;
        try {
            PaymentMethod pm = stripeClient.paymentMethods().retrieve(pmId);
            if (pm.getBillingDetails() == null) return;

            // Extraire le nom de facturation (billing_details.name)
            String name = pm.getBillingDetails().getName();
            if (name != null && !name.isBlank() && order.getBillingName() == null) {
                order.setBillingName(name);
                logger.debug("Billing name extracted from PM {}: '{}'", pmId, name);
            }

            if (pm.getBillingDetails().getAddress() == null) return;
            Address addr = pm.getBillingDetails().getAddress();
            applyAddress(order, addr.getLine1(), addr.getLine2(), addr.getCity(),
                    addr.getPostalCode(), addr.getCountry());
        } catch (Exception e) {
            logger.warn("Impossible d'extraire l'adresse de facturation du PI {} : {}",
                    paymentIntent.getId(), e.getMessage());
        }
    }

    private static void applyAddress(Order order, String line1, String line2,
                                     String city, String postalCode, String country) {
        String fullAddress = line1 != null ? line1 : "";
        if (line2 != null && !line2.isBlank()) {
            fullAddress = fullAddress.isBlank() ? line2 : fullAddress + ", " + line2;
        }
        if (!fullAddress.isBlank()) order.setBillingAddress(fullAddress);
        if (city != null && !city.isBlank()) order.setBillingCity(city);
        if (postalCode != null && !postalCode.isBlank()) order.setBillingPostalCode(postalCode);
        if (country != null && !country.isBlank()) order.setBillingCountry(country);

        logger.debug("Billing address applied to order {}: {}, {}, {} {}",
                order.getId(), fullAddress, city, postalCode, country);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace('_', ' ');
    }
}
