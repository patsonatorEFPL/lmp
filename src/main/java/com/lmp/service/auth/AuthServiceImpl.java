package com.lmp.service.auth;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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

    @Value("${mail.from.address:lmp.assistance@gmail.com}")
    private String fromEmail;

    @Value("${mail.from.name:LMP Services}")
    private String fromName;

    @Value("${company.name:LMP Services}")
    private String companyName;

    /**
     * Inscrit un nouvel utilisateur avec le rôle USER par défaut.
     * 
     * @param registerDto Les données d'inscription
     * @return L'utilisateur créé
     * @throws RuntimeException si l'email existe déjà ou si les données sont invalides
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

        // Créer le nouvel utilisateur
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        // firstName et lastName sont maintenant optionnels (peuvent être null)
        // Si vides, getDisplayName() générera automatiquement un pseudo depuis l'email
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

        // Envoyer l'email de bienvenue (simulation pour l'instant)
        sendWelcomeEmail(savedUser);

        return savedUser;
    }

    /**
     * Vérifie si un email existe déjà dans la base de données.
     * 
     * @param email L'email à vérifier
     * @return true si l'email existe, false sinon
     */
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Valide les données d'inscription.
     * 
     * @param registerDto Les données à valider
     * @throws RuntimeException si les données sont invalides
     */
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

        // Validation supplémentaire du format email
        if (registerDto.getEmail() == null || !registerDto.getEmail().contains("@")) {
            throw new RuntimeException("Format d'email invalide");
        }

        // Validation de la force du mot de passe
        if (registerDto.getPassword() == null || registerDto.getPassword().length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }

    /**
     * Envoie un email de bienvenue à l'utilisateur avec le template HTML.
     * 
     * @param user L'utilisateur nouvellement inscrit
     */
    @Override
    public void sendWelcomeEmail(User user) {
        try {
            logger.info("WELCOME_EMAIL_DEBUG - Début envoi email de bienvenue pour : {}", user.getEmail());
            
            // Création du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("companyName", companyName);
            
            // Rendu du template HTML
            logger.info("WELCOME_EMAIL_DEBUG - Rendu template 'emails/welcome-new-account'...");
            String htmlContent = templateEngine.process("emails/welcome-new-account", context);
            logger.info("WELCOME_EMAIL_DEBUG - Template rendu avec succès, taille: {} caractères", htmlContent.length());
            
            // Création du message email
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Configuration du message
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.getEmail());
            helper.setSubject("\uD83C\uDF89 Bienvenue chez " + companyName + " !");
            helper.setText(htmlContent, true);
            
            // Envoi de l'email
            logger.info("WELCOME_EMAIL_DEBUG - Tentative d'envoi via JavaMailSender...");
            javaMailSender.send(message);
            
            logger.info("Email de bienvenue envoyé avec succès à : {}", user.getEmail());
            
        } catch (MessagingException e) {
            logger.error("WELCOME_EMAIL_ERROR - MessagingException lors de l'envoi pour '{}': {}",
                        user.getEmail(), e.getMessage(), e);
            // Ne pas faire échouer l'inscription pour un problème d'email
            logger.warn("L'inscription a réussi mais l'email de bienvenue n'a pas pu être envoyé");
        } catch (Exception e) {
            logger.error("WELCOME_EMAIL_ERROR - Exception inattendue lors de l'envoi pour '{}': Type={}, Message='{}'",
                        user.getEmail(), e.getClass().getSimpleName(), e.getMessage(), e);
            // Ne pas faire échouer l'inscription pour un problème d'email
            logger.warn("L'inscription a réussi mais l'email de bienvenue n'a pas pu être envoyé");
        }
    }

    /**
     * Génère un token de vérification d'email unique.
     * 
     * @return Le token généré
     */
    @Override
    public String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Vérifie l'email d'un utilisateur avec un token.
     * 
     * @param token Le token de vérification
     * @return true si la vérification réussit, false sinon
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
        user.setVerificationToken(null); // Supprimer le token après utilisation
        userRepository.save(user);

        return true;
    }
}