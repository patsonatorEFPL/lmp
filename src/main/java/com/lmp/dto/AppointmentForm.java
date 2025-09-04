package com.lmp.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour le formulaire de prise de rendez-vous
 * 
 * Cette classe représente les données du formulaire de rendez-vous avec validation
 * pour s'assurer que tous les champs obligatoires sont remplis correctement.
 */
public class AppointmentForm {
    
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;
    
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 255, message = "L'email ne peut pas dépasser 255 caractères")
    private String email;
    
    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{10,20}$", message = "Format de téléphone invalide")
    private String phone;
    
    @NotNull(message = "La date du rendez-vous est obligatoire")
    @Future(message = "La date doit être dans le futur")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;
    
    @NotNull(message = "L'heure du rendez-vous est obligatoire")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;
    
    @NotBlank(message = "L'objet du rendez-vous est obligatoire")
    @Size(min = 10, max = 500, message = "L'objet doit contenir entre 10 et 500 caractères")
    private String subject;
    
    @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
    private String notes;
    
    @Size(max = 100, message = "Le nom de l'entreprise ne peut pas dépasser 100 caractères")
    private String company;
    
    // Constructeurs
    public AppointmentForm() {}
    
    public AppointmentForm(String name, String email, String phone, LocalDate appointmentDate, 
                          LocalTime appointmentTime, String subject) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.subject = subject;
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
    
    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }
    
    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
    
    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }
    
    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    /**
     * Vérifie si l'heure du rendez-vous est dans les créneaux autorisés
     * @return true si l'heure est valide (9h-17h, créneaux de 30min)
     */
    public boolean isValidAppointmentTime() {
        if (appointmentTime == null) {
            return false;
        }
        
        // Vérifier que l'heure est entre 9h00 et 17h00
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);
        
        if (appointmentTime.isBefore(start) || appointmentTime.isAfter(end)) {
            return false;
        }
        
        // Vérifier que les minutes sont 00 ou 30
        int minutes = appointmentTime.getMinute();
        return minutes == 0 || minutes == 30;
    }
    
    /**
     * Vérifie si l'objet du rendez-vous semble être professionnel
     * @return true si l'objet contient des mots-clés professionnels
     */
    public boolean isProfessionalSubject() {
        if (subject == null || subject.trim().isEmpty()) {
            return false;
        }
        
        String lowerSubject = subject.toLowerCase();
        
        // Mots-clés professionnels acceptés
        String[] professionalKeywords = {
            "consultation", "projet", "développement", "service", "devis", 
            "collaboration", "partenariat", "technique", "business", "entreprise",
            "solution", "stratégie", "analyse", "formation", "support",
            "maintenance", "audit", "conseil", "accompagnement", "expertise"
        };
        
        // Mots-clés à éviter (non professionnels)
        String[] nonProfessionalKeywords = {
            "personnel", "privé", "famille", "amitié", "rendez-vous galant",
            "sortie", "loisir", "vacances", "personnel", "intime"
        };
        
        // Vérifier qu'il n'y a pas de mots non professionnels
        for (String keyword : nonProfessionalKeywords) {
            if (lowerSubject.contains(keyword)) {
                return false;
            }
        }
        
        // Vérifier qu'il y a au moins un mot professionnel ou que c'est assez long
        for (String keyword : professionalKeywords) {
            if (lowerSubject.contains(keyword)) {
                return true;
            }
        }
        
        // Si pas de mots-clés spécifiques, accepter si assez détaillé (>= 20 caractères)
        return subject.trim().length() >= 20;
    }
    
    @Override
    public String toString() {
        return "AppointmentForm{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", appointmentDate=" + appointmentDate +
                ", appointmentTime=" + appointmentTime +
                ", subject='" + subject + '\'' +
                ", notes='" + notes + '\'' +
                ", company='" + company + '\'' +
                '}';
    }
}