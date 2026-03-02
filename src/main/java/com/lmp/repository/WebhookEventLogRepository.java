package com.lmp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.WebhookEventLog;

/**
 * Repository pour la gestion des logs d'événements webhook (idempotence).
 */
@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    /**
     * Vérifie si un événement a déjà été traité.
     */
    boolean existsByEventId(String eventId);

    /**
     * Trouve un log d'événement par son ID Stripe.
     */
    Optional<WebhookEventLog> findByEventId(String eventId);

    /**
     * Trouve les événements en échec pour un provider donné, triés par date.
     */
    List<WebhookEventLog> findByStatusAndProviderOrderByProcessedAtDesc(String status, String provider);
}
