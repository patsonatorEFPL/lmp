package com.lmp.shared.web.admin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lmp.shared.service.SystemConfigService;
import com.lmp.shared.dto.SystemSettingsDto;

import jakarta.validation.Valid;

/**
 * Contrôleur pour la gestion des paramètres système
 */
@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SystemSettingsController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT." + SystemSettingsController.class.getName());

        private final SystemConfigService systemConfigService;


    public SystemSettingsController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * Affiche la page des paramètres système
     */
    @GetMapping
    public String showSettingsPage(Model model) {
        try {
            // Charger les paramètres actuels
            SystemSettingsDto settings = systemConfigService.loadAllSettings();
            model.addAttribute("settings", settings);
            
            // Statistiques de configuration
            Map<String, Object> configStats = systemConfigService.getConfigurationStats();
            model.addAttribute("configStats", configStats);
            
            logger.info("Settings page loaded successfully");
            return "admin/settings";
            
        } catch (Exception e) {
            logger.error("Error loading settings page: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Erreur lors du chargement des paramètres: " + e.getMessage());
            
            // Créer un objet settings par défaut pour éviter les erreurs de template
            model.addAttribute("settings", new SystemSettingsDto());
            return "admin/settings";
        }
    }

    /**
     * Sauvegarde les paramètres système
     */
    @PostMapping("/save")
    public String saveSettings(@Valid @ModelAttribute("settings") SystemSettingsDto settings,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        try {
            if (bindingResult.hasErrors()) {
                // Recharger les statistiques en cas d'erreur
                Map<String, Object> configStats = systemConfigService.getConfigurationStats();
                model.addAttribute("configStats", configStats);
                model.addAttribute("errorMessage", "Veuillez corriger les erreurs dans le formulaire");
                return "admin/settings";
            }
            
            // Sauvegarder les paramètres
            systemConfigService.saveSettings(settings);
            
            auditLogger.info("System settings saved successfully");
            redirectAttributes.addFlashAttribute("successMessage", 
                "Paramètres système sauvegardés avec succès");
            
            return "redirect:/admin/settings";
            
        } catch (Exception e) {
            logger.error("Error saving settings: {}", e.getMessage(), e);
            
            // Recharger les statistiques en cas d'erreur
            Map<String, Object> configStats = systemConfigService.getConfigurationStats();
            model.addAttribute("configStats", configStats);
            model.addAttribute("errorMessage", "Erreur lors de la sauvegarde: " + e.getMessage());
            return "admin/settings";
        }
    }

    /**
     * Réinitialise les paramètres aux valeurs par défaut
     */
    @PostMapping("/reset")
    public String resetToDefaults(RedirectAttributes redirectAttributes) {
        try {
            SystemSettingsDto defaults = systemConfigService.resetToDefaults();
            
            auditLogger.info("System settings reset to defaults");
            redirectAttributes.addFlashAttribute("successMessage", 
                "Paramètres réinitialisés aux valeurs par défaut");
            redirectAttributes.addFlashAttribute("settings", defaults);
            
            return "redirect:/admin/settings";
            
        } catch (Exception e) {
            logger.error("Error resetting settings: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la réinitialisation: " + e.getMessage());
            return "redirect:/admin/settings";
        }
    }

    /**
     * Teste la configuration email
     */
    @PostMapping("/test-email")
    @ResponseBody
    public ResponseEntity<?> testEmailConfiguration(@RequestBody SystemSettingsDto settings) {
        try {
            boolean testResult = systemConfigService.testEmailConfiguration(settings);
            
            if (testResult) {
                auditLogger.info("Email configuration test successful");
                return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Configuration email valide\"}");
            } else {
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Configuration email invalide\"}");
            }
            
        } catch (Exception e) {
            logger.error("Error testing email configuration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Obtient un paramètre spécifique
     */
    @GetMapping("/property/{key}")
    @ResponseBody
    public ResponseEntity<?> getProperty(@PathVariable String key) {
        try {
            String value = systemConfigService.getProperty(key);
            
            if (value != null) {
                return ResponseEntity.ok().body("{\"key\": \"" + key + "\", \"value\": \"" + value + "\"}");
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error getting property {}: {}", key, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Obtient les statistiques de configuration
     */
    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<?> getConfigurationStats() {
        try {
            Map<String, Object> stats = systemConfigService.getConfigurationStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting configuration stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Valide une configuration sans la sauvegarder
     */
    @PostMapping("/validate")
    @ResponseBody
    public ResponseEntity<?> validateSettings(@Valid @RequestBody SystemSettingsDto settings,
                                             BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                StringBuilder errors = new StringBuilder();
                bindingResult.getAllErrors().forEach(error -> {
                    errors.append(error.getDefaultMessage()).append("; ");
                });
                
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"" + errors.toString() + "\"}");
            }
            
            // Validation métier supplémentaire
            if (settings.getMaxAmount() <= settings.getMinAmount()) {
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"Le montant maximum doit être supérieur au minimum\"}");
            }
            
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Configuration valide\"}");
            
        } catch (Exception e) {
            logger.error("Error validating settings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Exporte la configuration actuelle
     */
    @GetMapping("/export")
    @ResponseBody
    public ResponseEntity<?> exportConfiguration() {
        try {
            SystemSettingsDto settings = systemConfigService.loadAllSettings();
            
            auditLogger.info("Configuration exported");
            return ResponseEntity.ok(settings);
            
        } catch (Exception e) {
            logger.error("Error exporting configuration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Importe une configuration
     */
    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importConfiguration(@Valid @RequestBody SystemSettingsDto settings,
                                                BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                StringBuilder errors = new StringBuilder();
                bindingResult.getAllErrors().forEach(error -> {
                    errors.append(error.getDefaultMessage()).append("; ");
                });
                
                return ResponseEntity.badRequest()
                    .body("{\"success\": false, \"message\": \"" + errors.toString() + "\"}");
            }
            
            // Sauvegarder la configuration importée
            systemConfigService.saveSettings(settings);
            
            auditLogger.info("Configuration imported successfully");
            return ResponseEntity.ok().body("{\"success\": true, \"message\": \"Configuration importée avec succès\"}");
            
        } catch (Exception e) {
            logger.error("Error importing configuration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Vérifie l'état de santé de la configuration
     */
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<?> checkConfigurationHealth() {
        try {
            Map<String, Object> stats = systemConfigService.getConfigurationStats();
            
            // Vérifier si la configuration est suffisamment complète
            int configPercentage = (Integer) stats.getOrDefault("configurationPercentage", 0);
            boolean isHealthy = configPercentage >= 80; // 80% minimum
            
            return ResponseEntity.ok().body("{\"healthy\": " + isHealthy + 
                ", \"configurationPercentage\": " + configPercentage + 
                ", \"status\": \"" + (isHealthy ? "OK" : "WARNING") + "\"}");
            
        } catch (Exception e) {
            logger.error("Error checking configuration health: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body("{\"healthy\": false, \"status\": \"ERROR\", \"error\": \"" + e.getMessage() + "\"}");
        }
    }
}