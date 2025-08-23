package com.lmp.service.payment.processor;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.PaymentStatus;
import com.lmp.service.payment.PaymentProcessor;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.dto.RefundRequestDto;
import com.lmp.service.payment.dto.RefundResponseDto;
import com.lmp.service.payment.dto.CheckoutSessionRequestDto;
import com.lmp.service.payment.dto.CheckoutSessionResponseDto;
import com.lmp.service.payment.exception.PaymentProcessingException;
import com.lmp.service.payment.exception.PaymentProviderException;
import com.lmp.service.payment.exception.PaymentValidationException;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Processeur de paiement Stripe utilisant Checkout Sessions
 * Version simplifiée et moderne de l'intégration Stripe
 * Recommandé pour la plupart des cas d'usage
 */
@Component("stripePaymentProcessor")
public class StripeCheckoutPaymentProcessor implements PaymentProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutPaymentProcessor.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY." + StripeCheckoutPaymentProcessor.class.getName());
    
    private static final String PROCESSOR_NAME = "stripe-checkout";
    private static final String API_VERSION = "2023-10-16";
    
    // Devises supportées par Stripe Checkout
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
        "CAD", "USD", "EUR", "GBP", "AUD", "JPY", "CHF", "SEK", "NOK", "DKK"
    );
    
    // Frais de traitement Stripe (2.9% + 30¢ pour les cartes canadiennes)
    private static final BigDecimal STRIPE_PERCENTAGE_FEE = new BigDecimal("0.029");
    private static final BigDecimal STRIPE_FIXED_FEE_CAD = new BigDecimal("0.30");
    private static final BigDecimal STRIPE_FIXED_FEE_USD = new BigDecimal("0.30");
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;
    
    @Value("${app.base.url}")
    private String baseUrl;
    
    // URLs de retour par défaut
    @Value("${stripe.checkout.success.url:/stripe/checkout/success}")
    private String defaultSuccessUrl;
    
    @Value("${stripe.checkout.cancel.url:/stripe/checkout/cancel}")
    private String defaultCancelUrl;
    
    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }
    
    @Override
    public boolean supportsCurrency(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency.toUpperCase());
    }
    
    @Override
    public PaymentResponseDto processPayment(Order order, PaymentRequestDto paymentRequest) 
            throws PaymentProcessingException, PaymentValidationException {
        
        logger.info("Processing Stripe Checkout payment for order {} with amount {} {}", 
                   order.getId(), paymentRequest.getAmount(), paymentRequest.getCurrency());
        
        securityLogger.info("Stripe Checkout payment processing initiated - Order: {}, Amount: {} {}", 
                           order.getId(), paymentRequest.getAmount(), paymentRequest.getCurrency());
        
        try {
            // Initialiser Stripe avec la clé secrète
            Stripe.apiKey = stripeSecretKey;
            
            // Valider les données de paiement
            validatePayment(paymentRequest);
            
            // Créer la session Checkout
            CheckoutSessionResponseDto sessionResponse = createCheckoutSession(order, paymentRequest);
            
            // Créer la réponse de paiement
            PaymentResponseDto response = PaymentResponseDto.success(
                sessionResponse.getSessionId(),
                sessionResponse.getSessionId(),
                paymentRequest.getAmount(),
                paymentRequest.getCurrency(),
                PROCESSOR_NAME
            );
            
            response.setPaymentMethod("checkout_session");
            response.setProcessingFees(calculateProcessingFees(paymentRequest.getAmount(), paymentRequest.getCurrency()));
            response.setStatus(PaymentStatus.PENDING);
            response.setMessage("Session Stripe Checkout créée avec succès");
            
            // Ajouter l'URL de redirection
            response.setRedirectUrl(sessionResponse.getSessionUrl());
            response.setRequiresRedirect(true);
            
            // Ajouter les métadonnées de la réponse Stripe
            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("checkout_session_id", sessionResponse.getSessionId());
            providerResponse.put("checkout_url", sessionResponse.getSessionUrl());
            providerResponse.put("expires_at", sessionResponse.getExpiresAt());
            response.setProviderResponse(providerResponse);
            
            logger.info("Stripe Checkout session created successfully - Session: {}", sessionResponse.getSessionId());
            securityLogger.info("Stripe Checkout session created - Order: {}, Session: {}", 
                               order.getId(), sessionResponse.getSessionId());
            
            return response;
            
        } catch (StripeException e) {
            logger.error("Stripe Checkout session creation failed for order {}: {}", order.getId(), e.getMessage(), e);
            securityLogger.warn("Stripe Checkout session creation failed - Order: {}, Error: {}, Code: {}", 
                               order.getId(), e.getMessage(), e.getCode());
            
            throw new PaymentProviderException(
                "Échec de la création de la session Stripe Checkout: " + e.getUserMessage(),
                e.getCode(),
                e.getCode(),
                PROCESSOR_NAME,
                null,
                String.valueOf(e.getStatusCode()),
                e.getRequestId(),
                e.getMessage(),
                isRetryableError(e)
            );
        } catch (Exception e) {
            logger.error("Unexpected error during Stripe Checkout session creation for order {}: {}", 
                        order.getId(), e.getMessage(), e);
            securityLogger.error("Unexpected Stripe Checkout error - Order: {}, Error: {}", 
                                order.getId(), e.getMessage());
            
            throw new PaymentProcessingException(
                "Erreur inattendue lors de la création de la session Checkout",
                "INTERNAL_ERROR",
                PROCESSOR_NAME
            );
        }
    }
    
    @Override
    public RefundResponseDto refundPayment(PaymentTransaction transaction, RefundRequestDto refundRequest) 
            throws PaymentProcessingException {
        
        logger.info("Processing Stripe refund for transaction {} with amount {} {}", 
                   transaction.getTransactionId(), refundRequest.getAmount(), transaction.getCurrency());
        
        securityLogger.info("Stripe refund processing initiated - Transaction: {}, Amount: {} {}", 
                           transaction.getTransactionId(), refundRequest.getAmount(), transaction.getCurrency());
        
        try {
            Stripe.apiKey = stripeSecretKey;
            
            // Pour Stripe Checkout, nous devons utiliser le payment_intent_id pour les remboursements
            // Le transactionId dans ce cas sera le checkout session ID, nous devons récupérer le PaymentIntent
            Session session = Session.retrieve(transaction.getTransactionId());
            String paymentIntentId = session.getPaymentIntent();
            
            if (paymentIntentId == null) {
                throw new PaymentProcessingException(
                    "Impossible de trouver le PaymentIntent pour la session: " + transaction.getTransactionId(),
                    "PAYMENT_INTENT_NOT_FOUND",
                    PROCESSOR_NAME
                );
            }
            
            // Créer les paramètres de remboursement
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .setAmount(refundRequest.getAmount().multiply(new BigDecimal("100")).longValue())
                .setReason(mapRefundReason(refundRequest.getReason()));
            
            // Ajouter les métadonnées
            if (refundRequest.getMetadata() != null) {
                Map<String, String> metadata = new HashMap<>();
                refundRequest.getMetadata().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
                paramsBuilder.setMetadata(metadata);
            }
            
            // Exécuter le remboursement
            Refund refund = Refund.create(paramsBuilder.build());
            
            // Créer la réponse
            RefundResponseDto response = RefundResponseDto.success(
                refund.getId(),
                refund.getId(),
                refundRequest.getAmount(),
                transaction.getCurrency(),
                PROCESSOR_NAME
            );
            
            response.setOriginalTransactionId(transaction.getTransactionId());
            response.setReason(refundRequest.getReason());
            response.setPartialRefund(refundRequest.isPartialRefund());
            
            // Ajouter les métadonnées de la réponse Stripe
            Map<String, Object> providerResponse = new HashMap<>();
            providerResponse.put("refund_id", refund.getId());
            providerResponse.put("status", refund.getStatus());
            providerResponse.put("reason", refund.getReason());
            providerResponse.put("payment_intent_id", paymentIntentId);
            response.setProviderResponse(providerResponse);
            
            logger.info("Stripe refund processed successfully - Refund: {}, Status: {}", 
                       refund.getId(), refund.getStatus());
            
            securityLogger.info("Stripe refund processed successfully - Transaction: {}, Refund: {}, Status: {}", 
                               transaction.getTransactionId(), refund.getId(), refund.getStatus());
            
            return response;
            
        } catch (StripeException e) {
            logger.error("Stripe refund processing failed for transaction {}: {}", 
                        transaction.getTransactionId(), e.getMessage(), e);
            securityLogger.warn("Stripe refund processing failed - Transaction: {}, Error: {}, Code: {}", 
                               transaction.getTransactionId(), e.getMessage(), e.getCode());
            
            throw new PaymentProviderException(
                "Échec du traitement du remboursement Stripe: " + e.getUserMessage(),
                e.getCode(),
                e.getCode(),
                PROCESSOR_NAME,
                transaction.getTransactionId(),
                String.valueOf(e.getStatusCode()),
                e.getRequestId(),
                e.getMessage(),
                isRetryableError(e)
            );
        }
    }
    
    @Override
    public void validatePayment(PaymentRequestDto paymentRequest) throws PaymentValidationException {
        Map<String, String> errors = new HashMap<>();
        
        // Validation du montant
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("amount", "Le montant doit être supérieur à 0");
        }
        
        // Validation de la devise
        if (!supportsCurrency(paymentRequest.getCurrency())) {
            errors.put("currency", "Devise non supportée: " + paymentRequest.getCurrency());
        }
        
        // Pour Checkout, moins de validations côté client sont nécessaires
        // Stripe Checkout gère la plupart des validations
        
        if (!errors.isEmpty()) {
            throw new PaymentValidationException("Données de paiement invalides", errors);
        }
    }
    
    @Override
    public String checkTransactionStatus(String transactionId) throws PaymentProcessingException {
        try {
            Stripe.apiKey = stripeSecretKey;
            Session session = Session.retrieve(transactionId);
            return session.getPaymentStatus();
        } catch (StripeException e) {
            throw new PaymentProviderException(
                "Impossible de vérifier le statut de la session: " + e.getUserMessage(),
                e.getCode(),
                e.getCode(),
                PROCESSOR_NAME,
                transactionId
            );
        }
    }
    
    @Override
    public BigDecimal calculateProcessingFees(BigDecimal amount, String currency) {
        BigDecimal percentageFee = amount.multiply(STRIPE_PERCENTAGE_FEE);
        BigDecimal fixedFee = "CAD".equals(currency) ? STRIPE_FIXED_FEE_CAD : STRIPE_FIXED_FEE_USD;
        return percentageFee.add(fixedFee);
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Stripe.apiKey = stripeSecretKey;
            // Test simple pour vérifier la connectivité
            com.stripe.param.checkout.SessionListParams params =
                com.stripe.param.checkout.SessionListParams.builder()
                    .setLimit(1L)
                    .build();
            Session.list(params);
            return true;
        } catch (Exception e) {
            logger.warn("Stripe Checkout service unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getApiVersion() {
        return API_VERSION;
    }
    
    /**
     * Crée une session Stripe Checkout
     */
    public CheckoutSessionResponseDto createCheckoutSession(Order order, PaymentRequestDto paymentRequest) throws StripeException {
        
        // Construire les URLs de succès et d'annulation
        String successUrl = buildCallbackUrl(defaultSuccessUrl, order.getId(), "success");
        String cancelUrl = buildCallbackUrl(defaultCancelUrl, order.getId(), "cancel");
        
        // Créer les paramètres de session
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .setCustomerEmail(order.getUser().getEmail())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(paymentRequest.getCurrency().toLowerCase())
                            .setUnitAmount(paymentRequest.getAmount().multiply(new BigDecimal("100")).longValue())
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Commande LMP #" + order.getId())
                                    .setDescription("Services de marketing digital LMP")
                                    .build()
                            )
                            .build()
                    )
                    .setQuantity(1L)
                    .build()
            );
        
        // Ajouter les métadonnées
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", String.valueOf(order.getId()));
        metadata.put("customer_email", order.getUser().getEmail());
        metadata.put("integration", "lmp_checkout_system");
        metadata.put("processor", PROCESSOR_NAME);
        
        if (paymentRequest.getMetadata() != null) {
            paymentRequest.getMetadata().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
        }
        
        // Détecter la page source pour la redirection de cancel
        String sourcePage = "/"; // Page d'accueil par défaut
        if (paymentRequest.getMetadata() != null && paymentRequest.getMetadata().containsKey("source_page")) {
            sourcePage = String.valueOf(paymentRequest.getMetadata().get("source_page"));
        }
        metadata.put("source_page", sourcePage);
        
        paramsBuilder.putAllMetadata(metadata);
        
        // Configurer les options de paiement
        paramsBuilder
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setAllowPromotionCodes(true)
            .setAutomaticTax(
                SessionCreateParams.AutomaticTax.builder()
                    .setEnabled(false) // Désactivé pour simplifier
                    .build()
            )
            .setExpiresAt(Instant.now().plusSeconds(3600).getEpochSecond()); // Expire dans 1 heure
        
        // Créer la session
        Session session = Session.create(paramsBuilder.build());
        
        // Créer la réponse
        CheckoutSessionResponseDto response = CheckoutSessionResponseDto.success(
            session.getId(),
            session.getUrl(),
            paymentRequest.getAmount(),
            paymentRequest.getCurrency()
        );
        
        response.setCustomerEmail(session.getCustomerEmail());
        response.setExpiresAt(LocalDateTime.ofEpochSecond(session.getExpiresAt(), 0, ZoneOffset.UTC));
        response.setPaymentIntentId(session.getPaymentIntent());
        
        // Ajouter les métadonnées
        Map<String, Object> responseMetadata = new HashMap<>();
        responseMetadata.put("order_id", order.getId());
        responseMetadata.put("success_url", successUrl);
        responseMetadata.put("cancel_url", cancelUrl);
        response.setMetadata(responseMetadata);
        
        return response;
    }
    
    /**
     * Construit les URLs de callback avec les paramètres appropriés
     */
    private String buildCallbackUrl(String basePath, Long orderId, String type) {
        return String.format("%s%s?order_id=%d&session_id={CHECKOUT_SESSION_ID}&type=%s", 
                           baseUrl, basePath, orderId, type);
    }
    
    /**
     * Mappe les raisons de remboursement vers les valeurs Stripe
     */
    private RefundCreateParams.Reason mapRefundReason(String reason) {
        if (reason == null) return null;
        
        return switch (reason.toLowerCase()) {
            case "duplicate" -> RefundCreateParams.Reason.DUPLICATE;
            case "fraudulent" -> RefundCreateParams.Reason.FRAUDULENT;
            case "requested_by_customer" -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
            default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        };
    }
    
    /**
     * Détermine si une erreur Stripe peut être retentée
     */
    private boolean isRetryableError(StripeException e) {
        return e.getCode() != null && (
            e.getCode().equals("rate_limit") ||
            e.getCode().equals("api_connection_error") ||
            e.getCode().equals("api_error")
        );
    }
}