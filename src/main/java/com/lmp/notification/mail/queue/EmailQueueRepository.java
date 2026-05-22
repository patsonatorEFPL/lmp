package com.lmp.notification.mail.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueueEvent, UUID>, EmailQueueRepositoryCustom {

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
}
