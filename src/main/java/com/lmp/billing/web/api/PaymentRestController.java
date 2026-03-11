package com.lmp.billing.web.api;

import com.lmp.billing.domain.Order;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.billing.service.PaymentService;
import com.lmp.billing.dto.PaymentRequestDto;
import com.lmp.billing.dto.PaymentResponseDto;
import com.lmp.billing.service.processor.StripeCheckoutPaymentProcessor;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@Tag(name = "Payments", description = "Stripe Checkout et statut de paiement")
public class PaymentRestController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentRestController.class);

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ServiceCatalogService catalogService;
    private final StripeCheckoutPaymentProcessor stripeCheckoutProcessor;
    private final PaymentService paymentService;

    public PaymentRestController(OrderRepository orderRepository,
                                 UserService userService,
                                 ServiceCatalogService catalogService,
                                 StripeCheckoutPaymentProcessor stripeCheckoutProcessor,
                                 PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.catalogService = catalogService;
        this.stripeCheckoutProcessor = stripeCheckoutProcessor;
        this.paymentService = paymentService;
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
            boolean ready
    ) {}

    // =========================================================================
    // Endpoints
    // =========================================================================

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
        BigDecimal amount = offer.getPrice();
        String serviceName = offer.getService().getTitle();
        String currency = request.currency() != null ? request.currency() : "EUR";

        try {
            // Créer la commande
            Order order = new Order();
            order.setTotalAmount(amount);
            order.setCurrency(currency);
            order.setServiceName(serviceName);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setUser(user);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);

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
            metadata.put("customer_ip", getClientIp(httpRequest));

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

        } catch (Exception e) {
            logger.error("Checkout error for offer {}: {}", request.offerId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @GetMapping("/status/{orderId}")
    @Operation(summary = "Statut de paiement", description = "Vérifie si le webhook Stripe a confirmé le paiement")
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
                .map(order -> {
                    String status = order.getStatus().name();
                    String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus() : "pending";
                    boolean ready = !"PAYMENT_PENDING".equals(status) && !"PENDING".equals(status);

                    var statusResponse = new PaymentStatusResponse(orderId, status, paymentStatus, ready);
                    return ResponseEntity.ok(ApiResponse.ok(statusResponse));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri;
        }
        return request.getRemoteAddr();
    }
}
