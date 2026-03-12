package com.lmp.shared.web.api;

import com.lmp.auth.domain.User;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.auth.domain.UserStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.billing.domain.Order;
import com.lmp.billing.dto.OrderResponse;
import com.lmp.auth.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

    public AdminRestController(UserService userService, OrderRepository orderRepository,
                               AppointmentRepository appointmentRepository) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.appointmentRepository = appointmentRepository;
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
}
