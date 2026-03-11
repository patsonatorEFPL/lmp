package com.lmp.auth.service;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.lmp.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour le flux de vérification d'email complet.
 * Vérifie :
 * - La vérification d'email via token
 * - La réactivation d'un compte INACTIVE lors de la vérification
 * - Le scheduler d'auto-suspension
 * - Le rejet des emails jetables
 * - La requête JPQL pour les utilisateurs non vérifiés expirés
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class EmailVerificationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationScheduler scheduler;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });
    }

    private User createTestUser(String email, boolean emailVerified, UserStatus status,
                                 LocalDateTime registrationDate, String token) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRegistrationDate(registrationDate);
        user.setStatus(status);
        user.setAccountLocked(false);
        user.setEmailVerified(emailVerified);
        user.setVerificationToken(token);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    // ===== Tests verifyEmail =====

    @Test
    void testVerifyEmail_validToken_shouldVerifyAndKeepActive() {
        String token = UUID.randomUUID().toString();
        User user = createTestUser("verify-active@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now(), token);

        boolean result = authService.verifyEmail(token);

        assertTrue(result, "La vérification doit réussir");
        User updated = userRepository.findByEmail("verify-active@test.com").orElseThrow();
        assertTrue(updated.getEmailVerified(), "emailVerified doit être true");
        assertNull(updated.getVerificationToken(), "Le token doit être effacé");
        assertEquals(UserStatus.ACTIVE, updated.getStatus(), "Le statut doit rester ACTIVE");
    }

    @Test
    void testVerifyEmail_inactiveUser_shouldReactivate() {
        String token = UUID.randomUUID().toString();
        User user = createTestUser("verify-inactive@test.com", false, UserStatus.INACTIVE,
                LocalDateTime.now().minusHours(25), token);

        boolean result = authService.verifyEmail(token);

        assertTrue(result, "La vérification doit réussir même pour un compte INACTIVE");
        User updated = userRepository.findByEmail("verify-inactive@test.com").orElseThrow();
        assertTrue(updated.getEmailVerified(), "emailVerified doit être true");
        assertEquals(UserStatus.ACTIVE, updated.getStatus(), "Le statut doit être réactivé en ACTIVE");
    }

    @Test
    void testVerifyEmail_invalidToken_shouldReturnFalse() {
        assertFalse(authService.verifyEmail("token-inexistant"),
                "Un token invalide doit retourner false");
    }

    @Test
    void testVerifyEmail_nullToken_shouldReturnFalse() {
        assertFalse(authService.verifyEmail(null), "null doit retourner false");
    }

    @Test
    void testVerifyEmail_emptyToken_shouldReturnFalse() {
        assertFalse(authService.verifyEmail(""), "vide doit retourner false");
    }

    // ===== Tests disposable email =====

    @Test
    void testIsDisposableEmail() {
        assertTrue(authService.isDisposableEmail("test@mailinator.com"),
                "mailinator.com doit être détecté comme jetable");
        assertFalse(authService.isDisposableEmail("user@gmail.com"),
                "gmail.com ne doit pas être jetable");
    }

    // ===== Tests UserRepository JPQL =====

    @Test
    void testFindUnverifiedExpiredUsers_shouldFindExpired() {
        // Utilisateur non vérifié inscrit il y a 25h (expiré)
        createTestUser("expired@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(25), UUID.randomUUID().toString());

        // Utilisateur non vérifié inscrit il y a 1h (pas encore expiré)
        createTestUser("recent@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(1), UUID.randomUUID().toString());

        // Utilisateur vérifié inscrit il y a 25h (ne doit pas apparaître)
        createTestUser("verified@test.com", true, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(25), null);

        // Utilisateur déjà INACTIVE (ne doit pas apparaître)
        createTestUser("already-inactive@test.com", false, UserStatus.INACTIVE,
                LocalDateTime.now().minusHours(48), UUID.randomUUID().toString());

        LocalDateTime deadline = LocalDateTime.now().minusHours(24);
        List<User> expiredUsers = userRepository.findUnverifiedExpiredUsers(deadline);

        assertEquals(1, expiredUsers.size(), "Seul l'utilisateur expiré ACTIVE doit être retourné");
        assertEquals("expired@test.com", expiredUsers.get(0).getEmail());
    }

    // ===== Tests Scheduler =====

    @Test
    void testScheduler_shouldSuspendExpiredUsers() {
        // Créer un utilisateur expiré
        createTestUser("scheduler-test@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(25), UUID.randomUUID().toString());

        // Exécuter le scheduler manuellement
        scheduler.suspendUnverifiedAccounts();

        // Vérifier que le compte a été suspendu
        User updated = userRepository.findByEmail("scheduler-test@test.com").orElseThrow();
        assertEquals(UserStatus.INACTIVE, updated.getStatus(),
                "Le compte non vérifié expiré doit être passé en INACTIVE");
    }

    @Test
    void testScheduler_shouldNotSuspendRecentUsers() {
        // Créer un utilisateur récent (inscrit il y a 1h)
        createTestUser("recent-scheduler@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(1), UUID.randomUUID().toString());

        // Exécuter le scheduler
        scheduler.suspendUnverifiedAccounts();

        // Le compte doit rester ACTIVE
        User updated = userRepository.findByEmail("recent-scheduler@test.com").orElseThrow();
        assertEquals(UserStatus.ACTIVE, updated.getStatus(),
                "Le compte récent ne doit pas être suspendu");
    }

    @Test
    void testScheduler_shouldNotSuspendVerifiedUsers() {
        // Créer un utilisateur vérifié inscrit il y a longtemps
        createTestUser("verified-old@test.com", true, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(48), null);

        scheduler.suspendUnverifiedAccounts();

        User updated = userRepository.findByEmail("verified-old@test.com").orElseThrow();
        assertEquals(UserStatus.ACTIVE, updated.getStatus(),
                "Un utilisateur vérifié ne doit jamais être suspendu");
    }

    // ===== Test flux complet : inscription → suspension → vérification → réactivation =====

    @Test
    void testFullFlow_register_suspend_verify_reactivate() {
        // 1. Simuler un utilisateur inscrit il y a 25h sans vérification
        String token = UUID.randomUUID().toString();
        User user = createTestUser("fullflow@test.com", false, UserStatus.ACTIVE,
                LocalDateTime.now().minusHours(25), token);

        // 2. Le scheduler le suspend
        scheduler.suspendUnverifiedAccounts();
        User suspended = userRepository.findByEmail("fullflow@test.com").orElseThrow();
        assertEquals(UserStatus.INACTIVE, suspended.getStatus(), "Doit être suspendu");

        // 3. L'utilisateur clique le lien de vérification
        boolean verified = authService.verifyEmail(token);
        assertTrue(verified, "La vérification doit réussir");

        // 4. Le compte est réactivé
        User reactivated = userRepository.findByEmail("fullflow@test.com").orElseThrow();
        assertEquals(UserStatus.ACTIVE, reactivated.getStatus(), "Doit être réactivé");
        assertTrue(reactivated.getEmailVerified(), "Email doit être marqué vérifié");
        assertNull(reactivated.getVerificationToken(), "Token doit être null");
    }
}
