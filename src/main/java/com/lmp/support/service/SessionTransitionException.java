package com.lmp.support.service;

import com.lmp.support.domain.SessionStatus;

public class SessionTransitionException extends RuntimeException {
    public SessionTransitionException(SessionStatus from, SessionStatus to) {
        super("Illegal transition: " + from + " -> " + to);
    }
}
