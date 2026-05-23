package com.lmp.auth.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.lmp.notification.config.MailAddressConfig;
import com.lmp.notification.mail.queue.EmailQueueRequest;
import com.lmp.notification.mail.queue.MailQueueService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.auth.dto.PasswordResetEmailPayload;
import com.lmp.auth.dto.RegisterDto;
import com.lmp.auth.dto.ResetPasswordDto;

/**
 * Implémentation du service d'authentification.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

        private final UserRepository userRepository;

        private final RoleRepository roleRepository;

        private final PasswordEncoder passwordEncoder;

        private final JavaMailSender javaMailSender;

        private final TemplateEngine templateEngine;

        private final MailAddressConfig mailAddressConfig;

        private final DisposableEmailBlocklist disposableEmailBlocklist;

        private final SessionRegistry sessionRegistry;

        private final UserService userService;

        private final SessionSecurityService sessionSecurityService;

        private final MailQueueService mailQueueService;

        /**
         * Proxy lazy pour appeler les méthodes {@code @Async} depuis la même classe (évite l'auto-invocation).
         */
        private final AuthService authServiceAsync;

    @Value("${company.name:LMP Services}")
    private String companyName;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    @Value("${company.email:support@localhost}")
    private String companyEmail;

    @Value("${company.website:http://localhost:8080}")
    private String companyWebsite;

    @Value("${app.frontend.url:${app.base.url:http://localhost:4200}}")
    private String frontendUrl;

    /**
     * URL de base de l'host auth (issuer OIDC). Utilisée pour construire les
     * liens de verify-email et reset-password — ces flux ne sont accessibles
     * QUE sur l'host auth (les autres hosts retournent 404 via OidcHostGuardFilter).
     */
    @Value("${app.oauth2.issuer-uri:${app.base.url:http://localhost:8080}}")
    private String authBaseUrl;


    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JavaMailSender javaMailSender,
                           TemplateEngine templateEngine,
                           MailAddressConfig mailAddressConfig,
                           DisposableEmailBlocklist disposableEmailBlocklist,
                           SessionRegistry sessionRegistry,
                           UserService userService,
                           SessionSecurityService sessionSecurityService,
                           MailQueueService mailQueueService,
                           @Lazy AuthService authServiceAsync) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.mailAddressConfig = mailAddressConfig;
        this.disposableEmailBlocklist = disposableEmailBlocklist;
        this.sessionRegistry = sessionRegistry;
        this.userService = userService;
        this.sessionSecurityService = sessionSecurityService;
        this.mailQueueService = mailQueueService;
        this.authServiceAsync = authServiceAsync;
    }

    /**
     * Inscrit un nouvel utilisateur avec le rôle USER par défaut.
     */
    @Override
    @Transactional
    public User registerUser(RegisterDto registerDto) {
        // Valider les données d'inscription
        validateRegistrationData(registerDto);

        // Vérifier si l'email existe déjà
        if (existsByEmail(registerDto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        // Vérifier si l'email est jetable
        if (isDisposableEmail(registerDto.getEmail())) {
            throw new IllegalArgumentException(
                    "Les adresses email temporaires/jetables ne sont pas acceptées. Veuillez utiliser une adresse email permanente.");
        }

        // Créer le nouvel utilisateur
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setFirstName(deriveFirstName(registerDto.getFirstName(), registerDto.getEmail()));
        user.setLastName(registerDto.getLastName());
        user.setPhone(registerDto.getPhone());
        user.setAddress(registerDto.getAddress());
        user.setCity(registerDto.getCity());
        user.setPostalCode(registerDto.getPostalCode());
        user.setCountry(registerDto.getCountry());
        user.setCompanyName(registerDto.getCompanyName());
        user.setRegistrationDate(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setAccountLocked(false);
        user.setEmailVerified(false);
        user.setVerificationToken(generateVerificationToken());

        // Assigner le rôle USER par défaut (HashSet : Set.of() est immuable et provoque
        // UnsupportedOperationException quand Hibernate modifie la collection plus tard.)
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Rôle USER non trouvé"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);
        logger.info("Utilisateur inscrit : {}", savedUser.getEmail());

        // NOTE: Email sending (verification + welcome) is handled by the caller
        // via @Async proxy methods to avoid blocking the HTTP thread.
        // Self-invocation (this.sendXxx()) bypasses Spring's async proxy.

        return savedUser;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void validateRegistrationData(RegisterDto registerDto) {
        if (registerDto == null) {
            throw new IllegalArgumentException("Les données d'inscription ne peuvent pas être nulles");
        }

        if (!registerDto.isPasswordMatching()) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        if (!registerDto.isAcceptTerms()) {
            throw new IllegalArgumentException("Vous devez accepter les conditions d'utilisation");
        }

        if (registerDto.getEmail() == null || !registerDto.getEmail().contains("@")) {
            throw new IllegalArgumentException("Format d'email invalide");
        }

        if (registerDto.getPassword() == null || registerDto.getPassword().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }

    @Async
    @Override
    public void sendWelcomeEmail(User user) {
        try {
            logger.info("Envoi email de bienvenue pour : {}", user.getEmail());

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("companyName", companyName);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyEmail", companyEmail);
            context.setVariable("companyWebsite", companyWebsite);

            String htmlContent = templateEngine.process("emails/welcome-minimal-clean", context);

            mailQueueService.enqueue(EmailQueueRequest.builder()
                    .sender(mailAddressConfig.getNoreply())
                    .senderName(mailAddressConfig.getName())
                    .replyTo(mailAddressConfig.getNoreply())
                    .recipient(user.getEmail())
                    .subject("\uD83C\uDF89 Bienvenue chez " + companyName + " !")
                    .bodyHtml(htmlContent)
                    .priority(MailQueueService.PRIORITY_NORMAL)
                    .build());
            // (legacy direct send removed \u2014 kept call site for compile until refactor below)
            logger.info("Email de bienvenue envoyé avec succès à : {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur inattendue envoi bienvenue pour '{}': {}", user.getEmail(), e.getMessage(), e);
            logger.warn("L'inscription a réussi mais l'email de bienvenue n'a pas pu être envoyé");
        }
    }

    @Override
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     * Si le compte est INACTIVE, le réactive en ACTIVE.
     * Invalide toutes les sessions existantes via SessionRegistry.
     */
    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        User user = userRepository.findByVerificationToken(token);
        if (user == null) {
            return false;
        }

        // Marquer l'email comme vérifié
        user.setEmailVerified(true);
        user.setVerificationToken(null);

        // Si le compte a été suspendu (INACTIVE), le réactiver
        if (user.getStatus() == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            logger.info("Compte réactivé suite à la vérification email : {}", user.getEmail());
        }

        userRepository.save(user);

        // NOTE: Ne PAS invalider les sessions ici.
        // invalidateUserSessions() expirait la session courante, ce qui faisait que
        // Spring Security redirige vers /login?expired=true au lieu de /login?verified=true,
        // perdant ainsi le flash attribute de succès.

        logger.info("Email vérifié avec succès pour : {}", user.getEmail());
        return true;
    }

    @Async
    @Override
    public void sendVerificationEmail(User user) {
        try {
            // Lien sur l'host auth (canonique) — verify-email n'est accessible que sur auth.*
            String verificationUrl = authBaseUrl + "/verify-email?token=" + user.getVerificationToken();

            Context context = new Context();
            context.setVariable("userName", user.getDisplayName());
            context.setVariable("companyName", companyName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("companyWebsite", companyWebsite);

            String htmlContent = templateEngine.process("emails/email-verification", context);

            mailQueueService.enqueue(EmailQueueRequest.builder()
                    .sender(mailAddressConfig.getNoreply())
                    .senderName(mailAddressConfig.getName())
                    .replyTo(mailAddressConfig.getNoreply())
                    .recipient(user.getEmail())
                    .subject("✉ Vérifiez votre email - " + companyName)
                    .bodyHtml(htmlContent)
                    .priority(MailQueueService.PRIORITY_TRANSACTIONAL)
                    .build());
            logger.info("Email de vérification envoyé à : {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Erreur envoi email de vérification à '{}': {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email de vérification", e);
        }
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Votre email est déjà vérifié");
        }

        // Générer un nouveau token
        user.setVerificationToken(generateVerificationToken());
        userRepository.save(user);

        // Envoyer l'email
        sendVerificationEmail(user);
    }

    @Override
    public boolean isDisposableEmail(String email) {
        return disposableEmailBlocklist.isDisposable(email);
    }

    @Override
    @Async("authBackgroundExecutor")
    public void processForgotPasswordAsync(String email) {
        try {
            initiatePasswordReset(email).ifPresent(this::sendPasswordResetEmail);
        } catch (RuntimeException e) {
            logger.warn("Forgot-password async pipeline failed: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public Optional<PasswordResetEmailPayload> initiatePasswordReset(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalized = email.trim();
        Optional<User> opt = userRepository.findByEmail(normalized);
        if (opt.isEmpty()) {
            logger.info("Password reset requested: no account for email");
            return Optional.empty();
        }
        User user = opt.get();
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            logger.info("Password reset requested: account locked");
            return Optional.empty();
        }

        String token = generateVerificationToken();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
        return Optional.of(new PasswordResetEmailPayload(user.getEmail(), token, displayName));
    }

    @Async
    @Override
    public void sendPasswordResetEmail(PasswordResetEmailPayload payload) {
        if (payload == null) {
            return;
        }
        try {
            // Lien sur l'host auth (canonique) — reset-password n'est accessible que sur auth.*
            String resetUrl = authBaseUrl + "/reset-password?token=" + payload.token();

            Context context = new Context();
            context.setVariable("userName", payload.userDisplayName());
            context.setVariable("companyName", companyName);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("companyWebsite", companyWebsite);
            context.setVariable("supportEmail", mailAddressConfig.getSupport());

            String htmlContent = templateEngine.process("emails/password-reset", context);

            mailQueueService.enqueue(EmailQueueRequest.builder()
                    .sender(mailAddressConfig.getNoreply())
                    .senderName(mailAddressConfig.getName())
                    .replyTo(mailAddressConfig.getNoreply())
                    .recipient(payload.email())
                    .subject("Réinitialisation de votre mot de passe — " + companyName)
                    .bodyHtml(htmlContent)
                    .priority(MailQueueService.PRIORITY_TRANSACTIONAL)
                    .build());
            logger.info("Password reset email sent to: {}", payload.email());

        } catch (Exception e) {
            logger.error("Failed to send password reset email to '{}': {}", payload.email(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void completePasswordReset(ResetPasswordDto dto) {
        if (dto == null || dto.getToken() == null || dto.getToken().isBlank()) {
            throw new IllegalArgumentException("Lien invalide ou expiré");
        }
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        User user = userRepository.findByResetToken(dto.getToken().trim())
                .orElseThrow(() -> new IllegalArgumentException("Lien invalide ou expiré"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lien invalide ou expiré");
        }

        userService.changePassword(user.getId(), dto.getNewPassword());

        User refreshed = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après mise à jour"));
        refreshed.setResetToken(null);
        refreshed.setResetTokenExpiry(null);
        userRepository.save(refreshed);

        sessionSecurityService.invalidateAllUserSessions(refreshed);
        logger.info("Password reset completed for: {}", refreshed.getEmail());

        String displayName = refreshed.getDisplayName() != null ? refreshed.getDisplayName() : refreshed.getEmail();
        authServiceAsync.sendPasswordResetConfirmationEmail(refreshed.getEmail(), displayName);
    }

    @Async
    @Override
    public void sendPasswordResetConfirmationEmail(String email, String userDisplayName) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            Context context = new Context();
            context.setVariable("userName", userDisplayName != null && !userDisplayName.isBlank() ? userDisplayName : email);
            context.setVariable("companyName", companyName);
            context.setVariable("companyWebsite", companyWebsite);
            context.setVariable("supportEmail", mailAddressConfig.getSupport());

            String htmlContent = templateEngine.process("emails/password-reset-confirmation", context);

            mailQueueService.enqueue(EmailQueueRequest.builder()
                    .sender(mailAddressConfig.getNoreply())
                    .senderName(mailAddressConfig.getName())
                    .replyTo(mailAddressConfig.getNoreply())
                    .recipient(email)
                    .subject("Confirmation : votre mot de passe a été modifié — " + companyName)
                    .bodyHtml(htmlContent)
                    .priority(MailQueueService.PRIORITY_TRANSACTIONAL)
                    .build());
            logger.info("Password reset confirmation email sent to: {}", email);

        } catch (Exception e) {
            logger.error("Failed to send password reset confirmation email to '{}': {}", email, e.getMessage(), e);
        }
    }

    /**
     * Invalide toutes les sessions d'un utilisateur via le SessionRegistry.
     */
    private void invalidateUserSessions(String username) {
        try {
            List<Object> principals = sessionRegistry.getAllPrincipals();
            for (Object principal : principals) {
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    if (userDetails.getUsername().equals(username)) {
                        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                        for (SessionInformation session : sessions) {
                            session.expireNow();
                            logger.debug("Session invalidée pour {} : {}", username, session.getSessionId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de l'invalidation des sessions pour {} : {}", username, e.getMessage());
        }
    }

    /**
     * Fallback firstName : if the form omits it (legacy guest checkout, programmatic register),
     * derive a friendly capitalized token from the email local-part so the welcome mail does
     * not greet the user with "Utilisateur".
     */
    private static String deriveFirstName(String provided, String email) {
        if (provided != null && !provided.isBlank()) {
            return provided.trim();
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "";
        }
        String local = email.substring(0, email.indexOf('@'));
        String token = local.split("[._+-]")[0];
        if (token.isEmpty()) return "";
        return Character.toUpperCase(token.charAt(0)) + token.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
