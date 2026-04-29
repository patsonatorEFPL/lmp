package com.lmp.auth.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.ReviewRepository;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.service.SessionSecurityService;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation du service de gestion des utilisateurs.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

        private final UserRepository userRepository;

        private final RoleRepository roleRepository;

        private final PasswordEncoder passwordEncoder;

        private final SessionSecurityService sessionSecurityService;
    
        private final OrderRepository orderRepository;
    
        private final ReviewRepository reviewRepository;
    
        private final AppointmentRepository appointmentRepository;

        private final ApplicationEventPublisher eventPublisher;


    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           SessionSecurityService sessionSecurityService,
                           OrderRepository orderRepository,
                           ReviewRepository reviewRepository,
                           AppointmentRepository appointmentRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionSecurityService = sessionSecurityService;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Trouve un utilisateur par son ID.
     * 
     * @param id L'ID de l'utilisateur
     * @return L'utilisateur s'il existe
     */
    @Override
    public Optional<User> findById(UUID id) {
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
     * Supprime définitivement un utilisateur de la base de données (hard delete).
     * Cette opération est irréversible et gère les dépendances de manière sécurisée.
     * 
     * @param id L'ID de l'utilisateur à supprimer définitivement
     */
    @Override
    @Transactional
    public void deleteUser(UUID id) {
        logger.error("🚨 [HARD-DELETE] DÉBUT SUPPRESSION DÉFINITIVE - Utilisateur ID: {}", id);
        
        // Réutiliser la logique complète de hardDeleteUser
        hardDeleteUser(id);
        
        logger.error("✅ [HARD-DELETE] SUPPRESSION DÉFINITIVE TERMINÉE pour ID: {}", id);
    }

    /**
     * Active ou désactive un utilisateur.
     * 
     * @param id L'ID de l'utilisateur
     * @param active true pour activer, false pour désactiver
     */
    @Override
    @Transactional
    public void setUserActive(UUID id, boolean active) {
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
    public void setUserLocked(UUID id, boolean locked) {
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
    public void changePassword(UUID userId, String newPassword) {
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
    public void changePasswordWithValidation(UUID userId, String currentPassword, String newPassword) {
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
     * La validation (force, unicité) doit être faite en amont dans le contrôleur
     * pour éviter les exceptions transactionnelles.
     */
    @Override
    @Transactional
    public void changePasswordByAdmin(UUID userId, String newPassword, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrateur non trouvé avec l'ID: " + adminId));

        if (!hasRole(adminId, "ADMIN")) {
            throw new IllegalArgumentException("Seuls les administrateurs peuvent changer les mots de passe des autres utilisateurs");
        }

        if (!isPasswordStrong(newPassword)) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères, incluant majuscules, minuscules, chiffres et caractères spéciaux");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe est identique au mot de passe actuel de l'utilisateur");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password changed by admin {} for user {}", admin.getEmail(), user.getEmail());
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
    public boolean checkCurrentPassword(UUID userId, String password) {
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
     * Utilise JOIN FETCH pour éviter LazyInitializationException.
     * 
     * @param userId L'ID de l'utilisateur
     * @param roleName Le nom du rôle
     * @return true si l'utilisateur a le rôle, false sinon
     */
    @Override
    public boolean hasRole(UUID userId, String roleName) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> name.equals(roleName));
    }

    @Override
    @Transactional
    public void setUserAdminRole(UUID targetUserId, boolean grantAdmin, UUID actingAdminId) {
        if (!hasRole(actingAdminId, "ADMIN")) {
            throw new RuntimeException("Seuls les administrateurs peuvent modifier les rôles");
        }

        User target = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getRoles() == null) {
            target.setRoles(new HashSet<>());
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN introuvable"));
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Rôle USER introuvable"));

        boolean hasAdmin = target.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));

        boolean rolesMutated = false;

        if (grantAdmin) {
            if (!hasAdmin) {
                target.getRoles().add(adminRole);
                rolesMutated = true;
            }
            if (target.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
                target.getRoles().add(userRole);
                rolesMutated = true;
            }
            if (rolesMutated) {
                userRepository.save(target);
                logSessionInvalidationAfterRoleChange(target,
                        sessionSecurityService.invalidateAllUserSessions(target));
            }
            return;
        }

        if (targetUserId.equals(actingAdminId)) {
            throw new RuntimeException("Vous ne pouvez pas retirer votre propre rôle administrateur");
        }
        if (hasAdmin) {
            long adminCount = userRepository.countDistinctUsersWithRoleName("ADMIN");
            if (adminCount <= 1) {
                throw new RuntimeException("Impossible de retirer le dernier administrateur");
            }
            target.getRoles().removeIf(r -> "ADMIN".equals(r.getName()));
            rolesMutated = true;
        }
        if (target.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
            target.getRoles().add(userRole);
            rolesMutated = true;
        }
        if (rolesMutated) {
            userRepository.save(target);
            logSessionInvalidationAfterRoleChange(target,
                    sessionSecurityService.invalidateAllUserSessions(target));
        }
    }

    @Override
    @Transactional
    public void setUserStaffRole(UUID targetUserId, boolean grantStaff, UUID actingAdminId) {
        if (!hasRole(actingAdminId, "ADMIN")) {
            throw new RuntimeException("Seuls les administrateurs peuvent modifier les rôles");
        }

        User target = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getRoles() == null) {
            target.setRoles(new HashSet<>());
        }

        Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new RuntimeException("Rôle STAFF introuvable"));
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Rôle USER introuvable"));

        boolean hasStaff = target.getRoles().stream()
                .anyMatch(r -> "STAFF".equals(r.getName()));
        boolean rolesMutated = false;

        if (grantStaff) {
            if (!hasStaff) {
                target.getRoles().add(staffRole);
                rolesMutated = true;
            }
            if (target.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
                target.getRoles().add(userRole);
                rolesMutated = true;
            }
        } else if (hasStaff) {
            target.getRoles().removeIf(r -> "STAFF".equals(r.getName()));
            rolesMutated = true;
            if (target.getRoles().stream().noneMatch(r -> "USER".equals(r.getName()))) {
                target.getRoles().add(userRole);
                rolesMutated = true;
            }
        }

        if (rolesMutated) {
            userRepository.save(target);
            logSessionInvalidationAfterRoleChange(target,
                    sessionSecurityService.invalidateAllUserSessions(target));
        }
    }

    private void logSessionInvalidationAfterRoleChange(User target, int invalidatedCount) {
        if (invalidatedCount == 0) {
            logger.warn(
                    "Mutation de rôles pour {} : aucune session invalidée (déjà absentes du registry ou erreur d'invalidation)",
                    target.getEmail());
        }
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
    
    // Implémentation des méthodes JOIN FETCH pour éviter LazyInitializationException
    
    /**
     * Trouve un utilisateur par ID avec ses rôles chargés.
     */
    @Override
    public Optional<User> findByIdWithRoles(UUID id) {
        return userRepository.findByIdWithRoles(id);
    }
    
    /**
     * Trouve un utilisateur par email avec ses rôles chargés.
     */
    @Override
    public Optional<User> findByEmailWithRoles(String email) {
        return userRepository.findByEmailWithRoles(email);
    }
    
    /**
     * Trouve un utilisateur par email avec toutes ses collections chargées.
     */
    @Override
    public Optional<User> findByEmailWithAllCollections(String email) {
        return userRepository.findByEmailWithAllCollections(email);
    }
    
    /**
     * Supprime définitivement un utilisateur de la base de données (hard delete).
     * Cette opération est irréversible et gère les dépendances de manière sécurisée.
     */
    @Override
    @Transactional
    public void hardDeleteUser(UUID id) {
        logger.error("🚨 [HARD-DELETE] DÉBUT SUPPRESSION DÉFINITIVE - Utilisateur ID: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + id));
            
        String userEmail = user.getEmail();
        logger.error("🔍 [HARD-DELETE] Utilisateur à supprimer définitivement: ID={}, Email={}", id, userEmail);
        
        try {
            // ÉTAPE 1: Invalider toutes les sessions actives AVANT toute modification
            logger.warn("🔒 [HARD-DELETE] Étape 1/6 - Invalidation sessions pour {}", userEmail);
            int invalidatedSessions = sessionSecurityService.invalidateAllUserSessions(user);
            logger.info("✅ [HARD-DELETE] {} session(s) invalidée(s)", invalidatedSessions);
            
            // ÉTAPE 2: Supprimer les rôles utilisateur (table user_roles)
            logger.warn("🔄 [HARD-DELETE] Étape 2/6 - Suppression des rôles utilisateur");
            user.getRoles().clear();
            userRepository.save(user); // Sauvegarde pour supprimer les liaisons user_roles
            logger.info("✅ [HARD-DELETE] Rôles utilisateur supprimés");
            
            // ÉTAPE 3: Anonymiser les commandes (conserver pour historique comptable)
            logger.warn("💼 [HARD-DELETE] Étape 3/6 - Anonymisation des commandes");
            var userOrders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            logger.info("🔍 [HARD-DELETE] Trouvé {} commandes pour l'utilisateur", userOrders.size());
            int anonymizedOrders = 0;
            for (var order : userOrders) {
                try {
                    logger.info("🔄 [HARD-DELETE] Anonymisation commande ID: {}", order.getId());
                    // Anonymiser les données client mais conserver la commande pour audit/comptabilité
                    order.setUser(null);
                    order.setNotes("[Utilisateur supprimé] " + (order.getNotes() != null ? order.getNotes() : ""));
                    orderRepository.save(order);
                    anonymizedOrders++;
                    logger.info("✅ [HARD-DELETE] Commande {} anonymisée avec succès", order.getId());
                } catch (Exception e) {
                    logger.error("❌ [HARD-DELETE] Erreur anonymisation commande {}: {}", order.getId(), e.getMessage());
                    throw new RuntimeException("Erreur lors de l'anonymisation de la commande " + order.getId() + ": " + e.getMessage(), e);
                }
            }
            logger.info("✅ [HARD-DELETE] {} commande(s) anonymisée(s)", anonymizedOrders);
            if (user.getOrders() != null) {
                user.getOrders().clear();
            }

            // ÉTAPE 4: Anonymiser les avis (conserver pour historique des services)
            logger.warn("📝 [HARD-DELETE] Étape 4/7 - Anonymisation des avis");
            var userReviews = reviewRepository.findByUser(user);
            logger.info("🔍 [HARD-DELETE] Trouvé {} avis pour l'utilisateur", userReviews.size());
            int anonymizedReviews = 0;
            for (var review : userReviews) {
                try {
                    logger.info("🔄 [HARD-DELETE] Anonymisation avis ID: {}", review.getId());
                    review.setUser(null);
                    // Optionnel: modifier le commentaire pour indiquer l'anonymisation
                    if (review.getComment() != null && !review.getComment().startsWith("[Utilisateur supprimé]")) {
                        review.setComment("[Utilisateur supprimé] " + review.getComment());
                    }
                    reviewRepository.save(review);
                    anonymizedReviews++;
                    logger.info("✅ [HARD-DELETE] Avis {} anonymisé avec succès", review.getId());
                } catch (Exception e) {
                    logger.error("❌ [HARD-DELETE] Erreur anonymisation avis {}: {}", review.getId(), e.getMessage());
                    throw new RuntimeException("Erreur lors de l'anonymisation de l'avis " + review.getId() + ": " + e.getMessage(), e);
                }
            }
            logger.info("✅ [HARD-DELETE] {} avis anonymisé(s)", anonymizedReviews);
            if (user.getReviews() != null) {
                user.getReviews().clear();
            }

            // ÉTAPE 5: Anonymiser les rendez-vous (conserver pour historique des appointments)
            logger.warn("📅 [HARD-DELETE] Étape 5/7 - Anonymisation des rendez-vous");
            var userAppointments = appointmentRepository.findByUserOrderByAppointmentDateDesc(user);
            logger.info("🔍 [HARD-DELETE] Trouvé {} rendez-vous pour l'utilisateur", userAppointments.size());
            int anonymizedAppointments = 0;
            for (var appointment : userAppointments) {
                try {
                    logger.info("🔄 [HARD-DELETE] Anonymisation rendez-vous ID: {}", appointment.getId());
                    // Sauvegarder les informations du client avant anonymisation (gestion des valeurs NULL)
                    User appointmentUser = appointment.getUser();
                    String firstName = (appointmentUser.getFirstName() != null) ? appointmentUser.getFirstName() : "Inconnu";
                    String lastName = (appointmentUser.getLastName() != null) ? appointmentUser.getLastName() : "Inconnu";
                    String clientName = firstName + " " + lastName;
                    String clientEmail = (appointmentUser.getEmail() != null) ? appointmentUser.getEmail() : "email.inconnu@supprime.local";
                    String clientPhone = (appointmentUser.getPhone() != null) ? appointmentUser.getPhone() : "Non renseigné";
                    
                    logger.info("🔄 [HARD-DELETE] Données client sauvegardées: nom='{}', email='{}', phone='{}'", clientName, clientEmail, clientPhone);
                    
                    // Mettre l'utilisateur à null (anonymisation)
                    appointment.setUser(null);
                    
                    // Mettre les informations client dans les champs anonymes si pas déjà présentes
                    if (appointment.getClientName() == null) {
                        appointment.setClientName("[Supprimé] " + clientName);
                    }
                    if (appointment.getClientEmail() == null) {
                        appointment.setClientEmail(clientEmail);
                    }
                    if (appointment.getClientPhone() == null) {
                        appointment.setClientPhone(clientPhone);
                    }
                    
                    // Marquer le rendez-vous comme anonymisé dans les notes admin
                    String adminNotes = appointment.getAdminNotes() != null ? appointment.getAdminNotes() : "";
                    appointment.setAdminNotes("[Utilisateur supprimé] " + adminNotes);
                    
                    appointmentRepository.save(appointment);
                    anonymizedAppointments++;
                    logger.info("✅ [HARD-DELETE] Rendez-vous {} anonymisé avec succès", appointment.getId());
                } catch (Exception e) {
                    logger.error("❌ [HARD-DELETE] Erreur anonymisation rendez-vous {}: {}", appointment.getId(), e.getMessage());
                    throw new RuntimeException("Erreur lors de l'anonymisation du rendez-vous " + appointment.getId() + ": " + e.getMessage(), e);
                }
            }
            logger.info("✅ [HARD-DELETE] {} rendez-vous anonymisé(s)", anonymizedAppointments);
            
            // ÉTAPE 6: CONSERVER l'historique des statuts de commandes
            // (table order_status_history) - Ne rien faire, conservé pour audit
            logger.info("📋 [HARD-DELETE] Étape 6/8 - Historique des statuts conservé pour audit");

            // ÉTAPE 7: Publier USER_DELETED AVANT la suppression (pour conserver externalCustomerId)
            logger.warn("📡 [HARD-DELETE] Étape 7/8 - Publication événement USER_DELETED pour sync cascade");
            Map<String, Object> deletePayload = new HashMap<>();
            if (user.getExternalCustomerId() != null) {
                deletePayload.put("externalCustomerId", user.getExternalCustomerId());
            }
            if (user.getExternalContactId() != null) {
                deletePayload.put("externalContactId", user.getExternalContactId());
            }
            if (user.getExternalErpUserId() != null) {
                deletePayload.put("externalErpUserId", user.getExternalErpUserId());
            }
            deletePayload.put("email", userEmail);
            eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.USER_DELETED, "auth", id, deletePayload));
            logger.info("✅ [HARD-DELETE] Événement USER_DELETED publié avec payload: {}", deletePayload);
            
            // ÉTAPE 8: Suppression définitive de l'utilisateur
            logger.error("🗑️ [HARD-DELETE] Étape 8/8 - SUPPRESSION DÉFINITIVE de l'utilisateur");
            userRepository.delete(user);
            
            // AUDIT FINAL
            logger.error("✅ [HARD-DELETE] SUPPRESSION DÉFINITIVE TERMINÉE - Utilisateur {} complètement supprimé", userEmail);
            logger.error("📊 [HARD-DELETE] STATISTIQUES - Sessions: {}, Commandes: {}, Avis: {}, Rendez-vous: {}, Sync: événement USER_DELETED publié", 
                        invalidatedSessions, anonymizedOrders, anonymizedReviews, anonymizedAppointments);
                        
        } catch (Exception e) {
            logger.error("❌ [HARD-DELETE] ERREUR CRITIQUE lors de la suppression définitive de {}: {}", userEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la suppression définitive: " + e.getMessage(), e);
        }
    }
}