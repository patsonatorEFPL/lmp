package com.lmp.integration.sync.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncHealthSnapshotRepository extends JpaRepository<SyncHealthSnapshot, UUID> {

    Optional<SyncHealthSnapshot> findTopByOrderByRecordedAtDesc();
}
