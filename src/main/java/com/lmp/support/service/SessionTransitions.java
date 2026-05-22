package com.lmp.support.service;

import com.lmp.support.domain.SessionStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Allowed transitions for {@link SessionStatus}. Terminal states (ARCHIVED,
 * ABORTED) have no outgoing edges. Every non-terminal state may also exit
 * to ABORTED as a side-channel (cancelled/timed-out/refused).
 */
public final class SessionTransitions {

    private static final Map<SessionStatus, Set<SessionStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(SessionStatus.class);
        ALLOWED.put(SessionStatus.DRAFT,        EnumSet.of(SessionStatus.INVITED, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.INVITED,      EnumSet.of(SessionStatus.CONSENT_WAIT, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.CONSENT_WAIT, EnumSet.of(SessionStatus.ACTIVE, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.ACTIVE,       EnumSet.of(SessionStatus.ENDING, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.ENDING,       EnumSet.of(SessionStatus.MUXING, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.MUXING,       EnumSet.of(SessionStatus.ARCHIVED, SessionStatus.ABORTED));
        ALLOWED.put(SessionStatus.ARCHIVED,     EnumSet.noneOf(SessionStatus.class));
        ALLOWED.put(SessionStatus.ABORTED,      EnumSet.noneOf(SessionStatus.class));
    }

    private SessionTransitions() {}

    public static boolean isAllowed(SessionStatus from, SessionStatus to) {
        return ALLOWED.get(from).contains(to);
    }
}
