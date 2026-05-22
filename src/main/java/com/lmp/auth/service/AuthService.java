package com.lmp.auth.service;

import java.util.Optional;

import com.lmp.auth.domain.User;
import com.lmp.auth.dto.PasswordResetEmailPayload;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.ResetPasswordDto;

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

    /**
     * Prépare un jeton de réinitialisation si un compte existe pour cet e-mail.
     *
     * @return les données pour l'e-mail à envoyer, ou vide si aucun compte
     */
    Optional<PasswordResetEmailPayload> initiatePasswordReset(String email);

    /**
     * Envoie l'e-mail contenant le lien de réinitialisation (asynchrone).
     */
    void sendPasswordResetEmail(PasswordResetEmailPayload payload);

    /**
     * Pipeline complet forgot-password offload sur authBackgroundExecutor :
     * découple Tomcat thread du DB query+save (Druid master pool) et de
     * l'enqueue mail. Le controller peut répondre 200 instantanément.
     */
    void processForgotPasswordAsync(String email);

    /**
     * Applique un nouveau mot de passe à partir d'un jeton valide et non expiré.
     */
    void completePasswordReset(ResetPasswordDto dto);

    /**
     * E-mail de confirmation après changement de mot de passe (asynchrone).
     */
    void sendPasswordResetConfirmationEmail(String email, String userDisplayName);
}
