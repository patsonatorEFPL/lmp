package com.lmp.billing.web.api;

import com.lmp.billing.domain.Order;
import com.lmp.auth.domain.User;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.billing.dto.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * API REST pour les commandes de l'utilisateur authentifié.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Commandes de l'utilisateur")
public class OrderRestController {

    private final OrderRepository orderRepository;
    private final UserService userService;

    public OrderRestController(OrderRepository orderRepository, UserService userService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Mes commandes", description = "Retourne les commandes de l'utilisateur authentifié")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        List<OrderResponse> orders = orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail commande", description = "Retourne une commande par ID (si elle appartient à l'utilisateur)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        return orderRepository.findById(id)
                .filter(order -> order.getUser() != null && order.getUser().getId().equals(user.getId()))
                .map(order -> ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order))))
                .orElse(ResponseEntity.notFound().build());
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
