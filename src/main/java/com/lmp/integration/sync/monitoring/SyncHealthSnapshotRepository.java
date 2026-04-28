package com.lmp.integration.sync.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncHealthSnapshotRepository extends JpaRepository<SyncHealthSnapshot, UUID> {

    Optional<SyncHealthSnapshot> findTopByOrderByRecordedAtDesc();

    @Modifying
    @Query("DELETE FROM SyncHealthSnapshot s WHERE s.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") java.time.LocalDateTime before);
}
