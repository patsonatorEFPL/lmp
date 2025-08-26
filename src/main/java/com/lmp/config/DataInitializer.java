package com.lmp.config;

import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.lmp.domain.entity.Role;
import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;
import com.lmp.repository.RoleRepository;
import com.lmp.repository.UserRepository;

/**
 * Initialisateur de données pour l'application LMP.
 * 
 * Crée les données essentielles au démarrage de l'application
 * si elles n'existent pas déjà.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 === DÉMARRAGE INITIALISATION DONNÉES LMP ===");
        
        initializeRoles();
        initializeDefaultUsers();
        
        // Diagnostic des données initialisées
        logger.info("📊 Nombre total de rôles: {}", roleRepository.count());
        logger.info("👥 Nombre total d'utilisateurs: {}", userRepository.count());
        
        // Vérification du compte administrateur
        userRepository.findByEmail("admin@lmp.ca").ifPresentOrElse(
            admin -> logger.info("✅ Compte administrateur configuré: {}", admin.getEmail()),
            () -> logger.error("❌ Erreur: Compte administrateur non trouvé!")
        );
        
        logger.info("🏁 === Initialisation des données LMP terminée ===");
    }

    /**
     * Initialise les rôles par défaut s'ils n'existent pas.
     */
    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            System.out.println("Création des rôles par défaut...");
            
            Role userRole = new Role("USER");
            Role adminRole = new Role("ADMIN");
            
            roleRepository.save(userRole);
            roleRepository.save(adminRole);
            
            System.out.println("✓ Rôles créés: USER, ADMIN");
        }
    }

    /**
     * Initialise les utilisateurs par défaut s'ils n'existent pas.
     */
    private void initializeDefaultUsers() {
        // Créer l'administrateur par défaut
        if (!userRepository.existsByEmail("admin@lmp.ca")) {
            System.out.println("Création de l'utilisateur administrateur...");
            
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
            
            User admin = new User();
            admin.setEmail("admin@lmp.ca");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Admin");
            admin.setLastName("LMP");
            admin.setRegistrationDate(LocalDateTime.now());
            admin.setStatus(UserStatus.ACTIVE);
            admin.setAccountLocked(false);
            admin.setEmailVerified(true);
            admin.setRoles(Set.of(adminRole));
            
            userRepository.save(admin);
            System.out.println("✓ Administrateur créé: admin@lmp.ca");
        }

        System.out.println("✓ Utilisateur administrateur configuré");
    }


}