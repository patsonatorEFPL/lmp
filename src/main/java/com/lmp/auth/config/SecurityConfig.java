package com.lmp.auth.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
 * Spring Security LMP : chaîne {@code /api/**} (JSON, CSRF SPA) et chaîne Thymeleaf (formLogin).
 * Plafond de sessions : {@link #MAX_CONCURRENT_SESSIONS_PER_USER}, partagé avec
 * {@link ProgrammaticLoginSessionAuthenticationStrategyFactory} (login programmatique).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final int MAX_CONCURRENT_SESSIONS_PER_USER = 2;

        private final CustomUserDetailsService userDetailsService;

        private final PurchaseIntentAuthenticationSuccessHandler purchaseIntentAuthenticationSuccessHandler;
    private final AdminRateLimitFilter adminRateLimitFilter;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private CustomOidcUserService customOidcUserService;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String corsAllowedOrigins;

    @org.springframework.beans.factory.annotation.Value("${security.remember-me.secret:lmpRememberMe-dev-changeme}")
    private String rememberMeSecret;

    /** Cookie domain partagé cross-subdomain (ex. lmp-services.ca pour partager auth.* ↔ dev.* ↔ apex). */
    @org.springframework.beans.factory.annotation.Value("${server.servlet.session.cookie.domain:}")
    private String cookieDomain;

    /** URL de base de l'host auth (issuer OIDC). Utilisée pour les redirects Spring Security
     *  vers /login depuis n'importe quel host — la page login n'existe QUE sur auth.*. */
    @org.springframework.beans.factory.annotation.Value("${app.oauth2.issuer-uri:}")
    private String authBaseUrl;

    /** URL de base du site principal — pour redirects post-logout (le site, pas l'host auth). */
    @org.springframework.beans.factory.annotation.Value("${app.base.url:}")
    private String siteBaseUrl;


    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           PurchaseIntentAuthenticationSuccessHandler purchaseIntentAuthenticationSuccessHandler,
                           AdminRateLimitFilter adminRateLimitFilter) {
        this.userDetailsService = userDetailsService;
        this.purchaseIntentAuthenticationSuccessHandler = purchaseIntentAuthenticationSuccessHandler;
        this.adminRateLimitFilter = adminRateLimitFilter;
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
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/staff-invitations/preview",
                                "/api/v1/auth/staff-invitations/accept",
                                "/api/v1/services/**",
                                "/api/v1/contact",
                                "/api/v1/appointments/**",
                                "/api/v1/config",
                                "/api/v1/blog/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/guest-order/preview/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/guest-order/prepare")
                        .permitAll()

                        // Webhooks Stripe + sync externe (pas d'auth — validés par HMAC)
                        .requestMatchers(
                                "/api/webhooks/**",
                                "/api/v1/webhooks/**",
                                "/api/payments/**",
                                "/api/payment-status/**")
                        .permitAll()

                        // Sentry health check (monitoring externe)
                        .requestMatchers("/api/sentry/health")
                        .permitAll()

                        // Endpoints legacy publics
                        .requestMatchers(
                                "/api/orders/save-purchase-intent",
                                "/api/orders/get-purchase-intent",
                                "/api/orders/clear-purchase-intent")
                        .permitAll()

                        // Admin + Dev endpoints (DevSyncController n'existe qu'en @Profile("dev"))
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Dev endpoints — @Profile("dev") controller only exists in dev
                        .requestMatchers("/api/v1/dev/**").permitAll()

                        // Tout le reste nécessite authentification
                        .anyRequest().authenticated())

                // CSRF avec CookieCsrfTokenRepository pour SPA Angular
                // Use plain CsrfTokenRequestAttributeHandler (no XOR/BREACH protection)
                // so Angular can read the raw cookie value and send it back as header
                .csrf(csrf -> csrf
                        .csrfTokenRepository(buildCsrfRepository())
                        .csrfTokenRequestHandler(spaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                // CDN-cacheable public reads — CSRF bypassed for GET only so the
                                // eager spaCsrfTokenRequestHandler doesn't emit Set-Cookie
                                // (Cloudflare skips cache when Set-Cookie is present).
                                // Admin POST/PUT/DELETE on /blog and /services keep CSRF.
                                new AntPathRequestMatcher("/api/v1/config", "GET"),
                                new AntPathRequestMatcher("/api/v1/blog", "GET"),
                                new AntPathRequestMatcher("/api/v1/blog/**", "GET"),
                                new AntPathRequestMatcher("/api/v1/services", "GET"),
                                new AntPathRequestMatcher("/api/v1/services/**", "GET"),
                                new AntPathRequestMatcher("/api/v1/dev/**"),
                                new AntPathRequestMatcher("/api/webhooks/**"),
                                new AntPathRequestMatcher("/api/v1/webhooks/**"),
                                new AntPathRequestMatcher("/api/v1/auth/login"),
                                new AntPathRequestMatcher("/api/v1/auth/register"),
                                new AntPathRequestMatcher("/api/v1/auth/forgot-password"),
                                new AntPathRequestMatcher("/api/v1/auth/reset-password"),
                                new AntPathRequestMatcher("/api/v1/auth/staff-invitations/accept"),
                                new AntPathRequestMatcher("/api/v1/contact"),
                                new AntPathRequestMatcher("/api/v1/appointments"),
                                new AntPathRequestMatcher("/api/v1/payments/guest-order/prepare")))

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

                // Rate limiting for admin endpoints
                .addFilterBefore(adminRateLimitFilter,
                        org.springframework.security.web.csrf.CsrfFilter.class)

                // Filter to eagerly load CSRF token (sets cookie on every response)
                .addFilterAfter(csrfCookieFilter(),
                        org.springframework.security.web.csrf.CsrfFilter.class)

                // Sessions (partage avec Thymeleaf — même JSESSIONID)
                .sessionManagement(session -> session
                        .maximumSessions(MAX_CONCURRENT_SESSIONS_PER_USER)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry()));

        return http.build();
    }

    // =========================================================================
    // Chaîne 2 : Backend routes (redirects vers Angular, Stripe, webhooks, auth)
    // =========================================================================

    @Bean
    @Order(2)
    public SecurityFilterChain backendFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (redirections vers Angular + endpoints backend)
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/*.js",
                                "/*.css",
                                "/*.map",
                                "/*.ico",
                                "/*.png",
                                "/*.jpg",
                                "/*.jpeg",
                                "/*.svg",
                                "/*.woff2",
                                "/images/**",
                                "/about",
                                "/services",
                                "/contact",
                                "/contact/success",
                                "/map",
                                "/blog",
                                "/blog/**",
                                "/privacy",
                                "/terms",
                                "/register",
                                "/register-and-checkout",
                                "/auth/register-and-checkout",
                                "/login",

                                "/forgot-password",
                                "/reset-password",
                                "/verify-email",
                                "/resend-verification",
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

                        // Endpoints Stripe Checkout
                        .requestMatchers(
                                "/stripe/checkout/**",
                                "/stripe/webhook/**")
                        .permitAll()

                        // Endpoints de rendez-vous publics
                        .requestMatchers(
                                "/appointments/available-slots",
                                "/appointments/create")
                        .permitAll()

                        // Actuator : health + sous-probes (liveness/readiness) publics
                        // pour Dokploy/k8s. Le reste (metrics, info, etc.) → ADMIN.
                        .requestMatchers("/actuator/health/**", "/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Pages d'administration
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Pages utilisateur (redirections vers Angular)
                        .requestMatchers(
                                "/dashboard/**",
                                "/profile/**",
                                "/orders/**",
                                "/reviews/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated())

                // Configuration du formulaire de connexion (session-based auth pour backend).
                // loginPage absolu = host auth (la page n'existe QUE sur auth.* — sur les
                // autres hosts l'OidcHostGuardFilter retourne 404).
                .formLogin(form -> form
                        .loginPage(absoluteAuthUrl("/login"))
                        .loginProcessingUrl("/perform-login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(purchaseIntentAuthenticationSuccessHandler)
                        .failureUrl(absoluteAuthUrl("/login?error=true"))
                        .permitAll())

                // Configuration OAuth2 Login (Google & Microsoft)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage(absoluteAuthUrl("/login"))
                        .userInfoEndpoint(userInfo -> {
                            if (customOAuth2UserService != null) {
                                userInfo.userService(customOAuth2UserService);
                            }
                            if (customOidcUserService != null) {
                                userInfo.oidcUserService(customOidcUserService);
                            }
                        })
                        .successHandler(purchaseIntentAuthenticationSuccessHandler)
                        .failureUrl(absoluteAuthUrl("/login?error=true")))

                // Configuration de la déconnexion.
                // logoutSuccessUrl absolu vers baseUrl (sur auth.* "/" est 404).
                // deleteCookies couvre Spring Session (SESSION) + JSESSIONID legacy + XSRF-TOKEN.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(absoluteBaseUrl("/"))
                        .deleteCookies("SESSION", "JSESSIONID", "XSRF-TOKEN")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll())

                // Se souvenir de moi
                .rememberMe(remember -> remember
                        .key(rememberMeSecret)
                        .tokenValiditySeconds(86400)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("rememberMe"))

                // Sessions
                .sessionManagement(session -> session
                        .maximumSessions(MAX_CONCURRENT_SESSIONS_PER_USER)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                        .sessionRegistry(sessionRegistry()))

                // CSRF aligné avec chain 1 : cookie-based + plain token (Angular lit
                // XSRF-TOKEN cookie et envoie X-XSRF-TOKEN header).
                // Les endpoints JSON admin /admin/**/api/** sont skip — même profil de
                // sécurité que /api/v1/admin/** (auth via @PreAuthorize, SPA same-origin).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(buildCsrfRepository())
                        .csrfTokenRequestHandler(spaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                "/webhook/**",
                                "/stripe/**",
                                "/admin/*/api/**",
                                "/admin/api/**",
                                "/register-and-checkout",
                                "/auth/register-and-checkout",
                                "/appointments/create",
                                "/appointments/available-slots",
                                "/perform-login"))

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
        // Origins dynamiques depuis la propriété app.cors.allowed-origins
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(origins);
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

    // Pas de @Bean DaoAuthenticationProvider : Spring Security 7 le construit
    // automatiquement à partir des beans UserDetailsService + PasswordEncoder.
    // Définir le bean masque cette discovery (WARN
    // InitializeUserDetailsBeanManagerConfigurer au boot).

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Stratégie injectée dans {@link com.lmp.auth.web.session.ProgrammaticHttpSessionLogin}.
     * @see ProgrammaticLoginSessionAuthenticationStrategyFactory
     */
    @Bean(name = "programmaticLoginSessionAuthenticationStrategy")
    public SessionAuthenticationStrategy programmaticLoginSessionAuthenticationStrategy(SessionRegistry sessionRegistry) {
        return ProgrammaticLoginSessionAuthenticationStrategyFactory.create(
                sessionRegistry, MAX_CONCURRENT_SESSIONS_PER_USER, false);
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
     * Préfixe une URL relative avec authBaseUrl pour pointer vers l'host auth.
     * Si authBaseUrl est vide (local/dev sans issuer configuré), retourne l'URL
     * relative telle quelle — Spring Security gardera son comportement par défaut.
     */
    private String absoluteAuthUrl(String relativePath) {
        return relativePath;
    }

    private String absoluteBaseUrl(String relativePath) {
        return relativePath;
    }

    /**
     * Construit le CookieCsrfTokenRepository avec un Domain attribute partagé
     * cross-subdomain (auth.* ↔ apex/dev.*). Sans Domain, le cookie XSRF reste
     * scopé à l'host exact et le SPA cross-host ne peut pas l'envoyer.
     */
    private CookieCsrfTokenRepository buildCsrfRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            repo.setCookieCustomizer(c -> c.domain(cookieDomain));
        }
        return repo;
    }

    /**
     * Public read endpoints whose responses are cacheable on the CDN.
     * Skipping the eager CSRF cookie here keeps Set-Cookie off the response so
     * Cloudflare can store it. Angular still bootstraps the XSRF cookie on its
     * first /api/v1/auth/me call (provideAppInitializer in app.config.ts).
     */
    private static final List<String> CACHEABLE_PUBLIC_PATHS = List.of(
            "/api/v1/config",
            "/api/v1/blog",
            "/api/v1/services");

    private OncePerRequestFilter csrfCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                if (!isCacheablePath(request)) {
                    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                    if (csrfToken != null) {
                        csrfToken.getToken();
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    private static boolean isCacheablePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : CACHEABLE_PUBLIC_PATHS) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
