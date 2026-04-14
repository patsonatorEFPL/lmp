package com.lmp.shared.monitoring;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository pour les enregistrements bruts de santé API.
 */
public interface ApiHealthRecordRepository extends JpaRepository<ApiHealthRecord, Long> {

    /**
     * Récupère tous les enregistrements entre deux timestamps, ordonnés par date.
     */
    List<ApiHealthRecord> findByRecordedAtBetweenOrderByRecordedAtAsc(Instant from, Instant to);

    /**
     * Purge les enregistrements antérieurs au timestamp donné.
     */
    @Modifying
    @Query("DELETE FROM ApiHealthRecord r WHERE r.recordedAt < :cutoff")
    int deleteByRecordedAtBefore(@Param("cutoff") Instant cutoff);
}
