package com.lmp.support.service;

import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.ConsentRecord;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.repository.ConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsentService {

    private final ConsentRepository repo;
    private final SupportSessionService sessionService;
    private final AuditLogService audit;

    public ConsentService(ConsentRepository repo, SupportSessionService sessionService, AuditLogService audit) {
        this.repo = repo;
        this.sessionService = sessionService;
        this.audit = audit;
    }

    @Transactional
    public ConsentRecord record(UUID sessionId, boolean accepted, List<String> items,
                                String clientIp, String userAgent, String consentTextHash) {
        ConsentRecord r = new ConsentRecord();
        r.setSessionId(sessionId);
        r.setAccepted(accepted);
        r.setItemsConsented(items);
        r.setClientIp(clientIp);
        r.setClientUserAgent(userAgent);
        r.setConsentTextHash(consentTextHash);
        r = repo.save(r);

        if (accepted) {
            sessionService.transition(sessionId, SessionStatus.CONSENT_WAIT, null);
            audit.append(sessionId, AuditEventType.CONSENT_GIVEN, ActorType.CLIENT, null,
                Map.of("items", items == null ? List.of() : items,
                    "ip", clientIp == null ? "" : clientIp,
                    "hash", consentTextHash));
        } else {
            sessionService.transition(sessionId, SessionStatus.ABORTED, "CONSENT_REFUSED");
            audit.append(sessionId, AuditEventType.CONSENT_REFUSED, ActorType.CLIENT, null,
                Map.of("ip", clientIp == null ? "" : clientIp));
        }
        return r;
    }
}
