package com.lmp.support.repository;

import com.lmp.support.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * INSERT-only at the DB level (trigger {@code support_audit_log_block_modifications}
 * raises on UPDATE/DELETE). Spring Data still exposes save/saveAll which translate
 * to INSERT for new rows.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    List<AuditLogEntry> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);

    Optional<AuditLogEntry> findFirstByOrderByIdDesc();
}
