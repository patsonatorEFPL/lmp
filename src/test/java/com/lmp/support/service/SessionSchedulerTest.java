package com.lmp.support.service;

import com.lmp.support.config.SupportProperties;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionSchedulerTest {

    private SupportSessionRepository repo;
    private SupportSessionService sessionService;
    private SupportProperties props;

    // Fixed reference instant for deterministic test
    private final LocalDateTime now = LocalDateTime.of(2026, 5, 20, 12, 0, 0);
    private final Clock clock = Clock.fixed(
        now.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    @BeforeEach
    void setup() {
        repo = mock(SupportSessionRepository.class);
        sessionService = mock(SupportSessionService.class);
        props = new SupportProperties(
            Duration.ofMinutes(5),    // consentWaitTimeout
            Duration.ofMinutes(10),   // sessionIdleWarn
            Duration.ofMinutes(20),   // sessionIdleKill
            Duration.ofHours(4),      // sessionMaxDuration
            Duration.ofMinutes(15)    // runnerTokenTtl
        );
    }

    private SessionScheduler newScheduler() {
        return new SessionScheduler(repo, sessionService, props, clock);
    }

    private SupportSession session(SessionStatus status) {
        SupportSession s = new SupportSession();
        s.setId(UUID.randomUUID());
        s.setStatus(status);
        return s;
    }

    @Test
    void invitedExceedingConsentTimeoutGetsAborted() {
        SupportSession s = session(SessionStatus.INVITED);
        s.setInvitedAt(now.minusMinutes(6));  // 6 min > 5 min cap
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        int n = newScheduler().sweep();

        assertThat(n).isEqualTo(1);
        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_CONSENT_TIMEOUT));
    }

    @Test
    void consentWaitWithinTimeoutLeftAlone() {
        SupportSession s = session(SessionStatus.CONSENT_WAIT);
        s.setInvitedAt(now.minusMinutes(2));
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        int n = newScheduler().sweep();

        assertThat(n).isZero();
        verify(sessionService, never()).transition(any(), any(), any());
    }

    @Test
    void activeExceedingMaxDurationGetsAborted() {
        SupportSession s = session(SessionStatus.ACTIVE);
        s.setStartedAt(now.minusHours(5));  // > 4h cap
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        int n = newScheduler().sweep();

        assertThat(n).isEqualTo(1);
        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_MAX_DURATION));
    }

    @Test
    void activePastIdleKillGetsAborted() {
        SupportSession s = session(SessionStatus.ACTIVE);
        s.setStartedAt(now.minusMinutes(25));  // > 20 min idle
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        int n = newScheduler().sweep();

        assertThat(n).isEqualTo(1);
        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_IDLE_KILL));
    }

    @Test
    void maxDurationTakesPrecedenceOverIdle() {
        SupportSession s = session(SessionStatus.ACTIVE);
        s.setStartedAt(now.minusHours(5));  // also past idle, but max wins
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        newScheduler().sweep();

        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_MAX_DURATION));
    }

    @Test
    void activeWithinAllLimitsLeftAlone() {
        SupportSession s = session(SessionStatus.ACTIVE);
        s.setStartedAt(now.minusMinutes(5));
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        int n = newScheduler().sweep();
        assertThat(n).isZero();
    }

    @Test
    void invitedAtNullFallsBackToCreatedAt() {
        SupportSession s = session(SessionStatus.INVITED);
        s.setCreatedAt(now.minusMinutes(7));  // no invitedAt set
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        newScheduler().sweep();

        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_CONSENT_TIMEOUT));
    }

    @Test
    void startedAtNullFallsBackToCreatedAt() {
        SupportSession s = session(SessionStatus.ACTIVE);
        s.setCreatedAt(now.minusMinutes(25));
        when(repo.findByStatusIn(any())).thenReturn(List.of(s));

        newScheduler().sweep();

        verify(sessionService).transition(eq(s.getId()), eq(SessionStatus.ABORTED),
            eq(SessionScheduler.REASON_IDLE_KILL));
    }

    @Test
    void onlyNonTerminalStatusesScanned() {
        when(repo.findByStatusIn(any())).thenReturn(List.of());
        newScheduler().sweep();
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<SessionStatus>> captor =
            org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(repo).findByStatusIn(captor.capture());

        Collection<SessionStatus> scanned = captor.getValue();
        assertThat(scanned).contains(SessionStatus.INVITED, SessionStatus.CONSENT_WAIT, SessionStatus.ACTIVE);
        assertThat(scanned).doesNotContain(SessionStatus.ARCHIVED, SessionStatus.ABORTED, SessionStatus.DRAFT);
    }

    @Test
    void transitionFailureOnOneSessionDoesNotBreakSweepForOthers() {
        SupportSession s1 = session(SessionStatus.INVITED);
        s1.setInvitedAt(now.minusMinutes(6));
        SupportSession s2 = session(SessionStatus.INVITED);
        s2.setInvitedAt(now.minusMinutes(6));
        when(repo.findByStatusIn(any())).thenReturn(List.of(s1, s2));

        when(sessionService.transition(eq(s1.getId()), any(), any()))
            .thenThrow(new RuntimeException("simulated"));

        int n = newScheduler().sweep();

        // s2 still processed despite s1 failure
        assertThat(n).isEqualTo(1);
        verify(sessionService, times(2)).transition(any(), eq(SessionStatus.ABORTED), any());
    }

    @Test
    void emptyResultSetNoOp() {
        when(repo.findByStatusIn(any())).thenReturn(List.of());
        assertThat(newScheduler().sweep()).isZero();
        verify(sessionService, never()).transition(any(), any(), any());
    }
}
