package com.lmp.portal.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise à jour du profil utilisateur.
 */
public record UpdateProfileRequest(
        @Size(max = 50) String firstName,
        @Size(max = 50) String lastName,
        @Size(max = 20) String phone,
        @Size(max = 100) String companyName,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        @Size(max = 200) String address,
        @Size(max = 10) String postalCode,
        /** Si true, le client déclare l'autoliquidation TVA ; {@code vatNumber} doit être renseigné. */
        Boolean vatReverseCharge,
        @Size(max = 64) String vatNumber
) {}
