package com.lmp.support.web;

import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditLogEntry;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.AuditLogRepository;
import com.lmp.support.service.AuditLogService;
import com.lmp.support.service.SupportSessionService;
import com.lmp.support.web.dto.AuditChainVerificationResponse;
import com.lmp.support.web.dto.AuditEntryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditControllerTest {

    private AuditLogRepository repo;
    private AuditLogService service;
    private SupportSessionService sessionService;
    private AuditController controller;

    @BeforeEach
    void setup() {
        repo = mock(AuditLogRepository.class);
        service = mock(AuditLogService.class);
        sessionService = mock(SupportSessionService.class);
        controller = new AuditController(repo, service, sessionService);
    }

    private AuditLogEntry entry(long id, String event, byte[] prev, byte[] cur) {
        AuditLogEntry e = new AuditLogEntry();
        e.setId(id);
        e.setSessionId(UUID.randomUUID());
        e.setEventType(event);
        e.setActorType(ActorType.SYSTEM);
        e.setPayload(Map.of("k", "v"));
        e.setOccurredAt(LocalDateTime.now());
        e.setOccurredAtMicros(1L);
        e.setPrevHash(prev);
        e.setEntryHash(cur);
        return e;
    }

    @Test
    void listReturnsHexEncodedHashes() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession(); s.setId(sid);
        when(sessionService.findById(sid)).thenReturn(s);

        AuditLogEntry e1 = entry(1, "SESSION_CREATED", null, new byte[]{0x01, (byte) 0xAB});
        AuditLogEntry e2 = entry(2, "SESSION_INVITED", new byte[]{0x01, (byte) 0xAB}, new byte[]{(byte) 0xFF});
        when(repo.findBySessionIdOrderByOccurredAtAsc(sid)).thenReturn(List.of(e1, e2));

        List<AuditEntryResponse> result = controller.list(sid);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).prevHashHex()).isNull();
        assertThat(result.get(0).entryHashHex()).isEqualTo("01ab");
        assertThat(result.get(1).prevHashHex()).isEqualTo("01ab");
        assertThat(result.get(1).entryHashHex()).isEqualTo("ff");
    }

    @Test
    void listEmptyForKnownSessionReturnsEmpty() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession(); s.setId(sid);
        when(sessionService.findById(sid)).thenReturn(s);
        when(repo.findBySessionIdOrderByOccurredAtAsc(sid)).thenReturn(List.of());

        assertThat(controller.list(sid)).isEmpty();
    }

    @Test
    void listPropagates404FromSessionLookup() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid))
            .thenThrow(new IllegalArgumentException("Session not found"));

        assertThatThrownBy(() -> controller.list(sid))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyReturnsTrueAndCount() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession(); s.setId(sid);
        when(sessionService.findById(sid)).thenReturn(s);
        when(service.verifyChain(sid)).thenReturn(true);
        when(repo.findBySessionIdOrderByOccurredAtAsc(sid)).thenReturn(List.of(
            entry(1, "A", null, new byte[]{1}),
            entry(2, "B", new byte[]{1}, new byte[]{2}),
            entry(3, "C", new byte[]{2}, new byte[]{3})
        ));

        AuditChainVerificationResponse r = controller.verify(sid);

        assertThat(r.sessionId()).isEqualTo(sid);
        assertThat(r.valid()).isTrue();
        assertThat(r.entryCount()).isEqualTo(3);
    }

    @Test
    void verifyReportsFalseWhenServiceDetectsTamper() {
        UUID sid = UUID.randomUUID();
        SupportSession s = new SupportSession(); s.setId(sid);
        when(sessionService.findById(sid)).thenReturn(s);
        when(service.verifyChain(sid)).thenReturn(false);
        when(repo.findBySessionIdOrderByOccurredAtAsc(sid)).thenReturn(List.of(
            entry(1, "A", null, new byte[]{1})));

        AuditChainVerificationResponse r = controller.verify(sid);
        assertThat(r.valid()).isFalse();
        assertThat(r.entryCount()).isEqualTo(1);
    }

    @Test
    void verifyPropagates404FromSessionLookup() {
        UUID sid = UUID.randomUUID();
        when(sessionService.findById(sid))
            .thenThrow(new IllegalArgumentException("Session not found"));

        assertThatThrownBy(() -> controller.verify(sid))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
