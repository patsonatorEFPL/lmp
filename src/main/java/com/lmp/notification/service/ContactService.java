package com.lmp.notification.service;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.notification.dto.ContactForm;
import com.lmp.notification.mail.queue.MailQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    
    private final MailQueueService mailQueueService;

    private final TemplateEngine templateEngine;

    private final MailAddressConfig mailAddressConfig;

    public ContactService(MailQueueService mailQueueService,
                          TemplateEngine templateEngine,
                          MailAddressConfig mailAddressConfig) {
        this.mailQueueService = mailQueueService;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
    }

    @Value("${company.email:support@localhost}")
    private String adminEmail;
    
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
            
            logger.error("CONTACT_EMAIL_ERROR - Problème d'envoi d'email : {}", e.getMessage());
            
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
            logger.info("CONTACT_ADMIN_EMAIL_DEBUG - Début enqueue email admin pour : {}", contactForm.getEmail());

            Context context = new Context();
            context.setVariable("contactForm", contactForm);
            context.setVariable("companyName", companyName);
            context.setVariable("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));

            String htmlContent = templateEngine.process("emails/contact-notification", context);

            mailQueueService.enqueue(
                    mailAddressConfig.getContact(),
                    mailAddressConfig.getName(),
                    adminEmail,
                    "🔔 Nouveau contact reçu de " + contactForm.getName(),
                    htmlContent);

            logger.info("Email de notification admin enqueued avec succès pour le contact de : {}", contactForm.getEmail());

        } catch (Exception e) {
            logger.error("CONTACT_ADMIN_EMAIL_ERROR - Exception lors de l'enqueue admin pour '{}': Type={}, Message='{}'",
                        contactForm.getEmail(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'enqueue de l'email de notification admin", e);
        }
    }
    
    /**
     * Envoie un email de confirmation à l'utilisateur
     */
    private void sendUserConfirmation(ContactForm contactForm) {
        try {
            if (contactForm.getEmail() == null || contactForm.getEmail().isEmpty()) {
                logger.warn("CONTACT_USER_EMAIL_WARN - Pas d'email utilisateur pour envoyer la confirmation");
                return;
            }

            logger.info("CONTACT_USER_EMAIL_DEBUG - Début enqueue email confirmation pour : {}", contactForm.getEmail());

            Context context = new Context();
            ContactService.ContactConfirmation contact = new ContactService.ContactConfirmation();
            contact.setFirstName(extractFirstName(contactForm.getName()));
            contact.setSubject(contactForm.getSubject());
            contact.setSubmittedAt(LocalDateTime.now());

            context.setVariable("contact", contact);
            context.setVariable("companyName", companyName);
            context.setVariable("companyEmail", mailAddressConfig.getContact());

            String htmlContent = templateEngine.process("emails/contact-confirmation", context);

            mailQueueService.enqueue(
                    mailAddressConfig.getContact(),
                    mailAddressConfig.getName(),
                    contactForm.getEmail(),
                    "✅ Confirmation de réception - " + companyName,
                    htmlContent);

            logger.info("Email de confirmation utilisateur enqueued avec succès à : {}", contactForm.getEmail());

        } catch (Exception e) {
            logger.error("CONTACT_USER_EMAIL_ERROR - Exception lors de l'enqueue confirmation pour '{}': Type={}, Message='{}'",
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
