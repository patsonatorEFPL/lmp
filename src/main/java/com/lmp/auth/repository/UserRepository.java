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
    List<User> findByStatus(UserStatus status);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    long countByStatus(UserStatus status);
    long countByAccountLocked(Boolean locked);
    long countByRegistrationDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :login OR u.email = :login")
    Optional<User> findByLogin(@Param("login") String login);
    
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

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countDistinctUsersWithRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    Optional<User> findByExternalCustomerId(String externalCustomerId);

    Optional<User> findByExternalErpUserId(String externalErpUserId);

    List<User> findByExternalCustomerIdIsNullAndRegistrationDateBefore(LocalDateTime cutoff);
}
