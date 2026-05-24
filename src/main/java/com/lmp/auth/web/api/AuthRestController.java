package com.lmp.auth.web.api;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.AuthService;
import com.lmp.auth.service.UserService;
import com.lmp.integration.event.BusinessEventPayloadKeys;
import com.lmp.integration.event.LmpBusinessEvent;
import com.lmp.integration.event.LmpBusinessEvent.EventType;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.dto.ForgotPasswordRequest;
import com.lmp.auth.dto.LoginDto;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.ResetPasswordDto;
import com.lmp.auth.web.session.ProgrammaticHttpSessionLogin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * API REST d'authentification.
 * 
 * Sessions HTTP (cookies HttpOnly) — pas de JWT.
 * Compatible SPA Angular avec withCredentials: true.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Connexion, inscription et gestion de session")
public class AuthRestController {

    private static final Logger logger = LoggerFactory.getLogger(AuthRestController.class);

    private final AuthService authService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final SessionRegistry sessionRegistry;
    private final ProgrammaticHttpSessionLogin programmaticHttpSessionLogin;

    public AuthRestController(AuthService authService, UserService userService,
                              AuthenticationManager authenticationManager,
                              ApplicationEventPublisher eventPublisher,
                              SessionRegistry sessionRegistry,
                              ProgrammaticHttpSessionLogin programmaticHttpSessionLogin) {
        this.authService = authService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.eventPublisher = eventPublisher;
        this.sessionRegistry = sessionRegistry;
        this.programmaticHttpSessionLogin = programmaticHttpSessionLogin;
    }

    /**
     * Login — wrap en Callable comme register : bcrypt verify (~150 ms ARM) tourne
     * sur mvcTaskExecutor, thread Tomcat libéré pour servir des reads concurrents.
     * <p>
     * HttpServletRequest/Response sont safe à utiliser dans Callable car Spring MVC
     * tient le request lifecycle ouvert pendant async (asyncStarted + dispatcherType
     * gère ASYNC_DISPATCH au retour). La manipulation de session via
     * programmaticHttpSessionLogin se fait DANS le Callable, avant que MVC
     * re-dispatch la requête → cookies + Set-Cookie correctement écrits dans la
     * réponse finale.
     */
    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie l'utilisateur et crée une session HTTP")
    public Callable<ResponseEntity<ApiResponse<Map<String, Object>>>> login(
            @Valid @RequestBody LoginDto loginDto,
            HttpServletRequest request,
            HttpServletResponse response) {
        return () -> {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

                programmaticHttpSessionLogin.login(request, response, authentication);

                // Iter41 — Bug #1 fix : "Se souvenir de moi pendant 30 jours".
                // Avant : SecurityConfig.rememberMe() configuré (tokenValiditySeconds=86400=24h)
                // MAIS Spring RememberMeAuthFilter lit request.getParameter("rememberMe") en
                // form-encoded, ce qui ne marche PAS pour JSON LoginDto.rememberMe.
                // Résultat : cookie REMEMBER-ME jamais set, label UI trompeur.
                //
                // Fix : étendre directement la session Spring Session Redis à 30 jours via
                // setMaxInactiveInterval. Redis TTL aligne sur 30j (vs 30min staging default
                // ou 24h prod default). Cookie SESSION sans Max-Age (browser session) mais
                // backend valide 30j → user reste loggé jusqu'à fermeture browser, et après
                // ré-ouverture le cookie session est gone mais backend session encore valide
                // = nouvelle session liée à l'ancienne via Spring Session pas vraiment, mais
                // au moins l'inactivité 30j est respectée pour navigation continue.
                if (loginDto.isRememberMe()) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.setMaxInactiveInterval(30 * 24 * 60 * 60); // 30 jours
                    }
                }

                Optional<User> userOpt = userService.findByLogin(authentication.getName());
                if (userOpt.isEmpty()) {
                    logger.error("Utilisateur introuvable après authentification réussie: {}", authentication.getName());
                    programmaticHttpSessionLogin.revokeHttpSessionLogin(request, sessionRegistry);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(ApiResponse.error("Login failed"));
                }
                User user = userOpt.get();

                userService.updateLastLoginDate(user);

                // Restore any saved request (e.g. /oauth2/authorize flow)
                HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                // Default landing page : admin → /admin, autres → /dashboard.
                // Si une saved request existe (deep link, oauth2 flow), on respecte sa cible.
                String defaultLanding = user.getRoles().stream()
                        .anyMatch(r -> "ADMIN".equals(r.getName())) ? "/admin" : "/dashboard";
                String redirectUrl = savedRequest != null ? savedRequest.getRedirectUrl() : defaultLanding;
                // SECURITY (M7) : ne renvoyer qu'une URL relative same-origin pour bloquer
                // open-redirect. Toute saved request avec hôte externe OU URI non parsable
                // est remplacée par la landing par défaut.
                redirectUrl = sanitizeRedirect(redirectUrl, defaultLanding);

                Map<String, Object> data = new HashMap<>();
                data.put("user", UserResponse.from(user));
                data.put("redirectUrl", redirectUrl);

