package com.lmp.crm.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "appointments")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    @Column(name = "client_name", length = 100)
    private String clientName;
    
    @Column(name = "client_email", length = 100)
    private String clientEmail;
    
    @Column(name = "client_phone", length = 20)
    private String clientPhone;
    
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;
    
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @Column(name = "client_notes", columnDefinition = "TEXT")
    private String clientNotes;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;
    
    @Column(name = "priority")
    private Integer priority = 5;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;
    
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
    
    @Column(name = "confirmation_sent")
    private Boolean confirmationSent = false;
    
    @Column(name = "confirmation_sent_at")
    private LocalDateTime confirmationSentAt;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public Appointment() {}
    
    public Appointment(User user, LocalDateTime appointmentDate, String subject) {
        this.user = user;
        this.appointmentDate = appointmentDate;
        this.subject = subject;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }
    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) {
        this.status = status;
        if (status == AppointmentStatus.CONFIRMED && confirmedAt == null) confirmedAt = LocalDateTime.now();
        else if (status == AppointmentStatus.CANCELLED && cancelledAt == null) cancelledAt = LocalDateTime.now();
        else if (status == AppointmentStatus.IN_PROGRESS && startedAt == null) startedAt = LocalDateTime.now();
        else if (status == AppointmentStatus.COMPLETED && completedAt == null) completedAt = LocalDateTime.now();
    }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public String getClientNotes() { return clientNotes; }
    public void setClientNotes(String clientNotes) { this.clientNotes = clientNotes; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getReminderSent() { return reminderSent; }
    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
        if (reminderSent && reminderSentAt == null) reminderSentAt = LocalDateTime.now();
    }
    public LocalDateTime getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(LocalDateTime reminderSentAt) { this.reminderSentAt = reminderSentAt; }
    public Boolean getConfirmationSent() { return confirmationSent; }
    public void setConfirmationSent(Boolean confirmationSent) {
        this.confirmationSent = confirmationSent;
        if (confirmationSent && confirmationSentAt == null) confirmationSentAt = LocalDateTime.now();
    }
    public LocalDateTime getConfirmationSentAt() { return confirmationSentAt; }
    public void setConfirmationSentAt(LocalDateTime confirmationSentAt) { this.confirmationSentAt = confirmationSentAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    // Utility methods
    public String getEffectiveClientName() { return user != null ? user.getFirstName() + " " + user.getLastName() : clientName; }
    public String getEffectiveClientEmail() { return user != null ? user.getEmail() : clientEmail; }
    public String getEffectiveClientPhone() { return user != null ? user.getPhone() : clientPhone; }
    public boolean isAnonymous() { return user == null; }
    public boolean isModifiable() { return status != null && status.isModifiable(); }
    public boolean isCancellable() { return status != null && status.isCancellable(); }
    public boolean isActive() { return status != null && status.isActive(); }
    public boolean needsReminder() {
        if (reminderSent || !isActive() || appointmentDate == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(appointmentDate.minusHours(24)) || now.isEqual(appointmentDate.minusHours(24));
    }
    public LocalDateTime getEstimatedEndTime() {
        return (appointmentDate != null && durationMinutes != null) ? appointmentDate.plusMinutes(durationMinutes) : null;
    }

    public void confirm() {
        if (status == AppointmentStatus.PENDING) setStatus(AppointmentStatus.CONFIRMED);
        else throw new IllegalStateException("Seuls les rendez-vous en attente peuvent être confirmés");
    }
    public void cancel(String reason) {
        if (isCancellable()) { setCancellationReason(reason); setStatus(AppointmentStatus.CANCELLED); }
        else throw new IllegalStateException("Ce rendez-vous ne peut pas être annulé dans son état actuel");
    }
    public void start() {
        if (status == AppointmentStatus.CONFIRMED) setStatus(AppointmentStatus.IN_PROGRESS);
        else throw new IllegalStateException("Seuls les rendez-vous confirmés peuvent être démarrés");
    }
    public void complete() {
        if (status == AppointmentStatus.IN_PROGRESS) setStatus(AppointmentStatus.COMPLETED);
        else throw new IllegalStateException("Seuls les rendez-vous en cours peuvent être marqués comme terminés");
    }
}
