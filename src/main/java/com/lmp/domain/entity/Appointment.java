package com.lmp.domain.entity;

import java.time.LocalDateTime;

import com.lmp.domain.enums.AppointmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Entité représentant un rendez-vous professionnel.
 * 
 * Cette entité gère les rendez-vous avec validation des créneaux horaires,
 * notifications automatiques et gestion des statuts.
 */
@Entity
@Table(name = "appointments")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Utilisateur qui a pris le rendez-vous
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Date et heure du rendez-vous
     */
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;
    
    /**
     * Objet/sujet du rendez-vous (doit être professionnel)
     */
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;
    
    /**
     * Description détaillée du rendez-vous
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Statut actuel du rendez-vous
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    
    /**
     * Notes administratives (visibles par les admins uniquement)
     */
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    /**
     * Notes du client (visibles par le client)
     */
    @Column(name = "client_notes", columnDefinition = "TEXT")
    private String clientNotes;
    
    /**
     * Durée estimée du rendez-vous en minutes
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60; // 1 heure par défaut
    
    /**
     * Priorité du rendez-vous (1=urgent, 5=normal, 10=basse)
     */
    @Column(name = "priority")
    private Integer priority = 5;
    
    /**
     * Indication si un rappel a été envoyé
     */
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    /**
     * Date et heure d'envoi du rappel
     */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
    
    /**
     * Indication si une confirmation a été envoyée
     */
    @Column(name = "confirmation_sent")
    private Boolean confirmationSent = false;
    
    /**
     * Date et heure d'envoi de la confirmation
     */
    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;
    
    /**
     * Raison d'annulation (si applicable)
     */
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    /**
     * Date et heure de création
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * Date et heure de dernière modification
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Date et heure de confirmation
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    /**
     * Date et heure d'annulation
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    /**
     * Date et heure de début effectif
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    /**
     * Date et heure de fin effective
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Constructeurs
    public Appointment() {}
    
    public Appointment(User user, LocalDateTime appointmentDate, String subject) {
        this.user = user;
        this.appointmentDate = appointmentDate;
        this.subject = subject;
    }
    
    // Méthodes de cycle de vie JPA
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters et Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public AppointmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AppointmentStatus status) {
        this.status = status;
        
        // Mettre à jour les timestamps selon le statut
        if (status == AppointmentStatus.CONFIRMED && confirmedAt == null) {
            confirmedAt = LocalDateTime.now();
        } else if (status == AppointmentStatus.CANCELLED && cancelledAt == null) {
            cancelledAt = LocalDateTime.now();
        } else if (status == AppointmentStatus.IN_PROGRESS && startedAt == null) {
            startedAt = LocalDateTime.now();
        } else if (status == AppointmentStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }
    
    public String getAdminNotes() {
        return adminNotes;
    }
    
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
    
    public String getClientNotes() {
        return clientNotes;
    }
    
    public void setClientNotes(String clientNotes) {
        this.clientNotes = clientNotes;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getReminderSent() {
        return reminderSent;
    }
    
    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
        if (reminderSent && reminderSentAt == null) {
            reminderSentAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }
    
    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
    
    public Boolean getConfirmationSent() {
        return confirmationSent;
    }
    
    public void setConfirmationSent(Boolean confirmationSent) {
        this.confirmationSent = confirmationSent;
        if (confirmationSent && confirmationSentAt == null) {
            confirmationSentAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getConfirmationSentAt() {
        return confirmationSentAt;
    }
    
    public void setConfirmationSentAt(LocalDateTime confirmationSentAt) {
        this.confirmationSentAt = confirmationSentAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
    
    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    // Méthodes utilitaires
    
    /**
     * Retourne true si le rendez-vous peut être modifié
     */
    public boolean isModifiable() {
        return status != null && status.isModifiable();
    }
    
    /**
     * Retourne true si le rendez-vous peut être annulé
     */
    public boolean isCancellable() {
        return status != null && status.isCancellable();
    }
    
    /**
     * Retourne true si le rendez-vous est actif
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }
    
    /**
     * Retourne true si un rappel doit être envoyé (24h avant)
     */
    public boolean needsReminder() {
        if (reminderSent || !isActive() || appointmentDate == null) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = appointmentDate.minusHours(24);
        
        return now.isAfter(reminderTime) || now.isEqual(reminderTime);
    }
    
    /**
     * Retourne la date de fin estimée du rendez-vous
     */
    public LocalDateTime getEstimatedEndTime() {
        if (appointmentDate == null || durationMinutes == null) {
            return null;
        }
        return appointmentDate.plusMinutes(durationMinutes);
    }
    
    /**
     * Retourne un résumé formaté du rendez-vous
     */
    public String getFormattedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("RDV ");
        if (user != null) {
            summary.append("avec ").append(user.getDisplayName());
        }
        if (appointmentDate != null) {
            summary.append(" le ").append(appointmentDate.toLocalDate())
                   .append(" à ").append(appointmentDate.toLocalTime());
        }
        if (subject != null && !subject.trim().isEmpty()) {
            summary.append(" - ").append(subject);
        }
        return summary.toString();
    }
    
    // Méthodes métier pour la gestion du cycle de vie
    
    /**
     * Confirme le rendez-vous
     */
    public void confirm() {
        if (status == AppointmentStatus.PENDING) {
            setStatus(AppointmentStatus.CONFIRMED);
        } else {
            throw new IllegalStateException("Seuls les rendez-vous en attente peuvent être confirmés");
        }
    }
    
    /**
     * Annule le rendez-vous avec une raison
     */
    public void cancel(String reason) {
        if (isCancellable()) {
            setCancellationReason(reason);
            setStatus(AppointmentStatus.CANCELLED);
        } else {
            throw new IllegalStateException("Ce rendez-vous ne peut pas être annulé dans son état actuel");
        }
    }
    
    /**
     * Démarre le rendez-vous (passage en cours)
     */
    public void start() {
        if (status == AppointmentStatus.CONFIRMED) {
            setStatus(AppointmentStatus.IN_PROGRESS);
        } else {
            throw new IllegalStateException("Seuls les rendez-vous confirmés peuvent être démarrés");
        }
    }
    
    /**
     * Marque le rendez-vous comme terminé
     */
    public void complete() {
        if (status == AppointmentStatus.IN_PROGRESS) {
            setStatus(AppointmentStatus.COMPLETED);
        } else {
            throw new IllegalStateException("Seuls les rendez-vous en cours peuvent être marqués comme terminés");
        }
    }
}