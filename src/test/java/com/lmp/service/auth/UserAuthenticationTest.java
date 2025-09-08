package com.lmp.service.auth;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.RoleRepository;
import com.lmp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration pour vérifier que l'authentification utilisateur 
 * fonctionne correctement sans LazyInitializationException après nos corrections.
 * 
 * Ce test vérifie spécifiquement :
 * - Le chargement d'un utilisateur par email avec ses rôles
 * - L'accès aux rôles après chargement (pas de LazyInitializationException)
 * - La validation des permissions utilisateur
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserAuthenticationTest {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Créer les rôles de test
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });

        adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ADMIN");
                    return roleRepository.save(role);
                });

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setAccountLocked(false);
        testUser.setEmailVerified(true);
        testUser.setRegistrationDate(LocalDateTime.now());
        
        // Assigner les rôles
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);
        testUser.setRoles(roles);

        testUser = userRepository.save(testUser);
    }

    /**
     * Test principal : Vérifier que loadUserByUsername fonctionne correctement
     * et peut accéder aux rôles sans LazyInitializationException
     */
    @Test
    void testLoadUserByUsername_ShouldLoadUserWithRoles_WithoutLazyInitializationException() {
        // Act - Charger l'utilisateur par email (comme lors de la connexion)
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser@example.com");
        
        // Assert - Vérifier que l'utilisateur est chargé correctement
        assertNotNull(userDetails);
        assertEquals("testuser@example.com", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        
        // Vérifier que les rôles sont accessibles (c'est ici qu'était la LazyInitializationException)
        assertNotNull(userDetails.getAuthorities());
        assertEquals(2, userDetails.getAuthorities().size());
        
        // Vérifier que les noms des rôles sont corrects
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * Test pour loadUserById avec accès aux rôles
     */
    @Test
    void testLoadUserById_ShouldLoadUserWithRoles_WithoutLazyInitializationException() {
        // Act
        UserDetails userDetails = customUserDetailsService.loadUserById(testUser.getId());
        
        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser@example.com", userDetails.getUsername());
        
        // Vérifier l'accès aux rôles
        assertNotNull(userDetails.getAuthorities());
        assertEquals(2, userDetails.getAuthorities().size());
    }

    /**
     * Test avec un utilisateur qui n'existe pas
     */
    @Test
    void testLoadUserByUsername_UserNotFound_ShouldThrowException() {
        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nonexistent@example.com");
        });
    }

    /**
     * Test avec un utilisateur verrouillé - doit lever une exception
     */
    @Test
    void testLoadUserByUsername_LockedUser_ShouldThrowException() {
        // Arrange - Verrouiller l'utilisateur
        testUser.setAccountLocked(true);
        userRepository.save(testUser);
        
        // Act & Assert - Un compte verrouillé ne peut pas se connecter
        assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("testuser@example.com");
        });
    }

    /**
     * Test spécifique pour vérifier que l'utilisateur peut accéder aux rôles
     * après chargement (simulation du cas d'usage réel dans l'application)
     */
    @Test
    void testUserRoleAccess_AfterAuthentication_ShouldNotThrowLazyInitializationException() {
        // Act - Charger l'utilisateur comme lors de l'authentification
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser@example.com");
        
        // Simuler l'accès aux rôles comme dans le code de l'application
        // (par exemple dans un contrôleur ou service)
        boolean hasUserRole = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
        boolean hasAdminRole = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        // Assert - Aucune exception ne doit être levée et les rôles doivent être corrects
        assertTrue(hasUserRole);
        assertTrue(hasAdminRole);
    }

    /**
     * Test pour vérifier la cohérence entre loadUserByUsername et loadUserById
     */
    @Test
    void testConsistency_BetweenLoadUserByUsername_AndLoadUserById() {
        // Act
        UserDetails userDetailsByUsername = customUserDetailsService.loadUserByUsername("testuser@example.com");
        UserDetails userDetailsById = customUserDetailsService.loadUserById(testUser.getId());
        
        // Assert - Les deux méthodes doivent retourner des résultats cohérents
        assertEquals(userDetailsByUsername.getUsername(), userDetailsById.getUsername());
        assertEquals(userDetailsByUsername.getAuthorities().size(), userDetailsById.getAuthorities().size());
        assertEquals(userDetailsByUsername.isEnabled(), userDetailsById.isEnabled());
    }
}
