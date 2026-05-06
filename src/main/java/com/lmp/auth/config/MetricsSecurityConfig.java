package com.lmp.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Sécurise {@code /actuator/prometheus} via Basic Auth dédié.
 *
 * Chaîne ordonnée avant les autres et avec son propre AuthenticationManager pour que
 * Prometheus puisse scraper avec credentials (env: PROMETHEUS_BASIC_USER /
 * PROMETHEUS_BASIC_PASS) sans toucher au CustomUserDetailsService applicatif.
 *
 * <p>L'isolation est faite via un {@link ProviderManager} local injecté dans la chaîne :
 * {@code .authenticationManager(...)}. Sans cela, Spring Security tombe sur le bean
 * {@code AuthenticationManager} global (qui utilise le user store applicatif) et
 * cherche l'utilisateur "prometheus" en base — il n'existe pas et la requête est
 * rejetée avec 401 même si les credentials Basic Auth sont valides.
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
                // EndpointRequest.to(...) est le matcher Spring Boot dédié aux endpoints
                // Actuator. `securityMatcher("/actuator/prometheus")` brut utilise le
                // matcher MVC qui ne connaît pas les endpoints Actuator (mapping séparé)
                // → la chaîne ne s'activait pas et la requête tombait sur la chaîne
                // applicative globale.
                .securityMatcher(EndpointRequest.to("prometheus"))
                .authenticationManager(metricsAuthenticationManager())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("METRICS"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /**
     * AuthenticationManager dédié à la chaîne metrics. Utilise un
     * {@link DaoAuthenticationProvider} alimenté par le store in-memory ci-dessous.
     * Aucune dépendance avec l'AuthenticationManager applicatif.
     */
    @SuppressWarnings("deprecation")
    private AuthenticationManager metricsAuthenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(metricsUserDetailsService());
        // {noop} prefix dans le password stocké → NoOpPasswordEncoder. La valeur réelle
        // vient déjà d'un secret (env var) donc on ne hash pas une seconde fois.
        provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
        return new ProviderManager(provider);
    }

    /**
     * Service utilisateur isolé (in-memory) — découplé de {@code CustomUserDetailsService}
     * pour éviter qu'un utilisateur applicatif puisse se loguer sur l'endpoint metrics.
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
