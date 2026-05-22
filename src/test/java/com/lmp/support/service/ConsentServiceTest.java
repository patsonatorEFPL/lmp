package com.lmp.support.service;

import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.ConsentRecord;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConsentServiceTest {

    private ConsentRepository repo;
    private SupportSessionService sessionService;
    private AuditLogService audit;
    private ConsentService service;

    @BeforeEach
    void setup() {
        repo = mock(ConsentRepository.class);
        sessionService = mock(SupportSessionService.class);
        audit = mock(AuditLogService.class);
        service = new ConsentService(repo, sessionService, audit);
    }

    @Test
    void acceptedConsentTransitionsSessionFromInvitedToConsentWait() {
        UUID sessionId = UUID.randomUUID();
        when(repo.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRecord rec = service.record(
            sessionId, true,
            List.of("screen", "voice", "system_audio", "commands", "recording"),
            "203.0.113.5", "Mozilla/5.0 ...", "abc123hash"
        );

        assertThat(rec.isAccepted()).isTrue();
        verify(sessionService).transition(eq(sessionId), eq(SessionStatus.CONSENT_WAIT), eq(null));
        verify(audit).append(eq(sessionId), eq(AuditEventType.CONSENT_GIVEN), eq(ActorType.CLIENT), any(), any());
    }

    @Test
    void refusedConsentAbortsSession() {
        UUID sessionId = UUID.randomUUID();
        when(repo.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.record(sessionId, false, List.of(), "203.0.113.5", "UA", "abc123hash");

        verify(sessionService).transition(eq(sessionId), eq(SessionStatus.ABORTED), eq("CONSENT_REFUSED"));
        verify(audit).append(eq(sessionId), eq(AuditEventType.CONSENT_REFUSED), eq(ActorType.CLIENT), any(), any());
    }

    @Test
    void nullIpAndItemsHandledGracefully() {
        UUID sessionId = UUID.randomUUID();
        when(repo.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentRecord rec = service.record(sessionId, true, null, null, null, "hash");
        assertThat(rec).isNotNull();
        verify(sessionService).transition(eq(sessionId), eq(SessionStatus.CONSENT_WAIT), eq(null));
    }
}
