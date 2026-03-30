package com.lmp.shared.web.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.shared.dto.ApiResponse;
import com.lmp.shared.dto.CompanyAddressDto;
import com.lmp.shared.service.CompanyProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/company-profile")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Profil entreprise (adresse facturation)")
public class AdminCompanyProfileRestController {

    private final CompanyProfileService companyProfileService;

    public AdminCompanyProfileRestController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @GetMapping
    @Operation(summary = "Lire l'adresse et la ville/province utilisées sur les factures")
    public ResponseEntity<ApiResponse<CompanyAddressDto>> get() {
        return ResponseEntity.ok(ApiResponse.ok(companyProfileService.getCurrent()));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour l'adresse et la ville/province")
    public ResponseEntity<ApiResponse<CompanyAddressDto>> update(@Valid @RequestBody CompanyAddressDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(companyProfileService.update(dto)));
    }
}
