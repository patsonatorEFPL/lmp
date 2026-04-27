package com.lmp.shared.web.api;

import com.lmp.TestcontainersConfiguration;
import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminDashboardStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role userRole;
    private Role adminRole;
    private User adminUser;

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

        if (userRepository.findByEmail("admin@test.com").isEmpty()) {
            adminUser = new User();
            adminUser.setEmail("admin@test.com");
            adminUser.setPassword(passwordEncoder.encode("Admin123!"));
            adminUser.setFirstName("Admin");
            adminUser.setLastName("Test");
            adminUser.setRegistrationDate(LocalDateTime.now());
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser.setAccountLocked(false);
            adminUser.setEmailVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);
            adminUser.setRoles(roles);
            userRepository.save(adminUser);
        } else {
            adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();
        }
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void revenueSeries_week_returnsData() throws Exception {
        createOrder("Service A", BigDecimal.valueOf(100), LocalDateTime.now().minusDays(5));
        createOrder("Service B", BigDecimal.valueOf(200), LocalDateTime.now().minusDays(10));
        createOrder("Service C", BigDecimal.valueOf(300), LocalDateTime.now().minusDays(12));

        mockMvc.perform(get("/api/v1/admin/revenue-series")
                        .param("period", "week")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.current").isArray())
                .andExpect(jsonPath("$.data.labels.length()").value(16));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void revenueSeries_month_returnsData() throws Exception {
        createOrder("Service A", BigDecimal.valueOf(100), LocalDateTime.now().minusDays(5));
        createOrder("Service B", BigDecimal.valueOf(200), LocalDateTime.now().minusDays(10));
        createOrder("Service C", BigDecimal.valueOf(300), LocalDateTime.now().minusDays(20));

        mockMvc.perform(get("/api/v1/admin/revenue-series")
                        .param("period", "month")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.labels").isArray())
                .andExpect(jsonPath("$.data.current").isArray())
                .andExpect(jsonPath("$.data.labels.length()").value(12));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void topServices_returnsList() throws Exception {
        createOrder("SEO Premium", BigDecimal.valueOf(500), LocalDateTime.now().minusDays(5));
        createOrder("Web Design", BigDecimal.valueOf(300), LocalDateTime.now().minusDays(3));

        mockMvc.perform(get("/api/v1/admin/top-services").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("SEO Premium"))
                .andExpect(jsonPath("$.data[0].orders").value(1))
                .andExpect(jsonPath("$.data[1].name").value("Web Design"))
                .andExpect(jsonPath("$.data[1].orders").value(1));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void healthServices_returnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/admin/health/services").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Base de données"))
                .andExpect(jsonPath("$.data[0].status").value("UP"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void revenueSeries_unauthorized_forUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/revenue-series")
                        .param("period", "week")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private Order createOrder(String serviceName, BigDecimal amount, LocalDateTime createdAt) {
        Order order = new Order();
        order.setUser(adminUser);
        order.setServiceName(serviceName);
        order.setTotalAmount(amount);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCurrency("EUR");
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);
        return orderRepository.save(order);
    }
}
