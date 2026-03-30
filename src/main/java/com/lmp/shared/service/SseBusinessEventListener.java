package com.lmp.shared.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.lmp.integration.event.LmpBusinessEvent;

/**
 * Relaie les événements métier vers les clients SSE après commit transactionnel.
 * Évite les courses « notification reçue avant que la ligne ne soit visible en base ».
 */
@Component
public class SseBusinessEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SseBusinessEventListener.class);

    private final SseNotificationService sseNotificationService;

    public SseBusinessEventListener(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBusinessEventCommitted(LmpBusinessEvent event) {
        try {
            sseNotificationService.dispatchFromBusinessEvent(event);
        } catch (Exception e) {
            logger.warn("SSE dispatch failed for {}: {}", event.type(), e.getMessage());
        }
    }
}
