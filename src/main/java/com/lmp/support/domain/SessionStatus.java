package com.lmp.support.domain;

/**
 * State machine for {@link SupportSession}. Terminal states (ARCHIVED, ABORTED)
 * have no outgoing transitions. Allowed transitions are enforced by
 * {@code SessionTransitions} and the {@code SupportSessionService}.
 */
public enum SessionStatus {
    DRAFT,
    INVITED,
    CONSENT_WAIT,
    ACTIVE,
    ENDING,
    MUXING,
    ARCHIVED,
    ABORTED
}
