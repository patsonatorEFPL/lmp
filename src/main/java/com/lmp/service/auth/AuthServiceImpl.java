package com.lmp.service.auth;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
     * Envoie un email de bienvenue à l'utilisateur.
     * Pour l'instant, simulation avec log.
     * 
     * @param user L'utilisateur nouvellement inscrit
     */
    @Override
    public void sendWelcomeEmail(User user) {
        // TODO: Implémenter l'envoi d'email réel
        System.out.println("Email de bienvenue envoyé à: " + user.getEmail());
        System.out.println("Token de vérification: " + user.getVerificationToken());
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