package com.lmp.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Utilitaire pour les calculs de dates métier
 */
public class DateUtils {
    
    /**
     * Calcule le nombre de jours ouvrables entre deux dates
     * Les weekends (samedi et dimanche) ne sont pas comptés
     * 
     * @param startDate Date de début (non incluse dans le calcul)
     * @param endDate Date de fin (incluse dans le calcul)
     * @return Le nombre de jours ouvrables entre les deux dates
     */
    public static int calculateBusinessDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être nulles");
        }
        
        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            return 0;
        }
        
        int businessDays = 0;
        LocalDate currentDate = startDate.plusDays(1); // Start from the day after startDate
        
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                businessDays++;
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return businessDays;
    }
    
    /**
     * Calcule le nombre de jours ouvrables entre deux dates-heures
     * 
     * @param startDateTime Date-heure de début
     * @param endDateTime Date-heure de fin
     * @return Le nombre de jours ouvrables entre les deux dates
     */
    public static int calculateBusinessDaysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return calculateBusinessDaysBetween(
            startDateTime.toLocalDate(), 
            endDateTime.toLocalDate()
        );
    }
    
    /**
     * Vérifie si une date est un jour ouvrable (lundi à vendredi)
     * 
     * @param date La date à vérifier
     * @return true si c'est un jour ouvrable, false sinon
     */
    public static boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
    
    /**
     * Vérifie si une date-heure est un jour ouvrable
     * 
     * @param dateTime La date-heure à vérifier
     * @return true si c'est un jour ouvrable, false sinon
     */
    public static boolean isBusinessDay(LocalDateTime dateTime) {
        return isBusinessDay(dateTime.toLocalDate());
    }
    
    /**
     * Ajoute un nombre de jours ouvrables à une date
     * 
     * @param startDate Date de départ
     * @param businessDaysToAdd Nombre de jours ouvrables à ajouter
     * @return La date résultante après ajout des jours ouvrables
     */
    public static LocalDate addBusinessDays(LocalDate startDate, int businessDaysToAdd) {
        if (businessDaysToAdd < 0) {
            throw new IllegalArgumentException("Le nombre de jours ouvrables doit être positif");
        }
        
        LocalDate result = startDate;
        int addedDays = 0;
        
        while (addedDays < businessDaysToAdd) {
            result = result.plusDays(1);
            if (isBusinessDay(result)) {
                addedDays++;
            }
        }
        
        return result;
    }
    
    /**
     * Calcule la date limite maximale pour les rendez-vous (30 jours ouvrables)
     * 
     * @return La date maximale pour prendre un rendez-vous
     */
    public static LocalDate getMaxAppointmentDate() {
        return addBusinessDays(LocalDate.now(), 30);
    }
    
    /**
     * Vérifie si une date est dans la plage autorisée pour les rendez-vous
     * (Entre demain et 30 jours ouvrables dans le futur)
     * 
     * @param date La date à vérifier
     * @return true si la date est valide, false sinon
     */
    public static boolean isWithinAppointmentRange(LocalDate date) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate maxDate = getMaxAppointmentDate();
        
        return !date.isBefore(tomorrow) && !date.isAfter(maxDate);
    }
}
