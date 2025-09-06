package com.lmp.domain.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        String subjectNormalized = normalizeString(subject.toLowerCase().trim());
        
        // Liste des services valides de la liste déroulante (normalisés)
        List<String> validServices = Arrays.asList(
            "consultation", "développement web", "developpement web", "référencement seo", "referencement seo", "formation",
            "audit", "marketing digital", "sécurité web", "securite web", "autre",
            // Anciens services pour compatibilité
            "création de site web", "creation de site web", "maintenance", "design graphique", 
            "développement application", "developpement application", "audit seo", "optimisation performance",
            "optimisation performance", "support technique", "e-commerce", "hébergement", "hebergement", "nom de domaine"
        ).stream().map(this::normalizeString).collect(Collectors.toList());
        
        // Liste des mots inappropriés à filtrer
        List<String> inappropriateWords = Arrays.asList(
            "gratuit", "urgent", "rapide", "immédiat", "immediat", "arnaque",
            "scam", "hack", "crack", "pirate", "illegal", "illégal",
            "casino", "pari", "jeu", "poker", "sexe",
            "drogue", "alcool", "cigarette", "violence", "arme"
        ).stream().map(this::normalizeString).collect(Collectors.toList());
        
        // Vérifier que le sujet ne contient pas de mots inappropriés
        for (String word : inappropriateWords) {
            if (subjectNormalized.contains(word)) {
                return false;
            }
        }
        
        // Si le sujet contient un service valide ou "autre", c'est acceptable
        // On accepte aussi tout sujet qui contient un nom (pour "Service - Nom du client")
        return validServices.stream().anyMatch(service -> subjectNormalized.contains(service)) ||
               subjectNormalized.contains(" - "); // Format "Service - Nom"
    }
    
    /**
     * Normalise une chaîne en supprimant les accents et caractères spéciaux
     */
    private String normalizeString(String input) {
        if (input == null) return "";
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
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