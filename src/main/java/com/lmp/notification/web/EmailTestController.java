package com.lmp.notification.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    @ResponseBody
    public ResponseEntity<?> showEmailTestPage() {
        logger.info("=== DIAGNOSTIC CONFIGURATION EMAIL ===");
        
        boolean mailSenderExists = (mailSender != null);
        boolean notificationServiceExists = (notificationService != null);
        boolean emailServiceExists = (emailService != null);
        
        boolean connectivityTest = false;
        String connectivityMessage = "";
        try {
            if (mailSender != null) {
                mailSender.createMimeMessage();
                connectivityTest = true;
                connectivityMessage = "JavaMailSender fonctionne correctement";
            }
        } catch (Exception e) {
            connectivityMessage = "Erreur JavaMailSender: " + e.getMessage();
        }
        
        boolean notificationTest = notificationService != null && notificationService.testEmailConnectivity();
        boolean emailServiceTest = emailService != null && emailService.testConnection();
        
        return ResponseEntity.ok(java.util.Map.of(
            "mailSenderExists", mailSenderExists,
            "notificationServiceExists", notificationServiceExists,
            "emailServiceExists", emailServiceExists,
            "mailHost", mailHost,
            "mailPort", mailPort,
            "connectivityTest", connectivityTest,
            "connectivityMessage", connectivityMessage,
            "notificationTest", notificationTest,
            "emailServiceTest", emailServiceTest
        ));
    }
    
    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<?> sendTestEmail(@RequestParam String testEmail) {
        logger.info("=== TEST ENVOI EMAIL ===");
        try {
            if (emailService != null) {
                emailService.sendTestEmail(testEmail);
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Email de test envoyé à " + testEmail));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "EmailService non disponible"));
        } catch (Exception e) {
            logger.error("Erreur envoi email de test à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/test-welcome")
    @ResponseBody
    public ResponseEntity<?> sendTestWelcomeEmail(@RequestParam String testEmail) {
        logger.info("=== TEST ENVOI EMAIL DE BIENVENUE ===");
        try {
            if (emailService != null) {
                emailService.sendWelcomeEmail(testEmail, "Utilisateur Test");
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Email de bienvenue envoyé à " + testEmail));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "EmailService non disponible"));
        } catch (Exception e) {
            logger.error("Erreur envoi email de bienvenue à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @PostMapping("/test-support")
    @ResponseBody
    public ResponseEntity<?> sendTestSupportEmail(@RequestParam String testEmail) {
        logger.info("=== TEST ENVOI EMAIL DE SUPPORT ===");
        try {
            if (emailService != null) {
                String subject = "Test Support - LMP Digital Services";
                String body = "Bonjour,\n\nCeci est un email de test du service support.\n\nCordialement,\nL'équipe Support LMP Digital Services";
                emailService.sendSupportEmail(testEmail, subject, body);
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Email de support envoyé à " + testEmail));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "EmailService non disponible"));
        } catch (Exception e) {
            logger.error("Erreur envoi email de support à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
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