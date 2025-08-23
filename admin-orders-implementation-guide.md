# Guide d'Implémentation - Vue Administration des Commandes

## 🎯 Vue d'Ensemble

Ce guide détaille l'implémentation complète de la vue d'administration des commandes `/admin/orders` pour LMP Digital Services, incluant tous les composants techniques nécessaires.

---

## 📁 Structure des Fichiers

```
src/main/java/com/lmp/
├── web/
│   ├── controller/admin/
│   │   ├── AdminOrderController.java          # Controller principal
│   │   ├── AdminOrderApiController.java       # API REST
│   │   └── AdminOrderReportController.java    # Rapports et exports
│   └── dto/admin/
│       ├── OrderAdminDto.java                 # DTO principal
│       ├── OrderFilterDto.java                # Filtres de recherche
│       ├── OrderActionDto.java                # Actions administrateur
│       ├── OrderAnalyticsDto.java             # Analytics
│       └── RefundRequestDto.java              # Remboursements
├── service/admin/
│   ├── OrderAdminService.java                 # Service principal
│   ├── OrderNotificationService.java          # Notifications
│   ├── OrderReportService.java                # Rapports
│   ├── OrderRefundService.java                # Remboursements
│   └── OrderAuditService.java                 # Audit et logs
├── repository/
│   └── OrderRepository.java                   # Extensions repository
└── config/
    └── AdminOrderConfig.java                  # Configuration

src/main/resources/
├── templates/admin/
│   ├── orders/
│   │   ├── index.html                         # Vue principale
│   │   ├── detail.html                        # Détail commande
│   │   ├── dashboard.html                     # Dashboard analytics
│   │   └── fragments/
│   │       ├── order-table.html               # Tableau commandes
│   │       ├── order-filters.html             # Filtres avancés
│   │       ├── order-actions.html             # Actions modales
│   │       └── order-charts.html              # Graphiques
├── static/admin/
│   ├── js/
│   │   ├── orders-management.js               # JS principal
│   │   ├── orders-filters.js                  # Logique filtres
│   │   ├── orders-websocket.js                # Temps réel
│   │   └── orders-charts.js                   # Graphiques
│   └── css/
│       └── orders-admin.css                   # Styles spécifiques
└── application.yml                            # Configuration
```

---

## 🎮 Controllers Détaillés

### 1. AdminOrderController
```java
package com.lmp.web.controller.admin;

import com.lmp.service.admin.OrderAdminService;
import com.lmp.web.dto.admin.OrderFilterDto;
import com.lmp.web.dto.admin.OrderAdminDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller principal pour l'interface d'administration des commandes
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    
    @Autowired
    private OrderAdminService orderAdminService;
    
    /**
     * Page principale de gestion des commandes
     */
    @GetMapping
    public String ordersIndex(
            @ModelAttribute OrderFilterDto filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        // Configuration du tri
        Sort.Direction direction = "desc".equals(sortDir) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        // Application des filtres et recherche
        Page<OrderAdminDto> ordersPage = orderAdminService.findOrdersWithFilters(filters, pageable);
        
        // Préparation du modèle
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("filters", filters);
        model.addAttribute("currentPage", page);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        // Options pour les filtres
        model.addAttribute("statusOptions", OrderStatus.values());
        model.addAttribute("datePresets", getDatePresets());
        model.addAttribute("amountRanges", getAmountRanges());
        
        // Statistiques rapides
        model.addAttribute("dashboardStats", orderAdminService.getDashboardStats());
        
        return "admin/orders/index";
    }
    
    /**
     * Page de détail d'une commande
     */
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId, Model model) {
        
        OrderAdminDto order = orderAdminService.getOrderDetails(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée"));
        
        model.addAttribute("order", order);
        model.addAttribute("statusOptions", getAvailableStatusTransitions(order.getStatus()));
        model.addAttribute("canModify", order.isCanBeModified());
        model.addAttribute("canCancel", order.isCanBeCancelled());
        model.addAttribute("canRefund", order.isCanBeRefunded());
        
        return "admin/orders/detail";
    }
    
    /**
     * Dashboard analytics
     */
    @GetMapping("/dashboard")
    public String ordersDashboard(Model model) {
        
        // Statistiques principales
        model.addAttribute("stats", orderAdminService.getDashboardStats());
        
        // Données pour les graphiques
        model.addAttribute("chartData", orderAdminService.getChartData());
        
        // Commandes nécessitant une attention
        model.addAttribute("alertOrders", orderAdminService.getOrdersRequiringAttention());
        
        return "admin/orders/dashboard";
    }
    
    // Méthodes utilitaires privées
    
    private List<String> getDatePresets() {
        return List.of("today", "week", "month", "quarter", "year");
    }
    
    private List<String> getAmountRanges() {
        return List.of("0-100", "100-500", "500-1000", "1000+");
    }
    
    private List<OrderStatus> getAvailableStatusTransitions(OrderStatus currentStatus) {
        return switch(currentStatus) {
            case PAYMENT_PENDING -> List.of(OrderStatus.PENDING, OrderStatus.CANCELLED);
            case PENDING -> List.of(OrderStatus.IN_PROGRESS, OrderStatus.UNDER_REVIEW, OrderStatus.CANCELLED);
            case IN_PROGRESS -> List.of(OrderStatus.COMPLETED, OrderStatus.UNDER_REVIEW, OrderStatus.CANCELLED);
            case UNDER_REVIEW -> List.of(OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
            default -> List.of();
        };
    }
}
```

