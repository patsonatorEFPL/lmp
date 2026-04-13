package com.lmp.billing.web.api;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.service.PaymentReconciliationService;
import com.lmp.billing.service.PaymentService;
import com.lmp.billing.service.StripePaymentIntentCheckoutService;
import com.lmp.billing.dto.PaymentRequestDto;
import com.lmp.billing.exception.PaymentProcessingException;
import com.lmp.billing.exception.PaymentProviderException;
import com.lmp.billing.dto.PaymentResponseDto;
import com.lmp.billing.service.processor.StripeCheckoutPaymentProcessor;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.shared.geo.FraudScoringService;
import com.lmp.shared.geo.GeoCountryLookupService;
import com.lmp.shared.geo.GeoResolution;
import com.lmp.shared.geo.VpnDetectionService;
import com.lmp.shared.pricing.PricingContext;
import com.lmp.shared.pricing.RegionalPricingService;
import com.lmp.shared.pricing.VatCalculationService;
import com.lmp.shared.util.VatIdentifierUtils;
import com.lmp.shared.vat.ViesVatValidationService;
import com.lmp.shared.web.ClientIpResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API REST pour les paiements Stripe.
 *
 * Version REST pure (JSON in/out) du StripeCheckoutController legacy.
 * Destinée au frontend Angular SPA.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Transactional
