package com.lmp.integration.sync.repository;

import com.lmp.integration.sync.domain.SyncEvent;

import java.util.List;

/**
 * Fragment de repository pour les opérations qui ne sont pas représentables
 * proprement via {@code @Query} Spring Data — typiquement les UPDATE...RETURNING
 * natifs Postgres qui retournent des entités.
 */
public interface SyncEventRepositoryCustom {

    /**
     * Claim atomique du prochain batch d'événements QUEUED.
     * <p>
     * UPDATE ... RETURNING garantit que la transition QUEUED → PROCESSING
     * et la sélection des lignes sont une opération atomique : aucun autre
     * réplica ne peut récupérer les mêmes événements (FOR UPDATE SKIP LOCKED
     * sur le sous-SELECT, puis flip de statut, retour des rows mises à jour).
     */
    List<SyncEvent> claimNextBatch(int batchSize);
}
