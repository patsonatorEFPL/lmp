package com.lmp.service.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;

/**
 * Interface pour les services de gestion des utilisateurs.
 */
public interface UserService {

    /**
     * Trouve un utilisateur par son ID.
     *
     * @param id L'ID de l'utilisateur
     * @return L'utilisateur s'il existe
     */
    Optional<User> findById(Long id);

    /**
     * Trouve un utilisateur par son email.
     *
     * @param email L'email de l'utilisateur
     * @return L'utilisateur s'il existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Obtient tous les utilisateurs.
     *
     * @return La liste de tous les utilisateurs
     */
    List<User> findAll();

    /**
     * Obtient les utilisateurs par statut.
     *
     * @param status Le statut des utilisateurs
     * @return La liste des utilisateurs avec ce statut
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Sauvegarde un utilisateur.
     *
     * @param user L'utilisateur à sauvegarder
     * @return L'utilisateur sauvegardé
     */
    User save(User user);

    /**
     * Supprime définitivement un utilisateur de la base de données (hard delete).
     * Cette opération est irréversible et gère les dépendances de manière sécurisée :
     * - Invalide les sessions actives
     * - Supprime les rôles utilisateur
     * - Anonymise les commandes, avis et rendez-vous
     * - Préserve l'historique pour l'audit
     * 
     * ATTENTION : Cette action est irréversible !
     *
     * @param id L'ID de l'utilisateur à supprimer définitivement
     * @throws RuntimeException si l'utilisateur n'existe pas ou en cas d'erreur
     */
    void deleteUser(Long id);

    /**
     * Active ou désactive un utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param active true pour activer, false pour désactiver
     */
    void setUserActive(Long id, boolean active);

    /**
     * Verrouille ou déverrouille un compte utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param locked true pour verrouiller, false pour déverrouiller
     */
    void setUserLocked(Long id, boolean locked);

    /**
     * Change le mot de passe d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     */
    void changePassword(Long userId, String newPassword);

    /**
     * Change le mot de passe d'un utilisateur avec validation du mot de passe actuel.
     *
     * @param userId L'ID de l'utilisateur
     * @param currentPassword Le mot de passe actuel (en clair)
     * @param newPassword Le nouveau mot de passe (en clair)
     * @throws RuntimeException si le mot de passe actuel est incorrect
     */
    void changePasswordWithValidation(Long userId, String currentPassword, String newPassword);

    /**
     * Change le mot de passe d'un utilisateur par un administrateur.
     * Ne nécessite pas le mot de passe actuel.
     *
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     * @param adminId L'ID de l'administrateur qui effectue l'action
     */
    void changePasswordByAdmin(Long userId, String newPassword, Long adminId);

    /**
     * Valide la force d'un mot de passe.
     *
     * @param password Le mot de passe à valider
     * @return true si le mot de passe est suffisamment fort
     */
    boolean isPasswordStrong(String password);

    /**
     * Vérifie si un mot de passe correspond au mot de passe actuel de l'utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @param password Le mot de passe à vérifier
     * @return true si le mot de passe correspond
     */
    boolean checkCurrentPassword(Long userId, String password);

    /**
     * Met à jour la date de dernière connexion.
     *
     * @param email L'email de l'utilisateur
     */
    void updateLastLoginDate(String email);

    /**
     * Vérifie si un utilisateur a un rôle spécifique.
     *
     * @param userId L'ID de l'utilisateur
     * @param roleName Le nom du rôle
     * @return true si l'utilisateur a le rôle, false sinon
     */
    boolean hasRole(Long userId, String roleName);

    /**
     * Compte le nombre total d'utilisateurs.
     *
     * @return Le nombre d'utilisateurs
     */
    long countUsers();

    /**
     * Compte le nombre d'utilisateurs actifs.
     *
     * @return Le nombre d'utilisateurs actifs
     */
    long countActiveUsers();

    // Nouvelles méthodes pour l'administration avec pagination
    
    /**
     * Trouve tous les utilisateurs avec pagination.
     *
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs
     */
    Page<User> findAll(Pageable pageable);

    /**
     * Trouve les utilisateurs par statut avec pagination.
     *
     * @param status Le statut recherché
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs avec le statut spécifié
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Trouve les utilisateurs par email contenant le texte avec pagination.
     *
     * @param email Le texte à rechercher dans l'email
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs dont l'email contient le texte
     */
    Page<User> findByEmailContaining(String email, Pageable pageable);

    /**
     * Compte les utilisateurs par statut.
     *
     * @param status Le statut
     * @return Le nombre d'utilisateurs avec ce statut
     */
    long countByStatus(UserStatus status);

    /**
     * Compte les utilisateurs par statut de verrouillage.
     *
     * @param locked true pour compter les verrouillés, false pour les non-verrouillés
     * @return Le nombre d'utilisateurs
     */
    long countByAccountLocked(Boolean locked);

    /**
     * Compte total des utilisateurs.
     *
     * @return Le nombre total d'utilisateurs
     */
    long count();
    
    /**
     * Supprime définitivement un utilisateur de la base de données (hard delete).
     * Cette opération est irréversible et gère les dépendances de manière sécurisée :
     * - Supprime les rôles utilisateur
     * - Anonymise les commandes (user_id = NULL)
     * - Anonymise les reviews
     * - Préserve l'historique des statuts de commandes pour l'audit
     * 
     * ATTENTION : Cette action est irréversible !
     *
     * @param id L'ID de l'utilisateur à supprimer définitivement
     * @throws RuntimeException si l'utilisateur n'existe pas ou en cas d'erreur
     */
    void hardDeleteUser(Long id);
}