package com.lmp.auth.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Contrôleur pour la gestion des mots de passe par les utilisateurs
 */
@Controller
@RequestMapping("/user/password")
public class UserPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(UserPasswordController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + UserPasswordController.class.getName());

    private final UserService userService;

    public UserPasswordController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Change le mot de passe de l'utilisateur connecté
     */
    @PostMapping("/change")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                          Authentication authentication,
                                          HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        logger.info("🔐 DEBUG PASSWORD - Début changement mot de passe utilisateur");
        
        try {
            // Log de l'authentification
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("🔐 DEBUG PASSWORD - Utilisateur non authentifié: auth={}", authentication);
                return ResponseEntity.status(401)
                    .body("{\"success\": false, \"message\": \"Utilisateur non authentifié\"}");
            }
            
            logger.info("🔐 DEBUG PASSWORD - Utilisateur authentifié: {}", authentication.getName());

            // Log de la validation des données
            logger.info("🔐 DEBUG PASSWORD - Validation request: isValid={}, isPasswordMatching={}",
                       request.isValid(), request.isPasswordMatching());
            logger.info("🔐 DEBUG PASSWORD - Request fields: currentPassword={}, newPassword={}, confirmPassword={}",
                       request.getCurrentPassword() != null ? "présent" : "absent",
                       request.getNewPassword() != null ? "présent" : "absent",
                       request.getConfirmPassword() != null ? "présent" : "absent");

            // Validation des données de la requête
            if (!request.isValid()) {
                logger.warn("🔐 DEBUG PASSWORD - Données de requête invalides");
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Données de requête invalides\"}");
            }

            if (!request.isPasswordMatching()) {
                logger.warn("🔐 DEBUG PASSWORD - Mots de passe ne correspondent pas");
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Les mots de passe ne correspondent pas\"}");
            }

            // Récupérer l'utilisateur actuel
            logger.info("🔐 DEBUG PASSWORD - Recherche utilisateur par email: {}", authentication.getName());
            User currentUser = userService.findByLogin(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            
            logger.info("🔐 DEBUG PASSWORD - Utilisateur trouvé: ID={}, Email={}",
                       currentUser.getId(), currentUser.getEmail());

            // Changer le mot de passe avec validation
            logger.info("🔐 DEBUG PASSWORD - Appel changePasswordWithValidation pour user ID: {}", currentUser.getId());
            userService.changePasswordWithValidation(
                currentUser.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
            );

            logger.info("✅ DEBUG PASSWORD - Mot de passe changé avec succès pour user: {}", currentUser.getEmail());
            auditLogger.info("User password changed - User: {} (ID: {})",
                           currentUser.getEmail(), currentUser.getId());

            // 🔐 SÉCURITÉ: Invalidation de la session pour forcer une nouvelle authentification
            logger.info("🔐 DEBUG PASSWORD - Invalidation de la session après changement de mot de passe");
            
            try {
                // Invalidation de la session actuelle (sans redirection automatique)
                httpRequest.getSession().invalidate();
                
                // Nettoyage du contexte de sécurité
                SecurityContextHolder.clearContext();
                
                logger.info("✅ DEBUG PASSWORD - Session invalidée avec succès: {}", currentUser.getEmail());
                auditLogger.info("Session invalidated after password change - User: {} (ID: {})",
                               currentUser.getEmail(), currentUser.getId());
                
            } catch (Exception sessionException) {
                logger.error("❌ DEBUG PASSWORD - Erreur lors de l'invalidation de session: {}", sessionException.getMessage(), sessionException);
                // Continuer malgré l'erreur pour ne pas affecter la réponse principale
            }

            return ResponseEntity.ok()
                .body("{\"success\": true, \"message\": \"Mot de passe changé avec succès. Vous allez être redirigé vers la page de connexion.\", \"requiresReauth\": true, \"redirectToLogin\": true}");

        } catch (Exception e) {
            logger.error("❌ DEBUG PASSWORD - Erreur changement mot de passe: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Valide la force d'un mot de passe
     */
    @PostMapping("/validate")
    @ResponseBody
    public ResponseEntity<?> validatePassword(@RequestBody ValidatePasswordRequest request) {
        try {
            boolean isStrong = userService.isPasswordStrong(request.getPassword());
            
            return ResponseEntity.ok()
                .body("{\"isStrong\": " + isStrong + ", \"message\": \"" + 
                      (isStrong ? "Mot de passe valide" : "Mot de passe trop faible") + "\"}");

        } catch (Exception e) {
            logger.error("Error validating password: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Erreur lors de la validation\"}");
        }
    }

    /**
     * Vérifie le mot de passe actuel
     */
    @PostMapping("/check-current")
    @ResponseBody
    public ResponseEntity<?> checkCurrentPassword(@RequestBody CheckCurrentPasswordRequest request,
                                                 Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body("{\"success\": false, \"message\": \"Utilisateur non authentifié\"}");
            }

            User currentUser = userService.findByLogin(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            boolean isValid = userService.checkCurrentPassword(currentUser.getId(), request.getCurrentPassword());

            return ResponseEntity.ok()
                .body("{\"isValid\": " + isValid + "}");

        } catch (Exception e) {
            logger.error("Error checking current password: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"Erreur lors de la vérification\"}");
        }
    }

    /**
     * DTO pour les requêtes de changement de mot de passe
     */
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;

        public ChangePasswordRequest() {}

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        public boolean isPasswordMatching() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }

        public boolean isValid() {
            return currentPassword != null && !currentPassword.trim().isEmpty() &&
                   newPassword != null && !newPassword.trim().isEmpty() &&
                   confirmPassword != null && !confirmPassword.trim().isEmpty();
        }
    }

    /**
     * DTO pour la validation de mot de passe
     */
    public static class ValidatePasswordRequest {
        private String password;

        public ValidatePasswordRequest() {}

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * DTO pour vérifier le mot de passe actuel
     */
    public static class CheckCurrentPasswordRequest {
        private String currentPassword;

        public CheckCurrentPasswordRequest() {}

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    }
}