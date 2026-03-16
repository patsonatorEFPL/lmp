package com.lmp.notification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.notification.domain.InAppNotification;

/**
 * Repository pour les notifications in-app persistées.
 */
@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    /**
     * Récupère les 50 dernières notifications d'un utilisateur, triées par date décroissante.
     */
    @Query("SELECT n FROM InAppNotification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /**
     * Compte les notifications non lues d'un utilisateur.
     */
    @Query("SELECT COUNT(n) FROM InAppNotification n WHERE n.user.id = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") UUID userId);

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE InAppNotification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    /**
     * Supprime toutes les notifications d'un utilisateur.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM InAppNotification n WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
