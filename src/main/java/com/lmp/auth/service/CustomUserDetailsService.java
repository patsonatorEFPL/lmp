package com.lmp.auth.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service personnalisé pour charger les détails des utilisateurs lors de l'authentification.
 * Implémente UserDetailsService de Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

        private final UserRepository userRepository;

    /**
     * SECURITY (M2) : when {@code true}, block login if {@code emailVerified=false}.
     * Default {@code false} for backwards compatibility (bench users on staging). Flip
     * to {@code true} via env {@code LMP_AUTH_REQUIRE_EMAIL_VERIFIED} in prod.
     */
    @Value("${lmp.auth.require-email-verified:false}")
    private boolean requireEmailVerified;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge un utilisateur par son email (utilisé comme nom d'utilisateur).
     * 
     * @param email L'email de l'utilisateur
     * @return UserDetails contenant les informations de l'utilisateur pour Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        logger.debug("Tentative de connexion: {}", login);

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    logger.warn("Utilisateur non trouvé: {}", login);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + login);
                });

        logger.debug("Utilisateur trouvé: {} avec {} rôles", user.getUsername() != null ? user.getUsername() : user.getEmail(), user.getRoles().size());

        // SECURITY (L1) : prefix du hash retiré des logs.
        // Le codebase utilise DelegatingPasswordEncoder : Argon2id par défaut pour
        // les nouveaux hashes ({argon2id}...), bcrypt ($2a$) pour les legacy.
        // On warn seulement si l'entrée est null OU ne correspond à AUCUN format
        // connu — sinon on flood les logs sur chaque login Argon2.
        if (user.getPassword() == null
                || (!user.getPassword().startsWith("{argon2id}")
                        && !user.getPassword().startsWith("{bcrypt}")
                        && !user.getPassword().startsWith("$2a$")
                        && !user.getPassword().startsWith("$2b$")
                        && !user.getPassword().startsWith("$2y$"))) {
            logger.warn("⚠️ Mot de passe non reconnu (ni Argon2id ni BCrypt) pour user {}", user.getEmail());
        }
        
        return createUserPrincipal(user);
    }

    /**
     * Crée un objet UserDetails à partir d'un utilisateur.
     * 
     * @param user L'utilisateur de la base de données
     * @return UserDetails pour Spring Security
     */
    private UserDetails createUserPrincipal(User user) {
        logger.info("🔍 [SESSION-SECURITY] Création UserPrincipal pour: {} (Statut: {})", user.getEmail(), user.getStatus());

        // Note: Les utilisateurs supprimés sont maintenant physiquement effacés de la base (hard delete)
        // Cette vérification n'est plus nécessaire

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        // Autoriser le login même si INACTIVE (pour afficher la page suspendue / bannière).
        // SECURITY (M2) : si requireEmailVerified=true, un compte non-vérifié est désactivé
        // → Spring Security lèvera DisabledException, le front demandera renvoi du lien.
        boolean statusAllows = user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.INACTIVE;
        boolean emailVerifiedOk = !requireEmailVerified || Boolean.TRUE.equals(user.getEmailVerified());
        boolean isEnabled = statusAllows && emailVerifiedOk;

        // SECURITY (M1) : expose accountLocked sur UserDetails plutôt que throw
        // UsernameNotFoundException. DefaultPreAuthenticationChecks lève alors
        // LockedException, ce qui permet aux callers + handlers d'exception de
        // distinguer "compte verrouillé" de "compte absent".
        boolean isAccountNonLocked = !Boolean.TRUE.equals(user.getAccountLocked());

        if (!isAccountNonLocked) {
            logger.warn("🔒 [SESSION-SECURITY] Compte verrouillé pour {} — LockedException sera levée par DaoAuthenticationProvider",
                    user.getEmail());
        }
        if (statusAllows && !emailVerifiedOk) {
            logger.warn("🔒 [SESSION-SECURITY] BLOCAGE login non-vérifié pour {} (require-email-verified actif)",
                    user.getEmail());
        }

        // SECURITY (L1) : hash bcrypt prefix retiré du log debug aussi.
        logger.debug("🔐 Statut utilisateur - Actif: {}, Non verrouillé: {}",
                    isEnabled, isAccountNonLocked);
        logger.debug("🎭 Autorités utilisateur: {}", authorities);

        logger.info("✅ [SESSION-SECURITY] UserPrincipal créé avec succès pour: {} (Statut: {}, Locked: {})",
                    user.getEmail(), user.getStatus(), !isAccountNonLocked);

        String principal = user.getUsername() != null ? user.getUsername() : user.getEmail();
        return org.springframework.security.core.userdetails.User.builder()
                .username(principal)
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!isAccountNonLocked)
                .credentialsExpired(false)
                .disabled(!isEnabled) // Inactif si pas ACTIVE
                .build();
    }

    /**
     * Charge un utilisateur par son ID.
     * Utile pour les opérations internes de l'application.
     * 
     * @param userId L'ID de l'utilisateur
     * @return UserDetails de l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(java.util.UUID userId) throws UsernameNotFoundException {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'ID: " + userId));

        return createUserPrincipal(user);
    }

    /**
     * Vérifie si un utilisateur a un rôle spécifique.
     * 
     * @param user L'utilisateur à vérifier
     * @param roleName Le nom du rôle à vérifier
     * @return true si l'utilisateur a le rôle, false sinon
     */
    public boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(name -> name.equals(roleName));
    }

    /**
     * Vérifie si un utilisateur est un administrateur.
     * 
     * @param user L'utilisateur à vérifier
     * @return true si l'utilisateur est admin, false sinon
     */
    public boolean isAdmin(User user) {
        return hasRole(user, "ADMIN");
    }

    /**
     * Vérifie si un utilisateur est un utilisateur standard.
     * 
     * @param user L'utilisateur à vérifier
     * @return true si l'utilisateur a le rôle USER, false sinon
     */
    public boolean isUser(User user) {
        return hasRole(user, "USER");
    }
}