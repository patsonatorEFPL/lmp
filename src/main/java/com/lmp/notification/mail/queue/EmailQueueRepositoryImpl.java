package com.lmp.notification.mail.queue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class EmailQueueRepositoryImpl implements EmailQueueRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<EmailQueueEvent> claimNextBatch(int batchSize) {
        return entityManager.createNativeQuery("""
                UPDATE email_queue
                   SET status = 'SENDING',
                       status_changed_at = NOW()
                 WHERE id IN (
                     SELECT id FROM email_queue
                      WHERE status = 'NOT_SENT'
                        AND send_after <= NOW()
                      ORDER BY priority DESC, retry_count ASC, created_at ASC
                      LIMIT :batchSize
                      FOR UPDATE SKIP LOCKED
                 )
                 RETURNING *
                """, EmailQueueEvent.class)
                .setParameter("batchSize", batchSize)
                .getResultList();
    }
}
