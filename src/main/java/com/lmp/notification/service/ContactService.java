package com.lmp.notification.service;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.notification.dto.ContactForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour la gestion des contacts
 * 
 * Ce service centralise la logique métier liée aux formulaires de contact :
 * - Validation métier
 * - Sauvegarde des contacts
 * - Envoi d'emails (simulation pour l'instant)
 * - Logging des opérations
 */
@Service
public class ContactService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);
    
    private final JavaMailSender javaMailSender;
    
    private final TemplateEngine templateEngine;
    
    private final MailAddressConfig mailAddressConfig;

    public ContactService(JavaMailSender javaMailSender,
                          TemplateEngine templateEngine,
                          MailAddressConfig mailAddressConfig) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
    }

    // OBSOLÈTE - remplacé par mailAddressConfig.getSupport() pour ContactService
    // @Value("${mail.from.address:lmp.assistance@gmail.com}")
    // private String fromEmail;
    
    // @Value("${mail.from.name:LMP Digital Services}")
    // private String fromName;
    
    @Value("${company.email:lmp.assistance@gmail.com}")
    private String adminEmail; // Garde pour la réception admin
    
    @Value("${company.name:LMP Digital Services}")
    private String companyName;
    
    // Simulation d'une base de données en mémoire
    private final List<ContactEntry> contacts = new ArrayList<>();
    
    /**
     * Traite un formulaire de contact
     * 
     * @param contactForm Le formulaire de contact validé
     * @return true si le traitement a réussi, false sinon
     */
    public boolean processContact(ContactForm contactForm) {
        try {
            // LOG DE DIAGNOSTIC : Début du traitement
            logger.info("CONTACT_PROCESSING_DEBUG - Début traitement contact : nom='{}', email='{}', sujet='{}'",
                       contactForm.getName(), contactForm.getEmail(), contactForm.getSubject());
            
            // Sauvegarde du contact
            logger.info("CONTACT_PROCESSING_DEBUG - Sauvegarde du contact...");
            saveContact(contactForm);
            logger.info("CONTACT_PROCESSING_DEBUG - Contact sauvegardé avec succès");
            
            // Envoi des emails
            logger.info("CONTACT_PROCESSING_DEBUG - Tentative d'envoi des emails...");
            sendAdminNotification(contactForm);
            sendUserConfirmation(contactForm);
            logger.info("CONTACT_PROCESSING_DEBUG - Emails envoyés avec succès");
            
            logger.info("Contact traité avec succès pour : {}", contactForm.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("CONTACT_PROCESSING_ERROR - Erreur lors du traitement du contact pour '{}': Type={}, Message='{}'",
                        contactForm.getEmail(), e.getClass().getSimpleName(), e.getMessage(), e);
            
            // Diagnostic spécifique selon le type d'erreur
            if (e instanceof MessagingException) {
                logger.error("CONTACT_EMAIL_ERROR - Problème d'envoi d'email : {}", e.getMessage());
            } else if (e.getCause() instanceof MessagingException) {
                logger.error("CONTACT_EMAIL_ERROR - Problème d'envoi d'email (cause) : {}", e.getCause().getMessage());
            } else {
                logger.error("CONTACT_OTHER_ERROR - Autre type d'erreur : {}", e.getClass().getSimpleName());
            }
            
            return false;
        }
    }
    
    /**
     * Sauvegarde un contact en base (simulation en mémoire)
     */
    private void saveContact(ContactForm contactForm) {
        ContactEntry entry = new ContactEntry(
            contactForm.getName(),
            contactForm.getEmail(),
            contactForm.getSubject(),
            contactForm.getMessage(),
            LocalDateTime.now()
        );
        
        contacts.add(entry);
        logger.debug("Contact sauvegardé : {}", entry);
    }
    
    /**
     * Envoie un email de notification détaillé à l'administrateur
     */
    private void sendAdminNotification(ContactForm contactForm) {
        try {
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Début envoi email admin pour : {}", contactForm.getEmail());
            
            // LOG DE DIAGNOSTIC : Configuration email
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Config: fromEmail='{}' (support), adminEmail='{}', companyName='{}'",
                       mailAddressConfig.getSupport(), adminEmail, companyName);
            
            // Création du contexte Thymeleaf pour l'admin
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Création contexte Thymeleaf admin...");
            Context context = new Context();
            context.setVariable("contactForm", contactForm);
            context.setVariable("companyName", companyName);
            context.setVariable("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));
            
            // Rendu du template HTML pour l'admin
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Rendu template 'emails/contact-notification'...");
            String htmlContent = templateEngine.process("emails/contact-notification", context);
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Template rendu avec succès, taille: {} caractères", htmlContent.length());
            
            // Création du message email pour l'admin
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Création message MIME admin...");
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Configuration du message admin (vient de support@ - pas de reply-to)
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Configuration message admin...");
            helper.setFrom(mailAddressConfig.getSupport(), mailAddressConfig.getName());
            helper.setTo(adminEmail);
            helper.setSubject("🔔 Nouveau contact reçu de " + contactForm.getName());
            helper.setText(htmlContent, true);
            
            // Envoi de l'email admin
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Tentative d'envoi admin via JavaMailSender...");
            javaMailSender.send(message);
            
            logger.info("Email de notification admin envoyé avec succès pour le contact de : {}", contactForm.getEmail());
            
        } catch (MessagingException e) {
            logger.error("CONTACT_ADMIN_EMAIL_ERROR - MessagingException lors de l'envoi admin pour '{}': {}",
                        contactForm.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de notification admin", e);
        } catch (Exception e) {
            logger.error("CONTACT_ADMIN_EMAIL_ERROR - Exception inattendue lors de l'envoi admin pour '{}': Type={}, Message='{}'",
                        contactForm.getEmail(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors du traitement de l'email admin", e);
        }
    }
    
    /**
     * Envoie un email de confirmation à l'utilisateur
     */
    private void sendUserConfirmation(ContactForm contactForm) {
        try {
            // Vérification que l'utilisateur a un email valide
            if (contactForm.getEmail() == null || contactForm.getEmail().isEmpty()) {
                logger.warn("CONTACT_USER_EMAIL_WARN - Pas d'email utilisateur pour envoyer la confirmation");
                return;
            }
            
            logger.info("CONTACT_USER_EMAIL_DEBUG - Début envoi email confirmation pour : {}", contactForm.getEmail());
            
            // Création du contexte Thymeleaf pour l'utilisateur
            logger.info("CONTACT_USER_EMAIL_DEBUG - Création contexte Thymeleaf utilisateur...");
            Context context = new Context();
            
            // Création d'un objet contact avec les informations nécessaires
            ContactService.ContactConfirmation contact = new ContactService.ContactConfirmation();
            contact.setFirstName(extractFirstName(contactForm.getName()));
            contact.setSubject(contactForm.getSubject());
            contact.setSubmittedAt(LocalDateTime.now());
            
            context.setVariable("contact", contact);
            context.setVariable("companyName", companyName);
            
            // Rendu du template HTML pour l'utilisateur
            logger.info("CONTACT_USER_EMAIL_DEBUG - Rendu template 'emails/contact-confirmation'...");
            String htmlContent = templateEngine.process("emails/contact-confirmation", context);
            logger.info("CONTACT_USER_EMAIL_DEBUG - Template confirmation rendu avec succès, taille: {} caractères", htmlContent.length());
            
            // Création du message email pour l'utilisateur
            logger.info("CONTACT_USER_EMAIL_DEBUG - Création message MIME utilisateur...");
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Configuration du message utilisateur (vient de support@ - pas de reply-to)
            logger.info("CONTACT_USER_EMAIL_DEBUG - Configuration message utilisateur...");
            helper.setFrom(mailAddressConfig.getSupport(), mailAddressConfig.getName());
            helper.setTo(contactForm.getEmail());
            helper.setSubject("✅ Confirmation de réception - " + companyName);
            helper.setText(htmlContent, true);
            
            // Envoi de l'email utilisateur
            logger.info("CONTACT_USER_EMAIL_DEBUG - Tentative d'envoi confirmation via JavaMailSender...");
            javaMailSender.send(message);
            
            logger.info("Email de confirmation utilisateur envoyé avec succès à : {}", contactForm.getEmail());
            
        } catch (MessagingException e) {
            logger.error("CONTACT_USER_EMAIL_ERROR - MessagingException lors de l'envoi confirmation pour '{}': {}",
                        contactForm.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de confirmation utilisateur", e);
        } catch (Exception e) {
            logger.error("CONTACT_USER_EMAIL_ERROR - Exception inattendue lors de l'envoi confirmation pour '{}': Type={}, Message='{}'",
                        contactForm.getEmail(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors du traitement de l'email de confirmation", e);
        }
    }
    
    /**
     * Extrait le prénom d'un nom complet
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Client";
        }
        
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }
    
    /**
     * Récupère tous les contacts (pour administration future)
     */
    public List<ContactEntry> getAllContacts() {
        return new ArrayList<>(contacts);
    }
    
    /**
     * Compte le nombre de contacts reçus
     */
    public int getContactCount() {
        return contacts.size();
    }
    
    /**
     * Classe interne pour les données de confirmation email
     */
    public static class ContactConfirmation {
        private String firstName;
        private String subject;
        private LocalDateTime submittedAt;
        
        // Getters et setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    }
    
    /**
     * Classe interne pour représenter un contact sauvegardé
     */
    public static class ContactEntry {
        private final String name;
        private final String email;
        private final String subject;
        private final String message;
        private final LocalDateTime createdAt;
        

        public ContactEntry(String name, String email, String subject, String message, LocalDateTime createdAt) {
            this.name = name;
            this.email = email;
            this.subject = subject;
            this.message = message;
            this.createdAt = createdAt;
        }
        
        // Getters
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getSubject() { return subject; }
        public String getMessage() { return message; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        @Override
        public String toString() {
            return "ContactEntry{" +
                    "name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", subject='" + subject + '\'' +
                    ", createdAt=" + createdAt +
                    '}';
        }
    }
}
