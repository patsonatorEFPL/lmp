package com.lmp.integration.sync.repository;

import com.lmp.integration.sync.SyncDirection;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.domain.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, UUID> {

    // ==================== Queue FIFO ====================

    /**
     * Récupère le prochain batch d'événements à traiter (FIFO).
     * Utilise FOR UPDATE SKIP LOCKED pour le traitement concurrent.
     */
    @Query(value = """
            SELECT * FROM sync_event_log
            WHERE status = 'QUEUED'
              AND scheduled_at <= NOW()
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SyncEvent> findNextBatch(@Param("batchSize") int batchSize);

    /**
     * Événements FAILED retryables (retry_count < max_retries, scheduled_at passé).
     */
    @Query(value = """
            SELECT * FROM sync_event_log
            WHERE status = 'FAILED'
              AND retry_count < max_retries
              AND scheduled_at <= NOW()
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<SyncEvent> findRetryableFailed(@Param("batchSize") int batchSize);

    // ==================== Queries existantes ====================

    /**
     * Événements échoués retryables (direction sortante, retry < max).
     */
    List<SyncEvent> findByStatusAndDirectionAndRetryCountLessThan(
            SyncStatus status, SyncDirection direction, int maxRetryCount);

    /**
     * Événements par entité locale (pour audit / historique).
     */
    List<SyncEvent> findByLocalEntityIdOrderByCreatedAtDesc(UUID localEntityId);

    /**
     * Événements par type d'entité et statut.
     */
    List<SyncEvent> findByEntityTypeAndStatus(SyncEntityType entityType, SyncStatus status);

    /**
     * Compter les événements échoués depuis une date (monitoring).
     */
    long countByStatusAndCreatedAtAfter(SyncStatus status, LocalDateTime since);

    /**
     * Compter les événements dans un statut donné (monitoring queue).
     */
    long countByStatus(SyncStatus status);
}
