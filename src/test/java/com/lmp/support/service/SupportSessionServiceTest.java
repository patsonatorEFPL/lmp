package com.lmp.support.service;

import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportSessionServiceTest {

    private SupportSessionRepository repo;
    private AuditLogService audit;
    private SupportSessionService service;

    @BeforeEach
    void setup() {
        repo = mock(SupportSessionRepository.class);
        audit = mock(AuditLogService.class);
        service = new SupportSessionService(repo, audit);
    }

    @Test
    void createsSessionInDraftAndLogsEvent() {
        UUID techId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        when(repo.save(any(SupportSession.class))).thenAnswer(inv -> {
            SupportSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SupportSession created = service.create(ticketId, techId, clientId);

        assertThat(created.getStatus()).isEqualTo(SessionStatus.DRAFT);
        assertThat(created.getTicketId()).isEqualTo(ticketId);
        assertThat(created.getTechUserId()).isEqualTo(techId);
        assertThat(created.getClientUserId()).isEqualTo(clientId);
        assertThat(created.getRunnerTokenJti()).isNotNull();

        verify(audit).append(eq(created.getId()), eq(AuditEventType.SESSION_CREATED), any(), eq(techId), any());
    }

    @Test
    void transitionsDraftToInvitedAndStampsInvitedAt() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.DRAFT);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(SupportSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportSession updated = service.transition(id, SessionStatus.INVITED, null);

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.INVITED);
        assertThat(updated.getInvitedAt()).isNotNull();
        verify(audit).append(eq(id), eq(AuditEventType.SESSION_INVITED), any(), any(), any());
    }

    @Test
    void illegalTransitionThrows() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.transition(id, SessionStatus.DRAFT, null))
            .isInstanceOf(SessionTransitionException.class)
            .hasMessageContaining("ACTIVE -> DRAFT");
    }

    @Test
    void archivedIsTerminal() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.ARCHIVED);
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        for (SessionStatus s : SessionStatus.values()) {
            assertThatThrownBy(() -> service.transition(id, s, null))
                .isInstanceOf(SessionTransitionException.class);
        }
    }

    @Test
    void abortedIsTerminal() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.ABORTED);
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        for (SessionStatus s : SessionStatus.values()) {
            assertThatThrownBy(() -> service.transition(id, s, null))
                .isInstanceOf(SessionTransitionException.class);
        }
    }

    @Test
    void abortFromActiveStampsEndedAtAndReason() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.ACTIVE);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(SupportSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportSession aborted = service.transition(id, SessionStatus.ABORTED, "CLIENT_DISCONNECT");

        assertThat(aborted.getStatus()).isEqualTo(SessionStatus.ABORTED);
        assertThat(aborted.getEndedAt()).isNotNull();
        assertThat(aborted.getEndReason()).isEqualTo("CLIENT_DISCONNECT");
    }

    @Test
    void attachMeshGroupSavesId() {
        UUID id = UUID.randomUUID();
        SupportSession existing = new SupportSession();
        existing.setId(id);
        existing.setStatus(SessionStatus.DRAFT);
        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(SupportSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportSession updated = service.attachMeshGroup(id, "mesh//abc123");
        assertThat(updated.getMeshCentralGroupId()).isEqualTo("mesh//abc123");
    }
}
