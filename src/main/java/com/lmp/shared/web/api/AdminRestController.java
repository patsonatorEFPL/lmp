package com.lmp.shared.web.api;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.dto.AdminChangeUserPasswordRequest;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.service.AuthService;
import com.lmp.auth.service.SessionSecurityService;
import com.lmp.auth.service.UserService;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.domain.Refund;
import com.lmp.billing.dto.OrderResponse;
import com.lmp.billing.event.OrderRealtimeEventPublisher;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.billing.service.InvoicePdfService;
import com.lmp.crm.domain.Appointment;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.dto.AppointmentResponse;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import com.lmp.notification.service.EmailService;
import com.lmp.portal.dto.ChangePasswordRequest;
import com.lmp.integration.sync.SyncProperties;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.shared.dto.admin.RevenueSeriesDto;
import com.lmp.shared.dto.admin.TopServiceDto;
import com.lmp.shared.dto.admin.HealthServiceDto;
import com.lmp.shared.pricing.VatCalculationService;

import com.stripe.StripeClient;
import com.stripe.model.PaymentIntent;

import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * API REST d'administration (rôle ADMIN requis).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
@Tag(name = "Admin", description = "Endpoints d'administration (ADMIN only)")
public class AdminRestController {

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final SessionSecurityService sessionSecurityService;
    private final OrderRepository orderRepository;
    private final AppointmentRepository appointmentRepository;
    private final RefundRepository refundRepository;
    private final InvoicePdfService invoicePdfService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderRealtimeEventPublisher orderRealtimeEventPublisher;
    private final StripeClient stripeClient;
    private final VatCalculationService vatCalculationService;
    private final JdbcTemplate jdbcTemplate;
    private final SyncProperties syncProperties;

    public AdminRestController(UserService userService,
                               UserRepository userRepository,
                               AuthService authService,
                               SessionSecurityService sessionSecurityService,
                               OrderRepository orderRepository,
                               AppointmentRepository appointmentRepository,
                               RefundRepository refundRepository,
                               InvoicePdfService invoicePdfService,
                               EmailService emailService,
                               ApplicationEventPublisher eventPublisher,
                               OrderRealtimeEventPublisher orderRealtimeEventPublisher,
                               StripeClient stripeClient,
                               VatCalculationService vatCalculationService,
                               JdbcTemplate jdbcTemplate,
                               SyncProperties syncProperties) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.sessionSecurityService = sessionSecurityService;
        this.orderRepository = orderRepository;
        this.appointmentRepository = appointmentRepository;
        this.refundRepository = refundRepository;
        this.invoicePdfService = invoicePdfService;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
        this.orderRealtimeEventPublisher = orderRealtimeEventPublisher;
        this.stripeClient = stripeClient;
        this.vatCalculationService = vatCalculationService;
        this.jdbcTemplate = jdbcTemplate;
        this.syncProperties = syncProperties;
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques dashboard", description = "Retourne les KPIs principaux")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userService.count());
        stats.put("activeUsers", userService.countActiveUsers());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalAppointments", appointmentRepository.count());

        // Nouveaux utilisateurs ce mois
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        stats.put("newUsersThisMonth", userRepository.countByRegistrationDateBetween(startOfMonth, LocalDateTime.now()));

        // Rendez-vous aujourd'hui
        stats.put("appointmentsToday", appointmentRepository.countByAppointmentDate(LocalDateTime.now()));

