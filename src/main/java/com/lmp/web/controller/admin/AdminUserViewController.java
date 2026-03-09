package com.lmp.web.controller.admin;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lmp.domain.entity.User;
import com.lmp.domain.entity.Appointment;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.OrderRepository;
import com.lmp.repository.ReviewRepository;
import com.lmp.repository.AppointmentRepository;
import com.lmp.service.user.UserService;
import java.util.List;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur pour la gestion des utilisateurs via interface web admin
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserViewController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserViewController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + AdminUserViewController.class.getName());

    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;

    /**
     * Affiche la page de gestion des utilisateurs
     */
    @GetMapping
    public String showUsersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "registrationDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {
        
        try {
            // Configuration pagination et tri
            Sort sort = Sort.by(sortDir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Filtrage des utilisateurs
            Page<User> usersPage;
            if (status != null && !status.isEmpty()) {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                usersPage = userService.findByStatus(userStatus, pageable);
            } else if (search != null && !search.isEmpty()) {
                usersPage = userService.findByEmailContaining(search, pageable);
            } else {
                usersPage = userService.findAll(pageable);
            }
            
            // Statistiques rapides
            long totalUsers = userService.count();
            long activeUsers = userService.countByStatus(UserStatus.ACTIVE);
            long inactiveUsers = userService.countByStatus(UserStatus.INACTIVE);
            long lockedUsers = userService.countByAccountLocked(true);
            
            // Ajout au modèle
            model.addAttribute("usersPage", usersPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalElements", usersPage.getTotalElements());
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentSearch", search);
            model.addAttribute("currentSort", sortBy);
            model.addAttribute("currentSortDir", sortDir);
            
            // Statistiques
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("inactiveUsers", inactiveUsers);
            model.addAttribute("lockedUsers", lockedUsers);
            
            logger.info("Users page loaded successfully - Total: {}, Page: {}/{}", 
                       usersPage.getTotalElements(), page + 1, usersPage.getTotalPages());
            
            return "admin/users";
            
        } catch (Exception e) {
            logger.error("Error loading users page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des utilisateurs: " + e.getMessage());
            return "admin/users";
        }
    }

    /**
     * Active un utilisateur
     */
    @PostMapping("/{id}/activate")
    @ResponseBody
    public ResponseEntity<?> activateUser(@PathVariable java.util.UUID id, HttpServletRequest request) {
        logger.info("🔍 ADMIN DEBUG - Tentative activation utilisateur ID: {}", id);
        logger.info("🔍 ADMIN DEBUG - Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        logger.info("🔍 ADMIN DEBUG - Headers: {}",
                   java.util.Collections.list(request.getHeaderNames()).stream()
                   .collect(java.util.stream.Collectors.toMap(h -> h, request::getHeader)));
        
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setStatus(UserStatus.ACTIVE);
            user.setAccountLocked(false);
            userService.save(user);
            
            auditLogger.info("User activated - ID: {}, Email: {}", id, user.getEmail());
            logger.info("✅ ADMIN DEBUG - Activation utilisateur réussie: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur activé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur activation utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Désactive un utilisateur
     */
    @PostMapping("/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<?> deactivateUser(@PathVariable java.util.UUID id, HttpServletRequest request) {
        logger.info("🔍 ADMIN DEBUG - Tentative désactivation utilisateur ID: {}", id);
        logger.info("🔍 ADMIN DEBUG - Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setStatus(UserStatus.INACTIVE);
            userService.save(user);
            
            auditLogger.info("User deactivated - ID: {}, Email: {}", id, user.getEmail());
            logger.info("✅ ADMIN DEBUG - Désactivation utilisateur réussie: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur désactivé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur désactivation utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Verrouille un compte utilisateur
     */
    @PostMapping("/{id}/lock")
    @ResponseBody
    public ResponseEntity<?> lockUser(@PathVariable java.util.UUID id, HttpServletRequest request) {
        logger.info("🔍 ADMIN DEBUG - Tentative verrouillage utilisateur ID: {}", id);
        logger.info("🔍 ADMIN DEBUG - Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setAccountLocked(true);
            userService.save(user);
            
            auditLogger.info("User account locked - ID: {}, Email: {}", id, user.getEmail());
            logger.info("✅ ADMIN DEBUG - Verrouillage utilisateur réussi: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Compte verrouillé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur verrouillage utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Déverrouille un compte utilisateur
     */
    @PostMapping("/{id}/unlock")
    @ResponseBody
    public ResponseEntity<?> unlockUser(@PathVariable java.util.UUID id, HttpServletRequest request) {
        logger.info("🔍 ADMIN DEBUG - Tentative déverrouillage utilisateur ID: {}", id);
        logger.info("🔍 ADMIN DEBUG - Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            user.setAccountLocked(false);
            userService.save(user);
            
            auditLogger.info("User account unlocked - ID: {}, Email: {}", id, user.getEmail());
            logger.info("✅ ADMIN DEBUG - Déverrouillage utilisateur réussi: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Compte déverrouillé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur déverrouillage utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Change le mot de passe d'un utilisateur (action administrateur)
     */
    @PostMapping("/{id}/change-password")
    @ResponseBody
    public ResponseEntity<?> changeUserPassword(@PathVariable java.util.UUID id,
                                               @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        logger.info("🔐 DEBUG ADMIN PASSWORD - Début changement mot de passe admin pour user ID: {}", id);
        
        try {
            // Log de l'authentification
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("🔐 DEBUG ADMIN PASSWORD - Admin non authentifié: auth={}", authentication);
                return ResponseEntity.status(401)
                    .body("{\"success\": false, \"message\": \"Administrateur non authentifié\"}");
            }
            
            logger.info("🔐 DEBUG ADMIN PASSWORD - Admin authentifié: {}", authentication.getName());
            
            // Log de la validation request
            logger.info("🔐 DEBUG ADMIN PASSWORD - Request: newPassword={}, confirmPassword={}, passwordMatching={}",
                       request.getNewPassword() != null ? "présent" : "absent",
                       request.getConfirmPassword() != null ? "présent" : "absent",
                       request.isPasswordMatching());
            
            // Validation basique
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                logger.warn("🔐 DEBUG ADMIN PASSWORD - Nouveau mot de passe vide");
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Le nouveau mot de passe ne peut pas être vide\"}");
            }
            
            if (!request.isPasswordMatching()) {
                logger.warn("🔐 DEBUG ADMIN PASSWORD - Mots de passe ne correspondent pas");
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Les mots de passe ne correspondent pas\"}");
            }

            // Récupérer l'admin actuel
            logger.info("🔐 DEBUG ADMIN PASSWORD - Recherche admin par email: {}", authentication.getName());
            User admin = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé"));
            
            logger.info("🔐 DEBUG ADMIN PASSWORD - Admin trouvé: ID={}, Email={}", admin.getId(), admin.getEmail());
            
            // Récupérer l'utilisateur cible
            logger.info("🔐 DEBUG ADMIN PASSWORD - Recherche utilisateur cible ID: {}", id);
            User targetUser = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            logger.info("🔐 DEBUG ADMIN PASSWORD - Utilisateur cible trouvé: ID={}, Email={}",
                       targetUser.getId(), targetUser.getEmail());
            
            // Changer le mot de passe via l'admin
            logger.info("🔐 DEBUG ADMIN PASSWORD - Appel changePasswordByAdmin: userId={}, adminId={}",
                       id, admin.getId());
            userService.changePasswordByAdmin(id, request.getNewPassword(), admin.getId());
            
            logger.info("✅ DEBUG ADMIN PASSWORD - Mot de passe changé avec succès par admin {} pour user {}",
                       admin.getEmail(), targetUser.getEmail());
            auditLogger.info("Admin password change - Admin: {} changed password for User: {} (ID: {})",
                           admin.getEmail(), targetUser.getEmail(), id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Mot de passe changé avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ DEBUG ADMIN PASSWORD - Erreur changement mot de passe user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Supprime définitivement un utilisateur de la base de données (HARD DELETE)
     * ⚠️ ATTENTION: Cette action est IRRÉVERSIBLE !
     */
    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable java.util.UUID id, HttpServletRequest request) {
        logger.error("🚨 ADMIN DEBUG - Tentative suppression DÉFINITIVE utilisateur ID: {}", id);
        logger.info("🔍 ADMIN DEBUG - Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        
        try {
            // Vérifier que l'utilisateur existe avant suppression
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            logger.error("⚠️ ADMIN DEBUG - Utilisateur à supprimer DÉFINITIVEMENT: ID={}, Email={}", id, user.getEmail());
            
            // Utiliser la méthode deleteUser du service qui fait maintenant du HARD DELETE
            userService.deleteUser(id);
            
            auditLogger.error("User HARD DELETED (PERMANENT) - ID: {}, Email: {}", id, user.getEmail());
            logger.error("🗑️ ADMIN DEBUG - Suppression DÉFINITIVE utilisateur terminée: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur supprimé définitivement (irréversible)\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur suppression DÉFINITIVE utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    /**
     * Debug endpoint pour tester les contraintes de base de données
     */
    @GetMapping("/{id}/debug-hard-delete")
    @ResponseBody
    public ResponseEntity<?> debugHardDelete(@PathVariable java.util.UUID id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            StringBuilder debug = new StringBuilder();
            debug.append("DEBUG INFO pour utilisateur ID: ").append(id).append("\n");
            debug.append("Email: ").append(user.getEmail()).append("\n");
            
            // Compter les données liées
            var orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            var reviews = reviewRepository.findByUser(user);
            
            debug.append("Commandes liées: ").append(orders.size()).append("\n");
            debug.append("Avis liés: ").append(reviews.size()).append("\n");
            
            // Tester l'anonymisation d'une commande
            if (!orders.isEmpty()) {
                var firstOrder = orders.get(0);
                debug.append("Test order ID: ").append(firstOrder.getId()).append("\n");
                debug.append("Order user_id avant: ").append(firstOrder.getUser() != null ? firstOrder.getUser().getId() : "NULL").append("\n");
                
                // Tenter l'anonymisation
                firstOrder.setUser(null);
                try {
                    orderRepository.save(firstOrder);
                    debug.append("✅ Test anonymisation commande réussie\n");
                    
                    // Remettre en place pour ne pas casser les données
                    firstOrder.setUser(user);
                    orderRepository.save(firstOrder);
                } catch (Exception e) {
                    debug.append("❌ Erreur anonymisation commande: ").append(e.getMessage()).append("\n");
                }
            }
            
            return ResponseEntity.ok().body("{\"debug\": \"" + debug.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}");
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("{\"error\": \"" + e.getMessage().replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
        }
    }
    

    /**
     * Endpoint de test pour diagnostiquer les problèmes d'authentification
     */
    @GetMapping("/test-auth")
    @ResponseBody
    public ResponseEntity<?> testAuth() {
        try {
            logger.info("🧪 TEST AUTH - Endpoint accessible");
            return ResponseEntity.ok().body("\"{\\\"success\\\": true, \\\"message\\\": \\\"Authentification OK\\\"}\"");
        } catch (Exception e) {
            logger.error("❌ TEST AUTH - Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("\"{\\\"success\\\": false, \\\"message\\\": \\\"" + e.getMessage() + "\\\"}\"");
        }
    }
    
    /**
     * DTO pour les détails utilisateur
     */
    public static class UserDetailsDto {
        private java.util.UUID id;
        private String email;
        private String displayName;
        private String phone;
        private String address;
        private String city;
        private String postalCode;
        private String country;
        private String companyName;
        private String status;
        private Boolean accountLocked;
        private Boolean emailVerified;
        private String registrationDate;
        private String lastLoginDate;

        public UserDetailsDto(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.displayName = user.getDisplayName();
            this.phone = user.getPhone();
            this.address = user.getAddress();
            this.city = user.getCity();
            this.postalCode = user.getPostalCode();
            this.country = user.getCountry();
            this.companyName = user.getCompanyName();
            this.status = user.getStatus().name();
            this.accountLocked = user.getAccountLocked();
            this.emailVerified = user.getEmailVerified();
            this.registrationDate = user.getRegistrationDate() != null ? 
                user.getRegistrationDate().toString() : null;
            this.lastLoginDate = user.getLastLoginDate() != null ? 
                user.getLastLoginDate().toString() : null;
        }

        // Getters
        public java.util.UUID getId() { return id; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public String getCity() { return city; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }
        public String getCompanyName() { return companyName; }
        public String getStatus() { return status; }
        public Boolean getAccountLocked() { return accountLocked; }
        public Boolean getEmailVerified() { return emailVerified; }
        public String getRegistrationDate() { return registrationDate; }
        public String getLastLoginDate() { return lastLoginDate; }
    }

    /**
     * Affiche les détails d'un utilisateur
     */
    @GetMapping("/{id}/details")
    @ResponseBody
    public ResponseEntity<?> getUserDetails(@PathVariable java.util.UUID id) {
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            // Créer un DTO avec les détails
            UserDetailsDto details = new UserDetailsDto(user);
            
            return ResponseEntity.ok(details);
            
        } catch (Exception e) {
            logger.error("Error fetching user details {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Met à jour les informations d'un utilisateur
     */
    @PostMapping("/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateUser(@PathVariable java.util.UUID id, @RequestBody UserUpdateRequest request, Authentication authentication) {
        logger.info("🔄 ADMIN DEBUG - Mise à jour utilisateur ID: {}", id);
        
        try {
            // Vérifier l'authentification
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("🔄 ADMIN DEBUG - Admin non authentifié");
                return ResponseEntity.status(401)
                    .body("{\"success\": false, \"message\": \"Administrateur non authentifié\"}");
            }
            
            // Récupérer l'utilisateur à modifier
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
                
            String originalEmail = user.getEmail();
            logger.info("🔄 ADMIN DEBUG - Utilisateur à modifier: ID={}, Email={}", id, originalEmail);
            
            // Mettre à jour les champs si fournis
            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
                user.setFirstName(request.getFirstName().trim());
                logger.info("🔄 ADMIN DEBUG - Nouveau prénom: {}", request.getFirstName());
            }
            
            if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
                user.setLastName(request.getLastName().trim());
                logger.info("🔄 ADMIN DEBUG - Nouveau nom: {}", request.getLastName());
            }
            
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                String newEmail = request.getEmail().trim().toLowerCase();
                // Vérifier que l'email n'est pas déjà utilisé par un autre utilisateur
                Optional<User> existingUser = userService.findByEmail(newEmail);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    return ResponseEntity.badRequest()
                        .body("{\"success\": false, \"message\": \"Cet email est déjà utilisé par un autre utilisateur\"}");
                }
                user.setEmail(newEmail);
                logger.info("🔄 ADMIN DEBUG - Nouvel email: {}", newEmail);
            }
            
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
                logger.info("🔄 ADMIN DEBUG - Nouveau téléphone: {}", user.getPhone());
            }
            
            if (request.getCompanyName() != null) {
                user.setCompanyName(request.getCompanyName().trim().isEmpty() ? null : request.getCompanyName().trim());
                logger.info("🔄 ADMIN DEBUG - Nouvelle entreprise: {}", user.getCompanyName());
            }
            
            // Sauvegarder les modifications
            userService.save(user);
            
            auditLogger.info("User updated by admin - ID: {}, Original Email: {}, New Email: {}, Admin: {}", 
                           id, originalEmail, user.getEmail(), authentication.getName());
            logger.info("✅ ADMIN DEBUG - Utilisateur mis à jour avec succès: {}", id);
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Utilisateur mis à jour avec succès\"}");
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur mise à jour utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
    
    /**
     * Récupère les rendez-vous d'un utilisateur
     */
    @GetMapping("/{id}/appointments")
    @ResponseBody
    public ResponseEntity<?> getUserAppointments(@PathVariable java.util.UUID id) {
        logger.info("📅 ADMIN DEBUG - Récupération rendez-vous utilisateur ID: {}", id);
        
        try {
            User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            List<Appointment> appointments = appointmentRepository.findByUserOrderByAppointmentDateDesc(user);
            logger.info("📅 ADMIN DEBUG - Trouvé {} rendez-vous pour l'utilisateur {}", appointments.size(), user.getEmail());
            
            // Convertir en DTO pour l'affichage
            List<AppointmentDto> appointmentDtos = appointments.stream()
                .map(AppointmentDto::new)
                .toList();
            
            return ResponseEntity.ok(appointmentDtos);
            
        } catch (Exception e) {
            logger.error("❌ ADMIN DEBUG - Erreur récupération rendez-vous utilisateur {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
    
    /**
     * DTO pour les requêtes de mise à jour utilisateur
     */
    public static class UserUpdateRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String companyName;
        
        public UserUpdateRequest() {}
        
        // Getters et Setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
    }
    
    /**
     * DTO pour les rendez-vous
     */
    public static class AppointmentDto {
        private java.util.UUID id;
        private String appointmentDate;
        private String status;
        private String adminNotes;
        private String clientName;
        private String clientEmail;
        private String clientPhone;
        
        public AppointmentDto(Appointment appointment) {
            this.id = appointment.getId();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            this.appointmentDate = appointment.getAppointmentDate() != null ? 
                appointment.getAppointmentDate().format(formatter) : "N/A";
            this.status = appointment.getStatus() != null ? appointment.getStatus().toString() : "N/A";
            this.adminNotes = appointment.getAdminNotes();
            this.clientName = appointment.getClientName();
            this.clientEmail = appointment.getClientEmail();
            this.clientPhone = appointment.getClientPhone();
        }
        
        // Getters
        public java.util.UUID getId() { return id; }
        public String getAppointmentDate() { return appointmentDate; }
        public String getStatus() { return status; }
        public String getAdminNotes() { return adminNotes; }
        public String getClientName() { return clientName; }
        public String getClientEmail() { return clientEmail; }
        public String getClientPhone() { return clientPhone; }
    }

    /**
     * DTO pour les requêtes de changement de mot de passe
     */
    public static class ChangePasswordRequest {
        private String newPassword;
        private String confirmPassword;

        public ChangePasswordRequest() {}

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        
        public boolean isPasswordMatching() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }
    }
}