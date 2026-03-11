package com.lmp.auth.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import com.lmp.auth.service.CustomOAuth2UserService;
import com.lmp.auth.service.CustomOidcUserService;
import com.lmp.auth.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Configuration de sécurité Spring Security pour l'application LMP.
 * 
 * Deux chaînes de filtres :
 * 1. API REST (/api/**) → JSON 401/403, CSRF cookie, stateless-like sessions
 * 2. Thymeleaf (legacy) → formLogin redirects, CSRF standard
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;

        private final PurchaseIntentAuthenticationSuccessHandler purchaseIntentAuthenticationSuccessHandler;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private CustomOidcUserService customOidcUserService;


    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           PurchaseIntentAuthenticationSuccessHandler purchaseIntentAuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.purchaseIntentAuthenticationSuccessHandler = purchaseIntentAuthenticationSuccessHandler;
    }

    // =========================================================================
    // Chaîne 1 : API REST — JSON 401/403, CSRF cookie
    // =========================================================================

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics API
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/services/**",
                                "/api/v1/contact",
                                "/api/v1/appointments/available-slots")
                        .permitAll()

                        // Webhooks Stripe (pas d'auth)
                        .requestMatchers(
                                "/api/webhooks/**",
                                "/api/payments/**",
                                "/api/payment-status/**")
                        .permitAll()

                        // Endpoints legacy publics
                        .requestMatchers(
                                "/api/orders/save-purchase-intent",
                                "/api/orders/get-purchase-intent",
                                "/api/orders/clear-purchase-intent")
                        .permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Tout le reste nécessite authentification
                        .anyRequest().authenticated())

                // CSRF avec CookieCsrfTokenRepository pour SPA Angular
                // Use plain CsrfTokenRequestAttributeHandler (no XOR/BREACH protection)
                // so Angular can read the raw cookie value and send it back as header
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(spaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                "/api/webhooks/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/contact"))

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // JSON 401 (pas de redirect)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\",\"status\":401}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"error\":\"FORBIDDEN\",\"message\":\"Access denied\",\"status\":403}");
                        }))

                // Filter to eagerly load CSRF token (sets cookie on every response)
                .addFilterAfter(csrfCookieFilter(),
                        org.springframework.security.web.csrf.CsrfFilter.class)

                // Sessions (partage avec Thymeleaf — même JSESSIONID)
                .sessionManagement(session -> session
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry()));

        return http.build();
    }

    // =========================================================================
    // Chaîne 2 : Thymeleaf + pages classiques (legacy)
    // =========================================================================

    @Bean
    @Order(2)
    public SecurityFilterChain thymeleafFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Pages publiques accessibles à tous (visiteurs)
                        .requestMatchers(
                                "/",
                                "/about",
                                "/services",
                                "/contact",
                                "/contact/success",
                                "/map",
                                "/privacy",
                                "/terms",
                                "/register",
                                "/register-and-checkout",
                                "/auth/register-and-checkout",
                                "/login",
                                "/verify-email",
                                "/resend-verification",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error")
                        .permitAll()

                        // Swagger UI et OpenAPI docs
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Endpoints SEO
                        .requestMatchers(
                                "/sitemap.xml",
                                "/robots.txt",
                                "/googleb72d4c095922c4a8.html")
                        .permitAll()

                        // Endpoints Stripe Checkout (legacy)
                        .requestMatchers(
                                "/stripe/checkout/**",
                                "/stripe/webhook/**")
                        .permitAll()

                        // Endpoints temporaires de test
                        .requestMatchers("/temp/**").permitAll()

                        // Endpoints de rendez-vous publics
                        .requestMatchers(
                                "/appointments/available-slots",
                                "/appointments/create")
                        .permitAll()

                        // Actuator health
                        .requestMatchers("/actuator/health").permitAll()

                        // Pages d'administration
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Pages utilisateur
                        .requestMatchers(
                                "/dashboard/**",
                                "/profile/**",
                                "/orders/**",
                                "/reviews/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated())

                // Configuration du formulaire de connexion (Thymeleaf)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(purchaseIntentAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll())

                // Configuration OAuth2 Login (Google & Microsoft)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> {
                            if (customOAuth2UserService != null) {
                                userInfo.userService(customOAuth2UserService);
                            }
                            if (customOidcUserService != null) {
                                userInfo.oidcUserService(customOidcUserService);
                            }
                        })
                        .successHandler(purchaseIntentAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true"))

                // Configuration de la déconnexion
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll())

                // Se souvenir de moi
                .rememberMe(remember -> remember
                        .key("lmpSecretKey")
                        .tokenValiditySeconds(86400)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("rememberMe"))

                // Sessions
                .sessionManagement(session -> session
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                        .sessionRegistry(sessionRegistry()))

                // CSRF standard pour Thymeleaf + exceptions webhooks
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/webhook/**",
                                "/stripe/**",
                                "/register-and-checkout",
                                "/auth/register-and-checkout",
                                "/appointments/create",
                                "/appointments/available-slots",
                                "/temp/**"))

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    // =========================================================================
    // Beans communs
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://lmp-services.be",
                "https://*.lmp-services.be",
                "https://lmp-services.ca"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Plain CSRF handler for SPA: Angular reads the raw XSRF-TOKEN cookie
     * and sends it back as X-XSRF-TOKEN header. No XOR/BREACH encoding.
     */
    private CsrfTokenRequestAttributeHandler spaCsrfTokenRequestHandler() {
        CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
        // Setting to null forces eager CSRF token resolution (not deferred)
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }

    /**
     * Eagerly loads the CSRF token so the XSRF-TOKEN cookie is always sent.
     * Without this filter, Spring Security 6 defers token generation and the
     * cookie may not be set on initial page load.
     */
    private OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken(); // Force cookie to be set
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
