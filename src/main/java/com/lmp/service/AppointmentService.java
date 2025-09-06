package com.lmp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.domain.dto.AppointmentForm;
import com.lmp.domain.dto.AppointmentRequest;
import com.lmp.domain.entity.Appointment;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.AppointmentStatus;
import com.lmp.repository.AppointmentRepository;
import com.lmp.repository.UserRepository;
import com.lmp.util.DateUtils;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Service pour la gestion des rendez-vous.
 * 
 * Ce service gère toutes les opérations métier liées aux rendez-vous :
 * - Création, modification, annulation
 * - Validation des créneaux horaires
 * - Notifications par email
 * - Rappels automatiques
 * - Gestion des conflits d'horaires
 */
@Service
@Transactional
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;
    
    // Cache simple pour les créneaux (clé = date, valeur = créneaux disponibles)
    private final Map<String, CachedSlots> slotsCache = new ConcurrentHashMap<>();
    
    // Classe interne pour stocker les créneaux en cache avec timestamp
    private static class CachedSlots {
        List<LocalDateTime> slots;
        LocalDateTime cachedAt;
        
        CachedSlots(List<LocalDateTime> slots) {
            this.slots = slots;
            this.cachedAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            // Cache valide pendant 5 minutes
            return LocalDateTime.now().isAfter(cachedAt.plusMinutes(5));
        }
    }

    /**
     * Crée un nouveau rendez-vous
     */
    public Appointment createAppointment(AppointmentForm form, String userEmail) {
        logger.info("Création d'un nouveau rendez-vous pour l'utilisateur: {}", userEmail);

        // Récupération de l'utilisateur
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));

        // Validation du formulaire
        validateAppointmentForm(form);

        // Vérification des conflits d'horaires
        checkTimeConflicts(form.getAppointmentDate(), form.getDurationMinutes());

        // Création de l'entité
        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setSubject(form.getSubject());
        appointment.setDescription(form.getDescription());
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setDurationMinutes(form.getDurationMinutes());
        appointment.setPriority(form.getPriority());
        appointment.setStatus(AppointmentStatus.PENDING);

        // Sauvegarde
        appointment = appointmentRepository.save(appointment);
        
        // Invalider le cache pour cette date
        invalidateCacheForDate(appointment.getAppointmentDate().toLocalDate());

        // Envoi de la notification de confirmation au client
        try {
            sendConfirmationEmail(appointment);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation pour le RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }
        
        // Notifier l'équipe LMP du nouveau rendez-vous
        try {
            notifyTeamNewAppointment(appointment);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour le RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }

            logger.info("Emails de confirmation envoyés pour le rendez-vous ID: {}", appointment.getId());
        return appointment;
    }

    /**
     * Confirme un rendez-vous
     */
    public Appointment confirmAppointment(Long appointmentId) {
        return confirmAppointment(appointmentId, null);
    }
    
    /**
     * Confirme un rendez-vous (version avec admin)
     */
    public Appointment confirmAppointment(Long appointmentId, String adminEmail) {
        logger.info("Confirmation du rendez-vous ID: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);
        
        if (!appointment.getStatus().isModifiable()) {
            throw new IllegalStateException("Ce rendez-vous ne peut plus être modifié");
        }

        appointment.confirm();
        appointment = appointmentRepository.save(appointment);

        // Envoi d'email de confirmation au client
        try {
            sendStatusChangeEmail(appointment, "confirmé");
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation pour le RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }
        
        // Notifier l'équipe du changement de statut
        try {
            notifyTeamStatusChange(appointment, "PENDING", "CONFIRMED", adminEmail != null ? adminEmail : "Système");
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour le RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }

        logger.info("Rendez-vous confirmé - ID: {}", appointmentId);
        return appointment;
    }

    /**
     * Annule un rendez-vous
     */
    public Appointment cancelAppointment(Long appointmentId, String reason) {
        return cancelAppointment(appointmentId, reason, null);
    }
    
    /**
     * Annule un rendez-vous (version avec admin)
     */
    public Appointment cancelAppointment(Long appointmentId, String reason, String adminEmail) {
        logger.info("Annulation du rendez-vous ID: {} - Raison: {}", appointmentId, reason);

        Appointment appointment = findAppointmentById(appointmentId);

        if (!appointment.getStatus().isCancellable()) {
            throw new IllegalStateException("Ce rendez-vous ne peut pas être annulé");
        }

        appointment.cancel(reason);
        appointment = appointmentRepository.save(appointment);

        // Envoi d'email d'annulation au client
        try {
            sendCancellationEmail(appointment, reason);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email d'annulation pour le RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }
        
        // Notifier l'équipe de l'annulation
        try {
            notifyTeamCancellation(appointment, reason, adminEmail != null ? adminEmail : "Système");
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour l'annulation du RDV {}: {}", 
                appointment.getId(), e.getMessage());
        }

        logger.info("Rendez-vous annulé - ID: {}", appointmentId);
        return appointment;
    }
    
    /**
     * Supprime définitivement un rendez-vous (hard delete)
     */
    public void deleteAppointment(Long appointmentId) {
        deleteAppointment(appointmentId, null);
    }
    
    /**
     * Supprime définitivement un rendez-vous (version avec admin)
     */
    public void deleteAppointment(Long appointmentId, String adminEmail) {
        logger.info("Suppression définitive du rendez-vous ID: {} par: {}", appointmentId, 
            adminEmail != null ? adminEmail : "Système");

        Appointment appointment = findAppointmentById(appointmentId);
        
        // Sauvegarder les informations avant suppression pour les notifications
        String clientName = appointment.getEffectiveClientName();
        String clientEmail = appointment.getEffectiveClientEmail();
        String subject = appointment.getSubject();
        LocalDateTime appointmentDate = appointment.getAppointmentDate();
        String description = appointment.getDescription();
        
        // Notifier le client de la suppression (si email disponible)
        if (clientEmail != null && !clientEmail.trim().isEmpty()) {
            try {
                sendDeletionEmail(appointment, adminEmail);
            } catch (Exception e) {
                logger.error("Erreur lors de l'envoi de l'email de suppression au client pour le RDV {}: {}", 
                    appointmentId, e.getMessage());
            }
        }
        
        // Notifier l'équipe de la suppression
        try {
            notifyTeamDeletion(appointment, adminEmail);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour la suppression du RDV {}: {}", 
                appointmentId, e.getMessage());
        }
        
        // Invalider le cache pour cette date
        invalidateCacheForDate(appointment.getAppointmentDate().toLocalDate());
        
        // Suppression définitive de la base de données
        appointmentRepository.delete(appointment);
        
        logger.info("Rendez-vous supprimé définitivement - ID: {} (Client: {}, Date: {})", 
            appointmentId, clientName, appointmentDate.format(DATETIME_FORMATTER));
    }

    /**
     * Met à jour un rendez-vous existant
     */
    public Appointment updateAppointment(Long appointmentId, AppointmentForm form) {
        logger.info("Mise à jour du rendez-vous ID: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);

        if (!appointment.getStatus().isModifiable()) {
            throw new IllegalStateException("Ce rendez-vous ne peut plus être modifié");
        }

        // Validation du formulaire
        validateAppointmentForm(form);

        // Vérification des conflits uniquement si la date/heure change
        if (!appointment.getAppointmentDate().equals(form.getAppointmentDate()) ||
            !appointment.getDurationMinutes().equals(form.getDurationMinutes())) {
            checkTimeConflicts(form.getAppointmentDate(), form.getDurationMinutes(), appointmentId);
        }

        // Mise à jour des champs
        appointment.setSubject(form.getSubject());
        appointment.setDescription(form.getDescription());
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setDurationMinutes(form.getDurationMinutes());
        appointment.setPriority(form.getPriority());

        appointment = appointmentRepository.save(appointment);

        // Notification de la modification
        sendUpdateEmail(appointment);

        logger.info("Rendez-vous mis à jour - ID: {}", appointmentId);
        return appointment;
    }

    /**
     * Démarre un rendez-vous
     */
    public Appointment startAppointment(Long appointmentId) {
        logger.info("Démarrage du rendez-vous ID: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Seuls les rendez-vous confirmés peuvent être démarrés");
        }

        appointment.start();
        appointment = appointmentRepository.save(appointment);

        logger.info("Rendez-vous démarré - ID: {}", appointmentId);
        return appointment;
    }

    /**
     * Termine un rendez-vous
     */
    public Appointment completeAppointment(Long appointmentId, String notes) {
        logger.info("Finalisation du rendez-vous ID: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new IllegalStateException("Seuls les rendez-vous en cours peuvent être terminés");
        }

        appointment.complete();
        if (notes != null && !notes.trim().isEmpty()) {
            appointment.setAdminNotes(notes);
        }
        
        appointment = appointmentRepository.save(appointment);

        // Notification de fin de rendez-vous
        sendCompletionEmail(appointment);

        logger.info("Rendez-vous terminé - ID: {}", appointmentId);
        return appointment;
    }

    /**
     * Marque un rendez-vous comme "no-show"
     */
    public Appointment markAsNoShow(Long appointmentId) {
        logger.info("Marquage en no-show du rendez-vous ID: {}", appointmentId);

        Appointment appointment = findAppointmentById(appointmentId);

        if (!appointment.getStatus().isActive()) {
            throw new IllegalStateException("Ce rendez-vous n'est pas actif");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        appointment = appointmentRepository.save(appointment);

        logger.info("Rendez-vous marqué en no-show - ID: {}", appointmentId);
        return appointment;
    }

    // ======== MÉTHODES DE RECHERCHE ========

    /**
     * Trouve un rendez-vous par son ID
     */
    @Transactional(readOnly = true)
    public Appointment findAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé avec l'ID: " + id));
    }

    /**
     * Récupère tous les rendez-vous d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Appointment> findUserAppointments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));
        return appointmentRepository.findByUserOrderByAppointmentDateDesc(user);
    }

    /**
     * Récupère tous les rendez-vous d'un utilisateur avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findUserAppointments(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));
        return appointmentRepository.findByUserOrderByAppointmentDateDesc(user, pageable);
    }

    /**
     * Récupère les rendez-vous à venir d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Appointment> findUpcomingAppointments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));
        return appointmentRepository.findUpcomingAppointments(user, LocalDateTime.now());
    }

    /**
     * Récupère tous les rendez-vous actifs
     */
    @Transactional(readOnly = true)
    public List<Appointment> findActiveAppointments() {
        return appointmentRepository.findActiveAppointments();
    }

    /**
     * Récupère tous les rendez-vous actifs avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findActiveAppointments(Pageable pageable) {
        return appointmentRepository.findActiveAppointments(pageable);
    }

    /**
     * Recherche de rendez-vous par mots-clés
     */
    @Transactional(readOnly = true)
    public List<Appointment> searchAppointments(String keyword) {
        return appointmentRepository.searchByKeyword(keyword);
    }

    /**
     * Recherche de rendez-vous par mots-clés avec pagination
     */
    @Transactional(readOnly = true)
    public Page<Appointment> searchAppointments(String keyword, Pageable pageable) {
        return appointmentRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Récupère les rendez-vous d'une journée spécifique
     */
    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsByDate(LocalDateTime date) {
        return appointmentRepository.findByAppointmentDate(date);
    }

    /**
     * Récupère les rendez-vous dans une plage de dates
     */
    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return appointmentRepository.findByAppointmentDateBetween(startDate, endDate);
    }

    /**
     * Récupère les rendez-vous urgents
     */
    @Transactional(readOnly = true)
    public List<Appointment> findUrgentAppointments() {
        return appointmentRepository.findUrgentAppointments();
    }

    // ======== MÉTHODES DE VALIDATION ========

    /**
     * Valide un formulaire de rendez-vous
     */
    private void validateAppointmentForm(AppointmentForm form) {
        if (!form.isValidAppointmentTime()) {
            throw new IllegalArgumentException("L'heure du rendez-vous doit être entre 9h et 17h, par créneaux de 30 minutes");
        }

        if (!form.isProfessionalSubject()) {
            throw new IllegalArgumentException("Le sujet du rendez-vous contient des termes non professionnels ou inappropriés");
        }
        
        // Si le sujet contient "Autre", vérifier que la description est fournie
        if (form.getSubject() != null && form.getSubject().toLowerCase().contains("autre")) {
            if (form.getDescription() == null || form.getDescription().trim().length() < 20) {
                throw new IllegalArgumentException("Pour le service 'Autre', veuillez décrire votre besoin en détail (minimum 20 caractères)");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        if (form.getAppointmentDate().isBefore(now.plusHours(24))) {
            throw new IllegalArgumentException("Les rendez-vous doivent être pris au moins 24h à l'avance");
        }
        
        // Vérifier que la date n'est pas trop éloignée (max 30 jours ouvrables)
        LocalDate appointmentDate = form.getAppointmentDate().toLocalDate();
        LocalDate today = LocalDate.now();
        int businessDays = DateUtils.calculateBusinessDaysBetween(today, appointmentDate);
        
        if (businessDays > 30) {
            logger.debug("Date de rendez-vous trop éloignée : {} jours ouvrables", businessDays);
            throw new IllegalArgumentException("La date sélectionnée est trop éloignée. Veuillez choisir une date dans les 30 prochains jours ouvrables.");
        }
        
        // Vérifier que c'est un jour ouvrable
        if (!DateUtils.isBusinessDay(appointmentDate)) {
            throw new IllegalArgumentException("Les rendez-vous ne peuvent être pris que les jours ouvrables (lundi à vendredi)");
        }
    }

    /**
     * Vérifie les conflits d'horaires
     */
    private void checkTimeConflicts(LocalDateTime appointmentDate, Integer durationMinutes) {
        checkTimeConflicts(appointmentDate, durationMinutes, null);
    }

    /**
     * Vérifie les conflits d'horaires (en excluant un rendez-vous spécifique)
     */
    private void checkTimeConflicts(LocalDateTime appointmentDate, Integer durationMinutes, Long excludeAppointmentId) {
        LocalDateTime endTime = appointmentDate.plusMinutes(durationMinutes);
        
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(appointmentDate, endTime);
        
        // Filtrer l'exclusion si nécessaire
        if (excludeAppointmentId != null) {
            conflicts = conflicts.stream()
                    .filter(a -> !a.getId().equals(excludeAppointmentId))
                    .toList();
        }

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Un rendez-vous existe déjà à ce créneau horaire");
        }
    }

    // ======== MÉTHODES D'EMAIL ========
    
    // Email de l'équipe LMP pour les notifications internes
    private static final String TEAM_EMAIL = "lmp.assistance@gmail.com";
    private static final String ADMIN_EMAIL = "admin@lmp-services.ca"; // Email admin principal

    /**
     * Envoie un email de confirmation de rendez-vous
     */
    private void sendConfirmationEmail(Appointment appointment) {
        logger.info("📧 DÉBUT - Envoi email confirmation pour RDV ID: {}", appointment.getId());
        
        // Vérifier si on a un email valide
        String clientEmail = appointment.getEffectiveClientEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            logger.warn("Impossible d'envoyer l'email de confirmation pour le RDV {} : aucun email client", appointment.getId());
            return;
        }
        
        logger.info("📫 Destinataire: {}", clientEmail);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(clientEmail);
            message.setSubject("Confirmation de votre demande de rendez-vous - LMP");
            
            logger.info("📧 Message préparé, tentative d'envoi...");
            
            // Récupérer le nom du client (utilisateur ou anonyme)
            String clientName = appointment.getEffectiveClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Client"; // Nom par défaut
            }
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre demande de rendez-vous a été enregistrée avec succès.\n\n" +
                "Détails du rendez-vous :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n" +
                "- Statut : En attente de confirmation\n\n" +
                "Nous vous confirmerons ce rendez-vous dans les plus brefs délais.\n\n" +
                "Cordialement,\nL'équipe LMP",
                clientName,
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes()
            );
            
            message.setText(body);
            
            logger.info("🚀 Envoi via mailSender.send()...");
            mailSender.send(message);
            logger.info("✅ mailSender.send() exécuté avec succès !");
            
            // Marquer comme envoyé
            appointment.setConfirmationSent(true);
            appointment.setConfirmationSentAt(LocalDateTime.now());
            
            logger.info("🎉 Email de confirmation envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }

    /**
     * Envoie un email de changement de statut
     */
    private void sendStatusChangeEmail(Appointment appointment, String status) {
        // Vérifier si on a un email valide
        String clientEmail = appointment.getEffectiveClientEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            logger.warn("Impossible d'envoyer l'email de changement de statut pour le RDV {} : aucun email client", appointment.getId());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(clientEmail);
            message.setSubject("Votre rendez-vous a été " + status + " - LMP");
            
            // Récupérer le nom du client (utilisateur ou anonyme)
            String clientName = appointment.getEffectiveClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Client"; // Nom par défaut
            }
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre rendez-vous a été %s.\n\n" +
                "Détails du rendez-vous :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n\n" +
                "Merci de votre confiance.\n\n" +
                "Cordialement,\nL'équipe LMP",
                clientName,
                status,
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email de changement de statut envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de changement de statut pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }

    /**
     * Envoie un email d'annulation
     */
    private void sendCancellationEmail(Appointment appointment, String reason) {
        // Vérifier si on a un email valide
        String clientEmail = appointment.getEffectiveClientEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            logger.warn("Impossible d'envoyer l'email d'annulation pour le RDV {} : aucun email client", appointment.getId());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(clientEmail);
            message.setSubject("Annulation de votre rendez-vous - LMP");
            
            // Récupérer le nom du client (utilisateur ou anonyme)
            String clientName = appointment.getEffectiveClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Client"; // Nom par défaut
            }
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre rendez-vous a été annulé.\n\n" +
                "Détails du rendez-vous :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Raison de l'annulation : %s\n\n" +
                "N'hésitez pas à reprendre rendez-vous si nécessaire.\n\n" +
                "Cordialement,\nL'équipe LMP",
                clientName,
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                reason
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email d'annulation envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email d'annulation pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Envoie un email de suppression définitive
     */
    private void sendDeletionEmail(Appointment appointment, String adminEmail) {
        // Vérifier si on a un email valide
        String clientEmail = appointment.getEffectiveClientEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            logger.warn("Impossible d'envoyer l'email de suppression pour le RDV {} : aucun email client", appointment.getId());
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(clientEmail);
            message.setSubject("Suppression de votre rendez-vous - LMP");
            
            // Récupérer le nom du client (utilisateur ou anonyme)
            String clientName = appointment.getEffectiveClientName();
            if (clientName == null || clientName.trim().isEmpty()) {
                clientName = "Client"; // Nom par défaut
            }
            
            String adminInfo = adminEmail != null ? 
                "\n\nCette suppression a été effectuée par notre équipe administrative." :
                "\n\nCette suppression a été effectuée automatiquement par notre système.";
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Nous vous informons que votre rendez-vous a été supprimé de notre système.\n\n" +
                "Détails du rendez-vous supprimé :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n%s\n\n" +
                "Si vous souhaitez reprendre rendez-vous, n'hésitez pas à nous contacter \n" +
                "ou à utiliser notre système de prise de rendez-vous en ligne.\n\n" +
                "Pour toute question, vous pouvez nous contacter à lmp.assistance@gmail.com\n\n" +
                "Cordialement,\nL'équipe LMP",
                clientName,
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes(),
                adminInfo
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email de suppression envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de suppression pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    // ======== NOTIFICATIONS ÉQUIPE LMP ========
    
    /**
     * Notifie l'équipe LMP d'un nouveau rendez-vous
     */
    private void notifyTeamNewAppointment(Appointment appointment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(TEAM_EMAIL);
            if (!ADMIN_EMAIL.equals(TEAM_EMAIL)) {
                message.setCc(ADMIN_EMAIL);
            }
            message.setSubject("🎆 Nouveau rendez-vous reçu - LMP Admin");
            
            String clientInfo = appointment.getEffectiveClientName() + 
                (appointment.getEffectiveClientEmail() != null ? " (" + appointment.getEffectiveClientEmail() + ")" : "");
            
            String body = String.format(
                "💼 NOUVEAU RENDEZ-VOUS REÇU\n\n" +
                "👤 Client : %s\n" +
                "📧 Email : %s\n" +
                "📞 Téléphone : %s\n\n" +
                "🕰 Date et heure : %s\n" +
                "🎯 Sujet : %s\n" +
                "⏱ Durée : %d minutes\n" +
                "🔸 Priorité : %d/10\n" +
                "📝 Statut : %s\n\n" +
                "💬 Description :\n%s\n\n" +
                "---\n" +
                "⚡ Action requise : Ce rendez-vous est en attente de confirmation.\n" +
                "🔗 Accéder à l'admin : %s/admin/appointments/%d\n\n" +
                "Notification automatique LMP",
                clientInfo,
                appointment.getEffectiveClientEmail() != null ? appointment.getEffectiveClientEmail() : "Non renseigné",
                appointment.getEffectiveClientPhone() != null ? appointment.getEffectiveClientPhone() : "Non renseigné",
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getSubject(),
                appointment.getDurationMinutes(),
                appointment.getPriority(),
                appointment.getStatus().getDisplayName(),
                appointment.getDescription() != null ? appointment.getDescription() : "Aucune description",
                "https://lmp-services.ca", // Base URL de l'app
                appointment.getId()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("📧 Notification équipe envoyée pour nouveau RDV ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour nouveau RDV ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Notifie l'équipe LMP d'un changement de statut de rendez-vous
     */
    private void notifyTeamStatusChange(Appointment appointment, String oldStatus, String newStatus, String adminEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(TEAM_EMAIL);
            if (!ADMIN_EMAIL.equals(TEAM_EMAIL)) {
                message.setCc(ADMIN_EMAIL);
            }
            message.setSubject("🔄 Changement statut RDV #" + appointment.getId() + " - LMP Admin");
            
            String clientInfo = appointment.getEffectiveClientName() + 
                (appointment.getEffectiveClientEmail() != null ? " (" + appointment.getEffectiveClientEmail() + ")" : "");
            
            String body = String.format(
                "🔄 CHANGEMENT DE STATUT\n\n" +
                "🎯 RDV ID : %d\n" +
                "👤 Client : %s\n" +
                "🕰 Date : %s\n" +
                "🎯 Sujet : %s\n\n" +
                "🔴 Ancien statut : %s\n" +
                "🟢 Nouveau statut : %s\n\n" +
                "👨‍💼 Modifié par : %s\n" +
                "⏰ Horodatage : %s\n\n" +
                "🔗 Voir détails : %s/admin/appointments/%d\n\n" +
                "Notification automatique LMP",
                appointment.getId(),
                clientInfo,
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getSubject(),
                oldStatus,
                newStatus,
                adminEmail != null ? adminEmail : "Système",
                LocalDateTime.now().format(DATETIME_FORMATTER),
                "https://lmp-services.ca",
                appointment.getId()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("📧 Notification équipe envoyée pour changement statut RDV ID: {} ({} -> {})", 
                    appointment.getId(), oldStatus, newStatus);
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour changement statut RDV ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Notifie l'équipe LMP d'une annulation de rendez-vous
     */
    private void notifyTeamCancellation(Appointment appointment, String reason, String adminEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(TEAM_EMAIL);
            if (!ADMIN_EMAIL.equals(TEAM_EMAIL)) {
                message.setCc(ADMIN_EMAIL);
            }
            message.setSubject("❌ Annulation RDV #" + appointment.getId() + " - LMP Admin");
            
            String clientInfo = appointment.getEffectiveClientName() + 
                (appointment.getEffectiveClientEmail() != null ? " (" + appointment.getEffectiveClientEmail() + ")" : "");
            
            String body = String.format(
                "❌ RENDEZ-VOUS ANNULÉ\n\n" +
                "🎯 RDV ID : %d\n" +
                "👤 Client : %s\n" +
                "🕰 Date prévue : %s\n" +
                "🎯 Sujet : %s\n\n" +
                "💬 Raison d'annulation :\n%s\n\n" +
                "👨‍💼 Annulé par : %s\n" +
                "⏰ Horodatage : %s\n\n" +
                "📝 Note : L'email d'annulation a été envoyé au client.\n\n" +
                "Notification automatique LMP",
                appointment.getId(),
                clientInfo,
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getSubject(),
                reason != null ? reason : "Aucune raison spécifiée",
                adminEmail != null ? adminEmail : "Système",
                LocalDateTime.now().format(DATETIME_FORMATTER)
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("📧 Notification équipe envoyée pour annulation RDV ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour annulation RDV ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Notifie l'équipe LMP d'une suppression définitive de rendez-vous
     */
    private void notifyTeamDeletion(Appointment appointment, String adminEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(TEAM_EMAIL);
            if (!ADMIN_EMAIL.equals(TEAM_EMAIL)) {
                message.setCc(ADMIN_EMAIL);
            }
            message.setSubject("🗑️ Suppression définitive RDV #" + appointment.getId() + " - LMP Admin");
            
            String clientInfo = appointment.getEffectiveClientName() + 
                (appointment.getEffectiveClientEmail() != null ? " (" + appointment.getEffectiveClientEmail() + ")" : "");
            
            String body = String.format(
                "🗑️ RENDEZ-VOUS SUPPRIMÉ DÉFINITIVEMENT\n\n" +
                "⚠️ ATTENTION : Cette action est irréversible !\n\n" +
                "🎯 RDV ID : %d\n" +
                "👤 Client : %s\n" +
                "🕰 Date prévue : %s\n" +
                "🎯 Sujet : %s\n" +
                "📝 Description : %s\n\n" +
                "👨‍💼 Supprimé par : %s\n" +
                "⏰ Horodatage : %s\n\n" +
                "📧 Note : Un email de notification a été envoyé au client.\n" +
                "🔄 Action : Le rendez-vous a été définitivement supprimé de la base de données.\n\n" +
                "Notification automatique LMP",
                appointment.getId(),
                clientInfo,
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getSubject(),
                appointment.getDescription() != null ? appointment.getDescription() : "Aucune description",
                adminEmail != null ? adminEmail : "Système",
                LocalDateTime.now().format(DATETIME_FORMATTER)
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("📧 Notification équipe envoyée pour suppression définitive RDV ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de la notification équipe pour suppression définitive RDV ID: {}", 
                    appointment.getId(), e);
        }
    }

    /**
     * Envoie un email de modification
     */
    private void sendUpdateEmail(Appointment appointment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(appointment.getUser().getEmail());
            message.setSubject("Modification de votre rendez-vous - LMP");
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre rendez-vous a été modifié.\n\n" +
                "Nouveaux détails :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n\n" +
                "Merci de prendre note de ces modifications.\n\n" +
                "Cordialement,\nL'équipe LMP",
                appointment.getUser().getFirstName(),
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email de modification envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("❌ Erreur lors de l'envoi de l'email de confirmation pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Envoie une notification à l'équipe pour un nouveau rendez-vous
     */
    private void sendTeamNotificationEmail(Appointment appointment) {
        logger.info("📧 Envoi notification équipe pour nouveau RDV ID: {}", appointment.getId());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo("lmp.assistance@gmail.com");
            message.setSubject("📅 Nouveau rendez-vous - " + appointment.getUser().getFirstName() + " " + appointment.getUser().getLastName());
            
            String body = String.format(
                "Un nouveau rendez-vous a été créé !✨\n\n" +
                "Détails du client :\n" +
                "- Nom : %s %s\n" +
                "- Email : %s\n" +
                "- Téléphone : %s\n\n" +
                "Détails du rendez-vous :\n" +
                "- Service : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n" +
                "- Statut : %s\n" +
                "- Description : %s\n\n" +
                "ID du rendez-vous : #%d\n\n" +
                "Action requise : Confirmer le rendez-vous avec le client.\n\n" +
                "---\n" +
                "Notification automatique - LMP Services",
                appointment.getUser().getFirstName() != null ? appointment.getUser().getFirstName() : "",
                appointment.getUser().getLastName() != null ? appointment.getUser().getLastName() : "",
                appointment.getUser().getEmail(),
                appointment.getUser().getPhone() != null ? appointment.getUser().getPhone() : "Non fourni",
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes(),
                appointment.getStatus().toString(),
                appointment.getDescription() != null ? appointment.getDescription() : "Aucune description",
                appointment.getId()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("🎉 Notification équipe envoyée avec succès pour RDV ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("❌ Erreur lors de l'envoi de la notification équipe pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }

    /**
     * Envoie un email de fin de rendez-vous
     */
    private void sendCompletionEmail(Appointment appointment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(appointment.getUser().getEmail());
            message.setSubject("Merci pour votre rendez-vous - LMP");
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Merci d'avoir pris rendez-vous avec nous.\n\n" +
                "Votre rendez-vous du %s concernant \"%s\" s'est terminé avec succès.\n\n" +
                "N'hésitez pas à nous recontacter si vous avez besoin d'un nouveau rendez-vous.\n\n" +
                "Cordialement,\nL'équipe LMP",
                appointment.getUser().getFirstName(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getSubject()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Email de fin envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de fin pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }

    // ======== TÂCHES AUTOMATIQUES ========

    /**
     * Envoie les rappels automatiques (exécuté toutes les heures)
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    public void sendAutomaticReminders() {
        logger.info("Début de l'envoi des rappels automatiques");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);

        List<Appointment> appointmentsToRemind = appointmentRepository
                .findAppointmentsNeedingReminder(now, in24Hours);

        for (Appointment appointment : appointmentsToRemind) {
            sendReminderEmail(appointment);
            appointment.setReminderSent(true);
            appointment.setReminderSentAt(LocalDateTime.now());
            appointmentRepository.save(appointment);
        }

        logger.info("Rappels automatiques terminés - {} rappels envoyés", appointmentsToRemind.size());
    }

    /**
     * Envoie un email de rappel
     */
    private void sendReminderEmail(Appointment appointment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(appointment.getUser().getEmail());
            message.setSubject("Rappel : Votre rendez-vous de demain - LMP");
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Nous vous rappelons que vous avez un rendez-vous demain :\n\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n\n" +
                "Merci de nous confirmer votre présence ou de nous prévenir en cas d'empêchement.\n\n" +
                "Cordialement,\nL'équipe LMP",
                appointment.getUser().getFirstName(),
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("Rappel envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi du rappel pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }

    // ======== MÉTHODES STATISTIQUES ========

    /**
     * Compte le nombre de rendez-vous par statut
     */
    @Transactional(readOnly = true)
    public long countAppointmentsByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    /**
     * Compte le nombre de rendez-vous d'un utilisateur
     */
    @Transactional(readOnly = true)
    public long countUserAppointments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));
        return appointmentRepository.countByUser(user);
    }

    /**
     * Récupère le prochain rendez-vous d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Optional<Appointment> findNextAppointment(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + userEmail));
        return appointmentRepository.findNextAppointment(user, LocalDateTime.now());
    }

    // ======== MÉTHODES POUR LE CONTRÔLEUR ========

    /**
     * Trouve un rendez-vous par ID (pour le contrôleur)
     */
    @Transactional(readOnly = true)
    public Appointment findById(Long id) {
        return findAppointmentById(id);
    }

    /**
     * Récupère les créneaux horaires disponibles pour une date donnée
     * Créneaux disponibles : 9h, 10h, 11h, 13h, 14h, 15h, 16h (durée 1 heure chacun)
     * Avec système de cache intelligent
     */
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlots(LocalDate date) {
        String cacheKey = date.toString();
        
        // Vérifier le cache (sauf pour aujourd'hui qui change constamment)
        if (!date.equals(LocalDate.now())) {
            CachedSlots cached = slotsCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                logger.info("📦 Créneaux récupérés depuis le cache pour le {} ({} créneaux)", 
                    date, cached.slots.size());
                return new ArrayList<>(cached.slots);
            }
        }
        
        logger.info("🔍 Recherche des créneaux disponibles pour le {} (BD)", date);
        
        List<LocalDateTime> availableSlots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Définir les heures pleines disponibles (9h, 10h, 11h, 13h, 14h, 15h, 16h)
        int[] availableHours = {9, 10, 11, 13, 14, 15, 16};
        
        for (int hour : availableHours) {
            LocalDateTime slotStart = date.atTime(hour, 0);
            LocalDateTime slotEnd = slotStart.plusHours(1); // Créneau d'1 heure
            
            // Si c'est aujourd'hui, ignorer les créneaux passés
            if (date.equals(LocalDate.now())) {
                if (slotStart.isBefore(now) || slotStart.equals(now)) {
                    logger.debug("⏭️ Créneau passé ou en cours ignoré : {}h (heure actuelle: {})", 
                        hour, now.format(DateTimeFormatter.ofPattern("HH:mm")));
                    continue;
                }
                
                // Pour aujourd'hui, vérifier aussi qu'on a au moins 1h d'avance
                if (slotStart.isBefore(now.plusHours(1))) {
                    logger.debug("⏭️ Créneau trop proche ignoré (moins d'1h d'avance) : {}h", hour);
                    continue;
                }
            }
            
            // Vérifier s'il n'y a pas de conflit dans la BD
            List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(slotStart, slotEnd);
            
            if (conflicts.isEmpty()) {
                availableSlots.add(slotStart);
                logger.debug("✅ Créneau disponible : {}h", hour);
            } else {
                logger.debug("❌ Créneau occupé : {}h (conflit avec {} rendez-vous)", hour, conflicts.size());
                for (Appointment conflict : conflicts) {
                    logger.trace("  → Conflit avec RDV ID {} : {} à {}", 
                        conflict.getId(), conflict.getSubject(), 
                        conflict.getAppointmentDate().format(DateTimeFormatter.ofPattern("HH:mm")));
                }
            }
        }
        
        logger.info("📊 Résultat : {} créneaux disponibles sur {} possibles pour le {}", 
            availableSlots.size(), availableHours.length, date);
        
        // Mettre en cache (sauf pour aujourd'hui)
        if (!date.equals(LocalDate.now())) {
            slotsCache.put(cacheKey, new CachedSlots(availableSlots));
            logger.debug("💾 Créneaux mis en cache pour le {}", date);
        }
        
        return availableSlots;
    }

    /**
     * Trouve les rendez-vous d'un utilisateur par statut
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findByUserAndStatus(User user, AppointmentStatus status, Pageable pageable) {
        // Utilisation de la méthode existante sans pagination puis conversion
        List<Appointment> appointments = appointmentRepository.findByUserAndStatusOrderByAppointmentDateDesc(user, status);
        // Implémentation simple de pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), appointments.size());
        List<Appointment> pageContent = appointments.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, appointments.size());
    }

    /**
     * Trouve les rendez-vous d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findByUser(User user, Pageable pageable) {
        return appointmentRepository.findByUserOrderByAppointmentDateDesc(user, pageable);
    }

    /**
     * Met à jour un rendez-vous avec validation de l'utilisateur
     */
    public Appointment updateAppointment(Long appointmentId, AppointmentForm form, User user) {
        Appointment appointment = findAppointmentById(appointmentId);
        
        // Vérifier que l'utilisateur est propriétaire du rendez-vous
        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à modifier ce rendez-vous");
        }
        
        return updateAppointment(appointmentId, form);
    }

    /**
     * Annule un rendez-vous avec validation de l'utilisateur
     */
    public Appointment cancelAppointment(Long appointmentId, String reason, User user) {
        Appointment appointment = findAppointmentById(appointmentId);
        
        // Vérifier que l'utilisateur est propriétaire du rendez-vous
        if (!appointment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à annuler ce rendez-vous");
        }
        
        return cancelAppointment(appointmentId, reason);
    }

    /**
     * Recherche générale de rendez-vous pour l'admin
     */
    @Transactional(readOnly = true)
    public Page<Appointment> findAppointments(AppointmentStatus status, LocalDateTime startDate,
                                            LocalDateTime endDate, Pageable pageable) {
        if (status != null) {
            return appointmentRepository.findByStatusOrderByAppointmentDateAsc(status, pageable);
        } else if (startDate != null && endDate != null) {
            // Conversion de la liste en page pour les dates
            List<Appointment> appointments = appointmentRepository.findByAppointmentDateBetween(startDate, endDate);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), appointments.size());
            List<Appointment> pageContent = appointments.subList(start, end);
            return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, appointments.size());
        } else {
            return appointmentRepository.findAll(pageable);
        }
    }

    /**
     * Met à jour les notes administratives
     */
    public Appointment updateAdminNotes(Long appointmentId, String notes) {
        Appointment appointment = findAppointmentById(appointmentId);
        appointment.setAdminNotes(notes);
        return appointmentRepository.save(appointment);
    }

    /**
     * Crée un rendez-vous avec un objet User
     */
    public Appointment createAppointment(AppointmentForm form, User user) {
        return createAppointment(form, user.getEmail());
    }
    
    /**
     * Crée un rendez-vous anonyme (sans compte utilisateur)
     */
    public Appointment createAnonymousAppointment(AppointmentRequest request) {
        logger.info("🔄 DÉBUT createAnonymousAppointment pour: {}", request.getEmail());
        logger.info("📋 Détails: nom={}, service={}, date={}", request.getName(), request.getService(), request.getAppointmentDateTime());

        // Conversion vers AppointmentForm pour validation
        AppointmentForm form = request.toAppointmentForm();
        
        // Validation du formulaire
        validateAppointmentForm(form);

        // Vérification des conflits d'horaires
        checkTimeConflicts(form.getAppointmentDate(), form.getDurationMinutes());

        // Création de l'entité sans utilisateur
        Appointment appointment = new Appointment();
        appointment.setUser(null); // Pas d'utilisateur connecté
        appointment.setClientName(request.getName());
        appointment.setClientEmail(request.getEmail());
        appointment.setClientPhone(request.getPhone());
        appointment.setSubject(form.getSubject());
        appointment.setDescription(form.getDescription());
        appointment.setAppointmentDate(form.getAppointmentDate());
        appointment.setDurationMinutes(form.getDurationMinutes());
        appointment.setPriority(form.getPriority());
        appointment.setStatus(AppointmentStatus.PENDING);

        // Sauvegarde
        appointment = appointmentRepository.save(appointment);
        
        // Invalider le cache pour cette date
        invalidateCacheForDate(appointment.getAppointmentDate().toLocalDate());

        // Envoi de la notification de confirmation au client
        sendAnonymousConfirmationEmail(appointment);
        
        // Envoi de la notification à l'équipe
        sendAnonymousTeamNotificationEmail(appointment);

        logger.info("Rendez-vous anonyme créé avec succès - ID: {}", appointment.getId());
        return appointment;
    }
    
    /**
     * Invalide le cache pour une date spécifique
     */
    private void invalidateCacheForDate(LocalDate date) {
        String cacheKey = date.toString();
        if (slotsCache.remove(cacheKey) != null) {
            logger.debug("🗑️ Cache invalidé pour la date {}", date);
        }
    }
    
    /**
     * Vide complètement le cache des créneaux
     */
    public void clearSlotsCache() {
        slotsCache.clear();
        logger.info("🗑️ Cache des créneaux vidé complètement");
    }
    
    /**
     * Envoie un email de confirmation pour un rendez-vous anonyme
     */
    private void sendAnonymousConfirmationEmail(Appointment appointment) {
        logger.info("📧 DÉBUT - Envoi email confirmation anonyme pour RDV ID: {}", appointment.getId());
        logger.info("📫 Destinataire: {}", appointment.getEffectiveClientEmail());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo(appointment.getEffectiveClientEmail());
            message.setSubject("Confirmation de votre demande de rendez-vous - LMP");
            
            String body = String.format(
                "Bonjour %s,\n\n" +
                "Votre demande de rendez-vous a été enregistrée avec succès.\n\n" +
                "Détails du rendez-vous :\n" +
                "- Sujet : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n" +
                "- Statut : En attente de confirmation\n\n" +
                "Nous vous confirmerons ce rendez-vous dans les plus brefs délais.\n\n" +
                "Cordialement,\nL'équipe LMP",
                appointment.getEffectiveClientName(),
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            // Marquer comme envoyé
            appointment.setConfirmationSent(true);
            appointment.setConfirmationSentAt(LocalDateTime.now());
            
            logger.info("🎉 Email de confirmation anonyme envoyé pour le rendez-vous ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation anonyme pour le rendez-vous ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Envoie une notification à l'équipe pour un nouveau rendez-vous anonyme
     */
    private void sendAnonymousTeamNotificationEmail(Appointment appointment) {
        logger.info("📧 Envoi notification équipe pour nouveau RDV anonyme ID: {}", appointment.getId());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("lmp.assistance@gmail.com");
            message.setTo("lmp.assistance@gmail.com");
            message.setSubject("📅 Nouveau rendez-vous - " + appointment.getEffectiveClientName());
            
            String body = String.format(
                "Un nouveau rendez-vous a été créé !✨\n\n" +
                "Détails du client :\n" +
                "- Nom : %s\n" +
                "- Email : %s\n" +
                "- Téléphone : %s\n\n" +
                "Détails du rendez-vous :\n" +
                "- Service : %s\n" +
                "- Date et heure : %s\n" +
                "- Durée : %d minutes\n" +
                "- Statut : %s\n" +
                "- Description : %s\n\n" +
                "ID du rendez-vous : #%d\n\n" +
                "Action requise : Confirmer le rendez-vous avec le client.\n\n" +
                "---\n" +
                "Notification automatique - LMP Services",
                appointment.getEffectiveClientName(),
                appointment.getEffectiveClientEmail(),
                appointment.getEffectiveClientPhone() != null ? appointment.getEffectiveClientPhone() : "Non fourni",
                appointment.getSubject(),
                appointment.getAppointmentDate().format(DATETIME_FORMATTER),
                appointment.getDurationMinutes(),
                appointment.getStatus().toString(),
                appointment.getDescription() != null ? appointment.getDescription() : "Aucune description",
                appointment.getId()
            );
            
            message.setText(body);
            mailSender.send(message);
            
            logger.info("🎉 Notification équipe envoyée avec succès pour RDV anonyme ID: {}", appointment.getId());
            
        } catch (MailException e) {
            logger.error("❌ Erreur lors de l'envoi de la notification équipe pour le rendez-vous anonyme ID: {}", 
                    appointment.getId(), e);
        }
    }
}
