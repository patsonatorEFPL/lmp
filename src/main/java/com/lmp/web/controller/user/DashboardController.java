package com.lmp.web.controller.user;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.Review;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.service.payment.dto.CheckoutSessionResponseDto;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.processor.StripeCheckoutPaymentProcessor;
import com.lmp.service.user.UserService;
import com.lmp.web.controller.OrderController;
import com.lmp.web.dto.PurchaseIntent;

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

    @Autowired
    private UserService userService;

    @Autowired
    private StripeCheckoutPaymentProcessor stripeCheckoutProcessor;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderController orderController;

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
            return "redirect:/login";
        }

        String userEmail = authentication.getName();
        logger.info("Dashboard accessed by user: {}", userEmail);

        try {
            User user = userService.findByEmailWithAllCollections(userEmail)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Redirection intelligente : Les ADMIN sont automatiquement redirigés vers leur interface
            if (userService.hasRole(user.getId(), "ADMIN")) {
                logger.info("Admin user {} redirected to admin dashboard", userEmail);
                
                // Si une intention de paiement existe, la transférer à l'admin dashboard
                if (Boolean.TRUE.equals(processPurchase)) {
                    return "redirect:/admin/dashboard?processPurchase=true";
                }
                
                return "redirect:/admin/dashboard";
            }

            // Vérifier s'il faut traiter une intention de paiement pour les USER
            if (Boolean.TRUE.equals(processPurchase)) {
                logger.info("Processing purchase intent for user: {}", userEmail);
                return handlePurchaseIntentProcessing(user, request);
            }

            // Interface client pour les utilisateurs USER
            logger.info("User {} accessing client dashboard", userEmail);

            // Informations utilisateur
            model.addAttribute("user", user);
            model.addAttribute("userName", user.getDisplayName());

            // 🆕 Statistiques des commandes : exclure les PAYMENT_PENDING
            List<Order> userOrders = user.getOrders() != null ?
                    user.getOrders().stream()
                            .filter(order -> order.getStatus() != OrderStatus.PAYMENT_PENDING)
                            .toList() : List.of();
            model.addAttribute("totalOrders", userOrders.size());
            
            long completedOrders = userOrders.stream()
                    .filter(order -> order.getStatus().name().equals("COMPLETED"))
                    .count();
            model.addAttribute("completedOrders", completedOrders);
            
            long pendingOrders = userOrders.stream()
                    .filter(order -> order.getStatus().name().equals("PENDING") ||
                                   order.getStatus().name().equals("IN_PROGRESS"))
                    .count();
            model.addAttribute("pendingOrders", pendingOrders);

            // Commandes récentes (les 3 dernières pour le client) : exclure les PAYMENT_PENDING
            List<Order> recentOrders = userOrders.stream()
                    .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                    .limit(3)
                    .toList();
            model.addAttribute("recentOrders", recentOrders);

            // Avis récents
            List<Review> userReviews = user.getReviews() != null ? user.getReviews().stream().toList() : List.of();
            List<Review> recentReviews = userReviews.stream()
                    .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                    .limit(3)
                    .toList();
            model.addAttribute("recentReviews", recentReviews);
            model.addAttribute("totalReviews", userReviews.size());

            // Statut de vérification de l'email
            model.addAttribute("emailVerified", user.getEmailVerified());

            // Interface client épurée pour les utilisateurs USER
            return "user/client-dashboard";

        } catch (Exception e) {
            logger.error("Error loading dashboard for user {}: {}", userEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement du tableau de bord : " + e.getMessage());
            return "error/500";
        }
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
                return "redirect:/dashboard?error=no_intent_found";
            }
            
            if (!intent.isValid()) {
                logger.warn("Invalid or expired purchase intent for user: {}", user.getEmail());
                session.removeAttribute("pendingPurchaseIntent");
                return "redirect:/dashboard?error=intent_expired";
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
                return "redirect:/dashboard?error=payment_processing_failed";
            }
            
        } catch (Exception e) {
            logger.error("Error handling purchase intent for user {}: {}", user.getEmail(), e.getMessage(), e);
            return "redirect:/dashboard?error=processing_error";
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
        
        // Sauvegarder pour obtenir un ID
        return orderRepository.save(order);
    }

    /**
     * Affiche la liste complète des commandes de l'utilisateur.
     * 
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @return Le nom de la vue
     */
    @GetMapping("/orders")
    public String showOrders(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByEmailWithAllCollections(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // 🆕 Exclure les commandes PAYMENT_PENDING de la liste complète
            List<Order> orders = user.getOrders() != null ?
                    user.getOrders().stream()
                            .filter(order -> order.getStatus() != OrderStatus.PAYMENT_PENDING)
                            .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                            .toList() : List.of();

            model.addAttribute("user", user);
            model.addAttribute("orders", orders);

            return "user/orders";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des commandes : " + e.getMessage());
            return "error/500";
        }
    }

    /**
     * Affiche la liste des avis de l'utilisateur.
     * 
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @return Le nom de la vue
     */
    @GetMapping("/reviews")
    public String showReviews(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByEmailWithAllCollections(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            List<Review> reviews = user.getReviews() != null ? 
                    user.getReviews().stream()
                            .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                            .toList() : List.of();

            model.addAttribute("user", user);
            model.addAttribute("reviews", reviews);

            return "user/reviews";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des avis : " + e.getMessage());
            return "error/500";
        }
    }

    /**
     * Affiche les paramètres du compte utilisateur.
     * 
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @return Le nom de la vue
     */
    @GetMapping("/settings")
    public String showSettings(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            User user = userService.findByEmailWithAllCollections(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            model.addAttribute("user", user);

            return "user/settings";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors du chargement des paramètres : " + e.getMessage());
            return "error/500";
        }
    }
}