                logger.info("API login successful for: {} — redirectUrl={}", user.getEmail(), redirectUrl);
                return ResponseEntity.ok(ApiResponse.ok("Login successful", data));

            } catch (BadCredentialsException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid email or password"));
            } catch (SessionAuthenticationException e) {
                logger.warn("API login refusé (politique de session): {}", e.getMessage());
                programmaticHttpSessionLogin.revokeHttpSessionLogin(request, sessionRegistry);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                "Login blocked due to session policy. Close other sessions or try again."));
            } catch (AuthenticationException e) {
                logger.warn("API login refusé: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid email or password"));
            } catch (Exception e) {
                logger.error("API login erreur inattendue", e);
                programmaticHttpSessionLogin.revokeHttpSessionLogin(request, sessionRegistry);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Login failed"));
            }
        };
    }

    /**
     * Inscription — retourne {@link Callable} pour libérer le thread Tomcat pendant
     * que bcrypt (~150-200 ms CPU sur ARM) tourne. Spring MVC dispatch sur
     * {@code applicationTaskExecutor} puis re-dispatch la requête une fois la
     * Callable résolue. Net effet sous load : les reads ne queue plus derrière
     * les bcrypt en cours, le pool Tomcat reste disponible.
     */
    @PostMapping("/register")
    @Operation(summary = "Inscription", description = "Crée un nouveau compte utilisateur")
    public Callable<ResponseEntity<ApiResponse<Void>>> register(@Valid @RequestBody RegisterDto registerDto) {
        return () -> {
            // SECURITY (H5) : réponse générique constante pour éviter account enumeration.
            // Format/password issues = 400 (UX legit, attacker contrôle l'input). Email
            // déjà pris = 200 silencieux + reminder async — l'attaquant ne peut pas
            // distinguer un nouveau compte d'un email existant.
            ApiResponse<Void> generic = ApiResponse.ok(
                    "Si cette adresse est valide et nouvelle, un email de confirmation a été envoyé.",
                    null);

            if (!registerDto.isPasswordMatching()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Passwords do not match"));
            }
            if (authService.isDisposableEmail(registerDto.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Disposable email addresses are not allowed"));
            }

            if (authService.existsByEmail(registerDto.getEmail())) {
                // Log only (PII-safe : log.info already captures email at request edge). User
                // owning the email can use "Mot de passe oublié" to recover ; attacker gets
                // identical response shape & status as a fresh-email request.
                logger.info("API registration skipped — email already registered: {}", registerDto.getEmail());
                return ResponseEntity.ok(generic);
            }

            try {
                authService.validateRegistrationData(registerDto);
                User user = authService.registerUser(registerDto);

                authService.sendVerificationEmail(user);
                authService.sendWelcomeEmail(user);

                Map<String, Object> regPl = new HashMap<>();
                regPl.put(BusinessEventPayloadKeys.EMAIL, user.getEmail());
                regPl.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                regPl.put(BusinessEventPayloadKeys.MESSAGE,
                        "Nouvel utilisateur : " + user.getEmail());
                eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.USER_REGISTERED, "auth", user.getId(), regPl));

                logger.info("API registration successful for: {}", user.getEmail());
                return ResponseEntity.ok(generic);

            } catch (IllegalArgumentException e) {
                logger.warn("Registration validation: {}", e.getMessage());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Registration could not be completed"));
            } catch (Exception e) {
                logger.error("Registration failed", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Registration could not be completed"));
            }
        };
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur courant", description = "Retourne les données de l'utilisateur authentifié")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication,
                                                                    HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        return userService.findByLogin(authentication.getName())
                .map(user -> ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user))))
                .orElseGet(() -> {
                    // User authenticated but row missing (hard-deleted) — purge session + 401
                    // so the SPA logs out cleanly on the next /me poll.
                    SecurityContextHolder.clearContext();
                    HttpSession s = request.getSession(false);
                    if (s != null) s.invalidate();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.error("Session invalid — user no longer exists"));
                });
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Invalide la session HTTP")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        programmaticHttpSessionLogin.revokeHttpSessionLogin(request, sessionRegistry);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Vérifier email", description = "Vérifie l'email avec le token envoyé")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        boolean verified = authService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired verification token"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Renvoyer vérification", description = "Renvoie l'email de vérification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        try {
            authService.resendVerificationEmail(email);
            return ResponseEntity.ok(ApiResponse.ok("Verification email resent", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Mot de passe oublié", description = "Envoie un lien de réinitialisation par e-mail si le compte existe")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.processForgotPasswordAsync(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
                "Si un compte existe pour cette adresse, un e-mail de réinitialisation a été envoyé.",
                null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Définit un nouveau mot de passe à partir du jeton reçu par e-mail")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        if (!dto.isPasswordMatching()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Les mots de passe ne correspondent pas"));
        }
        try {
            authService.completePasswordReset(dto);
            return ResponseEntity.ok(ApiResponse.ok("Mot de passe mis à jour. Vous pouvez vous connecter.", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Reset password failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Normalize a post-login redirect to a same-origin relative path.
     *
     * <p>Accept only :</p>
     * <ul>
     *   <li>relative paths that start with {@code /} and not {@code //} or {@code /\} (block
     *       scheme-relative attacks like {@code //evil.com/x});</li>
     *   <li>absolute URLs whose authority is null after parsing (path-only)
     *       — extract path + query.</li>
     * </ul>
     * Any other shape (external host, malformed URI, empty) falls back to {@code defaultLanding}.
     */
    static String sanitizeRedirect(String candidate, String defaultLanding) {
        if (candidate == null || candidate.isBlank()) return defaultLanding;
        String trimmed = candidate.trim();
        // Scheme-relative / backslash tricks → always external.
        if (trimmed.startsWith("//") || trimmed.startsWith("/\\") || trimmed.startsWith("\\")) {
            return defaultLanding;
        }
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        try {
            java.net.URI uri = new java.net.URI(trimmed);
            // Reject any absolute URL with a host — even if it points to our own domain,
            // returning a relative path avoids origin-mismatch leaks.
            if (uri.getHost() != null) {
                return defaultLanding;
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/")) {
                return defaultLanding;
            }
            return uri.getRawQuery() != null ? path + "?" + uri.getRawQuery() : path;
        } catch (java.net.URISyntaxException e) {
            return defaultLanding;
        }
    }
}
