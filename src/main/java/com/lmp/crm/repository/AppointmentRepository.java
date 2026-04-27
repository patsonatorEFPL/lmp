package com.lmp.crm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.crm.domain.Appointment;
import com.lmp.auth.domain.User;
import com.lmp.crm.domain.AppointmentStatus;

import java.util.UUID;

/**
 * Repository pour la gestion des rendez-vous.
 * 
 * Fournit des méthodes de requête personnalisées pour les opérations
 * courantes sur les rendez-vous, incluant la recherche par utilisateur,
 * statut, plage de dates, et les rendez-vous nécessitant des rappels.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    /**
     * Trouve tous les rendez-vous d'un utilisateur donné
     */
    List<Appointment> findByUserOrderByAppointmentDateDesc(User user);

    /**
     * Trouve tous les rendez-vous d'un utilisateur avec pagination
     */
    Page<Appointment> findByUserOrderByAppointmentDateDesc(User user, Pageable pageable);

    /**
     * Trouve tous les rendez-vous par statut
     */
    List<Appointment> findByStatusOrderByAppointmentDateAsc(AppointmentStatus status);

    /**
     * Trouve tous les rendez-vous par statut avec pagination
     */
    Page<Appointment> findByStatusOrderByAppointmentDateAsc(AppointmentStatus status, Pageable pageable);

    /**
     * Trouve tous les rendez-vous d'un utilisateur avec un statut donné
     */
    List<Appointment> findByUserAndStatusOrderByAppointmentDateDesc(User user, AppointmentStatus status);

    /**
     * Trouve tous les rendez-vous dans une plage de dates
     */
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate BETWEEN :startDate AND :endDate ORDER BY a.appointmentDate ASC")
    List<Appointment> findByAppointmentDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Trouve tous les rendez-vous d'une journée spécifique
     */
    @Query("SELECT a FROM Appointment a WHERE DATE(a.appointmentDate) = DATE(:date) ORDER BY a.appointmentDate ASC")
    List<Appointment> findByAppointmentDate(@Param("date") LocalDateTime date);

    /**
     * Compte le nombre de rendez-vous d'une journée spécifique
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE DATE(a.appointmentDate) = DATE(:date)")
    long countByAppointmentDate(@Param("date") LocalDateTime date);

    /**
     * Trouve tous les rendez-vous actifs (PENDING, CONFIRMED, IN_PROGRESS)
     */
    @Query("SELECT a FROM Appointment a WHERE a.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS') ORDER BY a.appointmentDate ASC")
    List<Appointment> findActiveAppointments();

    /**
     * Trouve tous les rendez-vous actifs avec pagination
     */
    @Query("SELECT a FROM Appointment a WHERE a.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS') ORDER BY a.appointmentDate ASC")
    Page<Appointment> findActiveAppointments(Pageable pageable);

    /**
     * Trouve tous les rendez-vous nécessitant un rappel (24h avant)
     */
    @Query("SELECT a FROM Appointment a WHERE a.reminderSent = false AND a.status IN ('PENDING', 'CONFIRMED') AND a.appointmentDate BETWEEN :now AND :in24Hours")
    List<Appointment> findAppointmentsNeedingReminder(@Param("now") LocalDateTime now, @Param("in24Hours") LocalDateTime in24Hours);

    /**
     * Trouve tous les rendez-vous en conflit pour un créneau donné
     * Logique correcte de détection des chevauchements :
     * - Deux rendez-vous se chevauchent si l'un ne finit pas avant que l'autre ne commence
     * - Formule : NOT (nouveauRDV_fin <= existantRDV_debut OR nouveauRDV_debut >= existantRDV_fin)
     */
    // Les méthodes findConflictingAppointments ne sont plus utilisées
    // La détection de conflits se fait maintenant côté Java dans AppointmentService
    /**
     * Compte le nombre de rendez-vous par statut
     */
    long countByStatus(AppointmentStatus status);

    /**
     * Compte le nombre de rendez-vous d'un utilisateur
     */
    long countByUser(User user);

    /**
     * Compte le nombre de rendez-vous d'un utilisateur par statut
     */
    long countByUserAndStatus(User user, AppointmentStatus status);

    /**
     * Trouve les rendez-vous récents (derniers 30 jours)
     */
    @Query("SELECT a FROM Appointment a WHERE a.createdAt >= :thirtyDaysAgo ORDER BY a.createdAt DESC")
    List<Appointment> findRecentAppointments(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Trouve les rendez-vous à venir pour un utilisateur
     */
    @Query("SELECT a FROM Appointment a WHERE a.user = :user AND a.appointmentDate > :now AND a.status IN ('PENDING', 'CONFIRMED') ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingAppointments(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Trouve les rendez-vous passés pour un utilisateur
     */
    @Query("SELECT a FROM Appointment a WHERE a.user = :user AND a.appointmentDate < :now ORDER BY a.appointmentDate DESC")
    List<Appointment> findPastAppointments(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Recherche de rendez-vous par mots-clés dans le sujet ou la description
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(LOWER(a.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.appointmentDate DESC")
    List<Appointment> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Recherche de rendez-vous par mots-clés avec pagination
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(LOWER(a.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.appointmentDate DESC")
    Page<Appointment> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Trouve les rendez-vous par priorité
     */
    List<Appointment> findByPriorityOrderByAppointmentDateAsc(Integer priority);

    /**
     * Trouve les rendez-vous urgents (priorité <= 3)
     */
    @Query("SELECT a FROM Appointment a WHERE a.priority <= 3 AND a.status IN ('PENDING', 'CONFIRMED') ORDER BY a.priority ASC, a.appointmentDate ASC")
    List<Appointment> findUrgentAppointments();

    /**
     * Vérifie s'il existe déjà un rendez-vous pour un utilisateur à une date donnée
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.user = :user AND a.appointmentDate = :appointmentDate AND a.status IN ('PENDING', 'CONFIRMED')")
    boolean existsByUserAndAppointmentDate(@Param("user") User user, @Param("appointmentDate") LocalDateTime appointmentDate);

    /**
     * Trouve le prochain rendez-vous d'un utilisateur
     */
    @Query("SELECT a FROM Appointment a WHERE a.user = :user AND a.appointmentDate > :now AND a.status IN ('PENDING', 'CONFIRMED') ORDER BY a.appointmentDate ASC LIMIT 1")
    Optional<Appointment> findNextAppointment(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Statistiques : Nombre de rendez-vous par jour sur les 7 derniers jours
     */
    @Query("SELECT DATE(a.appointmentDate) as appointmentDate, COUNT(a) as count " +
           "FROM Appointment a WHERE a.appointmentDate >= :sevenDaysAgo " +
           "GROUP BY DATE(a.appointmentDate) ORDER BY DATE(a.appointmentDate)")
    List<Object[]> getAppointmentStatsLast7Days(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);

    /**
     * Statistiques : Nombre de rendez-vous par statut
     */
    @Query("SELECT a.status, COUNT(a) FROM Appointment a GROUP BY a.status")
    List<Object[]> getAppointmentStatsByStatus();
}