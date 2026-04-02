package com.lmp.billing.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.auth.domain.User;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.service.UserService;
import com.lmp.billing.dto.GuestCheckoutPrepareRequest;
import com.lmp.billing.exception.PaymentProcessingException;
import com.lmp.billing.service.GuestOrderCheckoutService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.core.AuthenticationException;

/**
 * Paiement d'une commande créée par l'admin pour un client sans compte (lien + inscription).
 */
@RestController
@RequestMapping("/api/v1/payments/guest-order")
@Transactional
@Tag(name = "Payments", description = "Commande invité (lien sécurisé)")
public class GuestOrderPaymentRestController {

    private static final Logger logger = LoggerFactory.getLogger(GuestOrderPaymentRestController.class);

    private final GuestOrderCheckoutService guestOrderCheckoutService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public GuestOrderPaymentRestController(GuestOrderCheckoutService guestOrderCheckoutService,
            AuthenticationManager authenticationManager,
            UserService userService) {
        this.guestOrderCheckoutService = guestOrderCheckoutService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    public record GuestPreviewResponse(
            java.util.UUID orderId,
            String serviceName,
            java.math.BigDecimal totalAmount,
            String currency) {}

    public record GuestPrepareResponse(
            java.util.UUID orderId,
            String clientSecret,
            String publishableKey,
            UserResponse user) {}

    @GetMapping("/preview/{token}")
    @Operation(summary = "Aperçu commande invité", description = "Données minimales pour afficher le montant avant inscription")
    public ResponseEntity<ApiResponse<GuestPreviewResponse>> preview(@PathVariable String token) {
        return guestOrderCheckoutService.previewByToken(token)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(new GuestPreviewResponse(
                        p.orderId(), p.serviceName(), p.totalAmount(), p.currency()))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Lien invalide ou commande déjà associée")));
    }

    @PostMapping("/prepare")
    @Operation(summary = "Inscription + session + PaymentIntent",
            description = "Crée le compte, rattache la commande, ouvre la session HTTP et retourne le clientSecret Stripe")
    public ResponseEntity<ApiResponse<GuestPrepareResponse>> prepare(
            @Valid @RequestBody GuestCheckoutPrepareRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            GuestOrderCheckoutService.GuestPrepareResult result =
                    guestOrderCheckoutService.prepareCheckout(body, getClientIp(request));

            RegisterDto reg = body.getRegistration();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(reg.getEmail(), reg.getPassword()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            User user = userService.findByEmailWithRoles(reg.getEmail())
                    .orElseThrow(() -> new IllegalStateException("User not found after auth"));
            userService.updateLastLoginDate(user.getEmail());

            var payload = new GuestPrepareResponse(
                    result.orderId(),
                    result.clientSecret(),
                    result.publishableKey(),
                    UserResponse.from(user));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Compte créé — procédez au paiement", payload));

        } catch (AuthenticationException e) {
            logger.warn("guest prepare auth: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Une erreur inattendue s'est produite"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("guest prepare: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (PaymentProcessingException e) {
            logger.error("guest prepare stripe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("guest prepare: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Une erreur inattendue s'est produite"));
        }
    }

    private static String getClientIp(HttpServletRequest request) {
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
