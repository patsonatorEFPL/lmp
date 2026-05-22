package com.lmp.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;

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
    Optional<User> findById(UUID id);

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
    void deleteUser(UUID id);

    /**
     * Active ou désactive un utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param active true pour activer, false pour désactiver
     */
    void setUserActive(UUID id, boolean active);

    /**
     * Verrouille ou déverrouille un compte utilisateur.
     *
     * @param id L'ID de l'utilisateur
     * @param locked true pour verrouiller, false pour déverrouiller
     */
    void setUserLocked(UUID id, boolean locked);

    /**
     * Change le mot de passe d'un utilisateur.
     *
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     */
    void changePassword(UUID userId, String newPassword);

    /**
     * Change le mot de passe d'un utilisateur avec validation du mot de passe actuel.
     *
     * @param userId L'ID de l'utilisateur
     * @param currentPassword Le mot de passe actuel (en clair)
     * @param newPassword Le nouveau mot de passe (en clair)
     * @throws RuntimeException si le mot de passe actuel est incorrect
     */
    void changePasswordWithValidation(UUID userId, String currentPassword, String newPassword);

    /**
     * Change le mot de passe d'un utilisateur par un administrateur.
     * Ne nécessite pas le mot de passe actuel.
     *
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     * @param adminId L'ID de l'administrateur qui effectue l'action
     */
    void changePasswordByAdmin(UUID userId, String newPassword, UUID adminId);

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
    boolean checkCurrentPassword(UUID userId, String password);

    /**
     * Met à jour la date de dernière connexion.
     *
     * @param email L'email de l'utilisateur
     */
    void updateLastLoginDate(String email);

    void updateLastLoginDate(User user);

    Optional<User> findByLogin(String login);

    /**
     * Vérifie si un utilisateur a un rôle spécifique.
     *
     * @param userId L'ID de l'utilisateur
     * @param roleName Le nom du rôle
     * @return true si l'utilisateur a le rôle, false sinon
     */
    boolean hasRole(UUID userId, String roleName);

    /**
     * Accorde ou retire le rôle ADMIN (réservé aux administrateurs).
     * Conserve le rôle USER ; empêche le retrait du dernier admin et l’auto-révocation.
     *
     * @param targetUserId utilisateur cible
     * @param grantAdmin true pour ajouter ADMIN, false pour le retirer
     * @param actingAdminId administrateur qui effectue l’action
     */
    void setUserAdminRole(UUID targetUserId, boolean grantAdmin, UUID actingAdminId);

    /**
     * Accorde ou retire le rôle STAFF (collaborateur — sera provisionné comme
     * DocType "User" côté externalErp, et non comme Customer).
     */
    void setUserStaffRole(UUID targetUserId, boolean grantStaff, UUID actingAdminId);
    
    // Méthodes avec JOIN FETCH pour éviter LazyInitializationException
    
    /**
     * Trouve un utilisateur par ID avec ses rôles chargés.
     * Évite LazyInitializationException lors de l'accès aux rôles.
     *
     * @param id L'ID de l'utilisateur
     * @return Optional contenant l'utilisateur avec ses rôles
     */
    Optional<User> findByIdWithRoles(UUID id);
    
    /**
     * Trouve un utilisateur par email avec ses rôles chargés.
     * Évite LazyInitializationException lors de l'accès aux rôles.
     *
     * @param email L'email de l'utilisateur
     * @return Optional contenant l'utilisateur avec ses rôles
     */
    Optional<User> findByEmailWithRoles(String email);
    
    /**
     * Trouve un utilisateur par email avec toutes ses collections chargées.
     * Évite LazyInitializationException pour les dashboards.
     *
     * @param email L'email de l'utilisateur
     * @return Optional contenant l'utilisateur avec toutes ses collections
     */
    Optional<User> findByEmailWithAllCollections(String email);

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
    void hardDeleteUser(UUID id);
}