package com.lmp.support.repository;

import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportSessionRepository extends JpaRepository<SupportSession, UUID> {

    Optional<SupportSession> findByRunnerTokenJti(UUID jti);

    Optional<SupportSession> findByMeshCentralGroupIdAndStatus(String meshId, SessionStatus status);

    List<SupportSession> findByStatus(SessionStatus status);

    List<SupportSession> findByTechUserIdOrderByCreatedAtDesc(UUID techUserId);
}
