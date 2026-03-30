package com.lmp.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Adresse postale et ville/province (factures, mentions légales).
 */
public record CompanyAddressDto(
        @NotBlank(message = "L'adresse est requise")
        @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
        String addressLine,

        @NotBlank(message = "La ville et la province sont requises")
        @Size(max = 200, message = "Ce champ ne peut pas dépasser 200 caractères")
        String cityRegion
) {
}
