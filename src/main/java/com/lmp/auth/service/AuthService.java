package com.lmp.auth.service;

import com.lmp.auth.domain.User;
import com.lmp.auth.dto.RegisterDto;

/**
 * Interface pour les services d'authentification.
 */
public interface AuthService {

    /**
     * Inscrit un nouvel utilisateur.
     */
    User registerUser(RegisterDto registerDto);

    /**
     * Vérifie si un email existe déjà.
     */
    boolean existsByEmail(String email);

    /**
     * Valide les données d'inscription.
     */
    void validateRegistrationData(RegisterDto registerDto);

    /**
     * Envoie un email de bienvenue à l'utilisateur.
     */
    void sendWelcomeEmail(User user);

    /**
     * Génère un token de vérification d'email.
     */
    String generateVerificationToken();

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     */
    boolean verifyEmail(String token);

    /**
     * Envoie un email de vérification à l'utilisateur.
     */
    void sendVerificationEmail(User user);

    /**
     * Renvoie l'email de vérification à l'utilisateur (avec nouveau token).
     *
     * @param email l'adresse email de l'utilisateur
     */
    void resendVerificationEmail(String email);

    /**
     * Vérifie si une adresse email utilise un domaine jetable.
     *
     * @param email l'adresse email à vérifier
     * @return true si c'est un domaine jetable
     */
    boolean isDisposableEmail(String email);
}