### 2. AdminOrderApiController
```java
package com.lmp.web.controller.admin;

import com.lmp.service.admin.OrderAdminService;
import com.lmp.web.dto.admin.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * API REST pour les actions AJAX sur les commandes
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderApiController {
    
    @Autowired
    private OrderAdminService orderAdminService;
    
    /**
     * Recherche avec filtres (AJAX)
     */
    @PostMapping("/search")
    public ResponseEntity<Page<OrderAdminDto>> searchOrders(
            @RequestBody OrderFilterDto filters,
            Pageable pageable) {
        
        Page<OrderAdminDto> results = orderAdminService.findOrdersWithFilters(filters, pageable);
        return ResponseEntity.ok(results);
    }
    
    /**
     * Mise à jour du statut d'une commande
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ORDER_MODIFY')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderActionDto actionDto,
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            // Enrichissement des métadonnées
            actionDto.setOrderId(orderId);
            actionDto.setIpAddress(getClientIpAddress(request));
            actionDto.setUserAgent(request.getHeader("User-Agent"));
            
            OrderAdminDto updatedOrder = orderAdminService.updateOrderStatus(
                actionDto, authentication.getName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Statut mis à jour avec succès",
                "order", updatedOrder
            ));
            
        } catch (OrderAdminException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Annulation d'une commande
     */
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL')")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderActionDto actionDto,
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            actionDto.setOrderId(orderId);
            actionDto.setActionType("CANCEL");
            actionDto.setIpAddress(getClientIpAddress(request));
            
            OrderAdminDto cancelledOrder = orderAdminService.cancelOrder(
                actionDto, authentication.getName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Commande annulée avec succès",
                "order", cancelledOrder
            ));
            
        } catch (OrderAdminException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Traitement d'un remboursement
     */
    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasAuthority('ORDER_REFUND')")
    public ResponseEntity<?> processRefund(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderActionDto actionDto,
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            actionDto.setOrderId(orderId);
            actionDto.setActionType("REFUND");
            actionDto.setIpAddress(getClientIpAddress(request));
            
            RefundResponseDto refundResponse = orderAdminService.processRefund(
                actionDto, authentication.getName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Remboursement traité avec succès",
                "refund", refundResponse
            ));
            
        } catch (OrderAdminException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Ajout de notes à une commande
     */
    @PostMapping("/{orderId}/notes")
    @PreAuthorize("hasAuthority('ORDER_MODIFY')")
    public ResponseEntity<?> addOrderNote(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        try {
            String note = request.get("note");
            if (note == null || note.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "La note ne peut pas être vide"
                ));
            }
            
            OrderAdminDto updatedOrder = orderAdminService.addOrderNote(
                orderId, note, authentication.getName());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Note ajoutée avec succès",
                "order", updatedOrder
            ));
            
        } catch (OrderAdminException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Autocomplétion pour la recherche d'utilisateurs
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<String>> searchUsers(@RequestParam String query) {
        List<String> suggestions = orderAdminService.getUserSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Statistiques en temps réel
     */
    @GetMapping("/stats/realtime")
    public ResponseEntity<OrderDashboardStatsDto> getRealTimeStats() {
        OrderDashboardStatsDto stats = orderAdminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
    
    // Méthodes utilitaires
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

---

## 🎨 Templates Thymeleaf

### 1. Template Principal (`index.html`)
```html
<!DOCTYPE html>
<html lang="fr" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Gestion des Commandes - LMP Digital Services</title>
    
    <!-- CSS Bootstrap et personnalisé -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <link th:href="@{/admin/css/orders-admin.css}" rel="stylesheet">
