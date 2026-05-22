package com.lmp.crm.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO pour les demandes de rendez-vous depuis le modal client.
 * 
 * Utilisé pour les rendez-vous pris via l'interface client,
 * que l'utilisateur soit connecté ou anonyme.
 */
public class AppointmentRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Size(min = 10, max = 20, message = "Le téléphone doit contenir entre 10 et 20 caractères")
    private String phone;

    @NotBlank(message = "Le service est obligatoire")
    private String service;

    @NotBlank(message = "La date est obligatoire")
    private String date; // Format: YYYY-MM-DD

    @NotBlank(message = "L'heure est obligatoire")
    private String time; // Format: HH:MM

    @Size(max = 500, message = "Le message ne peut pas dépasser 500 caractères")
    private String message;

    // Constructeurs
    public AppointmentRequest() {}

    public AppointmentRequest(String name, String email, String phone, String service, 
                            String date, String time, String message) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.service = service;
        this.date = date;
        this.time = time;
        this.message = message;
    }

    // Méthodes utilitaires
    
    /**
     * Convertit la date et l'heure en LocalDateTime
     */
    public LocalDateTime getAppointmentDateTime() {
        try {
            String dateTimeStr = date + " " + time;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format de date/heure invalide: " + date + " " + time);
        }
    }

    /**
     * Convertit vers AppointmentForm pour compatibilité
     */
    public AppointmentForm toAppointmentForm() {
        AppointmentForm form = new AppointmentForm();
        form.setSubject(resolveServiceLabel(service));
        form.setDescription(message);
        form.setAppointmentDate(getAppointmentDateTime());
        form.setDurationMinutes(60); // Défaut 60 minutes
        form.setPriority(5); // Priorité normale
        return form;
    }

    private static String resolveServiceLabel(String slug) {
        if (slug == null) return "Consultation";
        return switch (slug) {
            case "consultation" -> "Consultation générale";
            case "web"         -> "Développement Web";
            case "seo"         -> "Référencement SEO";
            case "formation"   -> "Formation";
            case "audit"       -> "Audit technique";
            case "marketing"   -> "Marketing Digital";
            case "security"    -> "Sécurité Web";
            case "other"       -> "Autre";
            default            -> slug;
        };
    }

    // Getters et Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "AppointmentRequest{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", service='" + service + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", message='" + (message != null ? message.substring(0, Math.min(50, message.length())) + "..." : "null") + '\'' +
                '}';
    }
}
