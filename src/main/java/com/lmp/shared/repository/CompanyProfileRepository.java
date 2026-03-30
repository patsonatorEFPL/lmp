package com.lmp.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lmp.shared.domain.CompanyProfile;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Short> {
}