        // MRR et répartition récurrent / ponctuel (30 derniers jours)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        String mrrSql = """
            SELECT
                CASE
                    WHEN so.duration_type = 'MONTHLY' THEN 'MONTHLY'
                    WHEN so.duration_type = 'YEARLY' THEN 'YEARLY'
                    ELSE 'ONE_TIME'
                END AS rev_type,
                SUM(o.total_amount) AS total
            FROM orders o
            LEFT JOIN services s ON LOWER(o.service_name) = LOWER(s.title)
            LEFT JOIN service_offers so ON so.service_id = s.id AND so.is_default = true
            WHERE o.created_at >= ?
              AND o.status NOT IN ('CANCELLED', 'REFUNDED', 'PAYMENT_PENDING')
            GROUP BY CASE
                    WHEN so.duration_type = 'MONTHLY' THEN 'MONTHLY'
                    WHEN so.duration_type = 'YEARLY' THEN 'YEARLY'
                    ELSE 'ONE_TIME'
                END
            """;
        BigDecimal mrr = BigDecimal.ZERO;
        BigDecimal recurringRevenue = BigDecimal.ZERO;
        BigDecimal oneTimeRevenue = BigDecimal.ZERO;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(mrrSql, thirtyDaysAgo);
            for (Map<String, Object> row : rows) {
                String type = (String) row.get("rev_type");
                BigDecimal total = (BigDecimal) row.get("total");
                if (total == null) continue;
                switch (type) {
                    case "MONTHLY" -> {
                        mrr = mrr.add(total);
                        recurringRevenue = recurringRevenue.add(total);
                    }
                    case "YEARLY" -> {
                        mrr = mrr.add(total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
                        recurringRevenue = recurringRevenue.add(total);
                    }
                    default -> oneTimeRevenue = oneTimeRevenue.add(total);
                }
            }
        } catch (Exception e) {
            // Non-blocking — si le schema diffère, on retourne 0
        }
        stats.put("mrr", mrr);
        stats.put("recurringRevenue30d", recurringRevenue);
        stats.put("oneTimeRevenue30d", oneTimeRevenue);

        // Statistiques par statut de commande
        var orderStatsByStatus = orderRepository.getOrderStatsByStatus();
        Map<String, Object> orderStats = new LinkedHashMap<>();
        for (Object[] row : orderStatsByStatus) {
            OrderStatus status = (OrderStatus) row[0];
            orderStats.put(status.name(), Map.of("count", row[1], "revenue", row[2]));
        }
        stats.put("ordersByStatus", orderStats);

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/users")
    @Operation(summary = "Lister les utilisateurs", description = "Retourne les utilisateurs avec pagination. "
            + "Si le paramètre search est renseigné, filtre par email (contient, insensible à la casse).")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        int safeSize = Math.clamp(size, 1, 500);
        PageRequest pageRequest = PageRequest.of(page, safeSize, Sort.by("registrationDate").descending());
        Page<User> users;

        if (search != null && !search.isBlank()) {
            users = userService.findByEmailContaining(search.trim(), pageRequest);
        } else if (status != null && !status.isEmpty()) {
            try {
                users = userService.findByStatus(UserStatus.valueOf(status), pageRequest);
            } catch (IllegalArgumentException e) {
                if (Sentry.isEnabled()) Sentry.captureException(e);
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status));
            }
        } else {
            users = userService.findAll(pageRequest);
        }

        Page<UserResponse> response = users.map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/orders")
    @Operation(summary = "Lister les commandes", description = "Retourne toutes les commandes avec pagination")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders;

        if (status != null && !status.isEmpty()) {
            try {
                orders = orderRepository.findByStatusOrderByCreatedAtDesc(
                        OrderStatus.valueOf(status), pageRequest)
                        .map(o -> OrderResponse.forAdmin(o, frontendUrl));
            } catch (IllegalArgumentException e) {
                if (Sentry.isEnabled()) Sentry.captureException(e);
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status));
            }
        } else {
            orders = orderRepository.findAll(pageRequest).map(o -> OrderResponse.forAdmin(o, frontendUrl));
        }

        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    // ========== User Management ==========

    @PostMapping("/users")
    @Transactional
    @Operation(summary = "Créer un utilisateur", description = "L'admin crée un utilisateur avec email et mot de passe")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody Map<String, Object> data,
            Authentication authentication) {
        String email = data.get("email") != null ? ((String) data.get("email")).trim().toLowerCase() : null;
        String firstName = data.get("firstName") != null ? ((String) data.get("firstName")).trim() : null;
        String lastName = data.get("lastName") != null ? ((String) data.get("lastName")).trim() : null;
        String password = data.get("password") != null ? (String) data.get("password") : null;
        boolean admin = Boolean.TRUE.equals(data.get("admin"));
        boolean staff = Boolean.TRUE.equals(data.get("staff"));

        // --- Validation AVANT toute opération transactionnelle ---
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("L'adresse email est obligatoire"));
        }
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Le mot de passe est obligatoire"));
        }
        if (!userService.isPasswordStrong(password)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux"));
        }
        if (userService.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Un utilisateur avec cet email existe déjà"));
        }
        if (authService.isDisposableEmail(email)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Adresse email jetable détectée. Même un administrateur ne peut pas créer de compte avec une adresse temporaire."));
        }

        try {
            RegisterDto registerDto = new RegisterDto();
            registerDto.setEmail(email);
            registerDto.setPassword(password);
            registerDto.setConfirmPassword(password);
            registerDto.setFirstName(firstName != null ? firstName : "");
            registerDto.setLastName(lastName != null ? lastName : "");
            registerDto.setAcceptTerms(true);

            User newUser = authService.registerUser(registerDto);

            User actor = null;
            if (admin || staff) {
                actor = userService.findByLogin(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Session administrateur invalide"));
            }
            if (admin) {
                userService.setUserAdminRole(newUser.getId(), true, actor.getId());
            }
            if (staff) {
                userService.setUserStaffRole(newUser.getId(), true, actor.getId());
            }

            // Publier l'événement de provisioning. Le routage Customer vs ERPNext User
            // est géré dans ErpEventListener selon les rôles du user (STAFF/ADMIN → User).
            Map<String, Object> regPl = new HashMap<>();
            regPl.put(BusinessEventPayloadKeys.EMAIL, newUser.getEmail());
            regPl.put("displayName", newUser.getDisplayName() != null ? newUser.getDisplayName() : newUser.getEmail());
            regPl.put("createdBy", "admin");
            eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.USER_REGISTERED, "admin", newUser.getId(), regPl));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Utilisateur créé : " + email, UserResponse.from(newUser)));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Modifier un utilisateur", description = "Met à jour le statut, verrouillage, email ou rôle administrateur (champ admin: true/false)")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable UUID id, @RequestBody Map<String, Object> data,
            Authentication authentication) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (data.containsKey("admin")) {
                boolean grantAdmin = Boolean.TRUE.equals(data.get("admin"));
                User actor = userService.findByLogin(authentication.getName())
                        .orElseThrow(() -> new RuntimeException("Session administrateur invalide"));
                userService.setUserAdminRole(id, grantAdmin, actor.getId());
            }

            if (data.containsKey("status")) {
                String status = (String) data.get("status");
                boolean active = "ACTIVE".equals(status);
                userService.setUserActive(id, active);
            }

            if (data.containsKey("locked")) {
                boolean locked = Boolean.TRUE.equals(data.get("locked"));
                userService.setUserLocked(id, locked);
            }

            // Admin email change: initiate verification flow
            if (data.containsKey("email")) {
                String newEmail = ((String) data.get("email")).trim().toLowerCase();
                if (!newEmail.equals(user.getEmail())) {
                    // Check if email already taken
                    Optional<User> existingUser = userService.findByEmail(newEmail);
                    if (existingUser.isPresent()) {
                        return ResponseEntity.badRequest().body(ApiResponse.error("Cet email est déjà utilisé par un autre compte"));
                    }

                    String oldEmail = user.getEmail();
                    // Generate a verification token
                    String token = UUID.randomUUID().toString();
                    user.setVerificationToken(token);
                    user.setEmailVerified(false);
                    user.setEmail(newEmail);
                    userService.save(user);

                    // Send notification to old email
                    try {
                        emailService.sendSimpleEmail(oldEmail,
                                "LMP — Changement d'email de votre compte",
                                "Bonjour,\n\nL'administrateur a modifié l'email de votre compte LMP.\n"
                                + "Ancien email : " + oldEmail + "\n"
                                + "Nouvel email : " + newEmail + "\n\n"
                                + "Si ce changement n'est pas de votre fait, veuillez contacter l'administrateur immédiatement.\n\n"
                                + "Cordialement,\nL'équipe LMP");
                    } catch (Exception e) {
                        // Non-blocking
                    }

                    // Send verification to new email
                    try {
                        String base = frontendUrl != null ? frontendUrl.replaceAll("/$", "") : "http://localhost:4200";
                        String verificationUrl = base + "/verify-email?token=" + token;

                        Map<String, Object> emailVars = new HashMap<>();
                        emailVars.put("userName", user.getDisplayName() != null ? user.getDisplayName() : newEmail);
                        emailVars.put("companyName", "LMP Services");
                        emailVars.put("verificationUrl", verificationUrl);

                        emailService.sendHtmlEmail(
                                newEmail,
                                "LMP — Vérifiez votre nouvel email",
                                "emails/email-verification",
                                emailVars);
                    } catch (Exception e) {
                        // Non-blocking
                    }
                }
            }

            return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Admin Password Change ==========

    @PutMapping("/change-password")
    @Transactional
    @Operation(summary = "Changer son propre mot de passe", description = "L'admin change son propre mot de passe (mot de passe actuel requis)")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (request.currentPassword() == null || request.currentPassword().isBlank()
                || request.newPassword() == null || request.newPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tous les champs sont obligatoires"));
        }

        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
        }

        if (!userService.isPasswordStrong(request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le mot de passe doit contenir au moins 8 caractères, "
                            + "incluant majuscules, minuscules, chiffres et caractères spéciaux"));
        }

        User admin = userService.findByLogin(authentication.getName())
                .orElse(null);
        if (admin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Administrateur non trouvé"));
        }

        if (!userService.checkCurrentPassword(admin.getId(), request.currentPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le mot de passe actuel est incorrect"));
        }

        if (userService.checkCurrentPassword(admin.getId(), request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le nouveau mot de passe doit être différent du mot de passe actuel"));
        }

        userService.changePassword(admin.getId(), request.newPassword());

        return ResponseEntity.ok(ApiResponse.ok("Mot de passe administrateur modifié avec succès", null));
    }

    @PutMapping("/users/{id}/change-password")
    @Transactional
    @Operation(summary = "Changer le mot de passe d'un utilisateur",
            description = "L'admin change le mot de passe d'un utilisateur sans connaître l'ancien")
    public ResponseEntity<ApiResponse<Void>> changeUserPassword(
            @PathVariable UUID id,
            @RequestBody AdminChangeUserPasswordRequest request,
            Authentication authentication) {

        if (request.newPassword() == null || request.newPassword().isBlank()
                || request.confirmPassword() == null || request.confirmPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tous les champs sont obligatoires"));
        }

        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
        }

        if (!userService.isPasswordStrong(request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le mot de passe doit contenir au moins 8 caractères, "
                            + "incluant majuscules, minuscules, chiffres et caractères spéciaux"));
        }

        User admin = userService.findByLogin(authentication.getName())
                .orElse(null);
        if (admin == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Administrateur non trouvé"));
        }

        User targetUser = userService.findById(id).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Utilisateur non trouvé"));
        }

        if (userService.checkCurrentPassword(targetUser.getId(), request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le nouveau mot de passe est identique au mot de passe actuel de l'utilisateur"));
        }

        userService.changePasswordByAdmin(id, request.newPassword(), admin.getId());
        sessionSecurityService.invalidateAllUserSessions(targetUser);

        return ResponseEntity.ok(ApiResponse.ok(
                "Mot de passe de " + targetUser.getEmail() + " modifié avec succès", null));
    }

    // ========== Order Management ==========

    @GetMapping("/orders/{id}")
    @Operation(summary = "Détail d'une commande", description = "Retourne le détail complet d'une commande")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetail(@PathVariable UUID id) {
        return orderRepository.findById(id)
                .map(order -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("id", order.getId());
                    detail.put("serviceName", order.getServiceName());
                    detail.put("totalAmount", order.getTotalAmount());
                    detail.put("currency", order.getCurrency());
                    detail.put("status", order.getStatus() != null ? order.getStatus().name() : null);
                    detail.put("paymentStatus", order.getPaymentStatus());
                    detail.put("paymentMethod", order.getPaymentMethod());
                    detail.put("createdAt", order.getCreatedAt());
                    detail.put("paidAt", order.getPaidAt());
                    detail.put("shippedAt", order.getShippedAt());
                    detail.put("deliveredAt", order.getDeliveredAt());
                    detail.put("cancelledAt", order.getCancelledAt());
                    detail.put("notes", order.getNotes());
                    detail.put("adminNotes", order.getAdminNotes());
                    detail.put("processingNotes", order.getProcessingNotes());
                    detail.put("progressPercentage", order.getProgressPercentage());
                    detail.put("progressStatus", order.getProgressStatus());
                    detail.put("priority", order.getPriority());
                    detail.put("stripeSessionId", order.getStripeSessionId());
                    detail.put("stripePaymentIntentId", order.getStripePaymentIntentId());
                    detail.put("cancellationReason", order.getCancellationReason());

                    // Adresse de facturation
                    detail.put("amountBaseEur", order.getAmountBaseEur());
                    detail.put("appliedVatRate", order.getAppliedVatRate());
                    // Montant TTC EUR calculé (pour affichage admin cohérent)
                    BigDecimal totalAmountEur;
                    if (order.getCurrency() == null || "EUR".equalsIgnoreCase(order.getCurrency())) {
                        totalAmountEur = order.getTotalAmount();
                    } else if (order.getAmountBaseEur() != null) {
                        if (Boolean.TRUE.equals(order.getVatReverseCharge())) {
                            totalAmountEur = order.getAmountBaseEur();
                        } else {
                            // Utiliser le taux snapshoté, fallback 0.20 pour les anciennes commandes
                            BigDecimal vatRate = order.getAppliedVatRate() != null
                                    ? order.getAppliedVatRate() : new BigDecimal("0.20");
                            totalAmountEur = order.getAmountBaseEur()
                                    .multiply(BigDecimal.ONE.add(vatRate))
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                        }
                    } else {
                        totalAmountEur = order.getTotalAmount();
                    }
                    detail.put("totalAmountEur", totalAmountEur);
                    detail.put("billingAddress", order.getBillingAddress());
                    detail.put("billingCity", order.getBillingCity());
                    detail.put("billingPostalCode", order.getBillingPostalCode());
                    detail.put("billingCountry", order.getBillingCountry());

                    if (order.getUser() != null) {
                        detail.put("userEmail", order.getUser().getEmail());
                        detail.put("userName", order.getUser().getDisplayName());
                        detail.put("userId", order.getUser().getId());
                    }

                    detail.put("guestPaymentLink", OrderResponse.computeGuestPaymentLink(order, frontendUrl));

                    // Anti-fraude
                    detail.put("billingName", order.getBillingName());
                    detail.put("customerVatNumber", order.getCustomerVatNumber());
                    detail.put("vatCompanyName", order.getVatCompanyName());
                    detail.put("vatReverseCharge", order.getVatReverseCharge());
                    detail.put("ipCountry", order.getIpCountry());
                    detail.put("ipAddress", order.getIpAddress());
                    detail.put("vpnScore", order.getVpnScore());
                    detail.put("vpnSources", order.getVpnSources());
                    detail.put("browserTimezone", order.getBrowserTimezone());
                    detail.put("geoCountry", order.getGeoCountry());
                    detail.put("cardCountry", order.getCardCountry());
                    detail.put("fraudScore", order.getFraudScore());
                    detail.put("fraudFlags", order.getFraudFlags());

                    return ResponseEntity.ok(ApiResponse.ok(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/orders/{id}")
    @Transactional
    @Operation(summary = "Modifier une commande", description = "Met à jour le statut, la progression et les notes d'une commande")
    public ResponseEntity<ApiResponse<Void>> updateOrder(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
            OrderStatus previousStatus = order.getStatus();

            if (data.containsKey("status")) {
                order.setStatus(OrderStatus.valueOf((String) data.get("status")));
            }
            if (data.containsKey("progressPercentage")) {
                order.setProgressPercentage(((Number) data.get("progressPercentage")).intValue());
            }
            if (data.containsKey("progressStatus")) {
                order.setProgressStatus((String) data.get("progressStatus"));
            }
            if (data.containsKey("status")) {
                OrderProgressSync.applyMinimumForStatus(order);
            }
            if (data.containsKey("adminNotes")) {
                order.setAdminNotes((String) data.get("adminNotes"));
            }
            if (data.containsKey("processingNotes")) {
                order.setProcessingNotes((String) data.get("processingNotes"));
            }
            if (data.containsKey("priority")) {
                order.setPriority(((Number) data.get("priority")).intValue());
            }

            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now());
            orderRepository.save(order);

            if (data.containsKey("status") && previousStatus != order.getStatus()) {
                orderRealtimeEventPublisher.publishOrderUpdated(order, previousStatus, order.getStatus());
            }

            return ResponseEntity.ok(ApiResponse.ok("Commande mise à jour", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/orders/{id}")
    @Transactional
    @Operation(summary = "Supprimer une commande",
            description = "Supprime une commande non payée en attente de paiement ou annulée, sans remboursement enregistré.")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable UUID id) {
        Optional<Order> opt = orderRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Order order = opt.get();
        if (order.getPaidAt() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Impossible de supprimer une commande marquée comme payée."));
        }
        OrderStatus st = order.getStatus();
        if (st != OrderStatus.PAYMENT_PENDING && st != OrderStatus.CANCELLED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Seules les commandes en attente de paiement ou annulées peuvent être supprimées."));
        }
        if (refundRepository.existsByOrderId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Impossible de supprimer une commande qui a des enregistrements de remboursement."));
        }
        orderRepository.delete(order);
        return ResponseEntity.ok(ApiResponse.ok("Commande supprimée", null));
    }

    // ========== Appointment Management ==========

    @GetMapping("/appointments")
    @Operation(summary = "Lister les rendez-vous", description = "Retourne tous les rendez-vous avec pagination et filtrage")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> getAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("appointmentDate").descending());
        Page<Appointment> appointments;

        if (status != null && !status.isEmpty()) {
            try {
                appointments = appointmentRepository.findByStatusOrderByAppointmentDateAsc(
                        AppointmentStatus.valueOf(status), pageRequest);
            } catch (IllegalArgumentException e) {
                if (Sentry.isEnabled()) Sentry.captureException(e);
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status));
            }
        } else {
            appointments = appointmentRepository.findAll(pageRequest);
        }

        Page<AppointmentResponse> response = appointments.map(AppointmentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Détail d'un rendez-vous")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppointmentDetail(@PathVariable UUID id) {
        return appointmentRepository.findById(id)
                .map(appt -> {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("id", appt.getId());
                    detail.put("clientName", appt.getEffectiveClientName());
                    detail.put("clientEmail", appt.getEffectiveClientEmail());
                    detail.put("clientPhone", appt.getEffectiveClientPhone());
                    detail.put("appointmentDate", appt.getAppointmentDate());
                    detail.put("subject", appt.getSubject());
                    detail.put("description", appt.getDescription());
                    detail.put("status", appt.getStatus() != null ? appt.getStatus().name() : null);
                    detail.put("adminNotes", appt.getAdminNotes());
                    detail.put("durationMinutes", appt.getDurationMinutes());
                    detail.put("priority", appt.getPriority());
                    detail.put("createdAt", appt.getCreatedAt());
                    detail.put("confirmedAt", appt.getConfirmedAt());
                    detail.put("cancelledAt", appt.getCancelledAt());
                    detail.put("cancellationReason", appt.getCancellationReason());
                    detail.put("isAnonymous", appt.isAnonymous());
                    if (appt.getUser() != null) {
                        detail.put("userId", appt.getUser().getId());
                        detail.put("userName", appt.getUser().getDisplayName());
                        detail.put("userEmail", appt.getUser().getEmail());
                    }
                    return ResponseEntity.ok(ApiResponse.ok(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/appointments/{id}")
    @Transactional
    @Operation(summary = "Modifier un rendez-vous", description = "Met à jour le statut, notes admin, etc.")
    public ResponseEntity<ApiResponse<Void>> updateAppointment(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            Appointment appt = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé"));

            if (data.containsKey("status")) {
                String newStatus = (String) data.get("status");
                AppointmentStatus targetStatus = AppointmentStatus.valueOf(newStatus);
                appt.setStatus(targetStatus);
            }
            if (data.containsKey("adminNotes")) {
                appt.setAdminNotes((String) data.get("adminNotes"));
            }
            if (data.containsKey("priority")) {
                appt.setPriority(((Number) data.get("priority")).intValue());
            }
            if (data.containsKey("cancellationReason")) {
                appt.setCancellationReason((String) data.get("cancellationReason"));
            }

            appointmentRepository.save(appt);
            return ResponseEntity.ok(ApiResponse.ok("Rendez-vous mis à jour", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/appointments/{id}")
    @Transactional
    @Operation(summary = "Supprimer un rendez-vous")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable UUID id) {
        try {
            appointmentRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Rendez-vous supprimé", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Admin Create Order for User ==========

    @PostMapping("/orders")
    @Transactional
    @Operation(summary = "Créer une commande pour un utilisateur", description = "L'admin crée une commande en attente de paiement pour un utilisateur donné")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderForUser(@RequestBody Map<String, Object> data) {
        try {
            String userId = (String) data.get("userId");
            String serviceName = (String) data.get("serviceName");
            double amount = ((Number) data.get("amount")).doubleValue();
            String currency = data.containsKey("currency") ? (String) data.get("currency") : "EUR";
            String notes = data.containsKey("notes") ? (String) data.get("notes") : null;

            User user = userService.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userId));

            // Le montant admin est HT — appliquer la TVA selon le statut du client
            BigDecimal amountHt = BigDecimal.valueOf(amount);
            boolean reverseCharge = Boolean.TRUE.equals(user.getVatReverseCharge());
            // Pays du client pour le taux TVA (fallback FR si non renseigné)
            String countryCode = user.getCountry() != null ? user.getCountry() : "FR";
            BigDecimal amountCharged = vatCalculationService.applyVat(amountHt, reverseCharge, countryCode);

            Order order = new Order();
            order.setUser(user);
            order.setServiceName(serviceName);
            order.setTotalAmount(amountCharged);
            order.setCurrency(currency);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setNotes(notes);
            order.setAdminNotes("[Créée par admin]");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setPriority(1); // High priority — admin-created
            order.setVatReverseCharge(reverseCharge);
            order.setAppliedVatRate(reverseCharge ? BigDecimal.ZERO : vatCalculationService.getVatRate(countryCode));
            String vat = com.lmp.shared.util.VatIdentifierUtils.normalize(user.getVatNumber());
            order.setCustomerVatNumber(reverseCharge && !vat.isEmpty() ? vat : null);

            OrderProgressSync.applyMinimumForStatus(order);

            order = orderRepository.save(order);

            // Send notification email to user about the new order
            try {
                Map<String, Object> emailVars = new HashMap<>();
                emailVars.put("customerName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                emailVars.put("order", order);
                emailVars.put("companyName", "LMP Digital Services");
                emailVars.put("frontendUrl", frontendUrl);
                emailVars.put("currentDate", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));

                emailService.sendHtmlEmail(
                        user.getEmail(),
                        "LMP — Nouvelle commande créée pour votre compte",
                        "emails/order-confirmation",
                        emailVars);
            } catch (Exception emailEx) {
                // Non-blocking — order is already created
            }

            try {
                Map<String, Object> pl = new HashMap<>();
                pl.put(BusinessEventPayloadKeys.USER_ID, user.getId().toString());
                pl.put(BusinessEventPayloadKeys.ORDER_ID, order.getId().toString());
                pl.put(BusinessEventPayloadKeys.SERVICE_NAME, serviceName);
                pl.put(BusinessEventPayloadKeys.AMOUNT, amount);
                pl.put(BusinessEventPayloadKeys.CUSTOMER_EMAIL, user.getEmail());
                pl.put(BusinessEventPayloadKeys.CUSTOMER_NAME,
                        user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                String who = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
                pl.put(BusinessEventPayloadKeys.MESSAGE, "Nouvelle commande — " + who);
                pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.TRUE);
                pl.put(BusinessEventPayloadKeys.IN_APP_NOTIFICATION_TYPE, "NEW_PENDING_ORDER");
                pl.put(BusinessEventPayloadKeys.USER_IN_APP_MESSAGE, String.format(
                        "Nouvelle commande en attente : %s (%.2f€)",
                        serviceName != null ? serviceName : "", amount));
                eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.ORDER_CREATED, "admin", order.getId(), pl));
            } catch (Exception ignored) {
                // Non-blocking
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Commande créée pour " + user.getEmail(), OrderResponse.from(order)));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/orders/guest")
    @Transactional
    @Operation(summary = "Créer une commande invité",
            description = "Commande sans utilisateur ; un lien avec token permet au client de s'inscrire puis de payer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createGuestOrder(@RequestBody Map<String, Object> data) {
        try {
            String serviceName = (String) data.get("serviceName");
            double amount = ((Number) data.get("amount")).doubleValue();
            String currency = data.containsKey("currency") ? (String) data.get("currency") : "EUR";
            String notes = data.containsKey("notes") ? (String) data.get("notes") : null;
            String guestEmail = data.containsKey("guestEmail") ? (String) data.get("guestEmail") : null;

            if (serviceName == null || serviceName.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("serviceName requis"));
            }

            Order order = new Order();
            order.setUser(null);
            order.setServiceName(serviceName.trim());
            order.setTotalAmount(BigDecimal.valueOf(amount));
            order.setCurrency(currency);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            StringBuilder noteBuilder = new StringBuilder();
            if (guestEmail != null && !guestEmail.isBlank()) {
                noteBuilder.append("[Contact invité] ").append(guestEmail.trim());
            }
            if (notes != null && !notes.isBlank()) {
                if (!noteBuilder.isEmpty()) {
                    noteBuilder.append("\n");
                }
                noteBuilder.append(notes);
            }
            order.setNotes(!noteBuilder.isEmpty() ? noteBuilder.toString() : null);
            order.setAdminNotes("[Créée par admin — lien invité]");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setLastModifiedAt(LocalDateTime.now());
            order.setPriority(1);
            order.setCheckoutToken(UUID.randomUUID().toString());

            OrderProgressSync.applyMinimumForStatus(order);

            order = orderRepository.save(order);

            String base = frontendUrl != null ? frontendUrl.replaceAll("/$", "") : "http://localhost:4200";
            String paymentLink = base + "/payment/guest?t=" + order.getCheckoutToken();

            String guestEmailTrimmed = guestEmail != null ? guestEmail.trim() : "";
            boolean guestEmailSent = false;
            boolean guestEmailInvalid = false;
            boolean guestEmailSendFailed = false;
            if (!guestEmailTrimmed.isEmpty()) {
                if (isPlausibleEmailAddress(guestEmailTrimmed)) {
                    try {
                        sendGuestCheckoutInvitationEmail(guestEmailTrimmed, order, paymentLink);
                        guestEmailSent = true;
                    } catch (Exception emailEx) {
                        guestEmailSendFailed = true;
                    }
                } else {
                    guestEmailInvalid = true;
                }
            }

            try {
                Map<String, Object> pl = new HashMap<>();
                pl.put(BusinessEventPayloadKeys.ORDER_ID, order.getId().toString());
                pl.put(BusinessEventPayloadKeys.SERVICE_NAME, serviceName);
                pl.put(BusinessEventPayloadKeys.AMOUNT, amount);
                pl.put(BusinessEventPayloadKeys.MESSAGE, "Commande invité — lien à transmettre au client");
                pl.put(BusinessEventPayloadKeys.NOTIFY_USER, Boolean.FALSE);
                eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.ORDER_CREATED, "admin", order.getId(), pl));
            } catch (Exception ignored) {
                // Non-blocking
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("order", OrderResponse.forAdmin(order, frontendUrl));
            payload.put("checkoutToken", order.getCheckoutToken());
            payload.put("paymentLink", paymentLink);
            payload.put("guestEmailSent", guestEmailSent);
            payload.put("guestEmailInvalid", guestEmailInvalid);
            payload.put("guestEmailSendFailed", guestEmailSendFailed);

            String message;
            if (guestEmailSent) {
                message = "Commande invité créée — un e-mail avec le lien a été envoyé au client.";
            } else if (guestEmailInvalid) {
                message = "Commande invité créée — l’adresse e-mail du client est invalide ; transmettez le lien manuellement.";
            } else if (guestEmailSendFailed) {
                message = "Commande invité créée — l’e-mail n’a pas pu être envoyé ; transmettez le lien manuellement.";
            } else {
                message = "Commande invité créée — transmettez le lien au client";
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(message, payload));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== User Soft Delete / Hard Delete ==========

    @PutMapping("/users/{id}/soft-delete")
    @Transactional
    @Operation(summary = "Soft delete", description = "Désactive un utilisateur (DELETED status)")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(@PathVariable UUID id) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            user.setStatus(UserStatus.DELETED);
            userService.save(user);
            return ResponseEntity.ok(ApiResponse.ok("Utilisateur désactivé (soft delete)", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Hard delete", description = "Supprime définitivement un utilisateur et anonymise ses données")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable UUID id) {
        try {
            userService.hardDeleteUser(id);
            return ResponseEntity.ok(ApiResponse.ok("Utilisateur supprimé définitivement", null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Invoice Download (API for Angular) ==========

    @GetMapping("/orders/{orderId}/invoice")
    @Operation(summary = "Télécharger la facture PDF", description = "Génère et retourne la facture PDF pour une commande (minimum confirmée)")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

            // Only allow invoice download for orders that have been at least confirmed
            if (!isInvoiceEligible(order.getStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            User user = order.getUser();

            byte[] pdf = invoicePdfService.generateInvoicePdf(order, user);
            String invoiceNumber = invoicePdfService.generateInvoiceNumber(order);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Facture_" + invoiceNumber + ".pdf");
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isInvoiceEligible(OrderStatus status) {
        if (status == null) return false;
        return switch (status) {
            case CONFIRMED, PROCESSING, IN_PROGRESS, SHIPPED, DELIVERED, COMPLETED, REFUNDED -> true;
            default -> false;
        };
    }

    // ========== Order Refunds (read) ==========

    // ========== Stripe Sync ==========

    @PostMapping("/orders/{orderId}/stripe-sync")
    @Transactional
    @Operation(summary = "Synchroniser avec Stripe", description = "Récupère le statut actuel du paiement depuis Stripe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncWithStripe(@PathVariable UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

            Map<String, Object> syncResult = new LinkedHashMap<>();
            syncResult.put("orderId", order.getId());
            syncResult.put("currentStatus", order.getStatus() != null ? order.getStatus().name() : null);
            syncResult.put("currentPaymentStatus", order.getPaymentStatus());

            if (order.getStripePaymentIntentId() != null && !order.getStripePaymentIntentId().isEmpty()) {
                OrderStatus statusBeforeSync = order.getStatus();
                PaymentIntent pi = stripeClient.paymentIntents().retrieve(order.getStripePaymentIntentId());

                syncResult.put("stripeStatus", pi.getStatus());
                syncResult.put("stripeAmount", pi.getAmount());
                syncResult.put("stripeCurrency", pi.getCurrency());

                // Update local payment status from Stripe
                String stripeStatus = pi.getStatus();
                if ("succeeded".equals(stripeStatus)) {
                    order.setPaymentStatus("succeeded");
                    if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                        order.setStatus(OrderStatus.CONFIRMED);
                        order.setPaidAt(LocalDateTime.now());
                    }
                    OrderProgressSync.applyMinimumForStatus(order);
                } else if ("canceled".equals(stripeStatus)) {
                    order.setPaymentStatus("cancelled");
                } else if ("requires_payment_method".equals(stripeStatus)) {
                    order.setPaymentStatus("requires_payment_method");
                } else {
                    order.setPaymentStatus(stripeStatus);
                }

                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                if (statusBeforeSync != order.getStatus()) {
                    orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, statusBeforeSync,
                            order.getStatus());
                }

                syncResult.put("updatedStatus", order.getStatus() != null ? order.getStatus().name() : null);
                syncResult.put("updatedPaymentStatus", order.getPaymentStatus());
                syncResult.put("synced", true);
            } else if (order.getStripeSessionId() != null && !order.getStripeSessionId().isEmpty()) {
                OrderStatus statusBeforeSync = order.getStatus();
                com.stripe.model.checkout.Session session = stripeClient.checkout().sessions().retrieve(order.getStripeSessionId());

                syncResult.put("stripePaymentStatus", session.getPaymentStatus());
                syncResult.put("stripeSessionStatus", session.getStatus());

                if ("paid".equals(session.getPaymentStatus())) {
                    order.setPaymentStatus("succeeded");
                    if (session.getPaymentIntent() != null) {
                        order.setStripePaymentIntentId(session.getPaymentIntent());
                    }
                    if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                        order.setStatus(OrderStatus.CONFIRMED);
                        order.setPaidAt(LocalDateTime.now());
                    }
                    OrderProgressSync.applyMinimumForStatus(order);
                } else if ("expired".equals(session.getStatus())) {
                    order.setPaymentStatus("expired");
                    if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                        order.setStatus(OrderStatus.CANCELLED);
                        order.setCancelledAt(LocalDateTime.now());
                        order.setCancellationReason("Session Stripe expirée");
                        OrderProgressSync.applyMinimumForStatus(order);
                    }
                }

                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);

                if (statusBeforeSync != order.getStatus()) {
                    orderRealtimeEventPublisher.publishAutomatedStripeFlowTransition(order, statusBeforeSync,
                            order.getStatus());
                }

                syncResult.put("updatedStatus", order.getStatus() != null ? order.getStatus().name() : null);
                syncResult.put("updatedPaymentStatus", order.getPaymentStatus());
                syncResult.put("synced", true);
            } else {
                syncResult.put("synced", false);
                syncResult.put("message", "Aucun identifiant Stripe associé à cette commande");
            }

            return ResponseEntity.ok(ApiResponse.ok(syncResult));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Erreur Stripe: " + e.getMessage()));
        }
    }

    // ========== Admin Own Email Change ==========

    @PostMapping("/change-email")
    @Transactional
    @Operation(summary = "Changer l'email admin", description = "L'admin change son propre email. Un email de confirmation est envoyé à l'ancien email.")
    public ResponseEntity<ApiResponse<Void>> changeAdminEmail(@RequestBody Map<String, String> data, Authentication authentication) {
        try {
            String newEmail = data.get("newEmail");
            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Le nouvel email est requis"));
            }
            newEmail = newEmail.trim().toLowerCase();

            // Get current admin user
            String currentEmail = authentication.getName();
            User admin = userService.findByEmail(currentEmail)
                    .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

            if (newEmail.equals(admin.getEmail())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Le nouvel email est identique à l'ancien"));
            }

            // Check if new email is available
            if (userService.findByEmail(newEmail).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Cet email est déjà utilisé"));
            }

            // Generate confirmation token
            String token = UUID.randomUUID().toString();
            admin.setVerificationToken(token);
            // Store new email temporarily in resetToken field (reuse for this purpose)
            admin.setResetToken(newEmail);
            admin.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
            userService.save(admin);

            // Send confirmation email to CURRENT email
            try {
                emailService.sendSimpleEmail(currentEmail,
                        "LMP — Confirmer le changement d'email",
                        "Bonjour,\n\n"
                        + "Vous avez demandé à changer votre email de :\n"
                        + currentEmail + "\n"
                        + "vers :\n"
                        + newEmail + "\n\n"
                        + "Pour confirmer ce changement, cliquez sur le lien ci-dessous :\n"
                        + "Ce changement sera effectif après confirmation.\n\n"
                        + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n"
                        + "Cordialement,\nL'équipe LMP");
            } catch (Exception e) {
                // Non-blocking
            }

            return ResponseEntity.ok(ApiResponse.ok("Un email de confirmation a été envoyé à " + currentEmail, null));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/orders/{orderId}/refunds")
    @Operation(summary = "Remboursements d'une commande", description = "Retourne la liste des remboursements liés à une commande")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrderRefunds(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        List<Refund> refunds = refundRepository.findByOrderOrderByCreatedAtDesc(order);
        List<Map<String, Object>> refundList = refunds.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("amount", r.getAmount());
            map.put("currency", r.getCurrency());
            map.put("status", r.getStatus());
            map.put("reason", r.getReason());
            map.put("stripeRefundId", r.getStripeRefundId());
            map.put("createdAt", r.getCreatedAt());
            map.put("processedAt", r.getProcessedAt());
            map.put("failureReason", r.getFailureReason());
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.ok(refundList));
    }

    // ========== Dashboard Analytics ==========

    @GetMapping("/revenue-series")
    @Operation(summary = "Série de revenus", description = "Retourne les données pour le line chart de revenus (week, month, quarter)")
    public ResponseEntity<ApiResponse<RevenueSeriesDto>> getRevenueSeries(@RequestParam String period) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Number> current = new ArrayList<>();
            List<Number> previous = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);
            LocalDateTime startDate = now.minusYears(2).minusMonths(3);
            List<Order> orders = orderRepository.findRecentOrdersExcludingStatuses(startDate, excluded);

            switch (period) {
                case "week" -> {
                    LocalDate today = LocalDate.now();
                    WeekFields wf = WeekFields.ISO;
                    for (int i = 15; i >= 0; i--) {
                        LocalDate weekDate = today.minusWeeks(i);
                        int week = weekDate.get(wf.weekOfWeekBasedYear());
                        int year = weekDate.get(wf.weekBasedYear());
                        int prevYear = year - 1;

                        labels.add("S" + week);
                        current.add(sumRevenueForWeek(orders, year, week, wf));
                        previous.add(sumRevenueForWeek(orders, prevYear, week, wf));
                    }
                }
                case "month" -> {
                    LocalDate today = LocalDate.now();
                    for (int i = 11; i >= 0; i--) {
                        LocalDate monthDate = today.minusMonths(i);
                        int month = monthDate.getMonthValue();
                        int year = monthDate.getYear();
                        int prevYear = year - 1;

                        labels.add(monthDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH));
                        current.add(sumRevenueForMonth(orders, year, month));
                        previous.add(sumRevenueForMonth(orders, prevYear, month));
                    }
                }
                case "quarter" -> {
                    LocalDate today = LocalDate.now();
                    for (int i = 3; i >= 0; i--) {
                        LocalDate quarterDate = today.minusMonths(i * 3L);
                        int quarter = (quarterDate.getMonthValue() - 1) / 3 + 1;
                        int year = quarterDate.getYear();
                        int prevYear = year - 1;

                        labels.add("T" + quarter);
                        current.add(sumRevenueForQuarter(orders, year, quarter));
                        previous.add(sumRevenueForQuarter(orders, prevYear, quarter));
                    }
                }
                default -> {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid period: " + period));
                }
            }

            return ResponseEntity.ok(ApiResponse.ok(new RevenueSeriesDto(current, previous, labels)));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private BigDecimal sumRevenueForWeek(List<Order> orders, int year, int week, WeekFields wf) {
        return orders.stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return d.get(wf.weekBasedYear()) == year && d.get(wf.weekOfWeekBasedYear()) == week;
                })
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRevenueForMonth(List<Order> orders, int year, int month) {
        return orders.stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return d.getYear() == year && d.getMonthValue() == month;
                })
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumRevenueForQuarter(List<Order> orders, int year, int quarter) {
        return orders.stream()
                .filter(o -> {
                    LocalDate d = o.getCreatedAt().toLocalDate();
                    return d.getYear() == year && (d.getMonthValue() - 1) / 3 + 1 == quarter;
                })
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/top-services")
    @Operation(summary = "Top 5 services", description = "Retourne les top 5 services par chiffre d'affaires réel sur les 90 derniers jours")
    public ResponseEntity<ApiResponse<List<TopServiceDto>>> getTopServices() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startCurrent = now.minusDays(90);
            LocalDateTime startPrevious = startCurrent.minusDays(90);
            List<OrderStatus> excluded = List.of(OrderStatus.CANCELLED, OrderStatus.REFUNDED);
            PageRequest top5 = PageRequest.ofSize(5);

            List<Object[]> currentRows = orderRepository.getTopServicesByRevenue(startCurrent, excluded, top5);
            List<Object[]> previousRows = orderRepository.getTopServicesByRevenue(startPrevious, excluded, top5);

            Map<String, BigDecimal> previousRevenueMap = new HashMap<>();
            for (Object[] row : previousRows) {
                String name = (String) row[0];
                BigDecimal revenue = (BigDecimal) row[2];
                previousRevenueMap.put(name, revenue != null ? revenue : BigDecimal.ZERO);
            }

            List<TopServiceDto> result = new ArrayList<>();
            for (Object[] row : currentRows) {
                String name = (String) row[0];
                long orders = ((Number) row[1]).longValue();
                BigDecimal revenue = (BigDecimal) row[2];
                if (revenue == null) revenue = BigDecimal.ZERO;

                BigDecimal prevRevenue = previousRevenueMap.getOrDefault(name, BigDecimal.ZERO);
                int growthPercent = 0;
                if (prevRevenue.compareTo(BigDecimal.ZERO) > 0) {
                    growthPercent = revenue.subtract(prevRevenue)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(prevRevenue, 0, RoundingMode.HALF_UP)
                            .intValue();
                }
                result.add(new TopServiceDto(name, orders, revenue, growthPercent));
            }

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/health/services")
    @Operation(summary = "Santé des services", description = "Retourne un snapshot de santé des services (DB, Stripe, ERPNext)")
    public ResponseEntity<ApiResponse<List<HealthServiceDto>>> getServicesHealth() {
        try {
            List<HealthServiceDto> healthList = new ArrayList<>();

            // DB
            long dbStart = System.currentTimeMillis();
            String dbStatus;
            String dbLatency;
            String dbTone;
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                long dbMs = System.currentTimeMillis() - dbStart;
                dbLatency = dbMs + " ms";
                if (dbMs < 100) {
                    dbStatus = "UP";
                    dbTone = "is-ok";
                } else {
                    dbStatus = "DEGRADED";
                    dbTone = "is-warn";
                }
            } catch (Exception e) {
                dbStatus = "DOWN";
                dbLatency = "-";
                dbTone = "is-danger";
            }
            healthList.add(new HealthServiceDto("Base de données", dbStatus, dbLatency, "100 %", dbTone));

            // Stripe
            String stripeStatus = stripeClient != null ? "UP" : "DOWN";
            String stripeTone = stripeClient != null ? "is-ok" : "is-danger";
            healthList.add(new HealthServiceDto("Stripe", stripeStatus, "-", "100 %", stripeTone));

            // ERPNext
            String erpUrl = syncProperties.getExternal() != null ? syncProperties.getExternal().getBaseUrl() : null;
            boolean erpConfigured = erpUrl != null && !erpUrl.isBlank();
            String erpStatus = erpConfigured ? "UP" : "DOWN";
            String erpTone = erpConfigured ? "is-ok" : "is-danger";
            healthList.add(new HealthServiceDto("ERPNext", erpStatus, "-", "100 %", erpTone));

            return ResponseEntity.ok(ApiResponse.ok(healthList));
        } catch (Exception e) {
            if (Sentry.isEnabled()) Sentry.captureException(e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private static boolean isPlausibleEmailAddress(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        try {
            InternetAddress addr = new InternetAddress(email.trim());
            addr.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    private void sendGuestCheckoutInvitationEmail(String to, Order order, String paymentLink) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String currency = order.getCurrency() != null ? order.getCurrency() : "EUR";
        String formattedAmount = nf.format(order.getTotalAmount()) + " " + currency;

        Map<String, Object> vars = new HashMap<>();
        vars.put("companyName", "LMP Digital Services");
        vars.put("serviceName", order.getServiceName());
        vars.put("formattedAmount", formattedAmount);
        vars.put("paymentLink", paymentLink);
        vars.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));

        emailService.sendHtmlEmail(
                to,
                "LMP — Finalisez votre commande (lien sécurisé)",
                "emails/guest-checkout-invitation",
                vars);
    }
}
