package com.lmp.support.service;

import com.lmp.support.config.SupportProperties;
import com.lmp.support.domain.SessionStatus;
import com.lmp.support.domain.SupportSession;
import com.lmp.support.repository.SupportSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Enforces HelpDesk session timeouts. Runs on a fixed delay, scans every
 * non-terminal session, transitions stuck ones to {@code ABORTED} with an
 * end reason:
 *
 * <ul>
 *   <li>{@code CONSENT_WAIT_TIMEOUT} — invited too long without client consent.</li>
 *   <li>{@code SESSION_IDLE_KILL} — ACTIVE but no recent activity. (Idle detection
 *   currently uses {@code startedAt} as a baseline; finer-grained heartbeat lands
 *   when the activity tracker arrives — see P6.)</li>
 *   <li>{@code SESSION_MAX_DURATION} — ACTIVE past the absolute cap.</li>
 * </ul>
 *
 * Clock-injected so tests can fast-forward without sleeping.
 */
@Component
public class SessionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionScheduler.class);

    static final String REASON_CONSENT_TIMEOUT = "CONSENT_WAIT_TIMEOUT";
    static final String REASON_IDLE_KILL = "SESSION_IDLE_KILL";
    static final String REASON_MAX_DURATION = "SESSION_MAX_DURATION";

    private static final EnumSet<SessionStatus> SCANNED = EnumSet.of(
        SessionStatus.INVITED,
        SessionStatus.CONSENT_WAIT,
        SessionStatus.ACTIVE
    );

    private final SupportSessionRepository repo;
    private final SupportSessionService sessionService;
    private final SupportProperties props;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SessionScheduler(SupportSessionRepository repo,
                            SupportSessionService sessionService,
                            SupportProperties props) {
        this(repo, sessionService, props, Clock.systemDefaultZone());
    }

    /** Test ctor. */
    SessionScheduler(SupportSessionRepository repo,
                     SupportSessionService sessionService,
                     SupportProperties props,
                     Clock clock) {
        this.repo = repo;
        this.sessionService = sessionService;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${lmp.helpdesk.scheduler-interval:PT30S}",
               initialDelayString = "${lmp.helpdesk.scheduler-initial-delay:PT30S}")
    public void enforceTimeouts() {
        sweep();
    }

    /** Visible-for-test public entrypoint. */
    public int sweep() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SupportSession> candidates = repo.findByStatusIn(SCANNED);
        int aborted = 0;
        for (SupportSession s : candidates) {
            String reason = decide(s, now);
            if (reason != null) {
                try {
                    sessionService.transition(s.getId(), SessionStatus.ABORTED, reason);
                    aborted++;
                } catch (RuntimeException e) {
                    log.warn("scheduler failed to abort session {} reason={}: {}",
                        s.getId(), reason, e.getMessage());
                }
            }
        }
        return aborted;
    }

    private String decide(SupportSession s, LocalDateTime now) {
        Duration consentTimeout = props.consentWaitTimeout();
        Duration idleKill = props.sessionIdleKill();
        Duration maxDuration = props.sessionMaxDuration();

        switch (s.getStatus()) {
            case INVITED, CONSENT_WAIT -> {
                LocalDateTime since = s.getInvitedAt() != null ? s.getInvitedAt() : s.getCreatedAt();
                if (since != null && Duration.between(since, now).compareTo(consentTimeout) > 0) {
                    return REASON_CONSENT_TIMEOUT;
                }
            }
            case ACTIVE -> {
                LocalDateTime started = s.getStartedAt() != null ? s.getStartedAt() : s.getCreatedAt();
                if (started != null) {
                    Duration alive = Duration.between(started, now);
                    if (alive.compareTo(maxDuration) > 0) {
                        return REASON_MAX_DURATION;
                    }
                    if (alive.compareTo(idleKill) > 0) {
                        return REASON_IDLE_KILL;
                    }
                }
            }
            default -> { /* terminal or unscanned */ }
        }
        return null;
    }
}
