package com.lmp.service.auth;

import com.lmp.domain.entity.User;
import com.lmp.web.dto.RegisterDto;

/**
 * Interface pour les services d'authentification.
 */
public interface AuthService {

    /**
     * Inscrit un nouvel utilisateur.
     * 
     * @param registerDto Les données d'inscription
     * @return L'utilisateur créé
     * @throws RuntimeException si l'email existe déjà ou si les données sont invalides
     */
    User registerUser(RegisterDto registerDto);

    /**
     * Vérifie si un email existe déjà.
     * 
     * @param email L'email à vérifier
     * @return true si l'email existe, false sinon
     */
    boolean existsByEmail(String email);

    /**
     * Valide les données d'inscription.
     * 
     * @param registerDto Les données à valider
     * @throws RuntimeException si les données sont invalides
     */
    void validateRegistrationData(RegisterDto registerDto);

    /**
     * Envoie un email de bienvenue à l'utilisateur.
     * 
     * @param user L'utilisateur nouvellement inscrit
     */
    void sendWelcomeEmail(User user);

    /**
     * Génère un token de vérification d'email.
     * 
     * @return Le token généré
     */
    String generateVerificationToken();

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     * 
     * @param token Le token de vérification
     * @return true si la vérification réussit, false sinon
     */
    boolean verifyEmail(String token);
}