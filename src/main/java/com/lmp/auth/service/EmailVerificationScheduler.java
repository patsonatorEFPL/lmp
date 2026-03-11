package com.lmp.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.UserRepository;

/**
 * Scheduler qui suspend automatiquement les comptes dont l'email
 * n'a pas été vérifié dans les 24h suivant l'inscription.
 * S'exécute toutes les 15 minutes.
 */
@Component
public class EmailVerificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationScheduler.class);

        private final UserRepository userRepository;


    public EmailVerificationScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Toutes les 15 minutes, passe en INACTIVE les utilisateurs ACTIVE
     * qui n'ont pas vérifié leur email dans les 24h.
     */
    @Scheduled(fixedRate = 900_000) // 15 minutes
    @Transactional
    public void suspendUnverifiedAccounts() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(24);

        List<User> expiredUsers = userRepository.findUnverifiedExpiredUsers(deadline);

        if (expiredUsers.isEmpty()) {
            logger.debug("Aucun compte non vérifié expiré à suspendre");
            return;
        }

        for (User user : expiredUsers) {
            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
            logger.info("Compte suspendu (email non vérifié après 24h) : {}", user.getEmail());
        }

        logger.info("Auto-suspension terminée : {} compte(s) suspendu(s)", expiredUsers.size());
    }
}
