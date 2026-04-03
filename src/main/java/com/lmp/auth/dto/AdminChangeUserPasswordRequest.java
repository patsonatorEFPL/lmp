package com.lmp.auth.dto;

/**
 * DTO pour le changement de mot de passe d'un utilisateur par un administrateur.
 * Ne requiert pas le mot de passe actuel de l'utilisateur cible.
 * La validation est effectuée explicitement dans le contrôleur.
 */
public record AdminChangeUserPasswordRequest(
        String newPassword,
        String confirmPassword
) {
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
