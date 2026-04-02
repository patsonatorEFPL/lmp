package com.lmp.auth.web;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.beans.factory.annotation.Value;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.dto.CheckoutSessionResponseDto;
import com.lmp.billing.dto.PaymentRequestDto;
import com.lmp.billing.service.processor.StripeCheckoutPaymentProcessor;
import com.lmp.auth.service.UserService;
import com.lmp.billing.web.OrderController;
import com.lmp.auth.dto.PurchaseIntent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Contrôleur pour le tableau de bord utilisateur.
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + DashboardController.class.getName());

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

        private final UserService userService;

        private final StripeCheckoutPaymentProcessor stripeCheckoutProcessor;
    
        private final OrderRepository orderRepository;
    
        private final OrderController orderController;


    public DashboardController(UserService userService,
                           StripeCheckoutPaymentProcessor stripeCheckoutProcessor,
                           OrderRepository orderRepository,
                           OrderController orderController) {
        this.userService = userService;
        this.stripeCheckoutProcessor = stripeCheckoutProcessor;
        this.orderRepository = orderRepository;
        this.orderController = orderController;
    }

    /**
     * Affiche le tableau de bord principal de l'utilisateur.
     * Gère également la redirection automatique vers Stripe Checkout
     * si une intention de paiement est détectée en session.
     *
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @param processPurchase Paramètre indiquant qu'il faut traiter un paiement en attente
     * @param request La requête HTTP pour récupérer l'intention de paiement
     * @return Le nom de la vue ou une redirection vers Stripe
     */
    @GetMapping
    public String showDashboard(Model model,
                              Authentication authentication,
                              @RequestParam(name = "processPurchase", required = false) Boolean processPurchase,
                              HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:" + frontendUrl + "/login";
        }

        // Vérifier s'il faut traiter une intention de paiement
        if (Boolean.TRUE.equals(processPurchase)) {
            String userEmail = authentication.getName();
            logger.info("Processing purchase intent for user: {}", userEmail);
            try {
                User user = userService.findByEmailWithAllCollections(userEmail)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                return handlePurchaseIntentProcessing(user, request);
            } catch (Exception e) {
                logger.error("Error processing purchase intent for user {}: {}", userEmail, e.getMessage(), e);
            }
        }

        // Rediriger vers le frontend Angular
        return "redirect:" + frontendUrl + "/dashboard";
    }

    /**
     * Traite une intention de paiement en session après authentification.
     * Crée une commande temporaire et génère l'URL de redirection Stripe.
     *
     * @param user L'utilisateur authentifié
     * @param request La requête HTTP contenant l'intention de paiement
     * @return Une redirection vers Stripe ou le dashboard avec erreur
     */
    private String handlePurchaseIntentProcessing(User user, HttpServletRequest request) {
        try {
            // Récupérer l'intention de paiement depuis la session
            HttpSession session = request.getSession();
            PurchaseIntent intent = (PurchaseIntent) session.getAttribute("pendingPurchaseIntent");
            
            if (intent == null) {
                logger.warn("No purchase intent found in session for user: {}", user.getEmail());
                return "redirect:" + frontendUrl + "/dashboard?error=no_intent_found";
            }
            
            if (!intent.isValid()) {
                logger.warn("Invalid or expired purchase intent for user: {}", user.getEmail());
                session.removeAttribute("pendingPurchaseIntent");
                return "redirect:" + frontendUrl + "/dashboard?error=intent_expired";
            }
            
            logger.info("Valid purchase intent found for user {}: {} - Amount: {} {}",
                       user.getEmail(), intent.getServiceName(),
                       intent.getAmount(), intent.getCurrency());
            
            try {
                // Créer une commande temporaire pour le processeur Stripe
                Order tempOrder = createTempOrderFromIntent(user, intent);
                
                // Créer le PaymentRequestDto
                PaymentRequestDto paymentRequest = new PaymentRequestDto();
                paymentRequest.setAmount(intent.getAmount());
                paymentRequest.setCurrency(intent.getCurrency());
                paymentRequest.setPaymentProvider("stripe");
                paymentRequest.setPaymentMethod("checkout_session");
                
                // Ajouter métadonnées pour traçabilité
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", "purchase_intent");
                metadata.put("service_name", intent.getServiceName());
                metadata.put("user_id", user.getId());
                paymentRequest.setMetadata(metadata);
                
                // Créer la session Stripe Checkout
                CheckoutSessionResponseDto sessionResponse = stripeCheckoutProcessor.createCheckoutSession(tempOrder, paymentRequest);
                
                // Sauvegarder la commande temporaire
                orderRepository.save(tempOrder);
                
                // Nettoyer l'intention de paiement
                session.removeAttribute("pendingPurchaseIntent");
                
                auditLogger.info("Purchase intent processed successfully for user {} - Service: {}, Amount: {} {}, Session: {}",
                               user.getId(), intent.getServiceName(), intent.getAmount(), intent.getCurrency(), sessionResponse.getSessionId());
                
                // Rediriger vers Stripe Checkout
                return "redirect:" + sessionResponse.getSessionUrl();
                
            } catch (Exception e) {
                logger.error("Error processing purchase intent for user {}: {}", user.getId(), e.getMessage(), e);
                
                auditLogger.error("Purchase intent processing failed for user {} - Service: {}, Error: {}", 
                                user.getId(), intent.getServiceName(), e.getMessage());
                
                // Rediriger vers dashboard avec erreur
                return "redirect:" + frontendUrl + "/dashboard?error=payment_processing_failed";
            }
            
        } catch (Exception e) {
            logger.error("Error handling purchase intent for user {}: {}", user.getEmail(), e.getMessage(), e);
            return "redirect:" + frontendUrl + "/dashboard?error=processing_error";
        }
    }

    /**
     * Crée une commande temporaire à partir d'une intention de paiement
     */
    private Order createTempOrderFromIntent(User user, PurchaseIntent intent) {
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(intent.getAmount());
        // 🆕 Utiliser PAYMENT_PENDING au lieu de PENDING
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // Ajouter le nom du service dans les notes pour le moment
        order.setNotes("Service: " + intent.getServiceName() + " | Currency: " + intent.getCurrency());

        OrderProgressSync.applyMinimumForStatus(order);

        // Sauvegarder pour obtenir un ID
        return orderRepository.save(order);
    }

    /**
     * Affiche la liste des factures de l'utilisateur.
     * Seules les commandes éligibles (CONFIRMED, COMPLETED, DELIVERED, PROCESSING, IN_PROGRESS)
     * sont affichées dans cette vue.
     *
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @return Le nom de la vue
     */
    @GetMapping("/invoices")
    public String showInvoices() {
        return "redirect:" + frontendUrl + "/dashboard";
    }

    @GetMapping("/orders")
    public String showOrders() {
        return "redirect:" + frontendUrl + "/dashboard";
    }

    @GetMapping("/reviews")
    public String showReviews() {
        return "redirect:" + frontendUrl + "/dashboard";
    }

    @GetMapping("/settings")
    public String showSettings() {
        return "redirect:" + frontendUrl + "/settings";
    }
}