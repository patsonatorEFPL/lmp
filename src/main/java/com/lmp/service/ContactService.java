package com.lmp.service;

import com.lmp.dto.ContactForm;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private JavaMailSender javaMailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Value("${mail.from.address:lmp.assistance@gmail.com}")
    private String fromEmail;
    
    @Value("${mail.from.name:LMP Digital Services}")
    private String fromName;
    
    @Value("${company.email:contact@lmp-digital.ca}")
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
            // Log de l'opération
            logger.info("Traitement d'un nouveau contact de : {}", contactForm.getEmail());
            
            // Sauvegarde du contact
            saveContact(contactForm);
            
            // Envoi de l'email (simulation)
            sendNotificationEmail(contactForm);
            
            logger.info("Contact traité avec succès pour : {}", contactForm.getEmail());
            return true;
            
        } catch (Exception e) {
            logger.error("Erreur lors du traitement du contact pour : {}", contactForm.getEmail(), e);
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
     * Envoie un email de notification pour un nouveau contact
     */
    private void sendNotificationEmail(ContactForm contactForm) {
        try {
            logger.info("Envoi d'un email de notification pour le contact de : {}", contactForm.getEmail());
            
            // Création du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("contactForm", contactForm);
            context.setVariable("companyName", companyName);
            context.setVariable("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")));
            
            // Rendu du template HTML
            String htmlContent = templateEngine.process("emails/contact-notification", context);
            
            // Création du message email
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Configuration du message
            helper.setFrom(fromEmail, fromName);
            helper.setTo(adminEmail);
            helper.setSubject("🔔 Nouveau contact reçu de " + contactForm.getName());
            helper.setText(htmlContent, true);
            
            // Copie à l'expéditeur pour confirmation
            if (contactForm.getEmail() != null && !contactForm.getEmail().isEmpty()) {
                helper.setBcc(contactForm.getEmail());
            }
            
            // Envoi de l'email
            javaMailSender.send(message);
            
            logger.info("Email de notification envoyé avec succès pour le contact de : {}", contactForm.getEmail());
            
        } catch (MessagingException e) {
            logger.error("Erreur lors de l'envoi de l'email de notification pour : {}", contactForm.getEmail(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de notification", e);
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'envoi de l'email pour : {}", contactForm.getEmail(), e);
            throw new RuntimeException("Erreur lors du traitement de l'email", e);
        }
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
