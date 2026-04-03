package com.lmp.auth.web.session;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.TestcontainersConfiguration;
import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Vérifie que le login programmatique enregistre bien le principal dans le {@link SessionRegistry},
 * comme la chaîne filtre API avec {@code maximumSessions} (régression si la factory diverge).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ProgrammaticHttpSessionLoginIntegrationTest {

    @Autowired
    private ProgrammaticHttpSessionLogin programmaticHttpSessionLogin;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "session-registry-itest@example.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepository.save(r);
                });

        User user = new User();
        user.setFirstName("Session");
        user.setLastName("RegistryItest");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        user.setEmailVerified(true);
        user.setRegistrationDate(LocalDateTime.now());
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Test
    void loginEnregistreLePrincipalDansLeSessionRegistry() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD));

        programmaticHttpSessionLogin.login(request, response, auth);

        assertFalse(
                sessionRegistry.getAllSessions(auth.getPrincipal(), false).isEmpty(),
                "SessionRegistry doit référencer la session après login programmatique (aligné chaîne API)");
    }
}
