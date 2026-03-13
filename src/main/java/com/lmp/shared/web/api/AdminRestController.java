package com.lmp.shared.web.api;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.domain.Refund;
import com.lmp.billing.dto.OrderResponse;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.billing.service.InvoicePdfService;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.service.UserService;
import com.lmp.crm.domain.Appointment;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.dto.AppointmentResponse;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final AppointmentRepository appointmentRepository;
    private final RefundRepository refundRepository;
    private final InvoicePdfService invoicePdfService;

    public AdminRestController(UserService userService, OrderRepository orderRepository,
                               AppointmentRepository appointmentRepository,
                               RefundRepository refundRepository,
                               InvoicePdfService invoicePdfService) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.appointmentRepository = appointmentRepository;
        this.refundRepository = refundRepository;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques dashboard", description = "Retourne les KPIs principaux")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userService.count());
        stats.put("activeUsers", userService.countActiveUsers());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalAppointments", appointmentRepository.count());

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
    @Operation(summary = "Lister les utilisateurs", description = "Retourne les utilisateurs avec pagination")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("registrationDate").descending());
        Page<User> users;

        if (status != null && !status.isEmpty()) {
            try {
                users = userService.findByStatus(UserStatus.valueOf(status), pageRequest);
            } catch (IllegalArgumentException e) {
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
                        OrderStatus.valueOf(status), pageRequest).map(OrderResponse::from);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status: " + status));
            }
        } else {
            orders = orderRepository.findAll(pageRequest).map(OrderResponse::from);
        }

        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    // ========== User Management ==========

    @PutMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Modifier un utilisateur", description = "Met à jour le statut, verrouillage ou rôle d'un utilisateur")
    public ResponseEntity<ApiResponse<Void>> updateUser(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (data.containsKey("status")) {
                String status = (String) data.get("status");
                boolean active = "ACTIVE".equals(status);
                userService.setUserActive(id, active);
            }

            if (data.containsKey("locked")) {
                boolean locked = Boolean.TRUE.equals(data.get("locked"));
                userService.setUserLocked(id, locked);
            }

            return ResponseEntity.ok(ApiResponse.ok("Utilisateur mis à jour", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
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

                    if (order.getUser() != null) {
                        detail.put("userEmail", order.getUser().getEmail());
                        detail.put("userName", order.getUser().getDisplayName());
                        detail.put("userId", order.getUser().getId());
                    }

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

            if (data.containsKey("status")) {
                order.setStatus(OrderStatus.valueOf((String) data.get("status")));
            }
            if (data.containsKey("progressPercentage")) {
                order.setProgressPercentage(((Number) data.get("progressPercentage")).intValue());
            }
            if (data.containsKey("progressStatus")) {
                order.setProgressStatus((String) data.get("progressStatus"));
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

            return ResponseEntity.ok(ApiResponse.ok("Commande mise à jour", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
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

            Order order = new Order();
            order.setUser(user);
            order.setServiceName(serviceName);
            order.setTotalAmount(BigDecimal.valueOf(amount));
            order.setCurrency(currency);
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setNotes(notes);
            order.setAdminNotes("[Créée par admin]");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setPriority(1); // High priority — admin-created

            order = orderRepository.save(order);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Commande créée pour " + user.getEmail(), OrderResponse.from(order)));
        } catch (Exception e) {
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
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== Invoice Download (API for Angular) ==========

    @GetMapping("/orders/{orderId}/invoice")
    @Operation(summary = "Télécharger la facture PDF", description = "Génère et retourne la facture PDF pour une commande")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

            User user = order.getUser();
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }

            byte[] pdf = invoicePdfService.generateInvoicePdf(order, user);
            String invoiceNumber = invoicePdfService.generateInvoiceNumber(order);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Facture_" + invoiceNumber + ".pdf");
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Order Refunds (read) ==========

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
}
