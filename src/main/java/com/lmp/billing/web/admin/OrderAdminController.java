package com.lmp.billing.web.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderProgressSync;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.service.admin.OrderAdminService;
import com.lmp.billing.service.admin.OrderStatusHistoryService;
import com.lmp.billing.service.admin.RefundService;
import com.lmp.billing.service.admin.ReportsService;
import com.lmp.shared.service.SystemConfigService;
import com.lmp.billing.dto.admin.OrderDto;
import com.lmp.billing.dto.admin.OrderSearchDto;
import com.lmp.billing.dto.admin.OrderActionDto;
import com.lmp.billing.dto.admin.OrderReportDto;
import com.lmp.billing.dto.admin.RefundDto;
import com.lmp.billing.dto.admin.OrderStatusHistoryDto;

/**
 * Contrôleur pour l'administration des commandes.
 * Fournit toutes les fonctionnalités de gestion des commandes pour les administrateurs.
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {

    private static final Logger logger = LoggerFactory.getLogger(OrderAdminController.class);

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

        private final OrderAdminService orderAdminService;
        private final OrderStatusHistoryService historyService;
        private final RefundService refundService;
        private final ReportsService reportsService;
        private final SystemConfigService systemConfigService;
        private final OrderRepository orderRepository;

    public OrderAdminController(OrderAdminService orderAdminService,
                           OrderStatusHistoryService historyService,
                           RefundService refundService,
                           ReportsService reportsService,
                           SystemConfigService systemConfigService,
                           OrderRepository orderRepository) {
        this.orderAdminService = orderAdminService;
        this.historyService = historyService;
        this.refundService = refundService;
        this.reportsService = reportsService;
        this.systemConfigService = systemConfigService;
        this.orderRepository = orderRepository;
    }

    // ========== Pages principales ==========

    /**
     * Redirige vers le frontend Angular pour la gestion des commandes.
     */
    @GetMapping
    public String ordersPage() {
        return "redirect:" + frontendUrl + "/admin/orders";
    }

    /**
     * Redirige vers le frontend Angular pour les détails d'une commande.
     */
    @GetMapping("/{orderId}")
    public String orderDetailsRedirect(@PathVariable java.util.UUID orderId) {
        return "redirect:" + frontendUrl + "/admin/orders/" + orderId;
    }

    /**
     * Redirige vers le frontend Angular pour les rapports.
     */
    @GetMapping("/reports")
    public String reportsPage() {
        return "redirect:" + frontendUrl + "/admin/orders/reports";
    }

    // ========== API REST pour recherche et pagination ==========

    /**
     * Recherche avancée des commandes avec pagination
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Page<OrderDto>> searchOrders(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Boolean hasRefunds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            OrderSearchDto searchDto = new OrderSearchDto();
            searchDto.setSearchTerm(searchTerm);
            searchDto.setCustomerEmail(customerEmail);
            searchDto.setServiceName(serviceName);
            searchDto.setStatus(status != null ? status.toString() : null);
            searchDto.setMinAmount(minAmount);
            searchDto.setMaxAmount(maxAmount);
            searchDto.setStartDate(startDate);
            searchDto.setEndDate(endDate);
            searchDto.setPaymentStatus(paymentStatus);
            searchDto.setHasRefunds(hasRefunds);

            Page<OrderDto> orders = orderAdminService.searchOrders(searchDto, page, size, sortBy, sortDir);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            logger.error("Erreur recherche commandes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les détails d'une commande via API
     */
    @GetMapping("/api/{orderId}")
    @ResponseBody
    public ResponseEntity<OrderDto> getOrderApi(@PathVariable java.util.UUID orderId) {
        try {
            OrderDto order = orderAdminService.getOrderDetails(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.error("Erreur récupération commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // ========== Actions sur les commandes ==========

    /**
     * Change le statut d'une commande
     */
    @PutMapping("/api/{orderId}/status")
    @ResponseBody
    public ResponseEntity<?> changeOrderStatus(@PathVariable java.util.UUID orderId,
                                              @RequestParam OrderStatus newStatus,
                                              @RequestParam(required = false) String note) {
        try {
            OrderDto updatedOrder = orderAdminService.changeOrderStatus(orderId, newStatus, note);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Statut mis à jour avec succès",
                "order", updatedOrder
            ));
        } catch (Exception e) {
            logger.error("Erreur changement statut commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Annule une commande
     */
    @PutMapping("/api/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable java.util.UUID orderId,
                                        @RequestParam String reason,
                                        @RequestParam(defaultValue = "false") boolean processRefund) {
        try {
            OrderDto cancelledOrder = orderAdminService.cancelOrder(orderId, reason, processRefund);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Commande annulée avec succès",
                "order", cancelledOrder
            ));
        } catch (Exception e) {
            logger.error("Erreur annulation commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Met à jour les notes administratives
     */
    @PutMapping("/api/{orderId}/notes")
    @ResponseBody
    public ResponseEntity<?> updateOrderNotes(@PathVariable java.util.UUID orderId,
                                             @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            OrderDto updatedOrder = orderAdminService.updateAdminNotes(orderId, notes);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notes mises à jour",
                "order", updatedOrder
            ));
        } catch (Exception e) {
            logger.error("Erreur mise à jour notes commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Met à jour la priorité d'une commande
     */
    @PutMapping("/api/{orderId}/priority")
    @ResponseBody
    public ResponseEntity<?> updateOrderPriority(@PathVariable java.util.UUID orderId,
                                                @RequestParam Integer priority) {
        try {
            OrderDto updatedOrder = orderAdminService.updateOrderPriority(orderId, priority);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Priorité mise à jour",
                "order", updatedOrder
            ));
        } catch (Exception e) {
            logger.error("Erreur mise à jour priorité commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Synchronise une commande avec Stripe
     */
    @PostMapping("/api/{orderId}/sync-stripe")
    @ResponseBody
    public ResponseEntity<?> syncWithStripe(@PathVariable java.util.UUID orderId) {
        try {
            OrderDto syncedOrder = orderAdminService.syncWithStripe(orderId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Synchronisation Stripe réussie",
                "order", syncedOrder
            ));
        } catch (Exception e) {
            logger.error("Erreur synchronisation Stripe commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ========== Gestion des remboursements ==========

    /**
     * Crée un remboursement
     */
    @PostMapping("/api/{orderId}/refund")
    @ResponseBody
    public ResponseEntity<?> createRefund(@PathVariable java.util.UUID orderId,
                                         @RequestParam BigDecimal amount,
                                         @RequestParam String reason) {
        try {
            RefundDto refund = refundService.createRefund(orderId, amount, reason);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Remboursement créé avec succès",
                "refund", refund
            ));
        } catch (Exception e) {
            logger.error("Erreur création remboursement commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Récupère les remboursements d'une commande
     */
    @GetMapping("/api/{orderId}/refunds")
    @ResponseBody
    public ResponseEntity<List<RefundDto>> getOrderRefunds(@PathVariable java.util.UUID orderId) {
        try {
            List<RefundDto> refunds = refundService.getOrderRefunds(orderId);
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            logger.error("Erreur récupération remboursements commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Avancement des commandes ==========

    /**
     * Met à jour l'avancement d'une commande (0-100 %) avec un libellé d'étape.
     */
    @PutMapping("/api/{orderId}/progress")
    @ResponseBody
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateOrderProgress(@PathVariable java.util.UUID orderId,
                                                 @RequestBody Map<String, Object> request) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

            if (request.containsKey("progressPercentage")) {
                int pct = ((Number) request.get("progressPercentage")).intValue();
                if (pct < 0 || pct > 100) throw new IllegalArgumentException("Pourcentage invalide (0-100)");
                order.setProgressPercentage(pct);
            }
            if (request.containsKey("progressStatus")) {
                order.setProgressStatus((String) request.get("progressStatus"));
            }
            OrderProgressSync.applyMinimumForStatus(order);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            logger.info("Progress updated for order {}: {}% - {}", orderId,
                    order.getProgressPercentage(), order.getProgressStatus());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Avancement mis à jour",
                    "progressPercentage", order.getProgressPercentage(),
                    "progressStatus", order.getProgressStatus() != null ? order.getProgressStatus() : ""
            ));
        } catch (Exception e) {
            logger.error("Erreur mise à jour avancement commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ========== Actions en lot ==========

    /**
     * Traite des actions en lot sur plusieurs commandes
     */
    @PostMapping("/api/bulk-actions")
    @ResponseBody
    public ResponseEntity<?> processBulkActions(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> orderIdStrings = (List<String>) request.get("orderIds");
            List<java.util.UUID> orderIds = orderIdStrings.stream()
                .map(java.util.UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
            String actionType = (String) request.get("actionType");

            OrderActionDto action = new OrderActionDto();
            action.setActionType(actionType);
            
            // Configuration spécifique selon l'action
            if (request.containsKey("newStatus")) {
                action.setNewStatus((String) request.get("newStatus"));
            }
            if (request.containsKey("note")) {
                action.setNote((String) request.get("note"));
            }
            if (request.containsKey("priority")) {
                action.setPriority(((Number) request.get("priority")).intValue());
            }
            if (request.containsKey("tags")) {
                action.setTags((String) request.get("tags"));
            }

            List<OrderDto> results = orderAdminService.processBulkAction(orderIds, action);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Actions en lot traitées",
                "processedCount", results.size(),
                "totalCount", orderIds.size(),
                "results", results
            ));
        } catch (Exception e) {
            logger.error("Erreur actions en lot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ========== Statistiques et rapports ==========

    /**
     * Génère un rapport pour une période donnée
     */
    @GetMapping("/api/reports/generate")
    @ResponseBody
    public ResponseEntity<?> generateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            OrderReportDto report = reportsService.generateOrderReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            logger.error("Erreur génération rapport: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Export des commandes au format CSV
     */
    @GetMapping("/api/export/csv")
    @ResponseBody
    public ResponseEntity<?> exportToCSV(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            List<String[]> csvData = reportsService.exportOrdersToCSV(startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "filename", "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv",
                "data", csvData
            ));
        } catch (Exception e) {
            logger.error("Erreur export CSV: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Analyse des tendances
     */
    @GetMapping("/api/analytics/trends")
    @ResponseBody
    public ResponseEntity<?> analyzeTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "daily") String period) {
        try {
            Map<String, Object> trends = reportsService.analyzeTrends(startDate, endDate, period);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.error("Erreur analyse tendances: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ========== Historique et audit ==========

    /**
     * Récupère l'historique d'une commande
     */
    @GetMapping("/api/{orderId}/history")
    @ResponseBody
    public ResponseEntity<List<OrderStatusHistoryDto>> getOrderHistory(@PathVariable java.util.UUID orderId) {
        try {
            List<OrderStatusHistoryDto> history = historyService.getOrderHistory(orderId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Erreur récupération historique commande {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Gestion des erreurs ==========

    /**
     * Gestion globale des erreurs pour ce contrôleur
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<?> handleException(Exception e) {
        logger.error("Erreur dans OrderAdminController: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Une erreur inattendue s'est produite",
            "error", e.getMessage()
        ));
    }
}