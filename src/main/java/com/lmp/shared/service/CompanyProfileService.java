package com.lmp.shared.service;

import java.time.LocalDateTime;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.shared.domain.CompanyProfile;
import com.lmp.shared.dto.CompanyAddressDto;
import com.lmp.shared.repository.CompanyProfileRepository;

/**
 * Adresse / ville-province entreprise : priorité à la base, sinon {@code company.*} dans l'Environment.
 */
@Service
public class CompanyProfileService {

    private static final String DEFAULT_ADDRESS = "123 Rue Principale, Ville, Province, Code Postal";
    private static final String DEFAULT_CITY = "Ville, Province";

    private final CompanyProfileRepository repository;
    private final Environment environment;

    public CompanyProfileService(CompanyProfileRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    @Transactional(readOnly = true)
    public String resolveAddressLine() {
        return repository.findById(CompanyProfile.SINGLETON_ID)
                .map(CompanyProfile::getAddressLine)
                .filter(s -> !s.isBlank())
                .orElse(environment.getProperty("company.address", DEFAULT_ADDRESS));
    }

    @Transactional(readOnly = true)
    public String resolveCityRegion() {
        return repository.findById(CompanyProfile.SINGLETON_ID)
                .map(CompanyProfile::getCityRegion)
                .filter(s -> !s.isBlank())
                .orElse(environment.getProperty("company.city", DEFAULT_CITY));
    }

    @Transactional(readOnly = true)
    public CompanyAddressDto getCurrent() {
        return new CompanyAddressDto(resolveAddressLine(), resolveCityRegion());
    }

    @Transactional
    public CompanyAddressDto update(CompanyAddressDto dto) {
        CompanyProfile row = repository.findById(CompanyProfile.SINGLETON_ID)
                .orElseGet(() -> {
                    CompanyProfile c = new CompanyProfile();
                    c.setId(CompanyProfile.SINGLETON_ID);
                    return c;
                });
        row.setAddressLine(dto.addressLine().trim());
        row.setCityRegion(dto.cityRegion().trim());
        row.setUpdatedAt(LocalDateTime.now());
        repository.save(row);
        return new CompanyAddressDto(row.getAddressLine(), row.getCityRegion());
    }
}
