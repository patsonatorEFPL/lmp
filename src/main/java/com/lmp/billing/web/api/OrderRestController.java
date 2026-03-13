package com.lmp.billing.web.api;

import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.Refund;
import com.lmp.auth.domain.User;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.billing.service.InvoicePdfService;
import com.lmp.auth.service.UserService;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.billing.dto.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final RefundRepository refundRepository;
    private final InvoicePdfService invoicePdfService;

    public OrderRestController(OrderRepository orderRepository, UserService userService,
                               RefundRepository refundRepository, InvoicePdfService invoicePdfService) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.refundRepository = refundRepository;
        this.invoicePdfService = invoicePdfService;
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

    @GetMapping("/{id}/refunds")
    @Operation(summary = "Remboursements d'une commande", description = "Retourne les remboursements d'une commande de l'utilisateur")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrderRefunds(
            @PathVariable UUID id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        Order order = orderRepository.findById(id)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .orElse(null);
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
            map.put("createdAt", r.getCreatedAt());
            map.put("processedAt", r.getProcessedAt());
            return map;
        }).toList();

        return ResponseEntity.ok(ApiResponse.ok(refundList));
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Télécharger la facture PDF", description = "Génère la facture PDF pour une commande de l'utilisateur")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Order order = orderRepository.findById(id)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(user.getId()))
                .orElse(null);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        try {
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

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
