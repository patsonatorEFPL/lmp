package com.lmp.web.controller.payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lmp.domain.entity.Order;
import com.lmp.domain.entity.PaymentTransaction;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.exception.ResourceNotFoundException;
import com.lmp.domain.entity.ServiceOffer;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.PaymentTransactionRepository;
import com.lmp.repository.UserRepository;
import com.lmp.service.catalog.ServiceCatalogService;
import com.lmp.service.payment.PaymentService;
import com.lmp.service.payment.dto.PaymentRequestDto;
import com.lmp.service.payment.dto.PaymentResponseDto;
import com.lmp.service.payment.processor.StripeCheckoutPaymentProcessor;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Contrôleur pour les paiements Stripe Checkout
 * Gère les sessions de checkout directes et legacy, ainsi que les URLs de
 * callback
 * Architecture webhook-driven : métadonnées enrichies pour création automatique
 * des commandes
 */
@Controller
@RequestMapping("/stripe/checkout")
public class StripeCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutController.class);
    private static final Logger auditLogger = LoggerFactory
            .getLogger("AUDIT." + StripeCheckoutController.class.getName());

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripeCheckoutPaymentProcessor stripeCheckoutProcessor;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceCatalogService serviceCatalogService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    /**
     * Crée une session Stripe Checkout directement avec les données du service
     * Architecture webhook-driven : ne dépend plus d'une commande existante
     * Les webhooks utilisent les métadonnées pour créer automatiquement les
     * commandes
     */
    @PostMapping("/create-session-direct")
    @ResponseBody
    public ResponseEntity<?> createDirectCheckoutSession(@RequestBody Map<String, Object> serviceData,
            HttpServletRequest request) {

        logger.info("Creating direct Stripe Checkout session - Service: {}", serviceData.get("serviceName"));
        auditLogger.info("Direct Stripe Checkout session creation initiated - Service: {}, Amount: {} {}, IP: {}",
                serviceData.get("serviceName"), serviceData.get("amount"), serviceData.get("currency"),
                getClientIpAddress(request));

        try {
            // Valider les données du service
            String serviceName = (String) serviceData.get("serviceName");
            Object amountObj = serviceData.get("amount");
            String currency = (String) serviceData.get("currency");
            Object userIdObj = serviceData.get("userId");
            String userEmail = (String) serviceData.get("userEmail");
            String userFirstName = (String) serviceData.get("userFirstName");
            String userLastName = (String) serviceData.get("userLastName");
            Object offerIdObj = serviceData.get("offerId");

            // ── Sécurisation par offerId ──
            // Si offerId est fourni, le prix est récupéré côté serveur (mode sécurisé)
            BigDecimal amount;
            if (offerIdObj != null) {
                Long offerId;
                try {
                    if (offerIdObj instanceof Number) {
                        offerId = ((Number) offerIdObj).longValue();
                    } else {
                        offerId = Long.valueOf(String.valueOf(offerIdObj));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_OFFER_ID",
                            "message", "ID d'offre invalide"));
                }

                java.util.Optional<ServiceOffer> offerOpt = serviceCatalogService.getValidOffer(offerId);
                if (offerOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_OFFER",
                            "message", "L'offre demandée n'existe pas, est inactive ou a expiré"));
                }

                ServiceOffer offer = offerOpt.get();
                amount = offer.getPrice();
                serviceName = offer.getService().getTitle();
                logger.info("SECURE_CHECKOUT - offerId={}, price={}, service='{}'", offerId, amount, serviceName);

            } else {
                // Legacy: accepter le montant du frontend (rétrocompatibilité)
                logger.warn("LEGACY_CHECKOUT - Montant reçu du frontend (pas d'offerId)");

                if (serviceName == null || serviceName.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_SERVICE_NAME",
                            "message", "Le nom du service est requis"));
                }

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
                            "message", "Montant invalide"));
                }

                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "INVALID_AMOUNT",
                            "message", "Le montant doit être supérieur à 0"));
                }
            }

            if (currency == null || currency.trim().isEmpty()) {
                currency = "EUR"; // Devise par défaut
            }

            Long userId;
            try {
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.valueOf((String) userIdObj);
                } else {
                    throw new IllegalArgumentException("Format d'ID utilisateur invalide");
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_USER_ID",
                        "message", "ID utilisateur invalide"));
            }

            if (userEmail == null || userEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_USER_EMAIL",
                        "message", "Email utilisateur requis"));
            }

            logger.info("Service data validated - Service: {}, Amount: {} {}, UserId: {}, UserEmail: {}",
                    serviceName, amount, currency, userId, userEmail);

            // Créer une commande persistante dans la base de données
            Order persistentOrder = new Order();
            persistentOrder.setTotalAmount(amount);
            persistentOrder.setCurrency(currency);
            persistentOrder.setServiceName(serviceName);
            persistentOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            persistentOrder.setCreatedAt(java.time.LocalDateTime.now());
            persistentOrder.setUpdatedAt(java.time.LocalDateTime.now());
            persistentOrder.setLastModifiedAt(java.time.LocalDateTime.now());

            // Récupérer l'utilisateur réel de la base de données
            Optional<com.lmp.domain.entity.User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                persistentOrder.setUser(userOpt.get());
                logger.info("User found by ID: {} - Email: {}", userId, userOpt.get().getEmail());
            } else {
                // Fallback: essayer de trouver par email
                userOpt = userRepository.findByEmail(userEmail);
                if (userOpt.isPresent()) {
                    persistentOrder.setUser(userOpt.get());
                    logger.info("User found by email: {} - ID: {}", userEmail, userOpt.get().getId());
                } else {
                    logger.error("User not found by ID {} or email {}", userId, userEmail);
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "USER_NOT_FOUND",
                            "message", "Utilisateur non trouvé. Veuillez vous reconnecter."));
                }
            }

            // Sauvegarder la commande dans la base de données
            Order savedOrder = orderRepository.save(persistentOrder);
            logger.info("Persistent order created with ID: {} - Service: {}, Amount: {}",
                    savedOrder.getId(), serviceName, amount, currency);

            // Créer la requête de paiement
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setAmount(amount);
            paymentRequest.setCurrency(currency);
            paymentRequest.setPaymentProvider("stripe");
            paymentRequest.setPaymentMethod("checkout_session");

            // Métadonnées enrichies pour création automatique par webhook
            Map<String, Object> metadata = new HashMap<>();

            // Métadonnées de contexte
            metadata.put("customer_ip", getClientIpAddress(request));
            metadata.put("user_agent", request.getHeader("User-Agent"));
            metadata.put("source_page", request.getHeader("Referer"));

            // Langue de l'utilisateur pour Stripe Checkout (depuis cookie googtrans ou
            // payload)
            String userLanguage = resolveUserLanguageFromRequest(request, serviceData);
            metadata.put("userLanguage", userLanguage);
            logger.info("STRIPE_LANG_DEBUG - User language detected for direct session: '{}'", userLanguage);

            // Métadonnées cruciales pour le webhook
            metadata.put("serviceName", serviceName);
            metadata.put("amount", String.valueOf(amount.multiply(new BigDecimal("100")).longValue()));
            metadata.put("currency", currency);
            metadata.put("userId", String.valueOf(userId));
            metadata.put("userEmail", userEmail);
            metadata.put("orderId", String.valueOf(savedOrder.getId())); // ID de la commande persistante

            // Métadonnées utilisateur optionnelles
            if (userFirstName != null && !userFirstName.trim().isEmpty()) {
                metadata.put("userFirstName", userFirstName);
            }
            if (userLastName != null && !userLastName.trim().isEmpty()) {
                metadata.put("userLastName", userLastName);
            }

            // Métadonnées de validation
            metadata.put("webhook_version", "v3");
            metadata.put("creation_mode", "direct");
            metadata.put("order_creation", "persistent"); // Indique que la commande est déjà persistée

            logger.info(
                    "Enriching Stripe session with webhook metadata - Order: {}, Service: {}, Amount: {} centimes, Currency: {}, UserId: {}",
                    savedOrder.getId(), serviceName, metadata.get("amount"), currency, userId);

            paymentRequest.setMetadata(metadata);

            // Utiliser directement le processeur Stripe avec la commande persistante
            PaymentResponseDto response = stripeCheckoutProcessor.processPayment(savedOrder, paymentRequest);

            if ((response.isSuccessful() || response.isPending()) && response.isRequiresRedirect()) {
                auditLogger.info("Direct Stripe Checkout session created successfully - Service: {}, Session: {}",
                        serviceName, response.getProviderTransactionId());

                // 🆕 SAUVEGARDER LE PAYMENT INTENT ET LA SESSION DANS L'ORDER
                savedOrder.setStripeSessionId(response.getProviderTransactionId());
                if (response.getPaymentIntentId() != null) {
                    savedOrder.setStripePaymentIntentId(response.getPaymentIntentId());
                    logger.info("Saved PaymentIntent ID {} to Order {}", response.getPaymentIntentId(),
                            savedOrder.getId());
                }
                orderRepository.save(savedOrder);

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("redirectUrl", response.getRedirectUrl());
                successResponse.put("sessionId", response.getProviderTransactionId());

                return ResponseEntity.ok(successResponse);

            } else {
                logger.warn("Failed to create direct Stripe Checkout session for service: {}", serviceName);
                auditLogger.warn("Direct Stripe Checkout session creation failed - Service: {}, Error: {}",
                        serviceName, response.getErrorMessage());

                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                        "error", "SESSION_CREATION_FAILED",
                        "message", response.getErrorMessage() != null ? response.getErrorMessage()
                                : "Impossible de créer la session de paiement"));
            }

        } catch (Exception e) {
            logger.error("Unexpected error creating direct Stripe Checkout session: {}", e.getMessage(), e);
            auditLogger.error("Direct Stripe Checkout session creation error - Error: {}", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Une erreur inattendue s'est produite"));
        }
    }

    /**
     * Méthode legacy pour les commandes existantes
     * Maintenue pour compatibilité - utilise les métadonnées enrichies
     */
    @PostMapping("/create-session/{orderId}")
    @ResponseBody
    public ResponseEntity<?> createCheckoutSession(@PathVariable Long orderId,
            HttpServletRequest request) {

        logger.info("Creating Stripe Checkout session for existing order: {}", orderId);
        auditLogger.info("Legacy Stripe Checkout session creation initiated - Order: {}, IP: {}",
                orderId, getClientIpAddress(request));

        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));

            // Vérifier que la commande peut être payée (PAYMENT_PENDING ou PENDING)
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING && order.getStatus() != OrderStatus.PENDING) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_ORDER_STATUS",
                        "message", "Cette commande ne peut pas être payée dans son état actuel"));
            }

            // Validation des métadonnées requises pour le webhook
            if (order.getServiceName() == null || order.getServiceName().trim().isEmpty()) {
                logger.error("Webhook metadata validation failed - Missing serviceName for order: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "MISSING_SERVICE_NAME",
                        "message", "Le nom du service est requis pour cette commande"));
            }

            if (order.getUser() == null || order.getUser().getId() == null) {
                logger.error("Webhook metadata validation failed - Missing user information for order: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "MISSING_USER_INFO",
                        "message", "Les informations utilisateur sont requises pour cette commande"));
            }

            if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("Webhook metadata validation failed - Invalid total amount for order: {}", orderId);
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "INVALID_AMOUNT",
                        "message", "Le montant de la commande doit être supérieur à zéro"));
            }

            logger.info(
                    "Legacy webhook metadata validation passed - Order: {}, ServiceName: '{}', UserId: {}, Amount: {} {}",
                    orderId, order.getServiceName(), order.getUser().getId(), order.getTotalAmount(),
                    order.getCurrency() != null ? order.getCurrency() : "EUR");

            // Créer la requête de paiement
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setAmount(order.getTotalAmount());
            paymentRequest.setCurrency(order.getCurrency() != null ? order.getCurrency() : "EUR");
            paymentRequest.setPaymentProvider("stripe");
            paymentRequest.setPaymentMethod("checkout_session");

            // Ajouter les métadonnées enrichies pour le webhook
            Map<String, Object> metadata = new HashMap<>();

            // Métadonnées existantes
            metadata.put("order_id", orderId);
            metadata.put("customer_ip", getClientIpAddress(request));
            metadata.put("user_agent", request.getHeader("User-Agent"));

            // Langue de l'utilisateur pour Stripe Checkout (depuis cookie googtrans)
            String userLanguage = resolveUserLanguageFromRequest(request, null);
            metadata.put("userLanguage", userLanguage);
            logger.info("STRIPE_LANG_DEBUG - User language detected for legacy session: '{}'", userLanguage);

            // Métadonnées requises pour le webhook
            metadata.put("serviceName", order.getServiceName());
            metadata.put("amount", String.valueOf(order.getTotalAmount().multiply(new BigDecimal("100")).longValue()));
            metadata.put("currency", order.getCurrency() != null ? order.getCurrency() : "EUR");
            metadata.put("userId", String.valueOf(order.getUser().getId()));

            // Métadonnées de validation
            metadata.put("webhook_version", "v2");
            metadata.put("order_status", order.getStatus().toString());
            metadata.put("creation_mode", "legacy");

            logger.info(
                    "Enriching legacy Stripe session with webhook metadata - Order: {}, ServiceName: {}, Amount: {} centimes, Currency: {}, UserId: {}",
                    orderId, order.getServiceName(), metadata.get("amount"), metadata.get("currency"),
                    metadata.get("userId"));

            paymentRequest.setMetadata(metadata);

            // Traiter le paiement (créer la session)
            PaymentResponseDto response = paymentService.processPayment(orderId, paymentRequest);

            if ((response.isSuccessful() || response.isPending()) && response.isRequiresRedirect()) {
                auditLogger.info("Legacy Stripe Checkout session created successfully - Order: {}, Session: {}",
                        orderId, response.getProviderTransactionId());

                // Sauvegarder le stripeSessionId dans l'order
                order.setStripeSessionId(response.getProviderTransactionId());
                orderRepository.save(order);

                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("redirectUrl", response.getRedirectUrl());
                successResponse.put("sessionId", response.getProviderTransactionId());

                return ResponseEntity.ok(successResponse);

            } else {
                logger.warn("Failed to create legacy Stripe Checkout session for order: {}", orderId);
                auditLogger.warn("Legacy Stripe Checkout session creation failed - Order: {}, Error: {}",
                        orderId, response.getErrorMessage());

                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of(
                        "error", "SESSION_CREATION_FAILED",
                        "message", response.getErrorMessage() != null
                                ? response.getErrorMessage()
                                : "Impossible de créer la session de paiement"));
            }

        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found for legacy Stripe Checkout: {}", orderId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Unexpected error creating legacy Stripe Checkout session for order {}: {}",
                    orderId, e.getMessage(), e);
            auditLogger.error("Legacy Stripe Checkout session creation error - Order: {}, Error: {}",
                    orderId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "INTERNAL_ERROR",
                    "message", "Une erreur inattendue s'est produite"));
        }
    }

    /**
     * Page intermédiaire de traitement du paiement.
     * Affiche une progression animée pendant que le webhook Stripe confirme le
     * paiement.
     * Redirige automatiquement vers la page succès une fois le paiement confirmé.
     */
    @GetMapping("/processing")
    public String paymentProcessing(@RequestParam("order_id") Long orderId,
            @RequestParam("session_id") String sessionId,
            @RequestParam(defaultValue = "success") String type,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Displaying payment processing page - Order: {}, Session: {}", orderId, sessionId);

        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));

            // Si le paiement est déjà confirmé (webhook rapide), rediriger directement vers
            // succès
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
                logger.info("Payment already confirmed for order #{}, redirecting to success", orderId);
                return "redirect:/stripe/checkout/success?order_id=" + orderId
                        + "&session_id=" + sessionId + "&type=success";
            }

            // Sinon, afficher la page de traitement avec les infos nécessaires au polling
            model.addAttribute("orderId", orderId);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("serviceName", order.getServiceName());
            model.addAttribute("totalAmount", order.getTotalAmount());
            model.addAttribute("currency", order.getCurrency() != null ? order.getCurrency() : "EUR");

            return "payment/processing";

        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found in processing callback: {}", orderId);
            redirectAttributes.addFlashAttribute("error",
                    "Commande non trouvée. Veuillez contacter le support.");
            return "redirect:/services";

        } catch (Exception e) {
            logger.error("Error processing payment processing callback - Order: {}, Session: {}: {}",
                    orderId, sessionId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Une erreur s'est produite. Veuillez contacter le support.");
            return "redirect:/services";
        }
    }

    /**
     * Page de succès après paiement Stripe Checkout
     * Architecture webhook-driven : le statut de la commande est mis à jour par le
     * webhook.
     * Cette page affiche simplement les informations de la commande.
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("order_id") Long orderId,
            @RequestParam("session_id") String sessionId,
            @RequestParam(defaultValue = "success") String type,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Processing Stripe Checkout success callback - Order: {}, Session: {}", orderId, sessionId);
        auditLogger.info("Payment success callback - Order: {}, Session: {}, Type: {}",
                orderId, sessionId, type);

        try {
            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));

            // Vérifier si le webhook a déjà mis à jour la commande
            boolean paymentConfirmed = order.getStatus() != OrderStatus.PAYMENT_PENDING;

            // Ajouter les informations de base au modèle
            model.addAttribute("order", order);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("success", true);
            model.addAttribute("totalAmount", order.getTotalAmount());
            model.addAttribute("currency", order.getCurrency() != null ? order.getCurrency() : "EUR");
            model.addAttribute("paymentMethod", "Stripe Checkout");

            // Essayer de récupérer la transaction (peut ne pas encore exister si webhook en
            // attente)
            Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByTransactionId(sessionId);

            if (transactionOpt.isPresent()) {
                PaymentTransaction transaction = transactionOpt.get();
                model.addAttribute("transaction", transaction);
                logger.info("Transaction found for payment success - Order: {}, Transaction: {}",
                        orderId, transaction.getId());
            } else {
                // La transaction n'existe pas encore (webhook en cours de traitement)
                logger.info("Transaction not yet created (webhook pending) - Order: {}, Session: {}",
                        orderId, sessionId);
                model.addAttribute("transaction", null);
                model.addAttribute("webhookPending", !paymentConfirmed);
            }

            auditLogger.info(
                    "Payment success page displayed - Order: {}, Amount: {} {}, Status: {}, PaymentConfirmed: {}",
                    orderId, order.getTotalAmount(), order.getCurrency(), order.getStatus(), paymentConfirmed);

            return "payment/success";

        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found in success callback: {}", orderId);
            redirectAttributes.addFlashAttribute("error",
                    "Commande non trouvée. Veuillez contacter le support.");
            return "redirect:/services";

        } catch (Exception e) {
            logger.error("Error processing payment success callback - Order: {}, Session: {}: {}",
                    orderId, sessionId, e.getMessage(), e);
            auditLogger.error("Payment success callback error - Order: {}, Session: {}, Error: {}",
                    orderId, sessionId, e.getMessage());

            redirectAttributes.addFlashAttribute("error",
                    "Une erreur s'est produite. Veuillez contacter le support.");
            return "redirect:/services";
        }
    }

    /**
     * Page d'annulation après annulation Stripe Checkout
     */
    @GetMapping("/cancel")
    public String paymentCancel(@RequestParam(value = "order_id", required = false) Long orderId,
            @RequestParam(value = "session_id", required = false) String sessionId,
            @RequestParam(defaultValue = "cancel") String type,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // 🔍 LOGS DE DIAGNOSTIC - Validation des paramètres reçus
        logger.warn("🔍 DIAGNOSTIC ALERTE CHROME - Cancel callback appelé");
        logger.warn("🔍 Parameters reçus - orderId: {}, sessionId: {}, type: {}", orderId, sessionId, type);
        logger.warn("🔍 Request URL complète: {}", request.getRequestURL() + "?" + request.getQueryString());
        logger.warn("🔍 Headers User-Agent: {}", request.getHeader("User-Agent"));
        logger.warn("🔍 Headers Referer: {}", request.getHeader("Referer"));

        logger.info("Processing Stripe Checkout cancel callback - Order: {}, Session: {}", orderId, sessionId);
        auditLogger.info("Payment cancel callback - Order: {}, Session: {}, Type: {}",
                orderId, sessionId, type);

        try {
            // Validation des paramètres critiques
            if (orderId == null) {
                logger.warn("🔍 DIAGNOSTIC - orderId est null, redirection vers /services");
                redirectAttributes.addFlashAttribute("info", "Paiement annulé. Vous pouvez réessayer à tout moment.");
                return "redirect:/services";
            }

            if (sessionId == null || sessionId.trim().isEmpty()) {
                logger.warn("🔍 DIAGNOSTIC - sessionId est null/vide, redirection vers /services");
                redirectAttributes.addFlashAttribute("info", "Paiement annulé. Vous pouvez réessayer à tout moment.");
                return "redirect:/services";
            }

            // Récupérer la commande
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée: " + orderId));

            // Récupérer la page source depuis les métadonnées de la session Stripe
            String sourcePage = getSourcePageFromStripeSession(sessionId);

            // 🔍 LOGS DE DIAGNOSTIC - Analyse de la sourcePage
            logger.warn("🔍 DIAGNOSTIC - sourcePage récupérée: '{}'", sourcePage);
            if (sourcePage != null) {
                logger.warn("🔍 DIAGNOSTIC - sourcePage validation:");
                logger.warn("🔍   - Commence par http: {}", sourcePage.startsWith("http"));
                logger.warn("🔍   - Commence par /: {}", sourcePage.startsWith("/"));
                logger.warn("🔍   - Contient domaine externe: {}",
                        sourcePage.contains("://") && !sourcePage.contains("lmp-services.ca"));
                logger.warn("🔍   - Longueur: {}", sourcePage.length());
            }

            auditLogger.info("Payment cancel callback - Order: {}, Amount: {} EUR, Source: {}",
                    orderId, order.getTotalAmount(), sourcePage);

            redirectAttributes.addFlashAttribute("info",
                    "Paiement annulé. Vous pouvez réessayer à tout moment.");

            // 🔒 SÉCURISATION - Validation stricte de la redirection
            String redirectUrl;
            if (sourcePage != null && isValidInternalUrl(sourcePage)) {
                redirectUrl = sourcePage;
                logger.warn("🔍 DIAGNOSTIC - Redirection vers sourcePage validée: {}", redirectUrl);
            } else {
                redirectUrl = "/services";
                logger.warn("🔍 DIAGNOSTIC - Redirection sécurisée vers /services");
                if (sourcePage != null) {
                    logger.warn("🔍 DIAGNOSTIC - sourcePage rejetée car non valide: {}", sourcePage);
                }
            }

            // Rediriger vers la page validée
            return "redirect:" + redirectUrl;

        } catch (ResourceNotFoundException e) {
            logger.warn("Order not found in cancel callback: {}", orderId);
            redirectAttributes.addFlashAttribute("info",
                    "Paiement annulé. Vous pouvez réessayer à tout moment.");
            return "redirect:/services";

        } catch (Exception e) {
            logger.error("Error processing payment cancel callback - Order: {}, Session: {}: {}",
                    orderId, sessionId, e.getMessage(), e);
            auditLogger.error("Payment cancel callback error - Order: {}, Session: {}, Error: {}",
                    orderId, sessionId, e.getMessage());

            redirectAttributes.addFlashAttribute("info",
                    "Paiement annulé. Vous pouvez réessayer à tout moment.");
            return "redirect:/services";
        }
    }

    /**
     * 🔒 Valide qu'une URL est interne et sécurisée
     */
    private boolean isValidInternalUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 🔍 LOGS DE DIAGNOSTIC - Validation URL
        logger.warn("🔍 DIAGNOSTIC - Validation URL: '{}'", url);

        // Nettoyer l'URL
        String cleanUrl = url.trim();

        // Rejeter les URLs absolues externes
        if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            if (!cleanUrl.contains("lmp-services.ca") && !cleanUrl.contains("localhost")) {
                logger.warn("🔍 DIAGNOSTIC - URL externe rejetée: {}", cleanUrl);
                return false;
            }
        }

        // Autoriser seulement les URLs relatives valides
        if (cleanUrl.startsWith("/")) {
            // Listes des chemins autorisés
            String[] allowedPaths = { "/services", "/", "/contact", "/about", "/legal", "/terms", "/privacy" };

            for (String allowedPath : allowedPaths) {
                if (cleanUrl.equals(allowedPath) || cleanUrl.startsWith(allowedPath + "/")
                        || cleanUrl.startsWith(allowedPath + "?")) {
                    logger.warn("🔍 DIAGNOSTIC - URL autorisée: {}", cleanUrl);
                    return true;
                }
            }

            logger.warn("🔍 DIAGNOSTIC - URL relative non autorisée: {}", cleanUrl);
            return false;
        }

        // Rejeter tout le reste
        logger.warn("🔍 DIAGNOSTIC - URL format invalide: {}", cleanUrl);
        return false;
    }

    /**
     * API pour vérifier le statut d'une session de paiement
     */
    @GetMapping("/status/{sessionId}")
    @ResponseBody
    public ResponseEntity<?> getPaymentStatus(@PathVariable String sessionId) {

        logger.info("Checking payment status for session: {}", sessionId);

        try {
            // Vérifier le statut via le service
            String status = paymentService.checkTransactionStatus(
                    paymentTransactionRepository.findByTransactionId(sessionId)
                            .map(PaymentTransaction::getId)
                            .orElseThrow(() -> new ResourceNotFoundException("Transaction non trouvée")));

            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("status", status);
            response.put("checkedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error checking payment status for session {}: {}", sessionId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "STATUS_CHECK_FAILED",
                    "message", "Impossible de vérifier le statut du paiement"));
        }
    }

    /**
     * Récupère la page source depuis les métadonnées de la session Stripe
     */
    private String getSourcePageFromStripeSession(String sessionId) {
        try {
            // Configurer la clé API Stripe
            com.stripe.Stripe.apiKey = stripeSecretKey;

            // Récupérer la session depuis Stripe
            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.retrieve(sessionId);

            if (session.getMetadata() != null && session.getMetadata().containsKey("source_page")) {
                String sourcePage = session.getMetadata().get("source_page");
                logger.info("Retrieved source page from Stripe session {}: {}", sessionId, sourcePage);
                return sourcePage;
            }

            logger.info("No source_page metadata found in Stripe session: {}", sessionId);

        } catch (Exception e) {
            logger.warn("Unable to retrieve source page from Stripe session {}: {}", sessionId, e.getMessage());
        }

        return null; // Retourner null si pas trouvé
    }

    /**
     * Résout la langue de l'utilisateur depuis la requête HTTP.
     * Priorité : 1) payload JSON (userLanguage), 2) cookie googtrans, 3) défaut
     * "fr"
     * 
     * Le cookie googtrans de GTranslate a le format : /fr/en (source/cible)
     * Les 8 langues supportées : fr, en, es, de, it, nl, pt, lb
     *
     * @param request     la requête HTTP contenant les cookies
     * @param serviceData les données de service du payload (peut être null)
     * @return le code langue détecté
     */
    private String resolveUserLanguageFromRequest(HttpServletRequest request, Map<String, Object> serviceData) {
        Set<String> supportedLanguages = Set.of("fr", "en", "es", "de", "it", "nl", "pt", "lb");

        // 1) Vérifier d'abord le payload JSON (envoyé par le JS client)
        if (serviceData != null && serviceData.containsKey("userLanguage")) {
            String lang = String.valueOf(serviceData.get("userLanguage")).toLowerCase().trim();
            if (supportedLanguages.contains(lang)) {
                logger.info("STRIPE_LANG_RESOLVE - Language from payload: '{}'", lang);
                return lang;
            }
        }

        // 2) Essayer de lire le cookie googtrans (format : /fr/en)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("googtrans".equals(cookie.getName())) {
                    String value = cookie.getValue();
                    logger.info("STRIPE_LANG_RESOLVE - googtrans cookie found: '{}'", value);
                    if (value != null && value.startsWith("/")) {
                        String[] parts = value.split("/");
                        if (parts.length >= 3) {
                            String targetLang = parts[2].toLowerCase().trim();
                            if (supportedLanguages.contains(targetLang)) {
                                logger.info("STRIPE_LANG_RESOLVE - Language from googtrans cookie: '{}'", targetLang);
                                return targetLang;
                            }
                        }
                        if (parts.length == 2) {
                            String sourceLang = parts[1].toLowerCase().trim();
                            if (supportedLanguages.contains(sourceLang)) {
                                return sourceLang;
                            }
                        }
                    }
                    break;
                }
            }
        }

        // 3) Défaut : français
        logger.info("STRIPE_LANG_RESOLVE - No language detected, using default 'fr'");
        return "fr";
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
}
