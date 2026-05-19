package com.lmp.support.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmp.support.domain.ActorType;
import com.lmp.support.domain.AuditEventType;
import com.lmp.support.domain.AuditLogEntry;
import com.lmp.support.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditLogServiceTest {

    private AuditLogRepository repo;
    private AuditLogService service;

    @BeforeEach
    void setup() {
        repo = mock(AuditLogRepository.class);
        service = new AuditLogService(repo, new ObjectMapper());
    }

    @Test
    void firstEntryHasNullPrevHashAndNonNullEntryHash() {
        when(repo.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
        when(repo.save(any(AuditLogEntry.class))).thenAnswer(inv -> {
            AuditLogEntry e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        AuditLogEntry saved = service.append(
            UUID.randomUUID(),
            AuditEventType.SESSION_CREATED,
            ActorType.SYSTEM,
            Map.of("foo", "bar")
        );

        assertThat(saved.getPrevHash()).isNull();
        assertThat(saved.getEntryHash()).isNotNull().hasSize(32);
    }

    @Test
    void subsequentEntryLinksPrevHashToLastEntryHash() {
        AuditLogEntry first = new AuditLogEntry();
        first.setId(1L);
        byte[] firstHash = new byte[32];
        for (int i = 0; i < 32; i++) firstHash[i] = (byte) i;
        first.setEntryHash(firstHash);

        when(repo.findFirstByOrderByIdDesc()).thenReturn(Optional.of(first));
        when(repo.save(any(AuditLogEntry.class))).thenAnswer(inv -> {
            AuditLogEntry e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        AuditLogEntry second = service.append(
            UUID.randomUUID(),
            AuditEventType.SESSION_INVITED,
            ActorType.TECH,
            Map.of("a", 1)
        );

        assertThat(second.getPrevHash()).containsExactly(firstHash);
        assertThat(second.getEntryHash()).isNotNull().hasSize(32);
    }

    @Test
    void verifyChainDetectsTampering() {
        UUID sessionId = UUID.randomUUID();

        List<AuditLogEntry> persisted = new ArrayList<>();
        when(repo.findFirstByOrderByIdDesc()).thenAnswer(inv ->
            persisted.isEmpty() ? Optional.empty() : Optional.of(persisted.get(persisted.size() - 1)));
        when(repo.save(any(AuditLogEntry.class))).thenAnswer(inv -> {
            AuditLogEntry e = inv.getArgument(0);
            e.setId((long) persisted.size() + 1);
            persisted.add(e);
            return e;
        });

        service.append(sessionId, AuditEventType.SESSION_CREATED, ActorType.SYSTEM, Map.of("step", 1));
        service.append(sessionId, AuditEventType.SESSION_INVITED, ActorType.TECH, Map.of("step", 2));
        service.append(sessionId, AuditEventType.SESSION_STARTED, ActorType.SYSTEM, Map.of("step", 3));

        when(repo.findBySessionIdOrderByOccurredAtAsc(sessionId)).thenReturn(persisted);
        assertThat(service.verifyChain(sessionId)).isTrue();

        // Tamper with middle entry's payload without recomputing its hash
        persisted.get(1).setPayload(Map.of("step", "TAMPERED"));
        assertThat(service.verifyChain(sessionId)).isFalse();
    }

    @Test
    void emptyChainVerifiesAsValid() {
        UUID sessionId = UUID.randomUUID();
        when(repo.findBySessionIdOrderByOccurredAtAsc(sessionId)).thenReturn(List.of());
        assertThat(service.verifyChain(sessionId)).isTrue();
    }
}
