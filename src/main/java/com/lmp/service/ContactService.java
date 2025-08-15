package com.lmp.service;

import com.lmp.dto.ContactForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
     * Simule l'envoi d'un email de notification
     */
    private void sendNotificationEmail(ContactForm contactForm) {
        // Simulation de l'envoi d'email
        logger.info("Envoi d'un email de notification pour le contact de : {}", contactForm.getEmail());
        
        // TODO: Implémenter l'envoi réel d'email avec JavaMailSender
        // Exemple d'intégration future :
        // - Configuration SMTP dans application.properties
        // - Utilisation de @Autowired JavaMailSender
        // - Templates d'email avec Thymeleaf
        
        // Simulation d'un délai d'envoi
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
