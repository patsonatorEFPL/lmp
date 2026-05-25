package com.lmp.notification.mail.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueueEvent, UUID>, EmailQueueRepositoryCustom, JpaSpecificationExecutor<EmailQueueEvent> {

    long countByStatus(EmailQueueStatus status);

    /**
     * Recovery des emails SENDING orphelins (réplica crash mid-send).
     * Reset à NOT_SENT pour que claim worker les reprenne.
     */
    @Modifying
    @Query(value = """
            UPDATE email_queue
               SET status = 'NOT_SENT',
                   status_changed_at = NOW(),
                   last_error = COALESCE(last_error, '') || E'\n[recovery] reset stale SENDING -> NOT_SENT'
             WHERE status = 'SENDING'
               AND status_changed_at < :staleBefore
            """, nativeQuery = true)
    int recoverStaleSending(@Param("staleBefore") LocalDateTime staleBefore);

    /**
     * Reset to NOT_SENT for a single id, only when currently in ERROR or NOT_SENT.
     * Avoids hijacking a SENDING row that a worker may have just claimed.
     */
    @Modifying
    @Query("""
            UPDATE EmailQueueEvent e
               SET e.status = com.lmp.notification.mail.queue.EmailQueueStatus.NOT_SENT,
                   e.statusChangedAt = CURRENT_TIMESTAMP,
                   e.lastError = CONCAT(COALESCE(e.lastError, ''), :tag)
             WHERE e.id = :id
               AND e.status IN (com.lmp.notification.mail.queue.EmailQueueStatus.ERROR,
                                com.lmp.notification.mail.queue.EmailQueueStatus.NOT_SENT)
            """)
    int markForRetry(@Param("id") UUID id, @Param("tag") String tag);

    /**
     * Bulk version of {@link #markForRetry} for ERROR rows only.
     */
    @Modifying
    @Query("""
            UPDATE EmailQueueEvent e
               SET e.status = com.lmp.notification.mail.queue.EmailQueueStatus.NOT_SENT,
                   e.statusChangedAt = CURRENT_TIMESTAMP,
                   e.lastError = CONCAT(COALESCE(e.lastError, ''), :tag)
             WHERE e.id IN :ids
               AND e.status = com.lmp.notification.mail.queue.EmailQueueStatus.ERROR
            """)
    int bulkMarkForRetry(@Param("ids") Collection<UUID> ids, @Param("tag") String tag);

    @Modifying
    @Query("DELETE FROM EmailQueueEvent e WHERE e.id IN :ids")
    int bulkDelete(@Param("ids") Collection<UUID> ids);

    /**
     * One query returning the count grouped by status — replaces 4 separate countByStatus calls.
     */
    @Query("SELECT e.status, COUNT(e) FROM EmailQueueEvent e GROUP BY e.status")
    List<Object[]> statsGroupByStatus();
}
