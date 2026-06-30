package com.lmp.auth.web.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.lmp.TestcontainersConfiguration;
import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Vérifie que le login via POST /api/v1/auth/login (login programmatique +
 * SessionRepositoryFilter Spring Session) enregistre bien le principal dans le
 * {@link SessionRegistry} backé Redis — régression si la stratégie de session
 * programmatique diverge de la chaîne filtre API.
 * <p>
 * Le login passe par MockMvc avec la chaîne de filtres complète : le
 * SessionRegistry étant un {@code SpringSessionBackedSessionRegistry}, la
 * session n'est indexée dans Redis qu'à la complétion de la requête HTTP —
 * un appel direct à {@code ProgrammaticHttpSessionLogin#login} hors requête
 * ne suffit pas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProgrammaticHttpSessionLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);

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

    @AfterEach
    void tearDown() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void loginEnregistreLePrincipalDansLeSessionRegistry() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk());

        assertFalse(
                sessionRegistry.getAllSessions(EMAIL, false).isEmpty(),
                "SessionRegistry doit référencer la session après login programmatique (aligné chaîne API)");
    }
}
