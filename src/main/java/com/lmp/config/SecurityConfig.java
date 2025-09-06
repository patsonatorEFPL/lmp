package com.lmp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import com.lmp.service.auth.CustomUserDetailsService;

/**
 * Configuration de sécurité Spring Security pour l'application LMP.
 * 
 * Définit les règles d'accès, l'authentification et l'autorisation.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PurchaseIntentAuthenticationSuccessHandler purchaseIntentAuthenticationSuccessHandler;

    /**
     * Configuration du filtre de sécurité HTTP.
     * 
     * @param http Configuration HTTP Security
     * @return SecurityFilterChain configuré
     * @throws Exception si erreur de configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error"
                ).permitAll()
                
                // Endpoints SEO - accès public pour les moteurs de recherche
                .requestMatchers(
                    "/sitemap.xml",
                    "/robots.txt",
                    "/googleb72d4c095922c4a8.html"
                ).permitAll()
                
                // Endpoints Stripe Checkout - accès public pour le processus de paiement
                .requestMatchers(
                    "/api/payments/**",
                    "/api/webhooks/**",
                    "/stripe/checkout/**",
                    "/stripe/webhook/**"
                ).permitAll()
                
                // Endpoints d'API pour intentions de paiement - accès public
                .requestMatchers(
                    "/api/orders/save-purchase-intent",
                    "/api/orders/get-purchase-intent",
                    "/api/orders/clear-purchase-intent"
                ).permitAll()
                
                // Endpoints de rendez-vous publics (consultation créneaux et création)
                .requestMatchers(
                    "/appointments/available-slots",
                    "/appointments/create"
                ).permitAll()
                
                // Endpoints d'API sécurisés - authentification requise
                .requestMatchers(
                    "/api/orders/**"
                ).authenticated()
                
                // Pages d'administration - rôle ADMIN requis
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Pages utilisateur - rôle USER ou ADMIN requis
                .requestMatchers(
                    "/dashboard/**", 
                    "/profile/**", 
                    "/orders/**",
                    "/reviews/**"
                ).hasAnyRole("USER", "ADMIN")
                
                // Toutes les autres requêtes nécessitent une authentification
                .anyRequest().authenticated()
            )
            
            // Configuration du formulaire de connexion
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform-login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(purchaseIntentAuthenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            
            // Configuration de la déconnexion
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            
            // Configuration "Se souvenir de moi"
            .rememberMe(remember -> remember
                .key("lmpSecretKey")
                .tokenValiditySeconds(86400) // 24 heures
                .userDetailsService(userDetailsService)
                .rememberMeParameter("rememberMe")
            )
            
            // Configuration de la gestion des sessions avec SessionRegistry
            .sessionManagement(session -> session
                .maximumSessions(2) // Maximum 2 sessions par utilisateur
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?expired=true")
                .sessionRegistry(sessionRegistry()) // Ajout du SessionRegistry
            )
            
            // Désactiver CSRF pour les webhooks et endpoints de paiement + appointments temporairement
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/webhook/**", 
                    "/api/**", 
                    "/stripe/**", 
                    "/register-and-checkout", 
                    "/auth/register-and-checkout",
                    "/appointments/create",  // Temporaire pour debug
                    "/appointments/available-slots"
                )
            )
            
            // Configuration CORS globale
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.List.of(
                    "http://localhost:*",
                    "https://lmp-services.ca"
                ));
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }));

        return http.build();
    }

    /**
     * Gestionnaire de succès d'authentification personnalisé par défaut.
     * Cette méthode est conservée pour compatibilité mais n'est plus utilisée.
     * Le gestionnaire principal est maintenant PurchaseIntentAuthenticationSuccessHandler.
     *
     * @return AuthenticationSuccessHandler configuré
     * @deprecated Utiliser PurchaseIntentAuthenticationSuccessHandler à la place
     */
    @Bean
    @Deprecated
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectUrl = "/dashboard";
            
            // Redirection selon le rôle
            if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                redirectUrl = "/admin/dashboard";
            } else if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"))) {
                redirectUrl = "/dashboard";
            }
            
            response.sendRedirect(redirectUrl);
        };
    }

    /**
     * Encodeur de mots de passe BCrypt.
     * 
     * @return PasswordEncoder configuré
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Fournisseur d'authentification DAO.
     * 
     * @return DaoAuthenticationProvider configuré
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Registre des sessions pour la gestion et l'invalidation des sessions actives.
     *
     * @return SessionRegistry configuré
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Gestionnaire d'authentification.
     *
     * @param config Configuration d'authentification
     * @return AuthenticationManager
     * @throws Exception si erreur de configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