</head>
<body>
    <div class="container-fluid">
        <!-- Header avec statistiques rapides -->
        <div class="row mb-4">
            <div class="col-12">
                <div th:replace="~{admin/orders/fragments/order-stats :: quick-stats}"></div>
            </div>
        </div>
        
        <!-- Section des filtres -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-header">
                        <h5 class="card-title mb-0">
                            <i class="bi bi-funnel"></i> Filtres Avancés
                        </h5>
                    </div>
                    <div class="card-body">
                        <div th:replace="~{admin/orders/fragments/order-filters :: advanced-filters}"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Section principale avec tableau -->
        <div class="row">
            <div class="col-12">
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="card-title mb-0">
                            <i class="bi bi-list-check"></i> Commandes
                            <span class="badge bg-primary ms-2" th:text="${ordersPage.totalElements}">0</span>
                        </h5>
                        <div class="btn-group">
                            <button type="button" class="btn btn-outline-secondary btn-sm" onclick="refreshTable()">
                                <i class="bi bi-arrow-clockwise"></i> Actualiser
                            </button>
                            <button type="button" class="btn btn-outline-primary btn-sm" onclick="exportData()">
                                <i class="bi bi-download"></i> Exporter
                            </button>
                        </div>
                    </div>
                    <div class="card-body p-0">
                        <div th:replace="~{admin/orders/fragments/order-table :: orders-table}"></div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Pagination -->
        <div class="row mt-4">
            <div class="col-12">
                <div th:replace="~{admin/orders/fragments/order-table :: pagination}"></div>
            </div>
        </div>
    </div>
    
    <!-- Modals pour les actions -->
    <div th:replace="~{admin/orders/fragments/order-actions :: all-modals}"></div>
    
    <!-- JavaScript -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script th:src="@{/admin/js/orders-management.js}"></script>
    <script th:src="@{/admin/js/orders-filters.js}"></script>
    <script th:src="@{/admin/js/orders-websocket.js}"></script>
    
    <script th:inline="javascript">
        // Configuration globale
        window.OrdersConfig = {
            apiBase: /*[[@{/api/admin/orders}]]*/ '/api/admin/orders',
            wsEndpoint: /*[[@{/ws/admin/orders}]]*/ '/ws/admin/orders',
            currentPage: /*[[${currentPage}]]*/ 0,
            pageSize: /*[[${ordersPage.size}]]*/ 20,
            totalPages: /*[[${ordersPage.totalPages}]]*/ 1
        };
        
        // Initialisation
        $(document).ready(function() {
            OrdersManagement.init();
            OrdersFilters.init();
            OrdersWebSocket.connect();
        });
    </script>
