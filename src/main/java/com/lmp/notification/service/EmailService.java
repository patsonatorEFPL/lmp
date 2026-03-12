package com.lmp.notification.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lmp.notification.config.MailAddressConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service moderne d'envoi d'emails utilisant Jakarta Mail et Mailtrap SMTP
 * Basé sur les meilleures pratiques de la documentation Mailtrap
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
        private final JavaMailSender mailSender;
    
        private final TemplateEngine templateEngine;
    
        private final MailAddressConfig mailAddressConfig;
    
    @Value("${app.name:LMP Digital Services}")
    private String appName;

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;


    public EmailService(JavaMailSender mailSender,
                           TemplateEngine templateEngine,
                           MailAddressConfig mailAddressConfig) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
    }

    /**
     * Envoie un email texte simple depuis noreply (Reply-To = noreply)
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailAddressConfig.getNoreply());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setReplyTo(mailAddressConfig.getNoreply()); // Reply-To = noreply
            
            mailSender.send(message);
            logger.info("Email simple envoyé avec succès à: {} (Reply-To: noreply)", to);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email simple à {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email", e);
        }
    }

    /**
     * Envoie un email HTML avec template Thymeleaf depuis noreply (Reply-To = noreply)
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Configuration de l'expéditeur
            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setReplyTo(mailAddressConfig.getNoreply()); // Reply-To = noreply
            
            // Rendu du template HTML
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Email HTML envoyé avec succès à: {} (template: {})", to, templateName);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email HTML à {} avec template {}: {}", 
                to, templateName, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email HTML", e);
        }
    }

    /**
     * Envoie un email avec pièces jointes
     */
    public void sendEmailWithAttachment(String to, String subject, String body, 
                                      String attachmentName, byte[] attachmentData, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setReplyTo(mailAddressConfig.getNoreply()); // Reply-To = noreply
            
            // Créer le multipart message
            Multipart multipart = new MimeMultipart();
            
            // Corps du message (HTML ou texte)
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            if (body.trim().startsWith("<!DOCTYPE") || body.trim().startsWith("<html")) {
                messageBodyPart.setContent(body, "text/html; charset=UTF-8");
            } else {
                messageBodyPart.setText(body, "UTF-8");
            }
            multipart.addBodyPart(messageBodyPart);
            
            // Pièce jointe
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setContent(attachmentData, contentType);
            attachmentPart.setFileName(attachmentName);
            multipart.addBodyPart(attachmentPart);
            
            message.setContent(multipart);
            
            mailSender.send(message);
            logger.info("Email avec pièce jointe envoyé avec succès à: {}", to);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email avec pièce jointe à {}: {}", 
                to, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email avec pièce jointe", e);
        }
    }

    /**
     * Envoie un email à plusieurs destinataires
     */
    public void sendEmailToMultipleRecipients(String[] to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.setReplyTo(mailAddressConfig.getNoreply()); // Reply-To = noreply
            
            mailSender.send(message);
            logger.info("Email envoyé avec succès à {} destinataires", to.length);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email multiple: {}", e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email multiple", e);
        }
    }

    /**
     * Teste la connectivité SMTP
     */
    public boolean testConnection() {
        try {
            mailSender.createMimeMessage();
            logger.info("Test de connectivité SMTP réussi");
            return true;
        } catch (Exception e) {
            logger.error("Test de connectivité SMTP échoué: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie un email de test simple
     */
    public void sendTestEmail(String to) {
        String subject = "Test Email - " + appName;
        String body = "Ceci est un email de test envoyé le " + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) + 
            "\n\nSi vous recevez cet email, la configuration Mailtrap fonctionne correctement.";
        
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Envoie un email de support (From et Reply-To = support@lmp-services.ca)
     */
    public void sendSupportEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(mailAddressConfig.getSupport(), mailAddressConfig.getName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.setReplyTo(mailAddressConfig.getSupport()); // Reply-To = support
            
            mailSender.send(message);
            logger.info("Email de support envoyé avec succès à: {} (Reply-To: support)", to);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de support à {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de support", e);
        }
    }

    /**
     * Envoie un email de bienvenue HTML avec template
     * Utilise TOUJOURS noreply@lmp-services.ca comme expéditeur ET Reply-To
     */
    public void sendWelcomeEmail(String to, String firstName) {
        logger.info("📞 Email de bienvenue - De: {} vers: {} (Prénom: {})", 
                   mailAddressConfig.getNoreply(), to, firstName);
        
        Map<String, Object> variables = new HashMap<>();
        // Utilise la structure attendue par le template
        Map<String, String> user = new HashMap<>();
        user.put("firstName", firstName);
        variables.put("user", user);
        variables.put("companyName", appName);
        variables.put("companyEmail", mailAddressConfig.getReplyToSupport());
        variables.put("companyWebsite", frontendUrl);
        variables.put("frontendUrl", frontendUrl);
        variables.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        String subject = "Bienvenue chez " + appName + " !";
        
        try {
            sendHtmlEmail(to, subject, "emails/welcome-minimal-clean", variables);
            logger.info("✅ Email de bienvenue HTML envoyé avec succès (Reply-To: noreply)");
        } catch (Exception e) {
            logger.error("Erreur template, tentative avec email simple: {}", e.getMessage());
            // Fallback vers email simple si template échoue
            String body = "Bienvenue " + firstName + " !\n\nVotre compte a été créé avec succès chez " + appName + ".\n\nCordialement,\nL'équipe " + appName;
            sendSimpleEmail(to, subject, body);
            logger.info("✅ Email de bienvenue simple envoyé avec succès (Reply-To: noreply)");
        }
    }

    /**
     * Envoie un email de confirmation de commande
     */
    public void sendOrderConfirmation(String to, String customerName, String orderId, double amount) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("customerName", customerName);
        variables.put("orderId", orderId);
        variables.put("amount", amount);
        variables.put("companyName", appName);
        variables.put("frontendUrl", frontendUrl);
        variables.put("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        
        String subject = "Confirmation de votre commande #" + orderId;
        sendHtmlEmail(to, subject, "emails/order-confirmation", variables);
    }
}
