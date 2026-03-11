package com.lmp.notification.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.lmp.notification.service.NotificationService;
import com.lmp.notification.service.EmailService;

/**
 * Controller de test pour diagnostiquer la configuration email
 */
@Controller
@RequestMapping("/admin/email-test")
@PreAuthorize("hasRole('ADMIN')")
public class EmailTestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTestController.class);

        private final JavaMailSender mailSender;
    
        private final NotificationService notificationService;
    
        private final EmailService emailService;

    @Value("${spring.mail.host:NON_CONFIGURÉ}")
    private String mailHost;
    
    @Value("${spring.mail.port:NON_CONFIGURÉ}")
    private String mailPort;
    
    @Value("${spring.mail.username:NON_CONFIGURÉ}")
    private String mailUsername;
    
    @Value("${spring.mail.password:NON_CONFIGURÉ}")
    private String mailPassword;


    public EmailTestController(JavaMailSender mailSender,
                           NotificationService notificationService,
                           EmailService emailService) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @GetMapping
    public String showEmailTestPage(Model model) {
        logger.info("=== DIAGNOSTIC CONFIGURATION EMAIL ===");
        
        // Diagnostic de la configuration
        boolean mailSenderExists = (mailSender != null);
        boolean notificationServiceExists = (notificationService != null);
        boolean emailServiceExists = (emailService != null);
        
        logger.info("JavaMailSender injecté: {}", mailSenderExists);
        logger.info("NotificationService injecté: {}", notificationServiceExists);
        logger.info("EmailService injecté: {}", emailServiceExists);
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
        
        // Test EmailService
        boolean emailServiceTest = false;
        if (emailService != null) {
            emailServiceTest = emailService.testConnection();
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
        model.addAttribute("emailServiceExists", emailServiceExists);
        model.addAttribute("mailHost", mailHost);
        model.addAttribute("mailPort", mailPort);
        model.addAttribute("mailUsername", mailUsername);
        model.addAttribute("mailPassword", maskPassword(mailPassword));
        model.addAttribute("connectivityTest", connectivityTest);
        model.addAttribute("connectivityMessage", connectivityMessage);
        model.addAttribute("notificationTest", notificationTest);
        model.addAttribute("emailServiceTest", emailServiceTest);
        model.addAttribute("requiredTemplates", requiredTemplates);
        
        return "admin/email-test";
    }
    
    @PostMapping("/test")
    public String sendTestEmail(@RequestParam String testEmail, RedirectAttributes redirectAttributes) {
        logger.info("=== TEST ENVOI EMAIL ===");
        logger.info("Tentative d'envoi à: {}", testEmail);
        
        try {
            if (emailService != null) {
                emailService.sendTestEmail(testEmail);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Email de test Mailtrap envoyé avec succès à " + testEmail);
                logger.info("Email de test Mailtrap envoyé avec succès à {}", testEmail);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "EmailService non disponible");
                logger.error("EmailService non disponible pour test email");
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
        logger.info("=== TEST ENVOI EMAIL DE BIENVENUE (noreply@lmp-services.ca) ===");
        logger.info("Tentative d'envoi email de bienvenue à: {}", testEmail);
        
        try {
            if (emailService != null) {
                // Utilise le nouveau EmailService avec noreply@lmp-services.ca
                emailService.sendWelcomeEmail(testEmail, "Utilisateur Test");
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Email de bienvenue envoyé avec succès depuis noreply@lmp-services.ca à " + testEmail);
                logger.info("Email de bienvenue envoyé avec succès depuis noreply@lmp-services.ca à {}", testEmail);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "EmailService non disponible");
                logger.error("EmailService non disponible pour test email de bienvenue");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'envoi de l'email de bienvenue: " + e.getMessage());
            logger.error("Erreur envoi email de bienvenue à {}: {}", testEmail, e.getMessage(), e);
        }
        
        return "redirect:/admin/email-test";
    }
    
    @PostMapping("/test-support")
    public String sendTestSupportEmail(@RequestParam String testEmail, RedirectAttributes redirectAttributes) {
        logger.info("=== TEST ENVOI EMAIL DE SUPPORT (support@lmp-services.ca) ===");
        logger.info("Tentative d'envoi email de support à: {}", testEmail);
        
        try {
            if (emailService != null) {
                String subject = "Test Support - LMP Digital Services";
                String body = "Bonjour,\n\nCeci est un email de test du service support.\n\n" +
                             "Vous pouvez répondre directement à cet email.\n\n" +
                             "Cordialement,\nL'équipe Support LMP Digital Services";
                
                // Utilise le nouveau service support avec support@lmp-services.ca
                emailService.sendSupportEmail(testEmail, subject, body);
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Email de support envoyé avec succès depuis support@lmp-services.ca à " + testEmail);
                logger.info("Email de support envoyé avec succès depuis support@lmp-services.ca à {}", testEmail);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "EmailService non disponible");
                logger.error("EmailService non disponible pour test email de support");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de l'envoi de l'email de support: " + e.getMessage());
            logger.error("Erreur envoi email de support à {}: {}", testEmail, e.getMessage(), e);
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