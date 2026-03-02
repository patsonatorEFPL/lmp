package com.lmp.web.advice;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.lmp.domain.entity.User;
import com.lmp.repository.UserRepository;

/**
 * Ajoute des attributs globaux au modèle pour toutes les vues Thymeleaf.
 * Fournit les informations d'affichage de l'utilisateur connecté (nom, initiales).
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName(Authentication authentication) {
        User user = resolveUser(authentication);
        return user != null ? user.getDisplayName() : null;
    }

    @ModelAttribute("currentUserInitials")
    public String currentUserInitials(Authentication authentication) {
        User user = resolveUser(authentication);
        if (user == null) return null;

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isBlank()) return "??";

        String[] parts = displayName.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        return userOpt.orElse(null);
    }
}
