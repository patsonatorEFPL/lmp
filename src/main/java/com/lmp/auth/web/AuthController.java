package com.lmp.auth.web;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.auth.domain.User;
import com.lmp.catalog.domain.ServiceOffer;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.auth.service.AuthService;
import com.lmp.catalog.service.ServiceCatalogService;
import com.lmp.shared.pricing.RegionalPricingService;
import com.lmp.auth.service.UserService;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.RegisterWithOrderDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Contrôleur pour les endpoints d'authentification qui nécessitent
 * une interaction serveur directe (vérification email, inscription+checkout).
 * 
 * Les pages de login/register sont désormais gérées par le frontend Angular.
 * Les API REST d'authentification sont dans AuthRestController.
 */
@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final AuthenticationManager authenticationManager;
    private final ServiceCatalogService serviceCatalogService;

    private final RegionalPricingService regionalPricingService;

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    public AuthController(AuthService authService,
                           UserService userService,
                           OrderRepository orderRepository,
                           AuthenticationManager authenticationManager,
                           ServiceCatalogService serviceCatalogService,
                           RegionalPricingService regionalPricingService) {
        this.authService = authService;
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.authenticationManager = authenticationManager;
        this.serviceCatalogService = serviceCatalogService;
        this.regionalPricingService = regionalPricingService;
    }

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     * Redirige vers le frontend Angular avec le résultat.
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token) {
        try {
            boolean verified = authService.verifyEmail(token);
            if (verified) {
                return "redirect:" + frontendUrl + "/login?verified=true";
            }
        } catch (Exception e) {
            // Log silently
        }
        return "redirect:" + frontendUrl + "/login?error=verification_failed";
    }

    /**
     * Renvoie l'email de vérification.
     * Rate limité à 1 renvoi par 2 minutes (via session).
     */
    @PostMapping("/resend-verification")
    public String resendVerification(Authentication authentication,
            HttpServletRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:" + frontendUrl + "/login";
        }

        // Rate limit basé sur la session (1 renvoi / 2 minutes)
        String sessionKey = "lastResendVerification";
        Long lastResend = (Long) request.getSession().getAttribute(sessionKey);
        long now = System.currentTimeMillis();

        if (lastResend != null && (now - lastResend) < 120_000) {
            return "redirect:" + frontendUrl + "/dashboard";
        }

        try {
            authService.resendVerificationEmail(authentication.getName());
            request.getSession().setAttribute(sessionKey, now);
        } catch (Exception e) {
            // Log silently
        }

        return "redirect:" + frontendUrl + "/dashboard";
    }

    /**
     * Endpoint pour l'inscription avec commande intégrée.
     * Ce endpoint combine l'inscription de l'utilisateur, la connexion automatique,
     * la création de commande et la redirection vers Stripe Checkout.
     */
    @PostMapping("/register-and-checkout")
    @ResponseBody
    public ResponseEntity<?> registerAndCheckout(@Valid @RequestBody RegisterWithOrderDto registerWithOrderDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            if (!registerWithOrderDto.isPasswordMatching()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PASSWORD_MISMATCH",
                        "message", "Les mots de passe ne correspondent pas"));
            }

            if (!registerWithOrderDto.getAcceptTerms()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "TERMS_NOT_ACCEPTED",
                        "message", "Vous devez accepter les conditions d'utilisation"));
            }

            if (authService.existsByEmail(registerWithOrderDto.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "EMAIL_EXISTS",
                        "message", "Un utilisateur avec cet email existe déjà"));
            }

            // Sécurisation prix : si offerId fourni, le prix vient de la DB
            java.math.BigDecimal validatedAmount = registerWithOrderDto.getAmount();
            String validatedServiceName = registerWithOrderDto.getServiceName();
            String orderCurrency = registerWithOrderDto.getCurrency();
            java.math.BigDecimal orderFxRate = null;
            String orderFxSource = null;
            java.math.BigDecimal orderAmountBaseEur = null;

            if (registerWithOrderDto.getOfferId() != null) {
                java.util.Optional<ServiceOffer> offerOpt = serviceCatalogService
                        .getValidOffer(registerWithOrderDto.getOfferId());
                if (offerOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_OFFER",
                            "message", "L'offre demandée n'existe pas, est inactive ou a expiré"));
                }
                ServiceOffer offer = offerOpt.get();
                var pricingContext = regionalPricingService.resolve(request);
                java.math.BigDecimal offerEurAmt = offer.getPrice();
                validatedAmount = regionalPricingService.convertFromEur(offerEurAmt, pricingContext);
                orderCurrency = pricingContext.currency();
                validatedServiceName = offer.getService().getTitle();
                orderFxRate = pricingContext.eurToTargetRate();
                orderFxSource = pricingContext.rateSource();
                orderAmountBaseEur = offerEurAmt;
            } else {
                if (validatedAmount == null ||
                        validatedAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_AMOUNT",
                            "message", "Le montant doit être supérieur à 0"));
                }
            }

            if (orderCurrency == null || orderCurrency.isBlank()) {
                orderCurrency = "EUR";
            }

            // Créer l'utilisateur
            RegisterDto registerDto = registerWithOrderDto.toRegisterDto();
            User newUser = authService.registerUser(registerDto);

            // Connexion automatique
            authenticateUser(registerWithOrderDto.getEmail(),
                    registerWithOrderDto.getPassword(), request, response);

            // Créer la commande avec snapshot FX figé
            Order order = new Order();
            order.setUser(newUser);
            order.setServiceName(validatedServiceName);
            order.setCurrency(orderCurrency);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setTotalAmount(validatedAmount);
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            if (orderAmountBaseEur != null) order.setAmountBaseEur(orderAmountBaseEur);
            if (orderFxRate != null) order.setFxRate(orderFxRate);
            if (orderFxSource != null) order.setFxSource(orderFxSource);
            OrderProgressSync.applyMinimumForStatus(order);
            order = orderRepository.save(order);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "Inscription réussie ! Redirection vers le paiement...");
            responseData.put("orderId", order.getId());
            responseData.put("userId", newUser.getId());
            responseData.put("redirectUrl", "/stripe/checkout/create-session/" + order.getId());
            responseData.put("authenticated", true);

            return ResponseEntity.ok(responseData);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "REGISTRATION_FAILED",
                    "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Une erreur inattendue s'est produite"));
        }
    }

    /**
     * Connecte automatiquement un utilisateur après inscription.
     */
    private void authenticateUser(String email, String password, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);
            authToken.setDetails(new WebAuthenticationDetails(request));
            Authentication authentication = authenticationManager.authenticate(authToken);

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
            securityContextRepository.saveContext(securityContext, request, response);

            userService.updateLastLoginDate(email);
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion automatique : " + e.getMessage());
        }
    }
}
