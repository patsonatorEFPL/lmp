package com.lmp.auth.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Service OAuth2 personnalisé pour gérer l'authentification Google et Microsoft.
 * Crée ou fusionne les comptes utilisateur à partir du profil OAuth.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

        private final UserRepository userRepository;

        private final RoleRepository roleRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private AuthService authService;


    public CustomOAuth2UserService(UserRepository userRepository,
                           RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google" or "microsoft"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);
        String firstName = extractFirstName(registrationId, attributes);
        String lastName = extractLastName(registrationId, attributes);
        String providerId = extractProviderId(registrationId, attributes);

        if (email == null || email.isBlank()) {
            logger.error("OAuth2: email manquant pour le provider {}", registrationId);
            throw new OAuth2AuthenticationException("Email non disponible depuis " + registrationId);
        }

        logger.info("OAuth2 login - Provider: {}, Email: {}, Name: {} {}", registrationId, email, firstName, lastName);

        // Chercher un utilisateur existant par email avec rôles (évite LazyInitializationException)
        Optional<User> existingUserOpt = userRepository.findByEmailWithRoles(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Mettre à jour les infos OAuth si pas encore liées
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(registrationId);
                user.setOauthProviderId(providerId);
                logger.info("OAuth2: compte existant fusionné - {} lié à {}", email, registrationId);
            }
            // Mettre à jour le nom si absent
            if ((user.getFirstName() == null || user.getFirstName().isBlank()) && firstName != null) {
                user.setFirstName(firstName);
            }
            if ((user.getLastName() == null || user.getLastName().isBlank()) && lastName != null) {
                user.setLastName(lastName);
            }
            user.setLastLoginDate(LocalDateTime.now());
            user.setEmailVerified(true); // L'email est vérifié via OAuth
            userRepository.save(user);
        } else {
            // Créer un nouveau compte
            user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString())); // Password aléatoire
            user.setFirstName(firstName != null ? firstName : "");
            user.setLastName(lastName != null ? lastName : "");
            user.setOauthProvider(registrationId);
            user.setOauthProviderId(providerId);
            user.setRegistrationDate(LocalDateTime.now());
            user.setLastLoginDate(LocalDateTime.now());
            user.setStatus(UserStatus.ACTIVE);
            user.setAccountLocked(false);
            user.setEmailVerified(true);

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Rôle USER non trouvé"));
            user.setRoles(Set.of(userRole));

            user = userRepository.save(user);
            logger.info("OAuth2: nouveau compte créé - {} via {}", email, registrationId);

            // Envoyer l'email de bienvenue pour les nouveaux utilisateurs OAuth2
            try {
                authService.sendWelcomeEmail(user);
                logger.info("OAuth2: email de bienvenue envoyé à {}", email);
            } catch (Exception e) {
                logger.error("OAuth2: erreur envoi email bienvenue pour {}: {}", email, e.getMessage());
            }
        }

        // Construire les autorités à partir des rôles réels en DB (pas hardcodé)
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        if (authorities.isEmpty()) {
            // Fallback : au minimum ROLE_USER
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        logger.info("OAuth2: autorités pour {} : {}", email, authorities);

        // Déterminer l'attribut clé pour le nom d'utilisateur (=> #authentication.name)
        String nameAttributeKey = "email";
        if ("microsoft".equals(registrationId)) {
            nameAttributeKey = attributes.containsKey("email") ? "email" : "preferred_username";
        }

        return new DefaultOAuth2User(
                authorities,
                attributes,
                nameAttributeKey);
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return (String) attributes.get("email");
        } else if ("microsoft".equals(provider)) {
            String email = (String) attributes.get("email");
            if (email == null) {
                email = (String) attributes.get("preferred_username");
            }
            return email;
        }
        return null;
    }

    private String extractFirstName(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return (String) attributes.get("given_name");
        } else if ("microsoft".equals(provider)) {
            return (String) attributes.get("givenName");
        }
        return null;
    }

    private String extractLastName(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return (String) attributes.get("family_name");
        } else if ("microsoft".equals(provider)) {
            return (String) attributes.get("surname");
        }
        return null;
    }

    private String extractProviderId(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return (String) attributes.get("sub");
        } else if ("microsoft".equals(provider)) {
            return (String) attributes.get("oid");
        }
        return null;
    }
}
