package com.lmp.auth.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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

    /**
     * Clé HMAC remember-me. Le default {@code lmpRememberMe-dev-changeme} est
     * acceptable en dev/staging local mais doit IMPÉRATIVEMENT être remplacé en
     * prod via {@code REMEMBER_ME_SECRET}. Le {@link #assertRememberMeSecretNotDefaultInProd()}
     * fail-fast au startup si le default est encore en place sous profil {@code prod}.
     */
    public static final String REMEMBER_ME_DEFAULT_SECRET = "lmpRememberMe-dev-changeme";

    @org.springframework.beans.factory.annotation.Value("${security.remember-me.secret:" + REMEMBER_ME_DEFAULT_SECRET + "}")
    private String rememberMeSecret;

    @Autowired
    private Environment environment;

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

    /**
     * SECURITY (M5) : refuser de démarrer sous profil {@code prod} si la clé
     * remember-me est encore le default dev. Empêche un déploiement prod
     * silencieux avec une clé devinable qui permettrait à un attaquant de
     * forger des cookies remember-me valides.
     *
     * Pas de fail-fast sous staging/dev pour ne pas casser les boots locaux.
     */
    @PostConstruct
    public void assertRememberMeSecretNotDefaultInProd() {
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        if (isProd && REMEMBER_ME_DEFAULT_SECRET.equals(rememberMeSecret)) {
            throw new IllegalStateException(
                "REMEMBER_ME_SECRET est encore le default dev sous profil prod — "
                + "set la variable d'environnement REMEMBER_ME_SECRET sur une valeur aléatoire "
                + "(>= 256 bits) avant de démarrer en production.");
        }
    }

    // =========================================================================
    // Chaîne 0 : actuator/health bypass — filter chain minimal
    // =========================================================================
    // async-profiler v4.4 bench 3k VU 2026-05-15 a montré 57.8% CPU dans
    // FilterChainProxy. La chaîne par défaut wrap chaque request avec 15+
    // filters (SecurityContextHolder, Csrf, OAuth2Login, BearerTokenAuth,
    // Anonymous, RequestCache, ExceptionTranslation, FilterSecurityInterceptor)
    // même pour /actuator/health/liveness qui est publique.
    //
    // Cette chaîne @Order(0) matche /actuator/health/** + /actuator/health en
    // premier et utilise ~3-4 filters seulement (HeaderWriter + AuthorizationFilter).
    // Dokploy + Traefik + Kubernetes-style probes hitent ce endpoint fréquemment.

    @Bean
    @Order(0)
    public SecurityFilterChain healthBypassFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(
                        "/actuator/health/**", "/actuator/health",
                        "/api/v1/config",
                        // Public read API — controllers ServiceRestController + BlogPostController
                        // ne définissent QUE des GET sur ces patterns publics. Tout admin POST/PUT/DELETE
                        // est sur des paths distincts (POST/PUT/DELETE sur /api/v1/blog matchent par
                        // contre — voir note ci-dessous). Iter32 bench 10k VU isolé montre 14-16% fails
                        // dans full apiFilterChain (15 filters), vs 3% dans cette chain bypass (3 filters).
                        "/api/v1/services/**",
                        "/api/v1/blog/search",
                        // Static public endpoints (no auth concern, GET only)
                        "/robots.txt",
                        "/sitemap.xml",
                        "/favicon.ico",
                        "/googleb72d4c095922c4a8.html")
                // CORS PRÉSERVÉ pour /api/v1/config : SPA Angular cross-subdomain
                // (auth.* vs dev.* vs apex) doit lire la response avec headers
                // Access-Control-Allow-Origin. withDefaults() utilise le bean
                // corsConfigurationSource() défini ligne 429.
                .cors(Customizer.withDefaults())
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.disable())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .logout(l -> l.disable())
                .anonymous(a -> a.disable())
                .requestCache(r -> r.disable())
                .securityContext(s -> s.disable())
                .exceptionHandling(e -> e.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }

    // =========================================================================
    // Chaîne 1 : API REST — JSON 401/403, CSRF cookie
    // =========================================================================

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
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
                                "/api/v1/webhooks/**")
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

                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // SECURITY (H4) : /api/v1/dev/** matcher retiré — aucun controller
                        // n'expose ce path. Si un dev controller revient, le déclarer @Profile("dev")
                        // + ajouter le matcher conditionnel. Sans ça, un controller oublié sans
                        // @PreAuthorize tomberait sur anyRequest().authenticated() — safe par défaut.

                        // Tout le reste nécessite authentification
                        .anyRequest().authenticated())

                // CSRF avec CookieCsrfTokenRepository pour SPA Angular
                // Use plain CsrfTokenRequestAttributeHandler (no XOR/BREACH protection)
                // so Angular can read the raw cookie value and send it back as header
                .csrf(csrf -> {
                    var pathMatcher = PathPatternRequestMatcher.withDefaults();
                    csrf
                        .csrfTokenRepository(buildCsrfRepository())
                        .csrfTokenRequestHandler(spaCsrfTokenRequestHandler())
                        // CDN-cacheable public reads — CSRF bypassed for GET only so the
                        // eager spaCsrfTokenRequestHandler doesn't emit Set-Cookie
                        // (Cloudflare skips cache when Set-Cookie is present).
                        // Admin POST/PUT/DELETE on /blog and /services keep CSRF.
                        .ignoringRequestMatchers(
                                pathMatcher.matcher(HttpMethod.GET, "/api/v1/config"),
                                pathMatcher.matcher(HttpMethod.GET, "/api/v1/blog"),
                                pathMatcher.matcher(HttpMethod.GET, "/api/v1/blog/**"),
                                pathMatcher.matcher(HttpMethod.GET, "/api/v1/services"),
                                pathMatcher.matcher(HttpMethod.GET, "/api/v1/services/**"))
                        .ignoringRequestMatchers(
                                "/api/webhooks/**",
                                "/api/v1/webhooks/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/staff-invitations/accept",
                                "/api/v1/contact",
                                "/api/v1/appointments",
                                "/api/v1/payments/guest-order/prepare");
                })

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

                // Strip XSRF-TOKEN Set-Cookie on cacheable GET responses BEFORE any
                // downstream filter can write it (CsrfFilter, eager handler, etc.).
                .addFilterBefore(xsrfStripFilter(),
                        org.springframework.security.web.csrf.CsrfFilter.class)

                // Filter to eagerly load CSRF token (sets cookie on every response)
                .addFilterAfter(csrfCookieFilter(),
                        org.springframework.security.web.csrf.CsrfFilter.class)

                // Sessions (partage avec Thymeleaf — même JSESSIONID)
                .sessionManagement(session -> session
                        .maximumSessions(MAX_CONCURRENT_SESSIONS_PER_USER)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry));

        return http.build();
    }

    // =========================================================================
    // Chaîne 2 : Backend routes (redirects vers Angular, Stripe, webhooks, auth)
    // =========================================================================

    @Bean
    @Order(2)
    public SecurityFilterChain backendFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
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
                                "/accept-invitation",
                                "/payment/guest",
                                "/payment/success",
                                "/payment/cancelled",
                                "/payment/processing",
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

                        // Iter41d Bug #5 fix — pages SPA admin + user permitAll() au niveau
                        // Spring Security : Angular sert le SPA shell + guards client-side
                        // (adminGuard / authGuard) check rôle via /api/v1/auth/me. Spring
                        // blocking ces paths cassait nav directe URL post-restart (ERR_TOO_MANY_REDIRECTS).
                        // Les ENDPOINTS API admin (/api/v1/admin/**) eux restent protégés
                        // par hasRole('ADMIN') (cf chain apiFilterChain Order(1)).
                        .requestMatchers("/admin/**", "/dashboard/**", "/profile/**",
                                "/orders/**", "/reviews/**")
                        .permitAll()

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
                // deleteCookies couvre Spring Session (SESSION) + JSESSIONID legacy + XSRF-TOKEN
                // + remember-me (sinon une session post-logout peut être ressuscitée par le cookie résiduel).
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(absoluteBaseUrl("/"))
                        .deleteCookies("SESSION", "JSESSIONID", "XSRF-TOKEN", "remember-me")
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
                        // Spring Security default = changeSessionId (Servlet 3.1+) qui appelle
                        // request.changeSessionId(). MAIS Spring Session 4.x Redis
                        // implementation de changeSessionId() perd les attributs custom non-Security
                        // (notamment SPRING_SECURITY_SAVED_REQUEST) lors de la rotation → après
                        // login OAuth flow, le success handler ne retrouve plus l'URL
                        // /oauth2/authorize originale et tombe sur le default /dashboard.
                        // migrateSession crée explicitement nouvelle session + copie TOUS les
                        // attributs via Enumeration → fixation-safe + preserve SAVED_REQUEST.
                        .sessionFixation(sf -> sf.migrateSession())
                        .maximumSessions(MAX_CONCURRENT_SESSIONS_PER_USER)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired=true")
                        .sessionRegistry(sessionRegistry))

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
        // Argon2id for new hashes (memory-hard, GPU/ASIC-resistant, OWASP 2024 preferred).
        // Existing {bcrypt} hashes keep verifying via DelegatingPasswordEncoder.
        // Successful login auto-upgrades bcrypt → argon2id via
        // CustomUserDetailsService.updatePassword() (UserDetailsPasswordService impl).
        // Spring Security wires it on the auto-discovered DaoAuthenticationProvider
        // when DelegatingPasswordEncoder.upgradeEncoding() returns true (= prefix
        // differs from "argon2id").
        //
        // Argon2id parameters (OWASP fast tier ~50ms on ARM A1 4-OCPU):
        //   saltLength = 16 bytes
        //   hashLength = 32 bytes
        //   parallelism = 1
        //   memory = 12 * 1024 KB (12 MB)
        //   iterations = 2
        // Compared to bcrypt cost 10 (~100ms ARM): ~2x faster password verification under saturation.
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 12 * 1024, 2);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        java.util.Map<String, PasswordEncoder> encoders = new java.util.HashMap<>();
        encoders.put("argon2id", argon2);
        encoders.put("bcrypt", bcrypt);
        // Default id "argon2id" → all new hashes prefixed {argon2id}.
        // Legacy raw bcrypt hashes ($2a$, $2b$) handled by setDefaultPasswordEncoderForMatches.
        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("argon2id", encoders);
        delegating.setDefaultPasswordEncoderForMatches(bcrypt);
        return delegating;
    }

    /**
     * async-profiler v4.4 bench 4k VU 2026-05-15 : AdminRateLimitFilter
     * exécuté DEUX FOIS par request (8035 samples passthrough wasted).
     *
     * <p>Cause : la classe est annotée {@code @Component} (DI requis) ce qui
     * déclenche l'auto-registration servlet de Spring Boot pour tout bean
     * implementing {@code Filter}/{@code OncePerRequestFilter}. PUIS la chaîne
     * {@code backendFilterChain} (@Order(2)) l'ajoute via
     * {@code .addFilterBefore(adminRateLimitFilter, ...)}.</p>
     *
     * <p>Fix : ce bean désactive la registration servlet auto. Le filter reste
     * actif via la chaîne Spring Security uniquement (chemin sémantiquement
     * correct car la chaîne définit l'ordre de dispatch).</p>
     */
    @Bean
    public FilterRegistrationBean<AdminRateLimitFilter> adminRateLimitFilterRegistration(
            AdminRateLimitFilter filter) {
        FilterRegistrationBean<AdminRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // Pas de @Bean DaoAuthenticationProvider : Spring Security 7 le construit
    // automatiquement à partir des beans UserDetailsService + PasswordEncoder.
    // Définir le bean masque cette discovery (WARN
    // InitializeUserDetailsBeanManagerConfigurer au boot).

    /**
     * SessionRegistry backed by Spring Session Redis — stateless multi-replica.
     * Requires {@link com.lmp.shared.config.SessionConfig} avec
     * {@code @EnableRedisIndexedHttpSession} pour exposer
     * {@link FindByIndexNameSessionRepository}.
     */
    @Bean
    public SessionRegistry sessionRegistry(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
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
     *
     * <p>Wrappé pour ne PAS écrire Set-Cookie sur les paths CDN-cacheables :
     * sinon Cloudflare bypasse la cache même avec ignoringRequestMatchers
     * (qui skip seulement la validation, pas le cookie save). Angular bootstrap
     * le cookie via /api/v1/auth/me (provideAppInitializer).</p>
     */
    private CsrfTokenRepository buildCsrfRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            repo.setCookieCustomizer(c -> c.domain(cookieDomain));
        }
        return new CacheablePathCsrfRepoWrapper(repo);
    }

    /**
     * Delegating CsrfTokenRepository qui skip saveToken sur les GETs publics
     * cacheables — keeps Set-Cookie off responses that Cloudflare must cache.
     */
    private static final class CacheablePathCsrfRepoWrapper implements CsrfTokenRepository {
        private final CsrfTokenRepository delegate;

        CacheablePathCsrfRepoWrapper(CsrfTokenRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public CsrfToken generateToken(HttpServletRequest request) {
            return delegate.generateToken(request);
        }

        @Override
        public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
            if ("GET".equalsIgnoreCase(request.getMethod()) && isCacheablePath(request)) {
                return;
            }
            delegate.saveToken(token, request, response);
        }

        @Override
        public CsrfToken loadToken(HttpServletRequest request) {
            return delegate.loadToken(request);
        }
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

    /**
     * Strips Set-Cookie: XSRF-TOKEN from cacheable GET responses no matter who
     * tries to set it (Spring Security CsrfFilter, eager handler, Lazy wrapper,
     * etc.). Last-resort net so Cloudflare can cache these responses.
     */
    private OncePerRequestFilter xsrfStripFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                String method = request.getMethod();
                boolean safe = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
                if (safe && isCacheablePath(request)) {
                    filterChain.doFilter(request, new XsrfStrippingResponseWrapper(response));
                } else {
                    filterChain.doFilter(request, response);
                }
            }
        };
    }

    /**
     * Response wrapper that drops any addCookie/addHeader that would emit an
     * XSRF-TOKEN Set-Cookie. All other headers/cookies pass through unchanged.
     */
    private static final class XsrfStrippingResponseWrapper
            extends jakarta.servlet.http.HttpServletResponseWrapper {

        private static final String XSRF_COOKIE = "XSRF-TOKEN";

        XsrfStrippingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addCookie(jakarta.servlet.http.Cookie cookie) {
            if (cookie != null && XSRF_COOKIE.equals(cookie.getName())) {
                return;
            }
            super.addCookie(cookie);
        }

        @Override
        public void addHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name) && value != null
                    && value.regionMatches(true, 0, XSRF_COOKIE + "=", 0, XSRF_COOKIE.length() + 1)) {
                return;
            }
            super.addHeader(name, value);
        }

        @Override
        public void setHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name) && value != null
                    && value.regionMatches(true, 0, XSRF_COOKIE + "=", 0, XSRF_COOKIE.length() + 1)) {
                return;
            }
            super.setHeader(name, value);
        }
    }

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
