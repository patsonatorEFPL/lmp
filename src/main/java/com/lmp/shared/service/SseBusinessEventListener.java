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

    /**
     * AFTER_COMMIT : relai SSE une fois la transaction émettrice validée.
     * {@code fallbackExecution = true} : certains modules publient l’événement après la fin du
     * {@code @Transactional} du service (ex. inscription dans {@code AuthRestController}) — sans
     * transaction active, sans ce flag le listener ne s’exécutait pas et aucun SSE admin n’était envoyé.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBusinessEventCommitted(LmpBusinessEvent event) {
        try {
            sseNotificationService.dispatchFromBusinessEvent(event);
        } catch (Exception e) {
            logger.warn("SSE dispatch failed for {}: {}", event.type(), e.getMessage());
        }
    }
}
