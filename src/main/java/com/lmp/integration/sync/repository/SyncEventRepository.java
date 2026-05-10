package com.lmp.integration.sync.repository;

import com.lmp.integration.sync.SyncDirection;
import com.lmp.integration.sync.SyncEntityType;
import com.lmp.integration.sync.SyncStatus;
import com.lmp.integration.sync.domain.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, UUID>, SyncEventRepositoryCustom {

    // ==================== Queue FIFO (claim/recovery dans Custom impl) ====================

    /**
     * Recovery des événements PROCESSING orphelins (réplica crashed mid-process).
     * Sans ça, claim atomique = trou noir : events stuck PROCESSING jamais retraités.
     */
    @Modifying
    @Query(value = """
            UPDATE sync_event_log
               SET status = 'QUEUED', processed_at = NULL
             WHERE status = 'PROCESSING'
               AND processed_at < :staleBefore
            """, nativeQuery = true)
    int recoverStaleProcessing(@Param("staleBefore") LocalDateTime staleBefore);

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

    // ==================== Verification ====================

    /**
     * Événements SUCCESS non encore vérifiés (verified_at IS NULL),
     * traités avant le cutoff. Limité pour ne pas surcharger l'API externe.
     */
    @Query(value = """
            SELECT * FROM sync_event_log
            WHERE status = 'SUCCESS'
              AND verified_at IS NULL
              AND processed_at IS NOT NULL
              AND processed_at < :cutoff
            ORDER BY processed_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<SyncEvent> findUnverifiedSuccessEvents(@Param("cutoff") LocalDateTime cutoff,
                                                 @Param("limit") int limit);

    /**
     * Compte les événements SUCCESS non vérifiés plus anciens qu'un seuil (alerting).
     */
    @Query(value = """
            SELECT COUNT(*) FROM sync_event_log
            WHERE status = 'SUCCESS'
              AND verified_at IS NULL
              AND processed_at IS NOT NULL
              AND processed_at < :olderThan
            """, nativeQuery = true)
    long countUnverifiedSuccessOlderThan(@Param("olderThan") LocalDateTime olderThan);

    // ==================== Monitoring / Health ====================

    long countByStatusIn(List<SyncStatus> statuses);

    long countByDirectionAndCreatedAtAfter(SyncDirection direction, LocalDateTime since);

    long countByStatusAndDirectionAndCreatedAtAfter(SyncStatus status, SyncDirection direction, LocalDateTime since);

    List<SyncEvent> findTop10ByStatusAndErrorMessageNotNullOrderByCreatedAtDesc(SyncStatus status);

    // ==================== Purge ====================

    /**
     * Supprime les événements dans un statut donné plus anciens qu'une date.
     * Retourne le nombre de lignes supprimées.
     */
    @Modifying
    @Query(value = """
            DELETE FROM sync_event_log
            WHERE status = :status
              AND created_at < :before
            """, nativeQuery = true)
    int deleteByStatusAndCreatedAtBefore(@Param("status") String status,
                                          @Param("before") LocalDateTime before);
}