</body>
</html>
```

### 2. Fragment Filtres (`order-filters.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div th:fragment="advanced-filters">
        <form id="ordersFilterForm" th:object="${filters}">
            <div class="row g-3">
                <!-- Filtres par statut -->
                <div class="col-md-3">
                    <label class="form-label">Statuts</label>
                    <div class="dropdown">
                        <button class="btn btn-outline-secondary dropdown-toggle w-100" type="button" 
                                data-bs-toggle="dropdown" id="statusFilterButton">
                            <span id="statusFilterText">Tous les statuts</span>
                        </button>
                        <div class="dropdown-menu p-3" style="min-width: 300px;">
                            <div class="form-check" th:each="status : ${statusOptions}">
                                <input class="form-check-input status-filter" type="checkbox" 
                                       th:id="'status-' + ${status}" th:value="${status}" 
                                       th:field="*{statuses}">
                                <label class="form-check-label" th:for="'status-' + ${status}">
                                    <span class="badge" th:classappend="${'badge-' + status.name().toLowerCase()}"
                                          th:text="${status.displayName}">Statut</span>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>
                
                <!-- Filtres de date -->
                <div class="col-md-3">
                    <label class="form-label">Période</label>
                    <div class="input-group">
                        <select class="form-select" th:field="*{datePreset}" onchange="applyDatePreset()">
                            <option value="">Période personnalisée</option>
                            <option value="today">Aujourd'hui</option>
                            <option value="week">Cette semaine</option>
                            <option value="month">Ce mois</option>
                            <option value="quarter">Ce trimestre</option>
                            <option value="year">Cette année</option>
                        </select>
                    </div>
                    <div class="row mt-2">
                        <div class="col-6">
                            <input type="datetime-local" class="form-control form-control-sm" 
                                   th:field="*{startDate}" placeholder="Date début">
                        </div>
                        <div class="col-6">
                            <input type="datetime-local" class="form-control form-control-sm" 
                                   th:field="*{endDate}" placeholder="Date fin">
                        </div>
                    </div>
                </div>
                
                <!-- Recherche utilisateur -->
                <div class="col-md-3">
                    <label class="form-label">Client</label>
                    <div class="position-relative">
                        <input type="text" class="form-control" th:field="*{userSearch}" 
                               placeholder="Nom, email ou ID..." autocomplete="off" 
                               id="userSearchInput">
                        <div id="userSuggestions" class="position-absolute w-100 bg-white border rounded shadow-sm" 
                             style="top: 100%; z-index: 1000; display: none; max-height: 200px; overflow-y: auto;">
                        </div>
                    </div>
                </div>
                
                <!-- Filtres montant -->
                <div class="col-md-3">
                    <label class="form-label">Montant</label>
                    <div class="input-group">
                        <select class="form-select" th:field="*{amountRange}" onchange="applyAmountRange()">
                            <option value="">Tous les montants</option>
                            <option value="0-100">0 - 100 CAD</option>
                            <option value="100-500">100 - 500 CAD</option>
                            <option value="500-1000">500 - 1000 CAD</option>
                            <option value="1000+">1000+ CAD</option>
                        </select>
                    </div>
                    <div class="row mt-2">
                        <div class="col-6">
                            <input type="number" class="form-control form-control-sm" 
                                   th:field="*{minAmount}" placeholder="Min" step="0.01">
                        </div>
                        <div class="col-6">
                            <input type="number" class="form-control form-control-sm" 
                                   th:field="*{maxAmount}" placeholder="Max" step="0.01">
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Boutons d'action -->
            <div class="row mt-3">
                <div class="col-12">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <button type="button" class="btn btn-primary" onclick="applyFilters()">
                                <i class="bi bi-search"></i> Rechercher
                            </button>
                            <button type="button" class="btn btn-outline-secondary" onclick="clearFilters()">
                                <i class="bi bi-x-circle"></i> Effacer
                            </button>
                        </div>
                        <div>
                            <button type="button" class="btn btn-outline-info btn-sm" onclick="saveFilters()">
                                <i class="bi bi-bookmark"></i> Sauvegarder filtres
                            </button>
                            <div class="dropdown d-inline-block">
                                <button class="btn btn-outline-secondary btn-sm dropdown-toggle" type="button" 
                                        data-bs-toggle="dropdown">
                                    <i class="bi bi-bookmarks"></i> Filtres sauvegardés
                                </button>
                                <ul class="dropdown-menu" id="savedFiltersList">
                                    <!-- Dynamically populated -->
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</body>
</html>
```

### 3. Fragment Tableau (`order-table.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div th:fragment="orders-table">
        <div class="table-responsive">
            <table class="table table-hover table-striped mb-0" id="ordersTable">
                <thead class="table-dark">
                    <tr>
                        <th scope="col">
                            <input type="checkbox" id="selectAllOrders" class="form-check-input">
                        </th>
                        <th scope="col">
                            <a href="#" class="text-white text-decoration-none" 
                               onclick="sortTable('id')" th:classappend="${sortBy == 'id' ? 'active' : ''}">
                                ID <i class="bi bi-arrow-down-up"></i>
                            </a>
                        </th>
                        <th scope="col">
                            <a href="#" class="text-white text-decoration-none" 
                               onclick="sortTable('customerName')">
                                Client <i class="bi bi-arrow-down-up"></i>
                            </a>
                        </th>
                        <th scope="col">
                            <a href="#" class="text-white text-decoration-none" 
                               onclick="sortTable('status')">
                                Statut <i class="bi bi-arrow-down-up"></i>
                            </a>
                        </th>
                        <th scope="col">
                            <a href="#" class="text-white text-decoration-none" 
                               onclick="sortTable('totalAmount')">
                                Montant <i class="bi bi-arrow-down-up"></i>
                            </a>
                        </th>
                        <th scope="col">
                            <a href="#" class="text-white text-decoration-none" 
                               onclick="sortTable('createdAt')">
                                Date création <i class="bi bi-arrow-down-up"></i>
                            </a>
                        </th>
                        <th scope="col">Dernière MAJ</th>
                        <th scope="col">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="order : ${ordersPage.content}" 
                        th:id="'order-row-' + ${order.id}"
                        th:classappend="${order.hasUnresolvedIssues ? 'table-warning' : ''}">
                        
                        <td>
                            <input type="checkbox" class="form-check-input order-checkbox" 
                                   th:value="${order.id}">
                        </td>
                        
                        <td>
                            <a th:href="@{/admin/orders/{id}(id=${order.id})}" 
                               class="text-decoration-none fw-bold">
                                #<span th:text="${order.id}">1234</span>
                            </a>
                            <span th:if="${order.hasUnresolvedIssues}" 
                                  class="badge bg-warning text-dark ms-1">
                                <i class="bi bi-exclamation-triangle"></i>
                            </span>
                        </td>
                        
                        <td>
                            <div>
                                <span class="fw-semibold" th:text="${order.customerName}">Nom Client</span>
                                <br>
                                <small class="text-muted" th:text="${order.customerEmail}">email@example.com</small>
                            </div>
                        </td>
                        
                        <td>
                            <span class="badge fs-6" 
                                  th:classappend="${order.statusBadgeClass}"
                                  th:text="${order.statusDisplayName}">
                                Statut
                            </span>
                        </td>
                        
                        <td>
                            <span class="fw-bold" th:text="${#numbers.formatCurrency(order.totalAmount)}">
                                $123.45
                            </span>
                            <br>
                            <small class="text-muted">
                                Payé: <span th:text="${#numbers.formatCurrency(order.totalPaid)}">$0.00</span>
                            </small>
                        </td>
                        
                        <td>
                            <span th:text="${#temporals.format(order.createdAt, 'dd/MM/yyyy HH:mm')}">
                                01/01/2024 10:00
                            </span>
                            <br>
                            <small class="text-muted">
                                Il y a <span th:text="${order.daysSinceCreation}">5</span> jours
                            </small>
                        </td>
                        
                        <td>
                            <span th:if="${order.updatedAt}" 
                                  th:text="${#temporals.format(order.updatedAt, 'dd/MM/yyyy HH:mm')}">
                                01/01/2024 15:30
                            </span>
                            <br>
                            <small class="text-muted" th:if="${order.lastUpdatedBy}" 
                                   th:text="'Par: ' + ${order.lastUpdatedBy}">
                                Par: admin@lmp.ca
                            </small>
                        </td>
                        
                        <td>
                            <div class="btn-group" role="group">
                                <button type="button" class="btn btn-outline-primary btn-sm" 
                                        th:onclick="'viewOrderDetail(' + ${order.id} + ')'"
                                        title="Voir détails">
                                    <i class="bi bi-eye"></i>
                                </button>
                                
                                <button type="button" class="btn btn-outline-success btn-sm" 
                                        th:if="${order.canBeModified}"
                                        th:onclick="'editOrderStatus(' + ${order.id} + ')'"
                                        title="Modifier statut">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                
                                <button type="button" class="btn btn-outline-danger btn-sm" 
                                        th:if="${order.canBeCancelled}"
                                        th:onclick="'cancelOrder(' + ${order.id} + ')'"
                                        title="Annuler">
                                    <i class="bi bi-x-circle"></i>
                                </button>
                                
                                <button type="button" class="btn btn-outline-warning btn-sm" 
                                        th:if="${order.canBeRefunded}"
                                        th:onclick="'refundOrder(' + ${order.id} + ')'"
                                        title="Rembourser">
                                    <i class="bi bi-arrow-return-left"></i>
                                </button>
                                
                                <div class="dropdown">
                                    <button class="btn btn-outline-secondary btn-sm dropdown-toggle" 
                                            type="button" data-bs-toggle="dropdown">
                                        <i class="bi bi-three-dots"></i>
                                    </button>
                                    <ul class="dropdown-menu">
                                        <li><a class="dropdown-item" href="#" 
                                               th:onclick="'addOrderNote(' + ${order.id} + ')'">
                                            <i class="bi bi-chat-text"></i> Ajouter note
                                        </a></li>
                                        <li><a class="dropdown-item" href="#" 
                                               th:onclick="'viewOrderHistory(' + ${order.id} + ')'">
                                            <i class="bi bi-clock-history"></i> Historique
                                        </a></li>
                                        <li><a class="dropdown-item" href="#" 
                                               th:onclick="'exportOrderData(' + ${order.id} + ')'">
                                            <i class="bi bi-download"></i> Exporter
                                        </a></li>
                                    </ul>
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        
        <!-- Message si aucune commande -->
        <div th:if="${ordersPage.empty}" class="text-center py-5">
            <i class="bi bi-inbox display-1 text-muted"></i>
            <h4 class="text-muted mt-3">Aucune commande trouvée</h4>
            <p class="text-muted">Modifiez vos critères de recherche ou effacez les filtres.</p>
            <button type="button" class="btn btn-outline-primary" onclick="clearFilters()">
                Effacer les filtres
            </button>
        </div>
    </div>
    
    <!-- Fragment Pagination -->
    <div th:fragment="pagination" th:if="${!ordersPage.empty}">
        <nav aria-label="Navigation des pages">
            <div class="d-flex justify-content-between align-items-center">
                <div>
                    <span class="text-muted">
                        Affichage de 
                        <strong th:text="${ordersPage.number * ordersPage.size + 1}">1</strong>
                        à 
                        <strong th:text="${T(java.lang.Math).min((ordersPage.number + 1) * ordersPage.size, ordersPage.totalElements)}">20</strong>
                        sur 
                        <strong th:text="${ordersPage.totalElements}">100</strong>
                        commandes
                    </span>
                </div>
                
                <div class="d-flex align-items-center">
                    <!-- Sélecteur de taille de page -->
                    <select class="form-select form-select-sm me-3" style="width: auto;" 
                            onchange="changePageSize(this.value)">
                        <option value="10" th:selected="${ordersPage.size == 10}">10</option>
                        <option value="20" th:selected="${ordersPage.size == 20}">20</option>
                        <option value="50" th:selected="${ordersPage.size == 50}">50</option>
                        <option value="100" th:selected="${ordersPage.size == 100}">100</option>
                    </select>
                    
                    <!-- Pagination -->
                    <ul class="pagination pagination-sm mb-0">
                        <li class="page-item" th:classappend="${ordersPage.first ? 'disabled' : ''}">
                            <a class="page-link" href="#" 
                               th:onclick="'changePage(' + ${ordersPage.number - 1} + ')'">
                                <i class="bi bi-chevron-left"></i>
                            </a>
                        </li>
                        
                        <li class="page-item" 
                            th:each="pageNum : ${#numbers.sequence(T(java.lang.Math).max(0, ordersPage.number - 2), T(java.lang.Math).min(ordersPage.totalPages - 1, ordersPage.number + 2))}"
                            th:classappend="${pageNum == ordersPage.number ? 'active' : ''}">
                            <a class="page-link" href="#" 
                               th:onclick="'changePage(' + ${pageNum} + ')'"
                               th:text="${pageNum + 1}">1</a>
                        </li>
                        
                        <li class="page-item" th:classappend="${ordersPage.last ? 'disabled' : ''}">
                            <a class="page-link" href="#" 
                               th:onclick="'changePage(' + ${ordersPage.number + 1} + ')'">
                                <i class="bi bi-chevron-right"></i>
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    </div>
</body>
</html>
```

---

## 📜 JavaScript Frontend

### 1. Gestion Principale (`orders-management.js`)
```javascript
/**
 * Gestionnaire principal pour l'interface d'administration des commandes
 */
