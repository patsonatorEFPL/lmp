package com.lmp.auth.dto;

import com.lmp.auth.domain.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO de réponse pour les données utilisateur (sans mot de passe).
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String phone,
        String city,
        String country,
        String companyName,
        String status,
        boolean emailVerified,
        boolean accountLocked,
        Set<String> roles,
        LocalDateTime registrationDate,
        LocalDateTime lastLoginDate,
        boolean vatReverseCharge,
        String vatNumber
) {
    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet())
                : Set.of();

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDisplayName(),
                user.getPhone(),
                user.getCity(),
                user.getCountry(),
                user.getCompanyName(),
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                Boolean.TRUE.equals(user.getEmailVerified()),
                Boolean.TRUE.equals(user.getAccountLocked()),
                roleNames,
                user.getRegistrationDate(),
                user.getLastLoginDate(),
                Boolean.TRUE.equals(user.getVatReverseCharge()),
                user.getVatNumber());
    }
}
