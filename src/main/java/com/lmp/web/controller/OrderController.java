package com.lmp.web.controller;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.UserRepository;
import com.lmp.web.dto.PurchaseIntent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur pour la gestion des commandes
 * Fournit les endpoints pour créer des commandes temporaires
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:3000", "https://lmp-digital.ca"})
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + OrderController.class.getName());
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Crée une commande temporaire pour un service
     * Cette commande sera utilisée pour initier le paiement Stripe Checkout
     */
    @PostMapping("/create-temp")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> createTemporaryOrder(@RequestBody Map<String, Object> request,
                                                  HttpServletRequest httpRequest,
                                                  Authentication authentication) {
        
        logger.info("DEBUG - Creating temporary order for service: {}", request.get("serviceName"));
        logger.info("DEBUG - Request headers: X-Requested-With={}, Content-Type={}, User-Agent={}",
                    httpRequest.getHeader("X-Requested-With"),
                    httpRequest.getHeader("Content-Type"),
                    httpRequest.getHeader("User-Agent"));
        auditLogger.info("Temporary order creation initiated - Service: {}, Amount: {} {}, IP: {}",
                         request.get("serviceName"), request.get("amount"), request.get("currency"),
                         getClientIpAddress(httpRequest));
        
        try {
            // Valider les données de la requête
            String serviceName = (String) request.get("serviceName");
            Object amountObj = request.get("amount");
            String currency = (String) request.get("currency");
            
            if (serviceName == null || serviceName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_SERVICE_NAME",
                    "message", "Le nom du service est requis"
                ));
            }
            
            BigDecimal amount;
            try {
                if (amountObj instanceof Number) {
                    amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else if (amountObj instanceof String) {
                    amount = new BigDecimal((String) amountObj);
                } else {
                    throw new IllegalArgumentException("Format de montant invalide");
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_AMOUNT",
                    "message", "Montant invalide"
                ));
            }
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_AMOUNT",
                    "message", "Le montant doit être supérieur à 0"
                ));
            }
            
            if (currency == null || !currency.equals("CAD")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_CURRENCY",
                    "message", "Seule la devise CAD est supportée"
                ));
            }
            
            // DEBUG: Ajouter des logs pour diagnostiquer le problème d'authentification
            logger.info("DEBUG: Authentication object: {}", authentication);
            logger.info("DEBUG: Authentication name: {}", authentication != null ? authentication.getName() : "null");
            logger.info("DEBUG: Authentication principal: {}", authentication != null ? authentication.getPrincipal() : "null");
            logger.info("DEBUG: Authentication authorities: {}", authentication != null ? authentication.getAuthorities() : "null");
            
            // Récupérer l'utilisateur authentifié
            if (authentication == null) {
                logger.error("DEBUG: Authentication is null");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "AUTHENTICATION_NULL",
                    "message", "Authentification manquante"
                ));
            }
            
            String userEmail = authentication.getName();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.error("DEBUG: Authentication name is null or empty");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "AUTHENTICATION_NAME_NULL",
                    "message", "Email d'authentification manquant"
                ));
            }
            
            logger.info("DEBUG: Searching for user with email: {}", userEmail);
            User authenticatedUser = userRepository.findByEmail(userEmail).orElse(null);
            
            if (authenticatedUser == null) {
                logger.error("DEBUG: User not found with email: {}", userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "USER_NOT_FOUND",
                    "message", "Utilisateur non trouvé en base de données"
                ));
            }
            
            logger.info("DEBUG: User found: {} (ID: {})", authenticatedUser.getEmail(), authenticatedUser.getId());
            
            // Créer la commande temporaire
            Order order = new Order();
            order.setUser(authenticatedUser);
            order.setServiceName(serviceName); // FIX: Ajouter le nom du service
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(amount);
            order.setCurrency(currency); // FIX: Ajouter la devise
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now()); // FIX: Ajouter lastModifiedAt
            
            logger.info("DEBUG: Saving order with serviceName: {}, amount: {}, currency: {}",
                       serviceName, amount, currency);
            
            // Sauvegarder la commande
            order = orderRepository.save(order);
            
            // Créer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("id", order.getId());
            response.put("status", order.getStatus().name());
            response.put("amount", order.getTotalAmount());
            response.put("currency", currency);
            response.put("serviceName", serviceName);
            response.put("createdAt", order.getCreatedAt());
            
            auditLogger.info("Temporary order created successfully - Order: {}, Amount: {} {}", 
                           order.getId(), amount, currency);
            
            logger.info("Temporary order created: {}", order.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating temporary order: {}", e.getMessage(), e);
            auditLogger.error("Temporary order creation failed - Error: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Erreur lors de la création de la commande"
            ));
        }
    }
    
    /**
     * Endpoint pour l'inscription et checkout intégré.
     * @deprecated Ce endpoint n'est plus utilisé. Utilisez /register-and-checkout dans AuthController
     *
     * @param request Les données d'inscription et de commande
     * @param httpRequest La requête HTTP
     * @return Redirection vers l'endpoint d'authentification
     */
    @PostMapping("/register-and-checkout")
    @Deprecated
    public ResponseEntity<?> registerAndCheckout(@RequestBody Map<String, Object> request,
                                                 HttpServletRequest httpRequest) {
        
        logger.warn("Deprecated endpoint /api/orders/register-and-checkout called. Use /register-and-checkout instead.");
        
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).body(Map.of(
            "error", "ENDPOINT_MOVED",
            "message", "Cet endpoint a été déplacé. Veuillez utiliser /register-and-checkout",
            "newEndpoint", "/register-and-checkout"
        ));
    }
    
    /**
     * Sauvegarde l'intention de paiement pour un utilisateur anonyme en session
     * Cet endpoint est accessible sans authentification et permet de stocker
     * temporairement les détails du service avant l'inscription/connexion
     */
    @PostMapping("/save-purchase-intent")
    public ResponseEntity<?> savePurchaseIntent(@RequestBody Map<String, Object> request,
                                              HttpSession session,
                                              HttpServletRequest httpRequest) {
        
        logger.info("DEBUG - Saving purchase intent for anonymous user");
        logger.info("DEBUG - Request headers: X-Requested-With={}, Content-Type={}, User-Agent={}",
                    httpRequest.getHeader("X-Requested-With"),
                    httpRequest.getHeader("Content-Type"),
                    httpRequest.getHeader("User-Agent"));
        logger.info("DEBUG - Session ID: {}", session.getId());
        auditLogger.info("Purchase intent save initiated - Service: {}, Amount: {} {}, IP: {}",
                         request.get("serviceName"), request.get("amount"), request.get("currency"),
                         getClientIpAddress(httpRequest));
        
        try {
            // Valider les données de la requête
            String serviceName = (String) request.get("serviceName");
            Object amountObj = request.get("amount");
            String currency = (String) request.get("currency");
            
            if (serviceName == null || serviceName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_SERVICE_NAME",
                    "message", "Le nom du service est requis"
                ));
            }
            
            BigDecimal amount;
            try {
                if (amountObj instanceof Number) {
                    amount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
                } else if (amountObj instanceof String) {
                    amount = new BigDecimal((String) amountObj);
                } else {
                    throw new IllegalArgumentException("Format de montant invalide");
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_AMOUNT",
                    "message", "Montant invalide"
                ));
            }
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_AMOUNT",
                    "message", "Le montant doit être supérieur à 0"
                ));
            }
            
            if (currency == null || !currency.equals("CAD")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_CURRENCY",
                    "message", "Seule la devise CAD est supportée"
                ));
            }
            
            // Créer l'intention de paiement
            PurchaseIntent purchaseIntent = new PurchaseIntent(serviceName, amount, currency);
            
            // Vérifier que l'intention est valide
            if (!purchaseIntent.isValid()) {
                logger.error("Invalid purchase intent created: {}", purchaseIntent);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "INVALID_PURCHASE_INTENT",
                    "message", "Intention de paiement invalide"
                ));
            }
            
            // Stocker en session avec une clé spécifique
            session.setAttribute("pendingPurchaseIntent", purchaseIntent);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes d'expiration
            
            logger.info("Purchase intent saved in session: {} - Amount: {} {}",
                       serviceName, amount, currency);
            auditLogger.info("Purchase intent saved successfully - Service: {}, Amount: {} {}, SessionId: {}",
                           serviceName, amount, currency, session.getId());
            
            // Créer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Intention de paiement sauvegardée");
            response.put("serviceName", serviceName);
            response.put("amount", amount);
            response.put("currency", currency);
            response.put("sessionId", session.getId());
            response.put("expiresIn", 30 * 60); // 30 minutes en secondes
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error saving purchase intent: {}", e.getMessage(), e);
            auditLogger.error("Purchase intent save failed - Error: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Erreur lors de la sauvegarde de l'intention de paiement"
            ));
        }
    }
    
    /**
     * Récupère l'intention de paiement en session pour un utilisateur anonyme
     * Utilisé principalement pour vérifier l'état de l'intention avant l'authentification
     */
    @GetMapping("/get-purchase-intent")
    public ResponseEntity<?> getPurchaseIntent(HttpSession session,
                                             HttpServletRequest httpRequest) {
        
        logger.info("Retrieving purchase intent from session");
        
        try {
            PurchaseIntent purchaseIntent = (PurchaseIntent) session.getAttribute("pendingPurchaseIntent");
            
            if (purchaseIntent == null) {
                return ResponseEntity.ok(Map.of(
                    "hasPurchaseIntent", false,
                    "message", "Aucune intention de paiement en session"
                ));
            }
            
            if (!purchaseIntent.isValid()) {
                // Nettoyer l'intention expirée
                session.removeAttribute("pendingPurchaseIntent");
                logger.info("Expired purchase intent removed from session");
                
                return ResponseEntity.ok(Map.of(
                    "hasPurchaseIntent", false,
                    "message", "Intention de paiement expirée"
                ));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasPurchaseIntent", true);
            response.put("serviceName", purchaseIntent.getServiceName());
            response.put("amount", purchaseIntent.getAmount());
            response.put("currency", purchaseIntent.getCurrency());
            response.put("timestamp", purchaseIntent.getTimestamp());
            response.put("expiresIn", Math.max(0, (30 * 60 * 1000) - (System.currentTimeMillis() - purchaseIntent.getTimestamp())) / 1000);
            
            logger.info("Purchase intent retrieved: {} - Amount: {} {}",
                       purchaseIntent.getServiceName(), purchaseIntent.getAmount(), purchaseIntent.getCurrency());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving purchase intent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Erreur lors de la récupération de l'intention de paiement"
            ));
        }
    }
    
    /**
     * Nettoie l'intention de paiement en session
     * Utilisé après un paiement réussi ou une annulation
     */
    @DeleteMapping("/clear-purchase-intent")
    public ResponseEntity<?> clearPurchaseIntent(HttpSession session) {
        
        logger.info("Clearing purchase intent from session");
        
        try {
            PurchaseIntent purchaseIntent = (PurchaseIntent) session.getAttribute("pendingPurchaseIntent");
            
            if (purchaseIntent != null) {
                session.removeAttribute("pendingPurchaseIntent");
                logger.info("Purchase intent cleared: {} - Amount: {} {}",
                           purchaseIntent.getServiceName(), purchaseIntent.getAmount(), purchaseIntent.getCurrency());
                auditLogger.info("Purchase intent cleared - Service: {}, Amount: {} {}",
                               purchaseIntent.getServiceName(), purchaseIntent.getAmount(), purchaseIntent.getCurrency());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Intention de paiement supprimée"
            ));
            
        } catch (Exception e) {
            logger.error("Error clearing purchase intent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Erreur lors de la suppression de l'intention de paiement"
            ));
        }
    }
    
    /**
     * Utilitaire pour récupérer l'adresse IP du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Récupère une commande par son ID avec toutes les données pour l'affichage client
     * LOGS DE VALIDATION: Trace les données récupérées pour diagnostiquer l'affichage statique
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId, Authentication authentication) {
        
        logger.info("DEBUG - Retrieving order details for ID: {}", orderId);
        auditLogger.info("Order details requested - OrderID: {}, User: {}", orderId, authentication.getName());
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
            
            // LOG DE VALIDATION 1: Vérifier les données de base de l'Order
            logger.info("DEBUG - Order found: ID={}, serviceName='{}', amount={}, currency='{}', status={}",
                       order.getId(), order.getServiceName(), order.getTotalAmount(), order.getCurrency(), order.getStatus());
            logger.info("DEBUG - Order dates: createdAt={}, updatedAt={}, lastModifiedAt={}",
                       order.getCreatedAt(), order.getUpdatedAt(), order.getLastModifiedAt());
            
            // Vérifier que l'utilisateur authentifié est le propriétaire de la commande ou un admin
            User authenticatedUser = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            boolean isOwner = order.getUser().getId().equals(authenticatedUser.getId());
            boolean isAdmin = authenticatedUser.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
            
            if (!isOwner && !isAdmin) {
                logger.warn("DEBUG - Access denied for user {} to order {}", authentication.getName(), orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "ACCESS_DENIED",
                    "message", "Vous n'avez pas accès à cette commande"
                ));
            }
            
            // LOG DE VALIDATION 2: Vérifier les données utilisateur
            logger.info("DEBUG - Order user: ID={}, firstName='{}', lastName='{}', email='{}'",
                       order.getUser().getId(), order.getUser().getFirstName(),
                       order.getUser().getLastName(), order.getUser().getEmail());
            
            // Construire la réponse avec TOUTES les données nécessaires pour l'affichage
            Map<String, Object> response = new HashMap<>();
            response.put("id", order.getId());
            response.put("status", order.getStatus().name());
            response.put("serviceName", order.getServiceName()); // ✅ DONNÉE RÉELLE
            response.put("amount", order.getTotalAmount());
            response.put("currency", order.getCurrency());
            response.put("createdAt", order.getCreatedAt());
            response.put("updatedAt", order.getUpdatedAt());
            response.put("lastModifiedAt", order.getLastModifiedAt());
            response.put("userId", order.getUser().getId());
            
            // Ajouter les informations utilisateur
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", order.getUser().getId());
            userInfo.put("firstName", order.getUser().getFirstName());
            userInfo.put("lastName", order.getUser().getLastName());
            userInfo.put("email", order.getUser().getEmail());
            response.put("user", userInfo);
            
            // Ajouter les détails de paiement
            Map<String, Object> paymentInfo = new HashMap<>();
            paymentInfo.put("paymentStatus", order.getPaymentStatus());
            paymentInfo.put("stripeSessionId", order.getStripeSessionId());
            paymentInfo.put("stripePaymentIntentId", order.getStripePaymentIntentId());
            paymentInfo.put("paymentMethod", order.getPaymentMethod());
            response.put("payment", paymentInfo);
            
            // LOG DE VALIDATION 3: Tracer les données de la réponse
            logger.info("DEBUG - Response data: serviceName='{}', amount={}, currency='{}', status={}",
                       response.get("serviceName"), response.get("amount"), response.get("currency"), response.get("status"));
            logger.info("DEBUG - Response complete. Sending to frontend.");
            
            auditLogger.info("Order details retrieved successfully - OrderID: {}, ServiceName: '{}'",
                           orderId, order.getServiceName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("ERROR - Failed to retrieve order {}: {}", orderId, e.getMessage(), e);
            auditLogger.error("Order details retrieval failed - OrderID: {}, Error: {}", orderId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Endpoint spécialisé pour les détails de commande utilisés dans les modaux
     * Fournit des données formatées pour l'affichage dans dashboard-components.html
     */
    @GetMapping("/{orderId}/details")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId, Authentication authentication) {
        
        logger.info("DEBUG - Getting modal details for order: {}", orderId);
        
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
            
            // Vérifier l'accès
            User authenticatedUser = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            boolean isOwner = order.getUser().getId().equals(authenticatedUser.getId());
            boolean isAdmin = authenticatedUser.getRoles().stream()
                    .anyMatch(role -> "ADMIN".equals(role.getName()));
            
            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "ACCESS_DENIED",
                    "message", "Accès refusé"
                ));
            }
            
            // LOG DE VALIDATION 4: Tracer les données spécifiques au modal
            logger.info("DEBUG - Modal data for order {}: serviceName='{}', createdAt={}, status={}",
                       orderId, order.getServiceName(), order.getCreatedAt(), order.getStatus());
            
            // Construire la réponse formatée pour les modaux
            Map<String, Object> response = new HashMap<>();
            
            // Informations de base
            response.put("orderId", order.getId().toString());
            response.put("orderNumber", "#" + order.getId());
            response.put("serviceName", order.getServiceName()); // ✅ VRAIE DONNÉE vs "Consultation Premium"
            response.put("amount", order.getTotalAmount());
            response.put("currency", order.getCurrency());
            response.put("status", order.getStatus().name());
            
            // Dates formatées pour l'affichage
            response.put("createdAt", order.getCreatedAt()); // ✅ VRAIE DONNÉE vs "01/01/2024 10:00"
            response.put("updatedAt", order.getUpdatedAt());
            response.put("lastModifiedAt", order.getLastModifiedAt());
            
            // Information utilisateur/technicien
            String fullName = order.getUser().getFirstName() + " " + order.getUser().getLastName();
            response.put("customerName", fullName);
            response.put("customerEmail", order.getUser().getEmail());
            
            // Détails de service (à adapter selon vos besoins métier)
            Map<String, Object> serviceDetails = new HashMap<>();
            serviceDetails.put("name", order.getServiceName());
            serviceDetails.put("duration", "À déterminer"); // ✅ Remplace "2 heures" statique
            serviceDetails.put("technician", "À assigner"); // ✅ Remplace "Jean Dupont" statique
            serviceDetails.put("location", "À confirmer"); // ✅ Remplace "À domicile" statique
            response.put("serviceDetails", serviceDetails);
            
            // Historique des statuts (simplifié)
            Map<String, Object> timeline = new HashMap<>();
            timeline.put("created", order.getCreatedAt());
            timeline.put("paid", order.getPaidAt());
            timeline.put("inProgress", order.getShippedAt()); // Réutilise shippedAt pour "en cours"
            timeline.put("completed", order.getDeliveredAt());
            response.put("timeline", timeline);
            
            logger.info("DEBUG - Modal response prepared with real data: serviceName='{}', customerName='{}'",
                       order.getServiceName(), fullName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("ERROR - Failed to get order details for modal {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}