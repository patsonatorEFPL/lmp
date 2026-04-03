package com.lmp.auth.dto;

/**
 * Données nécessaires à l'envoi asynchrone de l'e-mail de réinitialisation.
 */
public record PasswordResetEmailPayload(String email, String token, String userDisplayName) {}
