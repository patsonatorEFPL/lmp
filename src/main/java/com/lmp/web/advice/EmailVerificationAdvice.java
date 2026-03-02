package com.lmp.web.advice;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.lmp.domain.entity.User;
import com.lmp.repository.UserRepository;

/**
 * ControllerAdvice qui injecte les données de vérification d'email
 * dans le modèle de chaque page pour afficher la bannière.
 */
@ControllerAdvice
public class EmailVerificationAdvice {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("emailVerified")
    public Boolean emailVerified() {
        User user = getCurrentUser();
        if (user == null) {
            return true; // Pas de bannière si pas connecté
        }
        return user.getEmailVerified();
    }

    @ModelAttribute("verificationDeadline")
    public LocalDateTime verificationDeadline() {
        User user = getCurrentUser();
        if (user == null || user.getEmailVerified()) {
            return null;
        }
        return user.getRegistrationDate().plusHours(24);
    }

    @ModelAttribute("timeRemainingMinutes")
    public Long timeRemainingMinutes() {
        User user = getCurrentUser();
        if (user == null || user.getEmailVerified()) {
            return null;
        }
        LocalDateTime deadline = user.getRegistrationDate().plusHours(24);
        Duration remaining = Duration.between(LocalDateTime.now(), deadline);
        return remaining.isNegative() ? 0L : remaining.toMinutes();
    }

    @ModelAttribute("isAccountSuspended")
    public Boolean isAccountSuspended() {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        return !user.getEmailVerified()
                && user.getRegistrationDate().plusHours(24).isBefore(LocalDateTime.now());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
