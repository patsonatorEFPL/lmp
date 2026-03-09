package com.lmp.service.auth;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.UserRepository;

/**
 * Service personnalisé pour charger les détails des utilisateurs lors de l'authentification.
 * Implémente UserDetailsService de Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Charge un utilisateur par son email (utilisé comme nom d'utilisateur).
     * 
     * @param email L'email de l'utilisateur
     * @return UserDetails contenant les informations de l'utilisateur pour Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("🔍 Tentative de connexion pour l'email: {}", email);
        logger.error("🚨 [SESSION-SECURITY] loadUserByUsername appelé pour: {} - ATTENTION: cette méthode n'est appelée qu'une seule fois à la connexion", email);
    
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> {
                    logger.warn("❌ Utilisateur non trouvé avec l'email: {}", email);
                    return new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email);
                });
    
        logger.info("✅ Utilisateur trouvé: {} avec {} rôles", user.getEmail(), user.getRoles().size());
        logger.error("🔍 [SESSION-SECURITY] Statut utilisateur lors connexion: {} (Email: {})", user.getStatus(), user.getEmail());
        logger.error("⚠️ [SESSION-SECURITY] Spring Security ne re-vérifiera PAS ce statut tant que la session est active");
        
        logger.info("🔑 Mot de passe hashé (10 premiers caractères): {}", user.getPassword() != null ? user.getPassword().substring(0, 10) + "..." : "null");
        if (user.getPassword() != null && user.getPassword().startsWith("$2a$")) {
            logger.info("✅ Mot de passe encodé en BCrypt détecté.");
        } else {
            logger.warn("⚠️ Mot de passe NON encodé en BCrypt !");
        }
        
        // Log user roles for debugging navigation issues
        user.getRoles().forEach(role -> logger.info("🎭 Rôle utilisateur: {}", role.getName()));
        
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
        
        // VÉRIFICATION: Bloquer les comptes verrouillés
        if (user.getAccountLocked()) {
            logger.warn("🔒 [SESSION-SECURITY] BLOCAGE: Compte verrouillé pour {}", user.getEmail());
            throw new UsernameNotFoundException("Compte utilisateur verrouillé");
        }
        
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        // Autoriser le login même si INACTIVE (pour afficher la page suspendue / bannière)
        boolean isEnabled = user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.INACTIVE;
        boolean isAccountNonLocked = !user.getAccountLocked();
        
        logger.debug("🔐 Statut utilisateur - Actif: {}, Non verrouillé: {}, Mot de passe haché: {}",
                    isEnabled, isAccountNonLocked, user.getPassword().substring(0, 10) + "...");
        logger.debug("🎭 Autorités utilisateur: {}", authorities);
        
        logger.info("✅ [SESSION-SECURITY] UserPrincipal créé avec succès pour: {} (Statut: {})", user.getEmail(), user.getStatus());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false) // Déjà vérifié ci-dessus
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