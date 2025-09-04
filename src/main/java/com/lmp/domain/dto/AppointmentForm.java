package com.lmp.domain.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la création et modification de rendez-vous.
 * 
 * Contient toutes les validations nécessaires pour assurer
 * la cohérence des données de rendez-vous professionnels.
 */
public class AppointmentForm {

    @NotBlank(message = "Le sujet du rendez-vous est obligatoire")
    @Size(min = 5, max = 200, message = "Le sujet doit contenir entre 5 et 200 caractères")
    private String subject;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @NotNull(message = "La date et l'heure du rendez-vous sont obligatoires")
    @Future(message = "Le rendez-vous doit être planifié dans le futur")
    private LocalDateTime appointmentDate;

    @NotNull(message = "La durée du rendez-vous est obligatoire")
    @Min(value = 30, message = "La durée minimale d'un rendez-vous est de 30 minutes")
    private Integer durationMinutes = 60; // Durée par défaut : 1 heure

    private Integer priority = 5; // Priorité par défaut (1=urgent, 5=normal, 10=basse)

    /**
     * Valide que l'heure du rendez-vous respecte les créneaux autorisés
     * Heures d'ouverture : 9h à 17h, par créneaux de 30 minutes
     */
    public boolean isValidAppointmentTime() {
        if (appointmentDate == null) {
            return false;
        }

        int hour = appointmentDate.getHour();
        int minute = appointmentDate.getMinute();

        // Vérification des heures d'ouverture (9h à 17h)
        if (hour < 9 || hour >= 17) {
            return false;
        }

        // Vérification des créneaux de 30 minutes (00 ou 30)
        return minute == 0 || minute == 30;
    }

    /**
     * Valide que le sujet du rendez-vous est professionnel
     */
    public boolean isProfessionalSubject() {
        if (subject == null || subject.trim().isEmpty()) {
            return false;
        }

        String subjectLower = subject.toLowerCase().trim();
        
        // Liste des mots-clés professionnels acceptés
        List<String> professionalKeywords = Arrays.asList(
            "consultation", "conseil", "formation", "audit", "expertise",
            "analyse", "évaluation", "diagnostic", "stratégie", "développement",
            "optimisation", "assistance", "support", "accompagnement", "suivi",
            "présentation", "démonstration", "formation", "workshop", "séminaire",
            "réunion", "entretien", "négociation", "contrat", "partenariat",
            "projet", "planification", "coordination", "gestion", "supervision",
            "technique", "technologie", "innovation", "recherche", "étude",
            "marketing", "communication", "vente", "commercial", "business",
            "finance", "comptabilité", "juridique", "legal", "conformité",
            "qualité", "sécurité", "performance", "amélioration", "processus",
            "service", "prestation", "solution", "produit", "application",
            "système", "infrastructure", "réseau", "données", "information"
        );

        // Vérification qu'au moins un mot-clé professionnel est présent
        return professionalKeywords.stream()
                .anyMatch(keyword -> subjectLower.contains(keyword));
    }

    /**
     * Constructeur par défaut
     */
    public AppointmentForm() {
    }

    /**
     * Constructeur avec paramètres essentiels
     */
    public AppointmentForm(String subject, LocalDateTime appointmentDate, Integer durationMinutes) {
        this.subject = subject;
        this.appointmentDate = appointmentDate;
        this.durationMinutes = durationMinutes;
    }

    /**
     * Constructeur complet
     */
    public AppointmentForm(String subject, String description, LocalDateTime appointmentDate,
                          Integer durationMinutes, Integer priority) {
        this.subject = subject;
        this.description = description;
        this.appointmentDate = appointmentDate;
        this.durationMinutes = durationMinutes;
        this.priority = priority;
    }

    // ======== GETTERS ET SETTERS ========

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

    public LocalDateTime getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDateTime appointmentDate) {
        this.appointmentDate = appointmentDate;
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

    // ======== MÉTHODES UTILITAIRES ========

    @Override
    public String toString() {
        return "AppointmentForm{" +
                "subject='" + subject + '\'' +
                ", description='" + description + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", durationMinutes=" + durationMinutes +
                ", priority='" + priority + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppointmentForm that = (AppointmentForm) o;

        if (!subject.equals(that.subject)) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (!appointmentDate.equals(that.appointmentDate)) return false;
        if (!durationMinutes.equals(that.durationMinutes)) return false;
        return priority != null ? priority.equals(that.priority) : that.priority == null;
    }

    @Override
    public int hashCode() {
        int result = subject.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + appointmentDate.hashCode();
        result = 31 * result + durationMinutes.hashCode();
        result = 31 * result + (priority != null ? priority.hashCode() : 0);
        return result;
    }
}