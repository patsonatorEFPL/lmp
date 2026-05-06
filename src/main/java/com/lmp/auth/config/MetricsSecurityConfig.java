package com.lmp.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

/**
 * Sécurise {@code /actuator/prometheus} via Basic Auth dédié.
 *
 * Chaîne ordonnée avant les autres pour que Prometheus puisse scraper avec credentials
 * (env: PROMETHEUS_BASIC_USER / PROMETHEUS_BASIC_PASS) sans toucher aux sessions / OIDC
 * du reste de l'application.
 */
@Configuration
public class MetricsSecurityConfig {

    @Value("${lmp.metrics.basic-auth.username:prometheus}")
    private String username;

    @Value("${lmp.metrics.basic-auth.password:}")
    private String password;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain prometheusSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/prometheus")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("METRICS"))
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(metricsUserDetailsService())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /**
     * Service utilisateur isolé (in-memory) — découplé de {@code CustomUserDetailsService} pour
     * éviter qu'un utilisateur applicatif puisse se loguer sur l'endpoint metrics.
     *
     * Mot de passe en clair via {@code {noop}} car la valeur source vient déjà d'un secret env var.
     */
    private UserDetailsService metricsUserDetailsService() {
        if (password == null || password.isBlank()) {
            return new InMemoryUserDetailsManager();
        }
        UserDetails u = User.withUsername(username)
                .password("{noop}" + password)
                .roles("METRICS")
                .build();
        return new InMemoryUserDetailsManager(u);
    }
}
