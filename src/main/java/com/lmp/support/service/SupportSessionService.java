package com.lmp.support.service;

import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the {@link SupportSession} lifecycle: creation in DRAFT, transitions
 * via {@link SessionTransitions}, timestamp stamping, audit log integration.
 * No external integrations (MeshCentral, R2, Claude) live here — they
 * orchestrate around this service.
 */
@Service
public class SupportSessionService {

    private final SupportSessionRepository repo;
    private final AuditLogService audit;

    public SupportSessionService(SupportSessionRepository repo, AuditLogService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional
    public SupportSession create(UUID ticketId, UUID techUserId, UUID clientUserId) {
        SupportSession s = new SupportSession();
        s.setTicketId(ticketId);
        s.setTechUserId(techUserId);
        s.setClientUserId(clientUserId);
        s.setRunnerTokenJti(UUID.randomUUID());
        s.setStatus(SessionStatus.DRAFT);
        s = repo.save(s);
        audit.append(s.getId(), AuditEventType.SESSION_CREATED, ActorType.TECH, techUserId,
            Map.of("techUserId", techUserId.toString(), "clientUserId", clientUserId.toString(),
                "ticketId", ticketId.toString()));
        return s;
    }

    @Transactional
    public SupportSession transition(UUID sessionId, SessionStatus to, String endReason) {
        SupportSession s = repo.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        SessionStatus from = s.getStatus();
        if (!SessionTransitions.isAllowed(from, to)) {
            throw new SessionTransitionException(from, to);
        }
        s.setStatus(to);
        stampTransitionTimestamp(s, to);
        if (endReason != null) {
            s.setEndReason(endReason);
        }
        SupportSession saved = repo.save(s);
        audit.append(saved.getId(), eventTypeFor(to), ActorType.SYSTEM, null,
            Map.of("from", from.name(), "to", to.name(), "endReason", endReason == null ? "" : endReason));
        return saved;
    }

    @Transactional
    public SupportSession attachMeshGroup(UUID sessionId, String meshGroupId) {
        SupportSession s = repo.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        s.setMeshCentralGroupId(meshGroupId);
        return repo.save(s);
    }

    @Transactional
    public SupportSession attachMeshNode(UUID sessionId, String nodeId) {
        SupportSession s = repo.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        s.setMeshCentralNodeId(nodeId);
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public SupportSession findById(UUID id) {
        return repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
    }

    private void stampTransitionTimestamp(SupportSession s, SessionStatus to) {
        LocalDateTime now = LocalDateTime.now();
        switch (to) {
            case INVITED      -> s.setInvitedAt(now);
            case CONSENT_WAIT -> s.setConsentAt(now);
            case ACTIVE       -> s.setStartedAt(now);
            case ENDING       -> s.setEndedAt(now);
            case ABORTED      -> { if (s.getEndedAt() == null) s.setEndedAt(now); }
            case ARCHIVED     -> s.setArchivedAt(now);
            case MUXING, DRAFT -> { /* no specific timestamp */ }
        }
    }

    private AuditEventType eventTypeFor(SessionStatus to) {
        return switch (to) {
            case INVITED      -> AuditEventType.SESSION_INVITED;
            case CONSENT_WAIT -> AuditEventType.SESSION_INVITED;
            case ACTIVE       -> AuditEventType.SESSION_STARTED;
            case ENDING       -> AuditEventType.SESSION_ENDED;
            case ABORTED      -> AuditEventType.SESSION_ABORTED;
            case ARCHIVED     -> AuditEventType.RECORDING_AVAILABLE;
            case MUXING       -> AuditEventType.SESSION_ENDED;
            case DRAFT        -> AuditEventType.SESSION_CREATED;
        };
    }
}
