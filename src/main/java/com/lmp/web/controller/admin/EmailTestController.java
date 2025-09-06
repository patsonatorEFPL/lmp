package com.lmp.web.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lmp.service.admin.NotificationService;

/**
 * Controller de test pour diagnostiquer la configuration email
 */
@Controller
@RequestMapping("/admin/email-test")
@PreAuthorize("hasRole('ADMIN')")
public class EmailTestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTestController.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private NotificationService notificationService;

    @Value("${spring.mail.host:NON_CONFIGURÉ}")
    private String mailHost;
    
    @Value("${spring.mail.port:NON_CONFIGURÉ}")
    private String mailPort;
    
    @Value("${spring.mail.username:NON_CONFIGURÉ}")
    private String mailUsername;
    
    @Value("${spring.mail.password:NON_CONFIGURÉ}")
    private String mailPassword;

    @GetMapping
    public String showEmailTestPage(Model model) {
        logger.info("=== DIAGNOSTIC CONFIGURATION EMAIL ===");
        
        // Diagnostic de la configuration
        boolean mailSenderExists = (mailSender != null);
        boolean notificationServiceExists = (notificationService != null);
        
        logger.info("JavaMailSender injecté: {}", mailSenderExists);
        logger.info("NotificationService injecté: {}", notificationServiceExists);
        logger.info("Mail Host: {}", mailHost);
        logger.info("Mail Port: {}", mailPort);
        logger.info("Mail Username: {}", mailUsername);
        logger.info("Mail Password: {}", maskPassword(mailPassword));
        
        // Test de connectivité
        boolean connectivityTest = false;
        String connectivityMessage = "";
        
        try {
            if (mailSender != null) {
                mailSender.createMimeMessage();
                connectivityTest = true;
                connectivityMessage = "JavaMailSender fonctionne correctement";
                logger.info("Test connectivité JavaMailSender: SUCCÈS");
            }
        } catch (Exception e) {
            connectivityMessage = "Erreur JavaMailSender: " + e.getMessage();
            logger.error("Test connectivité JavaMailSender: ÉCHEC - {}", e.getMessage());
        }
        
        // Test NotificationService
        boolean notificationTest = false;
        if (notificationService != null) {
            notificationTest = notificationService.testEmailConnectivity();
        }
        
        // Diagnostic templates email
        String[] requiredTemplates = {
            "emails/order-status-change",
            "emails/order-cancellation", 
            "emails/order-refund",
            "emails/order-confirmation",
            "emails/order-shipping",
            "emails/admin-notification"
        };
        
        logger.info("Templates email requis:");
        for (String template : requiredTemplates) {
            logger.info("  - {}", template);
        }
        
        model.addAttribute("mailSenderExists", mailSenderExists);
        model.addAttribute("notificationServiceExists", notificationServiceExists);
        model.addAttribute("mailHost", mailHost);
        model.addAttribute("mailPort", mailPort);
        model.addAttribute("mailUsername", mailUsername);
        model.addAttribute("mailPassword", maskPassword(mailPassword));
        model.addAttribute("connectivityTest", connectivityTest);
        model.addAttribute("connectivityMessage", connectivityMessage);
        model.addAttribute("notificationTest", notificationTest);
        model.addAttribute("requiredTemplates", requiredTemplates);
        
        return "admin/email-test";
    }
    
    @PostMapping("/test")
    public String sendTestEmail(@RequestParam String testEmail, RedirectAttributes redirectAttributes) {
        logger.info("=== TEST ENVOI EMAIL ===");
        logger.info("Tentative d'envoi à: {}", testEmail);
        
        try {
            if (notificationService != null) {
                notificationService.sendTestEmail(testEmail);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Email de test envoyé avec succès à " + testEmail);
                logger.info("Email de test envoyé avec succès à {}", testEmail);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "NotificationService non disponible");
                logger.error("NotificationService non disponible pour test email");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'envoi: " + e.getMessage());
            logger.error("Erreur envoi email de test à {}: {}", testEmail, e.getMessage(), e);
        }
        
        return "redirect:/admin/email-test";
    }
    
    @PostMapping("/test-welcome")
    public String sendTestWelcomeEmail(@RequestParam String testEmail, RedirectAttributes redirectAttributes) {
        logger.info("=== TEST ENVOI EMAIL DE BIENVENUE ===");
        logger.info("Tentative d'envoi email de bienvenue à: {}", testEmail);
        
        try {
            if (notificationService != null) {
                notificationService.sendTestWelcomeEmail(testEmail);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Email de bienvenue de test envoyé avec succès à " + testEmail);
                logger.info("Email de bienvenue de test envoyé avec succès à {}", testEmail);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "NotificationService non disponible");
                logger.error("NotificationService non disponible pour test email de bienvenue");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
            logger.error("Erreur envoi email de bienvenue de test à {}: {}", testEmail, e.getMessage(), e);
        }
        
        return "redirect:/admin/email-test";
    }
    
    private String maskPassword(String password) {
        if (password == null || password.equals("NON_CONFIGURÉ")) {
            return password;
        }
        if (password.length() <= 4) {
            return "****";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }
}