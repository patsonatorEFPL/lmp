package com.lmp.portal.admin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.service.UserService;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.service.admin.OrderAdminService;
import com.lmp.crm.domain.AppointmentStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.catalog.repository.ServiceRepository;
import com.lmp.shared.service.SystemConfigService;

/**
 * Contrôleur pour le tableau de bord administrateur.
 * Accessible uniquement aux utilisateurs ayant le rôle ADMIN.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + AdminDashboardController.class.getName());

        private final UserService userService;
    
        private final OrderAdminService orderAdminService;

        private final SystemConfigService systemConfigService;

        private final OrderRepository orderRepository;

        private final AppointmentRepository appointmentRepository;

        private final ServiceRepository serviceRepository;


    public AdminDashboardController(UserService userService,
                           OrderAdminService orderAdminService,
                           SystemConfigService systemConfigService,
                           OrderRepository orderRepository,
                           AppointmentRepository appointmentRepository,
                           ServiceRepository serviceRepository) {
        this.userService = userService;
        this.orderAdminService = orderAdminService;
        this.systemConfigService = systemConfigService;
        this.orderRepository = orderRepository;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Affiche le tableau de bord administrateur avec les statistiques globales.
     * Peut également gérer les intentions de paiement transférées depuis /dashboard.
     *
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @param processPurchase Paramètre indiquant qu'il faut traiter un paiement transféré
     * @param request La requête HTTP pour récupérer l'intention de paiement
     * @return Le nom de la vue admin dashboard
     */
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model,
                                   Authentication authentication,
                                   @RequestParam(name = "processPurchase", required = false) Boolean processPurchase,
                                   HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt to admin dashboard");
            return "redirect:/login";
        }

        String adminEmail = authentication.getName();
        logger.info("Admin dashboard accessed by: {}", adminEmail);
        auditLogger.info("Admin dashboard access by user: {}", adminEmail);

        try {
            // Récupérer l'administrateur actuel
            User admin = userService.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Administrateur non trouvé"));

            // Notification si intention de paiement transférée
            if (Boolean.TRUE.equals(processPurchase)) {
                logger.info("Admin {} has purchase intent transferred from user dashboard", adminEmail);
                model.addAttribute("processPurchase", true);
                model.addAttribute("purchaseTransferNotice",
                    "Une intention de paiement a été détectée lors de votre connexion. Vous pouvez la traiter depuis l'interface utilisateur si nécessaire.");
            }

            // Statistiques globales des utilisateurs
            List<User> allUsers = userService.findAll();
            
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream()
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .count();
            long inactiveUsers = allUsers.stream()
                    .filter(user -> user.getStatus() == UserStatus.INACTIVE)
                    .count();
            
            // Utilisateurs verrouillés ou avec des problèmes
            List<User> lockedUsers = allUsers.stream()
                    .filter(user -> user.getStatus() == UserStatus.INACTIVE ||
                                   Boolean.TRUE.equals(user.getAccountLocked()))
                    .limit(5)
                    .toList();
            
            // Utilisateurs récents (les 10 derniers inscrits)
            List<User> recentUsers = allUsers.stream()
                    .filter(user -> user.getRegistrationDate() != null)
                    .sorted((u1, u2) -> u2.getRegistrationDate().compareTo(u1.getRegistrationDate()))
                    .limit(10)
                    .toList();

            // Récupérer les statistiques des commandes
            Long totalOrders = 0L;
            Double totalRevenue = 0.0;
            try {
                totalOrders = orderAdminService.getTotalOrdersCount();
                totalRevenue = orderAdminService.getTotalRevenue();
            } catch (Exception e) {
                logger.warn("Could not load order statistics: {}", e.getMessage());
                // En cas d'erreur, utiliser des valeurs par défaut
                totalOrders = 0L;
                totalRevenue = 0.0;
            }

            // Ajout des données au modèle
            model.addAttribute("admin", admin);
            model.addAttribute("currentUserDisplayName", admin.getDisplayName());
            String initials = admin.getDisplayName() != null && !admin.getDisplayName().isEmpty()
                    ? admin.getDisplayName().substring(0, 1).toUpperCase() : "A";
            model.addAttribute("currentUserInitials", initials);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("inactiveUsers", inactiveUsers);
            model.addAttribute("lockedUsersCount", lockedUsers.size());
            model.addAttribute("lockedUsers", lockedUsers);
            model.addAttribute("recentUsers", recentUsers);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalRevenue", totalRevenue);

            // Devise par défaut pour l'affichage
            String defaultCurrency = systemConfigService.getProperty("payment.default.currency", "EUR");
            model.addAttribute("defaultCurrency", defaultCurrency);

            // ── Enhanced dashboard data ──

            // Orders by status for donut chart
            long pendingOrders = 0;
            long confirmedOrders = 0;
            long completedOrders = 0;
            long cancelledOrders = 0;
            try {
                pendingOrders = orderAdminService.getOrdersCountByStatus(OrderStatus.PENDING)
                              + orderAdminService.getOrdersCountByStatus(OrderStatus.IN_PROGRESS)
                              + orderAdminService.getOrdersCountByStatus(OrderStatus.PROCESSING);
                confirmedOrders = orderAdminService.getOrdersCountByStatus(OrderStatus.CONFIRMED)
                                + orderAdminService.getOrdersCountByStatus(OrderStatus.SHIPPED);
                completedOrders = orderAdminService.getOrdersCountByStatus(OrderStatus.COMPLETED)
                                + orderAdminService.getOrdersCountByStatus(OrderStatus.DELIVERED);
                cancelledOrders = orderAdminService.getOrdersCountByStatus(OrderStatus.CANCELLED)
                                + orderAdminService.getOrdersCountByStatus(OrderStatus.REFUNDED);
            } catch (Exception e) {
                logger.warn("Could not load order status counts: {}", e.getMessage());
            }
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("confirmedOrders", confirmedOrders);
            model.addAttribute("completedOrders", completedOrders);
            model.addAttribute("cancelledOrders", cancelledOrders);

            // Monthly revenue data for chart (last 6 months)
            List<Map<String, Object>> monthlyRevenueData = new ArrayList<>();
            try {
                LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                List<Object[]> monthlyStats = orderRepository.getMonthlyOrderStats(sixMonthsAgo);
                String[] monthNames = {"", "Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};
                for (Object[] stat : monthlyStats) {
                    Map<String, Object> entry = new HashMap<>();
                    int monthIndex = ((Number) stat[1]).intValue();
                    entry.put("month", monthNames[monthIndex]);
                    entry.put("count", ((Number) stat[2]).longValue());
                    entry.put("revenue", stat[3] != null ? ((Number) stat[3]).doubleValue() : 0.0);
                    monthlyRevenueData.add(entry);
                }
            } catch (Exception e) {
                logger.warn("Could not load monthly revenue data: {}", e.getMessage());
            }
            // Reverse to chronological order
            java.util.Collections.reverse(monthlyRevenueData);
            model.addAttribute("monthlyRevenueData", monthlyRevenueData);

            // Appointments stats
            long totalAppointments = 0;
            long pendingAppointments = 0;
            long upcomingAppointmentsCount = 0;
            try {
                totalAppointments = appointmentRepository.count();
                pendingAppointments = appointmentRepository.countByStatus(AppointmentStatus.PENDING)
                                   + appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED);
                List<?> upcoming = appointmentRepository.findActiveAppointments();
                upcomingAppointmentsCount = upcoming.size();
            } catch (Exception e) {
                logger.warn("Could not load appointment stats: {}", e.getMessage());
            }
            model.addAttribute("totalAppointments", totalAppointments);
            model.addAttribute("pendingAppointments", pendingAppointments);
            model.addAttribute("upcomingAppointmentsCount", upcomingAppointmentsCount);

            // Recent orders (last 5)
            List<Map<String, String>> recentOrdersList = new ArrayList<>();
            try {
                var latestOrders = orderRepository.findLatestOrders(PageRequest.of(0, 5));
                for (var order : latestOrders) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("id", order.getId() != null ? order.getId().toString() : "");
                    entry.put("customerEmail", order.getUser() != null ? order.getUser().getEmail() : "N/A");
                    entry.put("customerName", order.getUser() != null ? order.getUser().getDisplayName() : "N/A");
                    entry.put("amount", order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0");
                    entry.put("status", order.getStatus() != null ? order.getStatus().name() : "PENDING");
                    entry.put("createdAt", order.getCreatedAt() != null ?
                        order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
                    entry.put("serviceName", order.getServiceName() != null ? order.getServiceName() : "—");
                    recentOrdersList.add(entry);
                }
            } catch (Exception e) {
                logger.warn("Could not load recent orders: {}", e.getMessage());
            }
            model.addAttribute("recentOrdersList", recentOrdersList);

            // Total services count
            long totalServices = 0;
            try {
                totalServices = serviceRepository.count();
            } catch (Exception e) {
                logger.warn("Could not load service count: {}", e.getMessage());
            }
            model.addAttribute("totalServices", totalServices);

            // Monthly revenue for current month
            double monthlyRevenue = 0.0;
            try {
                LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                monthlyRevenue = orderAdminService.getRevenueForPeriod(startOfMonth, LocalDateTime.now());
            } catch (Exception e) {
                logger.warn("Could not load monthly revenue: {}", e.getMessage());
            }
            model.addAttribute("monthlyRevenue", monthlyRevenue);

            logger.info("Admin dashboard loaded successfully for: {} - Stats: Total={}, Active={}, Inactive={}, Locked={}", 
                       adminEmail, totalUsers, activeUsers, inactiveUsers, lockedUsers.size());

            return "admin/dashboard";

        } catch (Exception e) {
            logger.error("Error loading admin dashboard for user {}: {}", adminEmail, e.getMessage(), e);
            auditLogger.error("Admin dashboard error for user {}: {}", adminEmail, e.getMessage());
            model.addAttribute("errorMessage", "Erreur lors du chargement du tableau de bord administrateur : " + e.getMessage());
            return "error/500";
        }
    }

    /**
     * Redirect root admin path to dashboard.
     *
     * @return Redirection vers le dashboard admin
     */
    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    /**
     * Affiche la page de statistiques administrateur.
     *
     * @param model Le modèle pour la vue
     * @param authentication L'authentification actuelle
     * @return Le nom de la vue statistiques admin
     */
    @GetMapping("/statistics")
    public String showStatistics(Model model, Authentication authentication) {
        String adminEmail = authentication.getName();
        logger.info("Admin statistics accessed by: {}", adminEmail);
        auditLogger.info("Admin statistics access by user: {}", adminEmail);

        try {
            // Récupérer les statistiques avancées
            List<User> allUsers = userService.findAll();
            
            // Statistiques par mois (exemple de données)
            // Dans une vraie application, ces données viendraient de la base de données
            model.addAttribute("monthlyRegistrations", List.of(12, 19, 15, 25, 22, 30));
            model.addAttribute("monthlyActivity", List.of(85, 92, 78, 95, 88, 94));
            
            // Répartition par statut (plus de statut DELETED car hard delete)
            long activeCount = allUsers.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
            long inactiveCount = allUsers.stream().filter(u -> u.getStatus() == UserStatus.INACTIVE).count();
            
            model.addAttribute("statusDistribution", List.of(activeCount, inactiveCount, 0L)); // 0 pour deleted (plus utilisé)
            model.addAttribute("totalUsers", allUsers.size());

            return "admin/statistics";

        } catch (Exception e) {
            logger.error("Error loading admin statistics for user {}: {}", adminEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des statistiques : " + e.getMessage());
            return "error/500";
        }
    }
}