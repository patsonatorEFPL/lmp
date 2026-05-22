package com.lmp.shared.config;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.lmp.auth.domain.Role;
import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;
import com.lmp.auth.repository.RoleRepository;
import com.lmp.auth.repository.UserRepository;

/**
 * Garantit la présence de l'utilisateur admin@lmp.ca / Admin@LMP-ChangeMe2026!
 * dans les profils dev et staging — protège contre les suppressions manuelles
 * accidentelles (bench cleanup, reset DB partiel) qui rendraient le login admin
 * impossible. Idempotent : skip si l'utilisateur existe déjà.
 *
 * NE TOURNE PAS en profil prod / production — l'admin prod est géré via
 * DataInitializer + ADMIN_PASSWORD env var sur le compte "Administrator".
 */
@Component
@Profile({"dev", "staging"})
@Order(2)
public class DevStagingAdminSeed implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevStagingAdminSeed.class);
    private static final String DEV_ADMIN_EMAIL = "admin@lmp.ca";
    private static final String DEV_ADMIN_PASSWORD = "Admin@LMP-ChangeMe2026!";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DevStagingAdminSeed(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(DEV_ADMIN_EMAIL).isPresent()) {
            logger.info("Dev/staging admin {} présent en base, skip seed.", DEV_ADMIN_EMAIL);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing"));
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role missing"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);

        User admin = new User();
        admin.setEmail(DEV_ADMIN_EMAIL);
        admin.setPassword(passwordEncoder.encode(DEV_ADMIN_PASSWORD));
        admin.setFirstName("Admin");
        admin.setLastName("LMP");
        admin.setRegistrationDate(LocalDateTime.now());
        admin.setStatus(UserStatus.ACTIVE);
        admin.setAccountLocked(false);
        admin.setEmailVerified(true);
        admin.setRoles(roles);

        userRepository.save(admin);
        logger.info("Dev/staging admin {} re-seedé (était absent en base).", DEV_ADMIN_EMAIL);
    }
}
