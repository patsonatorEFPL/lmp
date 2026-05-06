package com.lmp.shared.web.api;

import com.lmp.TestcontainersConfiguration;
import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour POST /api/v1/admin/users (création d'utilisateur par admin).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminCreateUserTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
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

        // Ensure the acting admin exists (matches @WithMockUser username)
        if (userRepository.findByEmail("admin@test.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@test.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setFirstName("Admin");
            admin.setLastName("Test");
            admin.setRegistrationDate(LocalDateTime.now());
            admin.setStatus(UserStatus.ACTIVE);
            admin.setAccountLocked(false);
            admin.setEmailVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);
            admin.setRoles(roles);
            userRepository.save(admin);
        }
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldCreateUser() throws Exception {
        String json = """
                {
                    "email": "newuser@gmail.com",
                    "firstName": "Jean",
                    "lastName": "Dupont",
                    "password": "Strong1@pass",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Optional<User> created = userRepository.findByEmail("newuser@gmail.com");
        assertThat(created).isPresent();
        assertThat(created.get().getFirstName()).isEqualTo("Jean");
        assertThat(created.get().getLastName()).isEqualTo("Dupont");
        assertThat(created.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldCreateAdminUser() throws Exception {
        String json = """
                {
                    "email": "newadmin@gmail.com",
                    "firstName": "Admin",
                    "lastName": "Nouveau",
                    "password": "Admin99@x",
                    "admin": true
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Optional<User> created = userRepository.findByEmail("newadmin@gmail.com");
        assertThat(created).isPresent();
        // Reload with roles to check admin role
        User user = userRepository.findByIdWithRoles(created.get().getId()).orElseThrow();
        assertThat(user.getRoles()).extracting(Role::getName).contains("ADMIN", "USER");
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldRejectDuplicateEmail() throws Exception {
        // Create a user first
        User existing = new User();
        existing.setEmail("duplicate@gmail.com");
        existing.setPassword(passwordEncoder.encode("Test123!"));
        existing.setFirstName("Exist");
        existing.setLastName("User");
        existing.setRegistrationDate(LocalDateTime.now());
        existing.setStatus(UserStatus.ACTIVE);
        existing.setAccountLocked(false);
        existing.setEmailVerified(false);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        existing.setRoles(roles);
        userRepository.save(existing);

        String json = """
                {
                    "email": "duplicate@gmail.com",
                    "password": "Strong1@pass",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldRejectWeakPassword() throws Exception {
        String json = """
                {
                    "email": "weak@gmail.com",
                    "password": "123",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldRejectMissingEmail() throws Exception {
        String json = """
                {
                    "password": "Strong1@pass",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void shouldRejectDisposableEmail() throws Exception {
        String json = """
                {
                    "email": "user@yopmail.com",
                    "password": "Strong1@pass",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Adresse email jetable détectée. Même un administrateur ne peut pas créer de compte avec une adresse temporaire."));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void shouldDenyNonAdmin() throws Exception {
        String json = """
                {
                    "email": "blocked@gmail.com",
                    "password": "Strong1@pass",
                    "admin": false
                }
                """;

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
