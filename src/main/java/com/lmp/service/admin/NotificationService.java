package com.lmp.service.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lmp.domain.entity.Order;
import com.lmp.domain.enums.OrderStatus;

import jakarta.mail.internet.MimeMessage;

/**
 * Service pour l'envoi de notifications automatiques par email.
 * Gère les notifications de changement de statut de commande.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private static final String FROM_EMAIL = "lmp.assistance@gmail.com";
    private static final String COMPANY_NAME = "LMP Digital Services";

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;
    
    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:NON_CONFIGURÉ}")
    private String mailHost;
    
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:NON_CONFIGURÉ}")
    private String mailUsername;
    
    @org.springframework.beans.factory.annotation.Value("${spring.mail.password:NON_CONFIGURÉ}")
    private String mailPassword;

    /**
     * Envoie une notification de changement de statut
     */
    public void sendOrderStatusNotification(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Impossible d'envoyer notification pour commande {} - pas d'email client", order.getId());
            return;
        }

        try {
            String subject = buildStatusChangeSubject(order, newStatus);
            String htmlContent = buildStatusChangeHtmlContent(order, oldStatus, newStatus);
            
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Notification statut envoyée pour commande {} : {} -> {}", 
                order.getId(), oldStatus, newStatus);
                
        } catch (Exception e) {
            logger.error("Erreur envoi notification statut pour commande {}: {}", 
                order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envoie une notification d'annulation
     */
    public void sendOrderCancellationNotification(Order order, String reason) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Impossible d'envoyer notification annulation pour commande {} - pas d'email client", order.getId());
            return;
        }

        try {
            String subject = "Annulation de votre commande #" + order.getId();
            String htmlContent = buildCancellationHtmlContent(order, reason);
            
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Notification annulation envoyée pour commande {}", order.getId());
                
        } catch (Exception e) {
            logger.error("Erreur envoi notification annulation pour commande {}: {}", 
                order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envoie une notification de remboursement
     */
    public void sendRefundNotification(Order order, String refundAmount, String refundId) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Impossible d'envoyer notification remboursement pour commande {} - pas d'email client", order.getId());
            return;
        }

        try {
            String subject = "Remboursement traité pour votre commande #" + order.getId();
            String htmlContent = buildRefundHtmlContent(order, refundAmount, refundId);
            
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Notification remboursement envoyée pour commande {}", order.getId());
                
        } catch (Exception e) {
            logger.error("Erreur envoi notification remboursement pour commande {}: {}", 
                order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envoie un email de confirmation de commande
     */
    public void sendOrderConfirmationNotification(Order order) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Impossible d'envoyer confirmation pour commande {} - pas d'email client", order.getId());
            return;
        }

        try {
            String subject = "Confirmation de votre commande #" + order.getId();
            String htmlContent = buildConfirmationHtmlContent(order);
            
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Confirmation de commande envoyée pour {}", order.getId());
                
        } catch (Exception e) {
            logger.error("Erreur envoi confirmation pour commande {}: {}", 
                order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envoie un email d'expédition
     */
    public void sendShippingNotification(Order order, String trackingNumber) {
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            logger.warn("Impossible d'envoyer notification expédition pour commande {} - pas d'email client", order.getId());
            return;
        }

        try {
            String subject = "Votre commande #" + order.getId() + " a été expédiée";
            String htmlContent = buildShippingHtmlContent(order, trackingNumber);
            
            sendHtmlEmail(order.getUser().getEmail(), subject, htmlContent);
            
            logger.info("Notification expédition envoyée pour commande {}", order.getId());
                
        } catch (Exception e) {
            logger.error("Erreur envoi notification expédition pour commande {}: {}", 
                order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Envoie une notification administrative interne
     */
    public void sendAdminNotification(String adminEmail, String subject, String message, Order order) {
        try {
            String htmlContent = buildAdminNotificationContent(subject, message, order);
            sendHtmlEmail(adminEmail, "[ADMIN] " + subject, htmlContent);
            
            logger.info("Notification admin envoyée à {} pour commande {}", adminEmail, order.getId());
                
        } catch (Exception e) {
            logger.error("Erreur envoi notification admin: {}", e.getMessage(), e);
        }
    }

    // ========== Méthodes privées de construction des contenus ==========

    private String buildStatusChangeSubject(Order order, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED:
                return "Votre commande #" + order.getId() + " a été confirmée";
            case PROCESSING:
                return "Votre commande #" + order.getId() + " est en préparation";
            case SHIPPED:
                return "Votre commande #" + order.getId() + " a été expédiée";
            case DELIVERED:
                return "Votre commande #" + order.getId() + " a été livrée";
            case CANCELLED:
                return "Votre commande #" + order.getId() + " a été annulée";
            case REFUNDED:
                return "Votre commande #" + order.getId() + " a été remboursée";
            default:
                return "Mise à jour de votre commande #" + order.getId();
        }
    }

    private String buildStatusChangeHtmlContent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("oldStatus", getStatusDisplayName(oldStatus));
        context.setVariable("newStatus", getStatusDisplayName(newStatus));
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/order-status-change", context);
    }

    private String buildCancellationHtmlContent(Order order, String reason) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("reason", reason);
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/order-cancellation", context);
    }

    private String buildRefundHtmlContent(Order order, String refundAmount, String refundId) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("refundAmount", refundAmount);
        context.setVariable("refundId", refundId);
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/order-refund", context);
    }

    private String buildConfirmationHtmlContent(Order order) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/order-confirmation", context);
    }

    private String buildShippingHtmlContent(Order order, String trackingNumber) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("trackingNumber", trackingNumber);
        context.setVariable("customerName", getCustomerName(order));
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/order-shipping", context);
    }

    private String buildAdminNotificationContent(String subject, String message, Order order) {
        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("message", message);
        context.setVariable("order", order);
        context.setVariable("companyName", COMPANY_NAME);
        context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        return templateEngine.process("emails/admin-notification", context);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(FROM_EMAIL);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(mimeMessage);
    }

    private void sendTextEmail(String to, String subject, String textContent) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_EMAIL);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(textContent);
        
        mailSender.send(message);
    }

    private String getCustomerName(Order order) {
        if (order.getUser() != null) {
            String firstName = order.getUser().getFirstName();
            String lastName = order.getUser().getLastName();
            
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            }
        }
        return "Cher client";
    }

    private String getStatusDisplayName(OrderStatus status) {
        if (status == null) return "Inconnu";
        
        switch (status) {
            case PENDING: return "En attente";
            case PAYMENT_PENDING: return "Paiement en attente";
            case CONFIRMED: return "Confirmée";
            case PROCESSING: return "En préparation";
            case SHIPPED: return "Expédiée";
            case DELIVERED: return "Livrée";
            case CANCELLED: return "Annulée";
            case REFUNDED: return "Remboursée";
            default: return status.toString();
        }
    }

    /**
     * Test de connectivité email
     */
    public boolean testEmailConnectivity() {
        try {
            mailSender.createMimeMessage();
            logger.info("Test connectivité email réussi");
            return true;
        } catch (Exception e) {
            logger.error("Test connectivité email échoué: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie un email de test
     */
    public void sendTestEmail(String toEmail) throws Exception {
        logger.info("=== DIAGNOSTIC EMAIL AUTHENTICATION ===");
        logger.info("FROM_EMAIL configuré: {}", FROM_EMAIL);
        logger.info("SMTP Host: {}", mailHost);
        logger.info("SMTP Username: {}", mailUsername);
        logger.info("SMTP Password: {}", maskPassword(mailPassword));
        logger.info("Destinataire: {}", toEmail);
        logger.info("Tentative d'authentification SMTP...");
        
        String subject = "Test Email - " + COMPANY_NAME;
        String content = "Ceci est un email de test envoyé le " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) +
            "\n\nSi vous recevez cet email, la configuration fonctionne correctement.";
        
        try {
            sendTextEmail(toEmail, subject, content);
            logger.info("✅ Email de test envoyé avec succès à {}", toEmail);
        } catch (Exception e) {
            logger.error("❌ ÉCHEC envoi email - Erreur: {}", e.getMessage());
            logger.error("❌ Type d'exception: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("❌ Cause racine: {}", e.getCause().getMessage());
            }
            throw e;
        }
    }

    /**
     * Teste l'email de bienvenue avec des données fictives
     */
    public void sendTestWelcomeEmail(String toEmail) throws Exception {
        logger.info("=== TEST EMAIL DE BIENVENUE ===");
        logger.info("Destinataire: {}", toEmail);
        
        try {
            // Création d'un utilisateur fictif pour le test
            Context context = new Context();
            
            // Simulation d'un objet User avec les propriétés nécessaires
            Map<String, Object> testUser = new HashMap<>();
            testUser.put("firstName", "Utilisateur Test");
            testUser.put("email", toEmail);
            
            context.setVariable("user", testUser);
            context.setVariable("companyName", COMPANY_NAME);
            context.setVariable("baseUrl", "https://lmp-services.ca");
            
            // Rendu du template HTML de bienvenue final
            logger.info("WELCOME_TEST - Rendu template 'emails/welcome-minimal-clean'...");
            String htmlContent = templateEngine.process("emails/welcome-minimal-clean", context);
            logger.info("WELCOME_TEST - Template rendu avec succès, taille: {} caractères", htmlContent.length());
            
            // Envoi de l'email de test
            String subject = "🎉 Test Email de Bienvenue - " + COMPANY_NAME;
            sendHtmlEmail(toEmail, subject, htmlContent);
            
            logger.info("✅ Email de bienvenue de test envoyé avec succès à {}", toEmail);
            
        } catch (Exception e) {
            logger.error("❌ ÉCHEC test email de bienvenue - Erreur: {}", e.getMessage());
            logger.error("❌ Type d'exception: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("❌ Cause racine: {}", e.getCause().getMessage());
            }
            throw e;
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