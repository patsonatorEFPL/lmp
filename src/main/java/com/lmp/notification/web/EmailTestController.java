package com.lmp.notification.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.lmp.notification.health.ErpEmailHealthIndicator;
import com.lmp.notification.mail.dispatch.EmailDispatcherConfigService;
import com.lmp.notification.service.NotificationService;
import com.lmp.notification.service.EmailService;
import com.lmp.shared.dto.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de test pour diagnostiquer la configuration email.
 */
@Controller
@RequestMapping("/admin/email-test")
@PreAuthorize("hasRole('ADMIN')")
public class EmailTestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailTestController.class);

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ErpEmailHealthIndicator erpEmailHealthIndicator;
    private final EmailDispatcherConfigService dispatcherConfigService;

    @Value("${spring.mail.host:NON_CONFIGURÉ}")
    private String mailHost;

    @Value("${spring.mail.port:NON_CONFIGURÉ}")
    private String mailPort;

    public EmailTestController(JavaMailSender mailSender,
                               NotificationService notificationService,
                               EmailService emailService,
                               ErpEmailHealthIndicator erpEmailHealthIndicator,
                               EmailDispatcherConfigService dispatcherConfigService) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.erpEmailHealthIndicator = erpEmailHealthIndicator;
        this.dispatcherConfigService = dispatcherConfigService;
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> showEmailTestPage() {
        Map<String, Object> result = new HashMap<>();
        result.put("mailSenderExists", mailSender != null);
        result.put("notificationServiceExists", notificationService != null);
        result.put("emailServiceExists", emailService != null);
        result.put("mailHost", mailHost);
        result.put("mailPort", mailPort);

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
        result.put("connectivityTest", connectivityTest);
        result.put("connectivityMessage", connectivityMessage);
        result.put("notificationTest", notificationService != null && notificationService.testEmailConnectivity());
        result.put("emailServiceTest", emailService != null && emailService.testConnection());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/erpnext-health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> erpnextHealth() {
        Health health = erpEmailHealthIndicator.health();
        Map<String, Object> details = health.getDetails();
        Object latency = details.get("latencyMs");
        Object error = details.get("error");
        Object message = details.get("message");
        return ResponseEntity.ok(Map.of(
                "status", health.getStatus().equals(Status.UP) ? "UP" : "DOWN",
                "latencyMs", latency != null ? latency : -1,
                "lastError", error != null ? error.toString() : "",
                "message", message != null ? message.toString() : ""
        ));
    }

    @GetMapping("/dispatcher")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDispatcher() {
        return ResponseEntity.ok(Map.of("strategy", dispatcherConfigService.getActiveStrategy()));
    }

    @PostMapping("/dispatcher")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> setDispatcher(@RequestParam String strategy) {
        try {
            dispatcherConfigService.setActiveStrategy(strategy);
            return ResponseEntity.ok(ApiResponse.ok(
                    "Stratégie changée en " + strategy,
                    Map.of("strategy", dispatcherConfigService.getActiveStrategy())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> sendTestEmail(@RequestParam String testEmail) {
        try {
            if (emailService == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("EmailService non disponible"));
            }
            emailService.sendTestEmail(testEmail);
            return ResponseEntity.ok(ApiResponse.ok("Email de test envoyé à " + testEmail, null));
        } catch (Exception e) {
            logger.error("Erreur envoi email de test à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/test-welcome")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> sendTestWelcomeEmail(@RequestParam String testEmail) {
        try {
            if (emailService == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("EmailService non disponible"));
            }
            emailService.sendWelcomeEmail(testEmail, "Utilisateur Test");
            return ResponseEntity.ok(ApiResponse.ok("Email de bienvenue envoyé à " + testEmail, null));
        } catch (Exception e) {
            logger.error("Erreur envoi email de bienvenue à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/test-support")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> sendTestSupportEmail(@RequestParam String testEmail) {
        try {
            if (emailService == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("EmailService non disponible"));
            }
            String subject = "Test Support - LMP Digital Services";
            String body = "Bonjour,\n\nCeci est un email de test du service support.\n\nCordialement,\nL'équipe Support LMP Digital Services";
            emailService.sendSupportEmail(testEmail, subject, body);
            return ResponseEntity.ok(ApiResponse.ok("Email de support envoyé à " + testEmail, null));
        } catch (Exception e) {
            logger.error("Erreur envoi email de support à {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
