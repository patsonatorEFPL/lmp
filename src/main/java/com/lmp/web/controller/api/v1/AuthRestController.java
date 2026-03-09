package com.lmp.web.controller.api.v1;

import com.lmp.domain.entity.User;
import com.lmp.service.auth.AuthService;
import com.lmp.service.user.UserService;
import com.lmp.web.controller.api.v1.dto.ApiResponse;
import com.lmp.web.controller.api.v1.dto.UserResponse;
import com.lmp.web.dto.LoginDto;
import com.lmp.web.dto.RegisterDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

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

    public AuthRestController(AuthService authService, UserService userService,
                              AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie l'utilisateur et crée une session HTTP")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginDto loginDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Sauvegarder le contexte dans la session HTTP
            request.getSession(true)
                    .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            // Charger l'utilisateur avec ses rôles
            User user = userService.findByEmailWithRoles(loginDto.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after auth"));

            userService.updateLastLoginDate(user.getEmail());

            logger.info("API login successful for: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("Login successful", UserResponse.from(user)));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password"));
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
            authService.sendVerificationEmail(user);

            logger.info("API registration successful for: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Registration successful — check your email for verification", UserResponse.from(user)));

        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
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
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
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
}
