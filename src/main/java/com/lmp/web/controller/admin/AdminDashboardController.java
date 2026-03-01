package com.lmp.web.controller.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.service.user.UserService;
import com.lmp.service.admin.OrderAdminService;
import com.lmp.service.system.SystemConfigService;

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

    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderAdminService orderAdminService;

    @Autowired
    private SystemConfigService systemConfigService;

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