package com.lmp.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduler qui marque automatiquement EXPIRED les invitations PENDING
 * dont la date d'expiration est dépassée.
 * S'exécute toutes les heures.
 */
@Component
public class StaffInvitationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(StaffInvitationScheduler.class);

    private final StaffInvitationService invitationService;

    public StaffInvitationScheduler(StaffInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Scheduled(fixedRate = 3_600_000) // 1 heure
    @Transactional
    public void expireStaleInvitations() {
        int count = invitationService.markExpired();
        if (count > 0) {
            logger.info("{} invitation(s) staff marquée(s) comme expirée(s)", count);
        }
    }
}
