package com.lmp.billing.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.auth.domain.User;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.service.AuthService;
import com.lmp.auth.service.UserService;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.dto.GuestCheckoutPrepareRequest;
import com.lmp.billing.exception.PaymentProcessingException;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.shared.util.VatIdentifierUtils;

/**
 * Aperçu et préparation (inscription + PaymentIntent) pour une commande admin « invité ».
 */
@Service
public class GuestOrderCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(GuestOrderCheckoutService.class);

    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final UserService userService;
    private final StripePaymentIntentCheckoutService stripePaymentIntentCheckoutService;

    public GuestOrderCheckoutService(OrderRepository orderRepository,
            AuthService authService,
            UserService userService,
            StripePaymentIntentCheckoutService stripePaymentIntentCheckoutService) {
        this.orderRepository = orderRepository;
        this.authService = authService;
        this.userService = userService;
        this.stripePaymentIntentCheckoutService = stripePaymentIntentCheckoutService;
    }

    @Transactional(readOnly = true)
    public Optional<GuestOrderPreview> previewByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return orderRepository.findByCheckoutToken(token.trim())
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_PENDING)
                .filter(this::guestOrderPreviewAllowedForPrincipal)
                .map(o -> new GuestOrderPreview(
                        o.getId(),
                        o.getServiceName(),
                        o.getTotalAmount(),
                        o.getCurrency() != null ? o.getCurrency() : "EUR"));
    }

    /**
     * Avant inscription : commande sans utilisateur. Après prepare : même lien + session du client
     * pour permettre un rechargement sur l’étape paiement (token conservé jusqu’au paiement confirmé).
     */
    private boolean guestOrderPreviewAllowedForPrincipal(Order order) {
        if (order.getUser() == null) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return false;
        }
        String orderEmail = order.getUser().getEmail();
        return orderEmail != null && orderEmail.equalsIgnoreCase(name.trim());
    }

    @Transactional
    public GuestPrepareResult prepareCheckout(GuestCheckoutPrepareRequest body, String customerIp)
            throws PaymentProcessingException {

        String token = body.getCheckoutToken() != null ? body.getCheckoutToken().trim() : "";
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Token de commande manquant");
        }

        Order order = orderRepository.findByCheckoutToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lien invalide ou expiré"));

        if (order.getUser() != null) {
            throw new IllegalStateException("Cette commande a déjà été associée à un compte");
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Cette commande n'est plus payable via ce lien");
        }

        RegisterDto reg = body.getRegistration();
        if (reg == null) {
            throw new IllegalArgumentException("Données d'inscription manquantes");
        }

        if (authService.existsByEmail(reg.getEmail())) {
            throw new IllegalStateException(
                    "Cet email est déjà enregistré. Connectez-vous et payez la commande depuis votre espace.");
        }

        authService.validateRegistrationData(reg);

        boolean reverse = Boolean.TRUE.equals(body.getVatReverseCharge());
        if (reverse) {
            String vat = VatIdentifierUtils.normalize(body.getVatNumber());
            if (vat.isEmpty() || !VatIdentifierUtils.isPlausibleEuVatFormat(vat)) {
                throw new IllegalArgumentException(
                        "Numéro de TVA manquant ou invalide pour l'autoliquidation.");
            }
        }

        User user = authService.registerUser(reg);

        User managed = userService.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après inscription"));
        managed.setVatReverseCharge(reverse);
        String normalizedVat = VatIdentifierUtils.normalize(body.getVatNumber());
        managed.setVatNumber(reverse && normalizedVat != null && !normalizedVat.isEmpty() ? normalizedVat : null);
        userService.save(managed);

        order.setUser(managed);
        // Conserver checkout_token jusqu’au paiement confirmé (webhook) pour que le lien ?t=… reste
        // résolvable après rechargement (étape paiement), pour un client déjà connecté.
        applyVatSnapshotFromUser(order, managed);
        order.setUpdatedAt(LocalDateTime.now());
        order.setLastModifiedAt(LocalDateTime.now());
        orderRepository.save(order);

        try {
            authService.sendVerificationEmail(managed);
            authService.sendWelcomeEmail(managed);
        } catch (Exception e) {
            logger.warn("Emails post-inscription invité non envoyés: {}", e.getMessage());
        }

        StripePaymentIntentCheckoutService.PaymentIntentResult pi =
                stripePaymentIntentCheckoutService.createOrRefreshPaymentIntent(
                        order, managed, customerIp, "guest_checkout");

        order.setStripePaymentIntentId(pi.paymentIntentId());
        order.setPaymentMethod("payment_element");
        order.setUpdatedAt(LocalDateTime.now());
        OrderProgressSync.applyMinimumForStatus(order);
        orderRepository.save(order);

        return new GuestPrepareResult(
                order.getId(),
                pi.clientSecret(),
                stripePaymentIntentCheckoutService.getPublishableKey());
    }

    private static void applyVatSnapshotFromUser(Order order, User user) {
        boolean rev = Boolean.TRUE.equals(user.getVatReverseCharge());
        order.setVatReverseCharge(rev);
        String vat = VatIdentifierUtils.normalize(user.getVatNumber());
        order.setCustomerVatNumber(rev && vat != null && !vat.isEmpty() ? vat : null);
    }

    public record GuestOrderPreview(
            java.util.UUID orderId,
            String serviceName,
            java.math.BigDecimal totalAmount,
            String currency) {}

    public record GuestPrepareResult(
            java.util.UUID orderId,
            String clientSecret,
            String publishableKey) {}
}
