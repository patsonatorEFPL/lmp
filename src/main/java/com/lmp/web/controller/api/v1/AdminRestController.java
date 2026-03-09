package com.lmp.web.controller.api.v1;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.OrderStatus;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.AppointmentRepository;
import com.lmp.repository.OrderRepository;
import com.lmp.service.user.UserService;
import com.lmp.web.controller.api.v1.dto.ApiResponse;
import com.lmp.web.controller.api.v1.dto.OrderResponse;
import com.lmp.web.controller.api.v1.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API REST d'administration (rôle ADMIN requis).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
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
}
