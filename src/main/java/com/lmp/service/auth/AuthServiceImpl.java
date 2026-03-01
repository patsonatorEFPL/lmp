package com.lmp.service.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import com.lmp.config.MailAddressConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.RoleRepository;
import com.lmp.repository.UserRepository;
import com.lmp.web.dto.RegisterDto;

/**
 * Implémentation du service d'authentification.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailAddressConfig mailAddressConfig;

    @Autowired
    private DisposableEmailBlocklist disposableEmailBlocklist;

    @Autowired
    private SessionRegistry sessionRegistry;

    @Value("${company.name:LMP Services}")
    private String companyName;

    @Value("${app.base.url:https://lmp-services.ca}")
    private String baseUrl;

    @Value("${company.email:lmp.assistance@gmail.com}")
    private String companyEmail;

    @Value("${company.website:https://lmp-services.ca}")
    private String companyWebsite;

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
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Vérifier si l'email est jetable
        if (isDisposableEmail(registerDto.getEmail())) {
            throw new RuntimeException("Les adresses email temporaires/jetables ne sont pas acceptées. Veuillez utiliser une adresse email permanente.");
        }

        // Créer le nouvel utilisateur
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setFirstName(registerDto.getFirstName());
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

        // Assigner le rôle USER par défaut
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Rôle USER non trouvé"));
        user.setRoles(Set.of(userRole));

        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(user);
        logger.info("Utilisateur inscrit : {}", savedUser.getEmail());

        // Envoyer l'email de vérification
        try {
            sendVerificationEmail(savedUser);
            logger.info("Email de vérification envoyé à {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Erreur envoi email de vérification : {}", e.getMessage(), e);
        }

        // Envoyer l'email de bienvenue
        try {
            sendWelcomeEmail(savedUser);
        } catch (Exception e) {
            logger.error("Erreur envoi email de bienvenue : {}", e.getMessage(), e);
        }

        return savedUser;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public void validateRegistrationData(RegisterDto registerDto) {
        if (registerDto == null) {
            throw new RuntimeException("Les données d'inscription ne peuvent pas être nulles");
        }

        if (!registerDto.isPasswordMatching()) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        if (!registerDto.isAcceptTerms()) {
            throw new RuntimeException("Vous devez accepter les conditions d'utilisation");
        }

        if (registerDto.getEmail() == null || !registerDto.getEmail().contains("@")) {
            throw new RuntimeException("Format d'email invalide");
        }

        if (registerDto.getPassword() == null || registerDto.getPassword().length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }

    @Override
    public void sendWelcomeEmail(User user) {
        try {
            logger.info("Envoi email de bienvenue pour : {}", user.getEmail());

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("companyName", companyName);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("companyEmail", companyEmail);
            context.setVariable("companyWebsite", companyWebsite);

            String htmlContent = templateEngine.process("emails/welcome-minimal-clean", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setReplyTo(mailAddressConfig.getNoreply());
            helper.setTo(user.getEmail());
            helper.setSubject("\uD83C\uDF89 Bienvenue chez " + companyName + " !");
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            logger.info("Email de bienvenue envoyé avec succès à : {}", user.getEmail());

        } catch (MessagingException e) {
            logger.error("Erreur MessagingException envoi bienvenue pour '{}': {}", user.getEmail(), e.getMessage(), e);
            logger.warn("L'inscription a réussi mais l'email de bienvenue n'a pas pu être envoyé");
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

    @Override
    public void sendVerificationEmail(User user) {
        try {
            String verificationUrl = baseUrl + "/verify-email?token=" + user.getVerificationToken();

            Context context = new Context();
            context.setVariable("userName", user.getDisplayName());
            context.setVariable("companyName", companyName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("companyWebsite", companyWebsite);

            String htmlContent = templateEngine.process("emails/email-verification", context);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailAddressConfig.getNoreply(), mailAddressConfig.getName());
            helper.setReplyTo(mailAddressConfig.getNoreply());
            helper.setTo(user.getEmail());
            helper.setSubject("✉ Vérifiez votre email - " + companyName);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
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
}