const OrdersManagement = {
    
    config: {
        apiBase: '/api/admin/orders',
        wsEndpoint: '/ws/admin/orders',
        currentPage: 0,
        pageSize: 20,
        totalPages: 1,
        currentSort: 'createdAt,desc'
    },
    
    /**
     * Initialisation du module
     */
    init() {
        this.setupEventListeners();
        this.loadSavedFilters();
        this.initializeTooltips();
        this.setupKeyboardShortcuts();
    },
    
    /**
     * Configuration des événements
     */
    setupEventListeners() {
        // Sélection multiple
        document.getElementById('selectAllOrders')?.addEventListener('change', (e) => {
            this.toggleSelectAll(e.target.checked);
        });
        
        // Actions groupées
        document.getElementById('bulkActionsBtn')?.addEventListener('click', () => {
            this.showBulkActionsModal();
        });
        
        // Auto-refresh périodique
        setInterval(() => {
            if (document.visibilityState === 'visible') {
                this.refreshStats();
            }
        }, 30000); // 30 secondes
        
        // Gestion des raccourcis clavier
        document.addEventListener('keydown', (e) => {
            this.handleKeyboardShortcut(e);
        });
    },
    
    /**
     * Actualisation du tableau
     */
    async refreshTable() {
        try {
            this.showLoading(true);
            
            const filters = OrdersFilters.getCurrentFilters();
            const params = new URLSearchParams({
                page: this.config.currentPage,
                size: this.config.pageSize,
                sort: this.config.currentSort,
                ...filters
            });
            
            const response = await fetch(`${this.config.apiBase}/search?${params}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(filters)
            });
            
            if (response.ok) {
                const data = await response.json();
                this.updateTableContent(data);
                this.updatePagination(data);
            } else {
                this.showNotification('Erreur lors du chargement des données', 'error');
            }
        } catch (error) {
            console.error('Erreur refresh table:', error);
            this.showNotification('Erreur de connexion', 'error');
        } finally {
            this.showLoading(false);
        }
    },
    
    /**
     * Mise à jour du contenu du tableau
     */
    updateTableContent(data) {
        const tbody = document.querySelector('#ordersTable tbody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        if (data.content.length === 0) {
            this.showEmptyState();
            return;
        }
        
        data.content.forEach(order => {
            const row = this.createOrderRow(order);
            tbody.appendChild(row);
        });
        
        this.initializeRowActions();
    },
    
    /**
     * Création d'une ligne de commande
     */
    createOrderRow(order) {
        const row = document.createElement('tr');
        row.id = `order-row-${order.id}`;
        row.className = order.hasUnresolvedIssues ? 'table-warning' : '';
        
        row.innerHTML = `
            <td>
                <input type="checkbox" class="form-check-input order-checkbox" value="${order.id}">
            </td>
            <td>
                <a href="/admin/orders/${order.id}" class="text-decoration-none fw-bold">
                    #${order.id}
                </a>
                ${order.hasUnresolvedIssues ? '<span class="badge bg-warning text-dark ms-1"><i class="bi bi-exclamation-triangle"></i></span>' : ''}
            </td>
            <td>
                <div>
                    <span class="fw-semibold">${order.customerName}</span><br>
                    <small class="text-muted">${order.customerEmail}</small>
                </div>
            </td>
            <td>
                <span class="badge fs-6 ${order.statusBadgeClass}">${order.statusDisplayName}</span>
            </td>
            <td>
                <span class="fw-bold">${this.formatCurrency(order.totalAmount)}</span><br>
                <small class="text-muted">Payé: ${this.formatCurrency(order.totalPaid)}</small>
            </td>
            <td>
                <span>${this.formatDateTime(order.createdAt)}</span><br>
                <small class="text-muted">Il y a ${order.daysSinceCreation} jours</small>
            </td>
            <td>
                ${order.updatedAt ? this.formatDateTime(order.updatedAt) : '-'}<br>
                ${order.lastUpdatedBy ? `<small class="text-muted">Par: ${order.lastUpdatedBy}</small>` : ''}
            </td>
            <td>
                ${this.createActionButtons(order)}
            </td>
        `;
        
        return row;
    },
    
    /**
     * Création des boutons d'action
     */
    createActionButtons(order) {
        return `
            <div class="btn-group" role="group">
                <button type="button" class="btn btn-outline-primary btn-sm" 
                        onclick="OrdersManagement.viewOrderDetail(${order.id})" title="Voir détails">
                    <i class="bi bi-eye"></i>
                </button>
                ${order.canBeModified ? `
                    <button type="button" class="btn btn-outline-success btn-sm" 
                            onclick="OrdersManagement.editOrderStatus(${order.id})" title="Modifier statut">
                        <i class="bi bi-pencil"></i>
                    </button>
                ` : ''}
                ${order.canBeCancelled ? `
                    <button type="button" class="btn btn-outline-danger btn-sm" 
                            onclick="OrdersManagement.cancelOrder(${order.id})" title="Annuler">
                        <i class="bi bi-x-circle"></i>
                    </button>
                ` : ''}
                ${order.canBeRefunded ? `
                    <button type="button" class="btn btn-outline-warning btn-sm" 
                            onclick="OrdersManagement.refundOrder(${order.id})" title="Rembourser">
                        <i class="bi bi-arrow-return-left"></i>
                    </button>
                ` : ''}
                <div class="dropdown">
                    <button class="btn btn-outline-secondary btn-sm dropdown-toggle" 
                            type="button" data-bs-toggle="dropdown">
                        <i class="bi bi-three-dots"></i>
                    </button>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="#" onclick="OrdersManagement.addOrderNote(${order.id})">
                            <i class="bi bi-chat-text"></i> Ajouter note
                        </a></li>
                        <li><a class="dropdown-item" href="#" onclick="OrdersManagement.viewOrderHistory(${order.id})">
                            <i class="bi bi-clock-history"></i> Historique
                        </a></li>
                        <li><a class="dropdown-item" href="#" onclick="OrdersManagement.exportOrderData(${order.id})">
                            <i class="bi bi-download"></i> Exporter
                        </a></li>
                    </ul>
                </div>
            </div>
        `;
    },
    
    /**
     * Actions sur les commandes
     */
    async viewOrderDetail(orderId) {
        window.location.href = `/admin/orders/${orderId}`;
    },
    
    async editOrderStatus(orderId) {
        try {
            // Charger les données de la commande
            const response = await fetch(`${this.config.apiBase}/${orderId}`);
            const order = await response.json();
            
            // Afficher le modal de modification
            this.showStatusEditModal(order);
        } catch (error) {
            console.error('Erreur chargement commande:', error);
            this.showNotification('Erreur lors du chargement de la commande', 'error');
        }
    },
    
    async cancelOrder(orderId) {
        const result = await this.showConfirmationDialog(
            'Confirmer l\'annulation',
            'Êtes-vous sûr de vouloir annuler cette commande ? Cette action ne peut pas être annulée.'
        );
        
        if (!result.confirmed) return;
        
        try {
            const response = await fetch(`${this.config.apiBase}/${orderId}/cancel`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({
                    reason: result.reason || 'Annulation par administrateur'
                })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showNotification('Commande annulée avec succès', 'success');
                this.refreshTable();
            } else {
                this.showNotification(data.error || 'Erreur lors de l\'annulation', 'error');
            }
        } catch (error) {
            console.error('Erreur annulation:', error);
            this.showNotification('Erreur de connexion', 'error');
        }
    },
    
    async refundOrder(orderId) {
        try {
            // Charger les données de la commande
            const response = await fetch(`${this.config.apiBase}/${orderId}`);
            const order = await response.json();
            
            // Afficher le modal de remboursement
            this.showRefundModal(order);
        } catch (error) {
            console.error('Erreur chargement commande:', error);
            this.showNotification('Erreur lors du chargement de la commande', 'error');
        }
    },
    
    async addOrderNote(orderId) {
        const note = await this.showNoteDialog('Ajouter une note à la commande');
        if (!note) return;
        
        try {
            const response = await fetch(`${this.config.apiBase}/${orderId}/notes`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({ note })
            });
            
            const data = await response.json();
            
            if (data.success) {
                this.showNotification('Note ajoutée avec succès', 'success');
                this.refreshTable();
            } else {
                this.showNotification(data.error || 'Erreur lors de l\'ajout de la note', 'error');
            }
        } catch (error) {
            console.error('Erreur ajout note:', error);
            this.showNotification('Erreur de connexion', 'error');
        }
    },
    
    /**
     * Gestion des notifications
     */
    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(notification);
        
        // Auto-suppression après 5 secondes
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    },
    
    /**
     * Utilitaires de formatage
     */
    formatCurrency(amount) {
        return new Intl.NumberFormat('fr-CA', {
            style: 'currency',
            currency: 'CAD'
        }).format(amount);
    },
    
    formatDateTime(dateTime) {
        return new Intl.DateTimeFormat('fr-CA', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(new Date(dateTime));
    },
    
    /**
     * Gestion du loading
     */
    showLoading(show) {
        const loader = document.getElementById('tableLoader');
        if (loader) {
            loader.style.display = show ? 'block' : 'none';
        }
        
        const table = document.getElementById('ordersTable');
        if (table) {
            table.style.opacity = show ? '0.5' : '1';
        }
    }
};

// Fonctions globales pour les événements onclick
function refreshTable() {
    OrdersManagement.refreshTable();
}

function sortTable(column) {
    OrdersManagement.sortTable(column);
}

function changePage(page) {
    OrdersManagement.changePage(page);
}

function changePageSize(size) {
    OrdersManagement.changePageSize(size);
}

function exportData() {
    OrdersManagement.exportData();
}
```

---

Ce guide d'implémentation fournit tous les éléments techniques nécessaires pour développer la vue d'administration des commandes complète avec toutes les fonctionnalités avancées demandées.