package com.lmp.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.User;
import com.lmp.domain.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    User findByVerificationToken(String verificationToken);
    Optional<User> findByResetToken(String resetToken);
    
    // Méthodes pour pagination et filtrage admin
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    long countByStatus(UserStatus status);
    long countByAccountLocked(Boolean locked);
    
    // Méthode pour les statistiques admin
    long countByRegistrationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
