package com.lmp.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.WebhookEventLog;

/**
 * Repository pour la gestion des logs d'événements webhook (idempotence).
 */
@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, UUID> {
    boolean existsByEventId(String eventId);
    Optional<WebhookEventLog> findByEventId(String eventId);
    List<WebhookEventLog> findByStatusAndProviderOrderByProcessedAtDesc(String status, String provider);
}
