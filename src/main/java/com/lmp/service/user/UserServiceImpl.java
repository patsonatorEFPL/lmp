package com.lmp.service.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.UserRepository;
import com.lmp.service.security.SessionSecurityService;

/**
 * Implémentation du service de gestion des utilisateurs.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionSecurityService sessionSecurityService;

    /**
     * Trouve un utilisateur par son ID.
     * 
     * @param id L'ID de l'utilisateur
     * @return L'utilisateur s'il existe
     */
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Trouve un utilisateur par son email.
     * 
     * @param email L'email de l'utilisateur
     * @return L'utilisateur s'il existe
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Obtient tous les utilisateurs.
     * 
     * @return La liste de tous les utilisateurs
     */
    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Obtient les utilisateurs par statut.
     * 
     * @param status Le statut des utilisateurs
     * @return La liste des utilisateurs avec ce statut
     */
    @Override
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == status)
                .toList();
    }

    /**
     * Sauvegarde un utilisateur.
     * 
     * @param user L'utilisateur à sauvegarder
     * @return L'utilisateur sauvegardé
     */
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Supprime un utilisateur (soft delete).
     * Met le statut à DELETED au lieu de supprimer physiquement.
     * 
     * @param id L'ID de l'utilisateur à supprimer
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        logger.warn("🚨 [SESSION-SECURITY] deleteUser appelé pour ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        logger.warn("🔍 [SESSION-SECURITY] Utilisateur à supprimer: Email={}, Statut actuel={}", user.getEmail(), user.getStatus());
        
        // ÉTAPE 1: Vérifier les sessions actives AVANT suppression
        boolean hadActiveSessions = sessionSecurityService.hasActiveSessions(user.getEmail());
        int activeSessionCount = sessionSecurityService.getActiveSessionCount(user.getEmail());
        logger.warn("📊 [SESSION-SECURITY] Sessions actives AVANT suppression: {} session(s) pour {}", activeSessionCount, user.getEmail());
        
        // ÉTAPE 2: Invalider TOUTES les sessions actives AVANT de marquer comme supprimé
        if (hadActiveSessions) {
            logger.warn("🔒 [SESSION-SECURITY] INVALIDATION FORCÉE des sessions pour utilisateur à supprimer: {}", user.getEmail());
            int invalidatedSessions = sessionSecurityService.invalidateAllUserSessions(user);
            logger.warn("✅ [SESSION-SECURITY] {} session(s) invalidée(s) avec succès pour {}", invalidatedSessions, user.getEmail());
        } else {
            logger.info("ℹ️ [SESSION-SECURITY] Aucune session active à invalider pour {}", user.getEmail());
        }
        
        // ÉTAPE 3: Marquer l'utilisateur comme supprimé (soft delete)
        user.setStatus(UserStatus.DELETED);
        user.setAccountLocked(true);
        userRepository.save(user);
        
        // ÉTAPE 4: Vérification finale - aucune session ne doit subsister
        int remainingSessions = sessionSecurityService.getActiveSessionCount(user.getEmail());
        if (remainingSessions > 0) {
            logger.error("🚨 [SESSION-SECURITY] ALERTE: {} session(s) ENCORE ACTIVE(S) après invalidation pour {}", remainingSessions, user.getEmail());
        } else {
            logger.info("✅ [SESSION-SECURITY] SÉCURITÉ CONFIRMÉE: Aucune session active restante pour l'utilisateur supprimé {}", user.getEmail());
        }
        
        logger.warn("🎯 [SESSION-SECURITY] SUPPRESSION SÉCURISÉE TERMINÉE pour {}: Sessions invalidées + Statut DELETED", user.getEmail());
    }

    /**
     * Active ou désactive un utilisateur.
     * 
     * @param id L'ID de l'utilisateur
     * @param active true pour activer, false pour désactiver
     */
    @Override
    @Transactional
    public void setUserActive(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        userRepository.save(user);
    }

    /**
     * Verrouille ou déverrouille un compte utilisateur.
     * 
     * @param id L'ID de l'utilisateur
     * @param locked true pour verrouiller, false pour déverrouiller
     */
    @Override
    @Transactional
    public void setUserLocked(Long id, boolean locked) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
        
        user.setAccountLocked(locked);
        userRepository.save(user);
    }

    /**
     * Change le mot de passe d'un utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     */
    @Override
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        if (!isPasswordStrong(newPassword)) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Change le mot de passe d'un utilisateur avec validation du mot de passe actuel.
     * 
     * @param userId L'ID de l'utilisateur
     * @param currentPassword Le mot de passe actuel (en clair)
     * @param newPassword Le nouveau mot de passe (en clair)
     */
    @Override
    @Transactional
    public void changePasswordWithValidation(Long userId, String currentPassword, String newPassword) {
        logger.info("🔐 DEBUG SERVICE - changePasswordWithValidation appelé pour userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        logger.info("🔐 DEBUG SERVICE - Utilisateur trouvé: email={}", user.getEmail());
        
        // Vérifier le mot de passe actuel
        logger.info("🔐 DEBUG SERVICE - Vérification mot de passe actuel");
        boolean currentPasswordValid = passwordEncoder.matches(currentPassword, user.getPassword());
        logger.info("🔐 DEBUG SERVICE - Mot de passe actuel valide: {}", currentPasswordValid);
        
        if (!currentPasswordValid) {
            logger.warn("🔐 DEBUG SERVICE - Mot de passe actuel incorrect pour userId: {}", userId);
            throw new RuntimeException("Le mot de passe actuel est incorrect");
        }
        
        // Vérifier que le nouveau mot de passe n'est pas identique à l'ancien
        logger.info("🔐 DEBUG SERVICE - Vérification nouveau mot de passe différent");
        boolean isSamePassword = passwordEncoder.matches(newPassword, user.getPassword());
        logger.info("🔐 DEBUG SERVICE - Nouveau mot de passe identique à l'ancien: {}", isSamePassword);
        
        if (isSamePassword) {
            logger.warn("🔐 DEBUG SERVICE - Nouveau mot de passe identique à l'ancien pour userId: {}", userId);
            throw new RuntimeException("Le nouveau mot de passe doit être différent du mot de passe actuel");
        }
        
        // Vérifier la force du mot de passe
        logger.info("🔐 DEBUG SERVICE - Vérification force du mot de passe");
        boolean isStrong = isPasswordStrong(newPassword);
        logger.info("🔐 DEBUG SERVICE - Mot de passe suffisamment fort: {}", isStrong);
        
        if (!isStrong) {
            logger.warn("🔐 DEBUG SERVICE - Mot de passe trop faible pour userId: {}", userId);
            throw new RuntimeException("Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }
        
        // Encoder et sauvegarder
        logger.info("🔐 DEBUG SERVICE - Encodage et sauvegarde du nouveau mot de passe");
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        logger.info("✅ DEBUG SERVICE - Mot de passe changé avec succès pour userId: {}", userId);
    }

    /**
     * Change le mot de passe d'un utilisateur par un administrateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param newPassword Le nouveau mot de passe (en clair)
     * @param adminId L'ID de l'administrateur qui effectue l'action
     */
    @Override
    @Transactional
    public void changePasswordByAdmin(Long userId, String newPassword, Long adminId) {
        logger.info("🔐 DEBUG SERVICE - changePasswordByAdmin appelé: userId={}, adminId={}", userId, adminId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        logger.info("🔐 DEBUG SERVICE - Utilisateur cible trouvé: email={}", user.getEmail());
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé avec l'ID: " + adminId));
        
        logger.info("🔐 DEBUG SERVICE - Admin trouvé: email={}", admin.getEmail());
        
        // Vérifier que l'admin a bien le rôle ADMIN
        logger.info("🔐 DEBUG SERVICE - Vérification rôle ADMIN");
        boolean hasAdminRole = hasRole(adminId, "ADMIN");
        logger.info("🔐 DEBUG SERVICE - Admin a rôle ADMIN: {}", hasAdminRole);
        
        if (!hasAdminRole) {
            logger.warn("🔐 DEBUG SERVICE - Accès refusé: adminId {} n'a pas le rôle ADMIN", adminId);
            throw new RuntimeException("Seuls les administrateurs peuvent changer les mots de passe des autres utilisateurs");
        }
        
        // Vérifier la force du mot de passe
        logger.info("🔐 DEBUG SERVICE - Vérification force du mot de passe");
        boolean isStrong = isPasswordStrong(newPassword);
        logger.info("🔐 DEBUG SERVICE - Mot de passe suffisamment fort: {}", isStrong);
        
        if (!isStrong) {
            logger.warn("🔐 DEBUG SERVICE - Mot de passe trop faible pour userId: {}", userId);
            throw new RuntimeException("Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }
        
        // Encoder et sauvegarder
        logger.info("🔐 DEBUG SERVICE - Encodage et sauvegarde du nouveau mot de passe");
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        logger.info("✅ DEBUG SERVICE - Mot de passe changé par admin avec succès: userId={}, adminId={}", userId, adminId);
    }

    /**
     * Valide la force d'un mot de passe.
     * 
     * @param password Le mot de passe à valider
     * @return true si le mot de passe est suffisamment fort
     */
    @Override
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // Vérifier la présence de différents types de caractères
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
        
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }

    /**
     * Vérifie si un mot de passe correspond au mot de passe actuel de l'utilisateur.
     * 
     * @param userId L'ID de l'utilisateur
     * @param password Le mot de passe à vérifier
     * @return true si le mot de passe correspond
     */
    @Override
    public boolean checkCurrentPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
        
        return passwordEncoder.matches(password, user.getPassword());
    }

    /**
     * Met à jour la date de dernière connexion.
     * 
     * @param email L'email de l'utilisateur
     */
    @Override
    @Transactional
    public void updateLastLoginDate(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email: " + email));
        
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Vérifie si un utilisateur a un rôle spécifique.
     * 
     * @param userId L'ID de l'utilisateur
     * @param roleName Le nom du rôle
     * @return true si l'utilisateur a le rôle, false sinon
     */
    @Override
    public boolean hasRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> name.equals(roleName));
    }

    /**
     * Compte le nombre total d'utilisateurs.
     * 
     * @return Le nombre d'utilisateurs
     */
    @Override
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * Compte le nombre d'utilisateurs actifs.
     *
     * @return Le nombre d'utilisateurs actifs
     */
    @Override
    public long countActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .count();
    }

    // Nouvelles méthodes pour l'administration avec pagination

    /**
     * Trouve tous les utilisateurs avec pagination.
     *
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs
     */
    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Trouve les utilisateurs par statut avec pagination.
     *
     * @param status Le statut recherché
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs avec le statut spécifié
     */
    @Override
    public Page<User> findByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    /**
     * Trouve les utilisateurs par email contenant le texte avec pagination.
     *
     * @param email Le texte à rechercher dans l'email
     * @param pageable La configuration de pagination
     * @return Page d'utilisateurs dont l'email contient le texte
     */
    @Override
    public Page<User> findByEmailContaining(String email, Pageable pageable) {
        return userRepository.findByEmailContainingIgnoreCase(email, pageable);
    }

    /**
     * Compte les utilisateurs par statut.
     *
     * @param status Le statut
     * @return Le nombre d'utilisateurs avec ce statut
     */
    @Override
    public long countByStatus(UserStatus status) {
        return userRepository.countByStatus(status);
    }

    /**
     * Compte les utilisateurs par statut de verrouillage.
     *
     * @param locked true pour compter les verrouillés, false pour les non-verrouillés
     * @return Le nombre d'utilisateurs
     */
    @Override
    public long countByAccountLocked(Boolean locked) {
        return userRepository.countByAccountLocked(locked);
    }

    /**
     * Compte total des utilisateurs.
     *
     * @return Le nombre total d'utilisateurs
     */
    @Override
    public long count() {
        return userRepository.count();
    }
}