@Tag(name = "Payments", description = "Stripe Checkout et statut de paiement")
public class PaymentRestController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRestController.class);

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ServiceCatalogService catalogService;
    private final StripeCheckoutPaymentProcessor stripeCheckoutProcessor;
    private final PaymentService paymentService;
    private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;
    private final PaymentReconciliationService paymentReconciliationService;
    private final StripePaymentIntentCheckoutService stripePaymentIntentCheckoutService;
    private final RegionalPricingService regionalPricingService;
    private final VatCalculationService vatCalculationService;
    private final GeoCountryLookupService geoCountryLookupService;
    private final VpnDetectionService vpnDetectionService;
    private final FraudScoringService fraudScoringService;
    private final ViesVatValidationService viesVatValidationService;

    public PaymentRestController(OrderRepository orderRepository,
                                 UserService userService,
                                 ServiceCatalogService catalogService,
                                 StripeCheckoutPaymentProcessor stripeCheckoutProcessor,
                                 PaymentService paymentService,
                                 OrderRealtimeEventPublisher orderRealtimeEventPublisher,
                                 PaymentReconciliationService paymentReconciliationService,
                                 StripePaymentIntentCheckoutService stripePaymentIntentCheckoutService,
                                 RegionalPricingService regionalPricingService,
                                 VatCalculationService vatCalculationService,
                                 GeoCountryLookupService geoCountryLookupService,
                                 VpnDetectionService vpnDetectionService,
                                 FraudScoringService fraudScoringService,
                                 ViesVatValidationService viesVatValidationService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.catalogService = catalogService;
        this.stripeCheckoutProcessor = stripeCheckoutProcessor;
        this.paymentService = paymentService;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
        this.paymentReconciliationService = paymentReconciliationService;
        this.stripePaymentIntentCheckoutService = stripePaymentIntentCheckoutService;
        this.regionalPricingService = regionalPricingService;
        this.vatCalculationService = vatCalculationService;
        this.geoCountryLookupService = geoCountryLookupService;
        this.vpnDetectionService = vpnDetectionService;
        this.fraudScoringService = fraudScoringService;
        this.viesVatValidationService = viesVatValidationService;
    }

    // =========================================================================
    // DTOs internes
    // =========================================================================

    public record CheckoutRequest(
            UUID offerId,
            String currency
    ) {}

    public record CheckoutResponse(
            boolean success,
            String redirectUrl,
            String sessionId,
            UUID orderId
    ) {}

    public record PaymentStatusResponse(
            UUID orderId,
            String status,
            String paymentStatus,
            boolean ready,
            boolean paymentConfirmed
    ) {}

    public record PaymentElementResponse(
            UUID orderId,
            String clientSecret,
            String publishableKey
    ) {}

    public record UpdateBillingNameRequest(
            String billingName
    ) {}

    public record CheckoutPreviewResponse(
            String serviceName,
            BigDecimal amountHt,
            BigDecimal vatAmount,
            BigDecimal totalAmount,
            int vatRate,
            boolean reverseCharge,
            String currency,
            String durationType,
            UUID offerId
    ) {}

    public record GeoCheckResponse(
            String ipCountry,
            double vpnScore,
            boolean vpnDetected,
            String vpnSources
    ) {}

    public record FraudSignalsRequest(
            String browserTimezone,
            String geoCountry,
            String billingCountry,
            boolean geoLocationDenied
    ) {}

    public record FraudCheckResponse(
            int score,
            boolean alert,
            java.util.List<String> flags
    ) {}

    public record VatValidationRequest(
            String vatNumber
    ) {}

    public record VatValidationResponse(
            boolean valid,
            boolean serviceAvailable,
            String companyName,
            String companyAddress
    ) {}

    // =========================================================================
    // Endpoints
    // =========================================================================

    @GetMapping("/checkout-preview")
    @Operation(summary = "Aperçu du checkout",
               description = "Retourne la décomposition HT / TVA / TTC pour une offre, sans créer de commande")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> checkoutPreview(
            @RequestParam UUID offerId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Optional<ServiceOffer> offerOpt = catalogService.getValidOffer(offerId);
        if (offerOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired offer"));
        }

        ServiceOffer offer = offerOpt.get();
        PricingContext displayCtx = regionalPricingService.resolve(httpRequest);
        PricingContext payCtx = regionalPricingService.resolveForPayment(displayCtx);
        BigDecimal amountEur = offer.getPrice();
        BigDecimal amountHt = regionalPricingService.convertFromEur(amountEur, payCtx);
        boolean reverseCharge = Boolean.TRUE.equals(user.getVatReverseCharge());
        BigDecimal vatAmt = vatCalculationService.vatAmount(amountHt, reverseCharge);
        BigDecimal total = vatCalculationService.applyVat(amountHt, reverseCharge);
        int vatPct = vatCalculationService.getVatRate()
                .multiply(new BigDecimal("100")).intValue();
        String currency = payCtx.currency();
        String durationType = offer.getDurationType() != null ? offer.getDurationType().name() : "ONE_TIME";

        return ResponseEntity.ok(ApiResponse.ok(new CheckoutPreviewResponse(
                offer.getService().getTitle(),
                amountHt,
                vatAmt,
                total,
                vatPct,
                reverseCharge,
                currency,
                durationType,
                offerId
        )));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Créer une session Stripe Checkout",
               description = "Crée une session de paiement sécurisée à partir d'un offerId (prix serveur-side)")
    public ResponseEntity<ApiResponse<CheckoutResponse>> createCheckout(
            @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // Vérifier l'authentification
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            String vat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Numéro de TVA manquant ou invalide : complétez un N° TVA au format intracommunautaire dans les paramètres du compte."));
            }
            // Vérification VIES — bloquer si le service confirme que le numéro est invalide
            Optional<ViesVatValidationService.ViesResult> viesResult = viesVatValidationService.validate(vat);
            if (viesResult.isPresent() && viesResult.get().serviceAvailable() && !viesResult.get().valid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Le numéro de TVA " + vat + " est invalide selon le registre VIES de la Commission européenne."));
            }
        }

        // Valider l'offre
        if (request.offerId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("offerId is required"));
        }

        Optional<ServiceOffer> offerOpt = catalogService.getValidOffer(request.offerId());
        if (offerOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired offer"));
        }

        ServiceOffer offer = offerOpt.get();
        PricingContext displayCtx = regionalPricingService.resolve(httpRequest);
        PricingContext payCtx = regionalPricingService.resolveForPayment(displayCtx);
        BigDecimal amountEur = offer.getPrice(); // HT en EUR
        BigDecimal amountHt = regionalPricingService.convertFromEur(amountEur, payCtx); // HT en devise cible
        boolean reverseCharge = Boolean.TRUE.equals(user.getVatReverseCharge());
        BigDecimal amount = vatCalculationService.applyVat(amountHt, reverseCharge); // TTC ou HT selon statut
        String serviceName = offer.getService().getTitle();
        String currency = payCtx.currency();

        logger.info("VAT_CHECKOUT - Order for user {} (reverseCharge={}): amountHT={} {}, amountCharged={} {}",
                user.getEmail(), reverseCharge, amountHt, currency, amount, currency);

        try {
            // Créer la commande avec snapshot FX figé
            Order order = new Order();
            order.setTotalAmount(amount);
            order.setCurrency(currency);
            order.setServiceName(serviceName);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setUser(user);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now());
            order.setAmountBaseEur(amountEur);
            order.setFxRate(payCtx.eurToTargetRate());
            order.setFxSource(payCtx.rateSource());

            applyVatSnapshotFromUser(order, user);

            OrderProgressSync.applyMinimumForStatus(order);

            Order savedOrder = orderRepository.save(order);
            orderRealtimeEventPublisher.publishOrderCreated(savedOrder);

            // Créer la requête de paiement
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setAmount(amount);
            paymentRequest.setCurrency(currency);
            paymentRequest.setPaymentProvider("stripe");
            paymentRequest.setPaymentMethod("checkout_session");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("serviceName", serviceName);
            metadata.put("amount", String.valueOf(amount.multiply(new BigDecimal("100")).longValue()));
            metadata.put("currency", currency);
            metadata.put("userId", String.valueOf(user.getId()));
            metadata.put("userEmail", user.getEmail());
            metadata.put("orderId", String.valueOf(savedOrder.getId()));
            metadata.put("webhook_version", "v3");
            metadata.put("creation_mode", "api_v1");
            metadata.put("order_creation", "persistent");
            metadata.put("customer_ip", ClientIpResolver.resolve(httpRequest));

            paymentRequest.setMetadata(metadata);

            // Créer la session Stripe
            PaymentResponseDto response = stripeCheckoutProcessor.processPayment(savedOrder, paymentRequest);

            if ((response.isSuccessful() || response.isPending()) && response.isRequiresRedirect()) {
                savedOrder.setStripeSessionId(response.getProviderTransactionId());
                if (response.getPaymentIntentId() != null) {
                    savedOrder.setStripePaymentIntentId(response.getPaymentIntentId());
                }
                orderRepository.save(savedOrder);

                var checkoutResponse = new CheckoutResponse(
                        true,
                        response.getRedirectUrl(),
                        response.getProviderTransactionId(),
                        savedOrder.getId()
                );
                return ResponseEntity.ok(ApiResponse.ok(checkoutResponse));
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(ApiResponse.error(
                                response.getErrorMessage() != null
                                        ? response.getErrorMessage()
                                        : "Failed to create checkout session"));
            }

        } catch (PaymentProcessingException e) {
            logger.error("Checkout (session) Stripe error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Checkout error for offer {}: {}", request.offerId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/checkout/payment-element")
    @Operation(summary = "Paiement intégré (Payment Element)",
            description = "Crée la commande et un PaymentIntent Stripe pour le formulaire embarqué (sans redirection Checkout hébergée)")
    public ResponseEntity<ApiResponse<PaymentElementResponse>> createCheckoutPaymentElement(
            @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            String vat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Numéro de TVA manquant ou invalide : complétez un N° TVA au format intracommunautaire dans les paramètres du compte."));
            }
            Optional<ViesVatValidationService.ViesResult> viesResult = viesVatValidationService.validate(vat);
            if (viesResult.isPresent() && viesResult.get().serviceAvailable() && !viesResult.get().valid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Le numéro de TVA " + vat + " est invalide selon le registre VIES de la Commission européenne."));
            }
        }

        if (request.offerId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("offerId is required"));
        }

        Optional<ServiceOffer> offerOpt = catalogService.getValidOffer(request.offerId());
        if (offerOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired offer"));
        }

        ServiceOffer offer = offerOpt.get();
        PricingContext displayCtx2 = regionalPricingService.resolve(httpRequest);
        PricingContext payCtx2 = regionalPricingService.resolveForPayment(displayCtx2);
        BigDecimal amountEur = offer.getPrice(); // HT en EUR
        BigDecimal amountHt = regionalPricingService.convertFromEur(amountEur, payCtx2); // HT en devise cible
        boolean reverseCharge = Boolean.TRUE.equals(user.getVatReverseCharge());
        BigDecimal amount = vatCalculationService.applyVat(amountHt, reverseCharge); // TTC ou HT selon statut
        String serviceName = offer.getService().getTitle();
        String currency = payCtx2.currency();

        logger.info("VAT_PAYMENT_ELEMENT - Order for user {} (reverseCharge={}): amountHT={} {}, amountCharged={} {}",
                user.getEmail(), reverseCharge, amountHt, currency, amount, currency);

        try {
            Order order = new Order();
            order.setTotalAmount(amount);
            order.setCurrency(currency);
            order.setServiceName(serviceName);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setUser(user);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now());
            order.setAmountBaseEur(amountEur);
            order.setFxRate(payCtx2.eurToTargetRate());
            order.setFxSource(payCtx2.rateSource());

            applyVatSnapshotFromUser(order, user);
            OrderProgressSync.applyMinimumForStatus(order);

            Order savedOrder = orderRepository.save(order);
            orderRealtimeEventPublisher.publishOrderCreated(savedOrder);

            StripePaymentIntentCheckoutService.PaymentIntentResult pi =
                    stripePaymentIntentCheckoutService.createOrRefreshPaymentIntent(
                            savedOrder, user, ClientIpResolver.resolve(httpRequest), "payment_element_offer");

            savedOrder.setStripePaymentIntentId(pi.paymentIntentId());
            savedOrder.setPaymentMethod("payment_element");
            savedOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);

            return ResponseEntity.ok(ApiResponse.ok(new PaymentElementResponse(
                    savedOrder.getId(),
                    pi.clientSecret(),
                    stripePaymentIntentCheckoutService.getPublishableKey())));

        } catch (PaymentProcessingException e) {
            logger.error("Payment Element checkout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Payment Element checkout error for offer {}: {}", request.offerId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PatchMapping("/orders/{orderId}/billing-name")
    @Operation(summary = "Mettre à jour le nom de facturation personnalisé",
            description = "Permet de définir un nom personnalisé sur la facture avant la confirmation du paiement")
    public ResponseEntity<ApiResponse<Void>> updateBillingName(
            @PathVariable UUID orderId,
            @RequestBody UpdateBillingNameRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Commande introuvable"));
        }

        if (request.billingName() != null && !request.billingName().isBlank()) {
            order.setBillingName(request.billingName().trim());
        } else {
            order.setBillingName(null);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/orders/{orderId}/finalize-checkout")
    @Operation(summary = "Finaliser le checkout avant confirmation Stripe",
            description = "Ré-applique le snapshot TVA depuis le profil, recalcule le montant et met à jour le PaymentIntent Stripe")
    public ResponseEntity<ApiResponse<PaymentElementResponse>> finalizeCheckout(
            @PathVariable UUID orderId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Commande introuvable"));
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cette commande n'est pas en attente de paiement"));
        }

        // Vérification VIES si reverse charge
        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            String vat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Numéro de TVA manquant ou invalide."));
            }
            Optional<ViesVatValidationService.ViesResult> viesResult = viesVatValidationService.validate(vat);
            if (viesResult.isPresent() && viesResult.get().serviceAvailable() && !viesResult.get().valid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Le numéro de TVA " + vat + " est invalide selon le registre VIES."));
            }
        }

        // Ré-appliquer le snapshot TVA
        applyVatSnapshotFromUser(order, user);

        // Recalculer le montant
        boolean reverseCharge = Boolean.TRUE.equals(order.getVatReverseCharge());
        BigDecimal amountHt = order.getAmountBaseEur() != null ? order.getAmountBaseEur() : order.getTotalAmount();
        // Si la commande a un taux FX, convertir
        if (order.getFxRate() != null && order.getAmountBaseEur() != null) {
            amountHt = order.getAmountBaseEur().multiply(order.getFxRate())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal newTotal = vatCalculationService.applyVat(amountHt, reverseCharge);
        order.setTotalAmount(newTotal);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        logger.info("FINALIZE_CHECKOUT - order={} reverseCharge={} amountHT={} total={} piId={}",
                orderId, reverseCharge, amountHt, newTotal, order.getStripePaymentIntentId());

        // Mettre à jour le PaymentIntent Stripe
        try {
            String piId = order.getStripePaymentIntentId();
            if (piId != null && !piId.isBlank()) {
                stripePaymentIntentCheckoutService.updatePaymentIntentAmount(
                        piId, newTotal, order.getCurrency());
            }
        } catch (Exception e) {
            logger.error("Erreur mise à jour PaymentIntent pour commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur de mise à jour du paiement"));
        }

        return ResponseEntity.ok(ApiResponse.ok(new PaymentElementResponse(
                order.getId(),
                null, // clientSecret inchangé
                null  // publishableKey inchangé
        )));
    }

    @PostMapping("/checkout-order/{orderId}")
    @Operation(summary = "Payer une commande existante",
               description = "Crée une session Stripe Checkout pour une commande en attente de paiement (ex: commande créée par admin)")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkoutExistingOrder(
            @PathVariable UUID orderId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            String vat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Numéro de TVA manquant ou invalide : complétez un N° TVA au format intracommunautaire dans les paramètres du compte."));
            }
            Optional<ViesVatValidationService.ViesResult> viesResult = viesVatValidationService.validate(vat);
            if (viesResult.isPresent() && viesResult.get().serviceAvailable() && !viesResult.get().valid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Le numéro de TVA " + vat + " est invalide selon le registre VIES de la Commission européenne."));
            }
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cette commande n'est pas en attente de paiement"));
        }

        try {
            applyVatSnapshotFromUser(order, user);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";

            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setAmount(order.getTotalAmount());
            paymentRequest.setCurrency(currency);
            paymentRequest.setPaymentProvider("stripe");
            paymentRequest.setPaymentMethod("checkout_session");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("serviceName", order.getServiceName());
            metadata.put("amount", String.valueOf(order.getTotalAmount().multiply(new BigDecimal("100")).longValue()));
            metadata.put("currency", currency);
            metadata.put("userId", String.valueOf(user.getId()));
            metadata.put("userEmail", user.getEmail());
            metadata.put("orderId", String.valueOf(order.getId()));
            metadata.put("webhook_version", "v3");
            metadata.put("creation_mode", "admin_order_checkout");
            metadata.put("order_creation", "persistent");
            metadata.put("customer_ip", ClientIpResolver.resolve(httpRequest));
            paymentRequest.setMetadata(metadata);

            PaymentResponseDto response = stripeCheckoutProcessor.processPayment(order, paymentRequest);

            if ((response.isSuccessful() || response.isPending()) && response.isRequiresRedirect()) {
                order.setStripeSessionId(response.getProviderTransactionId());
                if (response.getPaymentIntentId() != null) {
                    order.setStripePaymentIntentId(response.getPaymentIntentId());
                }
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                var checkoutResponse = new CheckoutResponse(
                        true,
                        response.getRedirectUrl(),
                        response.getProviderTransactionId(),
                        order.getId()
                );
                return ResponseEntity.ok(ApiResponse.ok(checkoutResponse));
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(ApiResponse.error(
                                response.getErrorMessage() != null
                                        ? response.getErrorMessage()
                                        : "Failed to create checkout session"));
            }
        } catch (Exception e) {
            logger.error("Checkout error for existing order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/checkout-order/{orderId}/payment-element")
    @Operation(summary = "Payer une commande existante (Payment Element)",
            description = "Crée un PaymentIntent pour une commande PAYMENT_PENDING du client connecté")
    public ResponseEntity<ApiResponse<PaymentElementResponse>> checkoutExistingOrderPaymentElement(
            @PathVariable UUID orderId,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .orElse(null);

        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (Boolean.TRUE.equals(user.getVatReverseCharge())) {
            String vat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Numéro de TVA manquant ou invalide : complétez un N° TVA au format intracommunautaire dans les paramètres du compte."));
            }
            Optional<ViesVatValidationService.ViesResult> viesResult = viesVatValidationService.validate(vat);
            if (viesResult.isPresent() && viesResult.get().serviceAvailable() && !viesResult.get().valid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Le numéro de TVA " + vat + " est invalide selon le registre VIES de la Commission européenne."));
            }
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cette commande n'est pas en attente de paiement"));
        }

        try {
            paymentReconciliationService.syncOrderPaymentImmediately(order);
        } catch (Exception e) {
            logger.warn("Sync Stripe avant Payment Element pour commande {} : {}", orderId, e.getMessage());
        }

        order = orderRepository.findById(orderId)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Paiement déjà pris en compte pour cette commande. La liste va se mettre à jour."));
        }

        try {
            applyVatSnapshotFromUser(order, user);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            StripePaymentIntentCheckoutService.PaymentIntentResult pi =
                    stripePaymentIntentCheckoutService.createOrRefreshPaymentIntent(
                            order, user, ClientIpResolver.resolve(httpRequest), "payment_element_existing_order");

            order.setStripePaymentIntentId(pi.paymentIntentId());
            order.setPaymentMethod("payment_element");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            return ResponseEntity.ok(ApiResponse.ok(new PaymentElementResponse(
                    order.getId(),
                    pi.clientSecret(),
                    stripePaymentIntentCheckoutService.getPublishableKey())));

        } catch (PaymentProcessingException e) {
            logger.error("Payment Element for order {}: {}", orderId, e.getMessage());
            if (e instanceof PaymentProviderException ppe && "ALREADY_PAID".equals(ppe.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Payment Element for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @GetMapping("/status/{orderId}")
    @Operation(summary = "Statut de paiement", description = "Indique si le paiement a été confirmé côté serveur (ex. après webhook prestataire)")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable UUID orderId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        return orderRepository.findById(orderId)
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()))
                .map(order -> ResponseEntity.ok(ApiResponse.ok(toPaymentStatusResponse(orderId, order))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Réconciliation immédiate avec le prestataire de paiement (implémentation actuelle : Stripe).
     * {@code verify-with-stripe} reste exposé pour compatibilité ; préférer {@code reconcile}.
     */
    @PostMapping(value = {
            "/status/{orderId}/reconcile",
            "/status/{orderId}/verify-with-stripe"
    })
    @Operation(summary = "Réconcilier le paiement",
               description = "Interroge le prestataire de paiement et met à jour la commande si le paiement est confirmé. "
                       + "Utile lorsque la notification asynchrone n'a pas encore été traitée.")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> reconcilePaymentStatus(
            @PathVariable UUID orderId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId)
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()));

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        try {
            paymentReconciliationService.syncOrderPaymentImmediately(order);
        } catch (Exception e) {
            logger.warn("payment reconcile failed for order {}: {}", orderId, e.getMessage());
        }

        return orderRepository.findById(orderId)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .map(o -> ResponseEntity.ok(ApiResponse.ok(toPaymentStatusResponse(orderId, o))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/vat/validate")
    @Operation(summary = "Valider un numéro de TVA via VIES",
            description = "Interroge le service VIES de la Commission européenne pour vérifier la validité d'un numéro de TVA intracommunautaire")
    public ResponseEntity<ApiResponse<VatValidationResponse>> validateVat(
            @RequestBody VatValidationRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        if (request.vatNumber() == null || request.vatNumber().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("vatNumber is required"));
        }

        String normalized = VatIdentifierUtils.normalize(request.vatNumber());
        if (!VatIdentifierUtils.isPlausibleEuVatFormat(normalized)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Format de numéro de TVA invalide"));
        }

        Optional<ViesVatValidationService.ViesResult> resultOpt =
                viesVatValidationService.validate(request.vatNumber());

        if (resultOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Format de numéro de TVA invalide"));
        }

        ViesVatValidationService.ViesResult result = resultOpt.get();
        return ResponseEntity.ok(ApiResponse.ok(new VatValidationResponse(
                result.valid(),
                result.serviceAvailable(),
                result.name(),
                result.address()
        )));
    }

    @GetMapping("/geo-check")
    @Operation(summary = "Geo + VPN check",
            description = "Returns IP country and VPN score for the connected client")
    public ResponseEntity<ApiResponse<GeoCheckResponse>> geoCheck(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        String ip = ClientIpResolver.resolve(httpRequest);
        logger.debug("[FRAUD-DEBUG] geo-check -> IP={}", ip);

        String ipCountry = geoCountryLookupService.resolve(httpRequest)
                .map(GeoResolution::countryCode).orElse(null);
        logger.debug("[FRAUD-DEBUG] geo-check -> ipCountry={}", ipCountry);

        VpnDetectionService.VpnCheckResult vpnResult = vpnDetectionService.check(ip);
        logger.debug("[FRAUD-DEBUG] geo-check -> vpnScore={} vpnDetected={} sources={}",
                vpnResult.normalizedScore(), vpnResult.vpnDetected(), vpnResult.sources());

        return ResponseEntity.ok(ApiResponse.ok(new GeoCheckResponse(
                ipCountry,
                vpnResult.normalizedScore(),
                vpnResult.vpnDetected(),
                vpnResult.sources())));
    }

    @PatchMapping("/orders/{orderId}/fraud-signals")
    @Operation(summary = "Record fraud signals",
            description = "Receives frontend signals (timezone, geolocation, billing country) and computes fraud score")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> updateFraudSignals(
            @PathVariable UUID orderId,
            @RequestBody FraudSignalsRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Commande introuvable"));
        }

        String ip = ClientIpResolver.resolve(httpRequest);
        logger.debug("[FRAUD-DEBUG] fraud-signals -> orderId={} IP={}", orderId, ip);

        String ipCountry = geoCountryLookupService.resolve(httpRequest)
                .map(GeoResolution::countryCode).orElse(null);

        VpnDetectionService.VpnCheckResult vpnResult = vpnDetectionService.check(ip);

        order.setIpCountry(ipCountry);
        order.setIpAddress(ip);
        order.setVpnScore(BigDecimal.valueOf(vpnResult.normalizedScore()));
        order.setVpnSources(vpnResult.sources());
        order.setBrowserTimezone(request.browserTimezone());
        order.setGeoCountry(request.geoCountry() != null ? request.geoCountry().trim().toUpperCase() : null);
        logger.debug("[FRAUD-DEBUG] fraud-signals -> persisted: ipCountry={} vpnScore={} tz={} geo={} billing={}",
                ipCountry, vpnResult.normalizedScore(), request.browserTimezone(),
                request.geoCountry(), request.billingCountry());

        // Resolve VAT-related fraud signals from user profile + VIES
        String vatCountryPrefix = null;
        String viesCompanyName = null;
        String vatCompanyNameForScoring = null;
        boolean viesUnavailable = false;

        if (Boolean.TRUE.equals(user.getVatReverseCharge()) && user.getVatNumber() != null) {
            String normalizedVat = VatIdentifierUtils.normalize(user.getVatNumber());
            if (VatIdentifierUtils.isPlausibleEuVatFormat(normalizedVat)) {
                vatCountryPrefix = normalizedVat.substring(0, 2);
                vatCompanyNameForScoring = user.getCompanyName();

                Optional<ViesVatValidationService.ViesResult> viesResult =
                        viesVatValidationService.validate(normalizedVat);
                if (viesResult.isPresent()) {
                    ViesVatValidationService.ViesResult vr = viesResult.get();
                    if (vr.serviceAvailable()) {
                        viesCompanyName = vr.name();
                    } else {
                        viesUnavailable = true;
                    }
                }
            }
        }

        FraudScoringService.FraudSignals signals = new FraudScoringService.FraudSignals(
                ipCountry,
                vpnResult.normalizedScore(),
                request.browserTimezone(),
                request.geoCountry() != null ? request.geoCountry().trim().toUpperCase() : null,
                request.billingCountry() != null ? request.billingCountry().trim().toUpperCase() : null,
                null,
                request.geoLocationDenied(),
                vatCountryPrefix,
                viesCompanyName,
                vatCompanyNameForScoring,
                viesUnavailable
        );
        FraudScoringService.FraudResult result = fraudScoringService.score(signals);

        order.setFraudScore(result.score());
        order.setFraudFlags(String.join(",", result.flags()));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        logger.info("[FRAUD-DEBUG] fraud-signals -> orderId={} score={} alert={} flags={}",
                orderId, result.score(), result.alert(), result.flags());

        return ResponseEntity.ok(ApiResponse.ok(new FraudCheckResponse(
                result.score(), result.alert(), result.flags())));
    }


    // =========================================================================
    // Helpers
    // =========================================================================

    private static PaymentStatusResponse toPaymentStatusResponse(UUID orderId, Order order) {
        String status = order.getStatus().name();
        String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : "pending";
        boolean ready = !"PAYMENT_PENDING".equals(status);
        boolean paymentConfirmed = ready && !Set.of(
                "CANCELLED", "REFUNDED", "PENDING"
        ).contains(status);
        return new PaymentStatusResponse(orderId, status, paymentStatus, ready, paymentConfirmed);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private static void applyVatSnapshotFromUser(Order order, User user) {
        boolean reverse = Boolean.TRUE.equals(user.getVatReverseCharge());
        order.setVatReverseCharge(reverse);
        String vat = VatIdentifierUtils.normalize(user.getVatNumber());
        order.setCustomerVatNumber(reverse && !vat.isEmpty() ? vat : null);
        order.setVatCompanyName(reverse && user.getCompanyName() != null && !user.getCompanyName().isBlank()
                ? user.getCompanyName().trim() : null);
    }

}
