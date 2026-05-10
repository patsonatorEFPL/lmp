package com.lmp.integration.sync.repository;

import com.lmp.integration.sync.domain.SyncEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation custom pour {@link SyncEventRepositoryCustom}.
 * <p>
 * Spring Data détecte la classe {@code <RepoName>Impl} dans le même package
 * que le repository et l'agrège automatiquement.
 */
public class SyncEventRepositoryImpl implements SyncEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<SyncEvent> claimNextBatch(int batchSize) {
        // UPDATE ... RETURNING : atomic claim. PostgreSQL exécute le SELECT
        // FOR UPDATE SKIP LOCKED comme sous-requête, verrouille les rows
        // sélectionnés, applique l'UPDATE, retourne les rows mises à jour.
        // Aucun autre réplica ne peut voir le statut QUEUED après ce point.
        return entityManager.createNativeQuery("""
                UPDATE sync_event_log
                   SET status = 'PROCESSING', processed_at = NOW()
                 WHERE id IN (
                     SELECT id FROM sync_event_log
                      WHERE status = 'QUEUED'
                        AND scheduled_at <= NOW()
                      ORDER BY created_at ASC
                      LIMIT :batchSize
                      FOR UPDATE SKIP LOCKED
                 )
                 RETURNING *
                """, SyncEvent.class)
                .setParameter("batchSize", batchSize)
                .getResultList();
    }
}
