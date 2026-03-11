package com.lmp.auth.service;

import java.time.LocalDateTime;
import java.util.HashMap;
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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Service OIDC personnalisé pour gérer l'authentification Google (et tout provider OIDC).
 * Google utilise le protocole OpenID Connect, donc ce service est appelé à la place
 * de CustomOAuth2UserService pour les providers OIDC.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

        private final UserRepository userRepository;

        private final RoleRepository roleRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private AuthService authService;


    public CustomOidcUserService(UserRepository userRepository,
                           RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String providerId = (String) attributes.get("sub");

        if (email == null || email.isBlank()) {
            logger.error("OIDC: email manquant pour le provider {}", registrationId);
            throw new OAuth2AuthenticationException("Email non disponible depuis " + registrationId);
        }

        logger.info("OIDC login - Provider: {}, Email: {}, Name: {} {}", registrationId, email, firstName, lastName);

        // Chercher un utilisateur existant par email avec rôles (évite LazyInitializationException)
        Optional<User> existingUserOpt = userRepository.findByEmailWithRoles(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Mettre à jour les infos OAuth si pas encore liées
            if (user.getOauthProvider() == null) {
                user.setOauthProvider(registrationId);
                user.setOauthProviderId(providerId);
                logger.info("OIDC: compte existant fusionné - {} lié à {}", email, registrationId);
            }
            // Mettre à jour le nom si absent
            if ((user.getFirstName() == null || user.getFirstName().isBlank()) && firstName != null) {
                user.setFirstName(firstName);
            }
            if ((user.getLastName() == null || user.getLastName().isBlank()) && lastName != null) {
                user.setLastName(lastName);
            }
            user.setLastLoginDate(LocalDateTime.now());
            user.setEmailVerified(true);
            userRepository.save(user);
        } else {
            // Créer un nouveau compte
            user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
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
            logger.info("OIDC: nouveau compte créé - {} via {}", email, registrationId);

            // Envoyer l'email de bienvenue
            try {
                authService.sendWelcomeEmail(user);
                logger.info("OIDC: email de bienvenue envoyé à {}", email);
            } catch (Exception e) {
                logger.error("OIDC: erreur envoi email bienvenue pour {}: {}", email, e.getMessage());
            }
        }

        // Construire les autorités à partir des rôles réels en DB
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        logger.info("OIDC: autorités pour {} : {}", email, authorities);

        // Retourner un OidcUser avec les bonnes autorités et "email" comme nameAttributeKey
        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email");
    }
}
