package com.lmp.auth.web;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.lmp.TestcontainersConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour les endpoints de vérification d'email dans AuthController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private User createTestUser(String email, String token) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRegistrationDate(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        user.setEmailVerified(false);
        user.setVerificationToken(token);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    // ===== Tests GET /verify-email =====

    @Test
    void testVerifyEmail_validToken_shouldRedirectWithSuccess() throws Exception {
        String token = UUID.randomUUID().toString();
        createTestUser("verify-ctrl@test.com", token);

        mockMvc.perform(get("/verify-email").param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified=true"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testVerifyEmail_invalidToken_shouldRedirectWithError() throws Exception {
        mockMvc.perform(get("/verify-email").param("token", "invalid-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ===== Tests POST /resend-verification =====

    @Test
    @WithMockUser(username = "resend-test@test.com", roles = "USER")
    void testResendVerification_authenticated_shouldRedirect() throws Exception {
        createTestUser("resend-test@test.com", UUID.randomUUID().toString());

        // Note: le send email peut échouer en test (pas de SMTP) mais l'endpoint doit fonctionner
        mockMvc.perform(post("/resend-verification").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/dashboard*"));
    }

    @Test
    void testResendVerification_unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/resend-verification").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // ===== Test que /verify-email est accessible publiquement =====

    @Test
    void testVerifyEmail_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/verify-email").param("token", "any-token"))
                .andExpect(status().is3xxRedirection()); // Redirige vers /login, pas 403
    }
}
