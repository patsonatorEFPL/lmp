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

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie l'utilisateur et crée une session HTTP")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginDto loginDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

            programmaticHttpSessionLogin.login(request, response, authentication);

            Optional<User> userOpt = userService.findByEmailWithRoles(authentication.getName());
            if (userOpt.isEmpty()) {
                logger.error("Utilisateur introuvable après authentification réussie: {}", authentication.getName());
                programmaticHttpSessionLogin.revokeHttpSessionLogin(request, sessionRegistry);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Login failed"));
            }
            User user = userOpt.get();

            userService.updateLastLoginDate(user.getEmail());

            // Restore any saved request (e.g. /oauth2/authorize flow)
            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            String redirectUrl = savedRequest != null ? savedRequest.getRedirectUrl() : "/dashboard";
            // Convert absolute URLs to relative so the browser stays on the same origin
            // (important when served through a reverse proxy / tunnel)
            try {
                java.net.URI uri = new java.net.URI(redirectUrl);
                redirectUrl = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
            } catch (java.net.URISyntaxException e) {
                // keep original relative URL
            }

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
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription", description = "Crée un nouveau compte utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterDto registerDto) {

        if (authService.existsByEmail(registerDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email already registered"));
        }

        if (!registerDto.isPasswordMatching()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Passwords do not match"));
        }

        if (authService.isDisposableEmail(registerDto.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Disposable email addresses are not allowed"));
        }

        try {
            authService.validateRegistrationData(registerDto);
            User user = authService.registerUser(registerDto);

            // Send emails asynchronously via Spring proxy (@Async) — non-blocking
            authService.sendVerificationEmail(user);
            authService.sendWelcomeEmail(user);

            Map<String, Object> regPl = new HashMap<>();
            regPl.put(BusinessEventPayloadKeys.EMAIL, user.getEmail());
            regPl.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
            regPl.put(BusinessEventPayloadKeys.MESSAGE,
                    "Nouvel utilisateur : " + user.getEmail());
            eventPublisher.publishEvent(LmpBusinessEvent.of(EventType.USER_REGISTERED, "auth", user.getId(), regPl));

            logger.info("API registration successful for: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Registration successful — check your email for verification", UserResponse.from(user)));

        } catch (IllegalArgumentException e) {
            logger.warn("Registration validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration could not be completed"));
        } catch (Exception e) {
            logger.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration could not be completed"));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur courant", description = "Retourne les données de l'utilisateur authentifié")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Not authenticated"));
        }

        return userService.findByEmailWithRoles(authentication.getName())
                .map(user -> ResponseEntity.ok(ApiResponse.ok(UserResponse.from(user))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found")));
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
        authService.initiatePasswordReset(request.getEmail())
                .ifPresent(authService::sendPasswordResetEmail);
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
}
