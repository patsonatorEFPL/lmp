package com.lmp.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.auth.domain.User;
import com.lmp.auth.domain.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    User findByVerificationToken(String verificationToken);
    Optional<User> findByResetToken(String resetToken);
    
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    long countByStatus(UserStatus status);
    long countByAccountLocked(Boolean locked);
    long countByRegistrationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);
    
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles " +
           "LEFT JOIN FETCH u.orders " +
           "LEFT JOIN FETCH u.reviews " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithAllCollections(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.emailVerified = false " +
           "AND u.status = com.lmp.auth.domain.UserStatus.ACTIVE " +
           "AND u.registrationDate < :deadline")
    List<User> findUnverifiedExpiredUsers(@Param("deadline") LocalDateTime deadline);
}
