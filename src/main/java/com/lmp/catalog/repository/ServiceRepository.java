package com.lmp.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;

import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByCategory(ServiceCategory category);

    List<Service> findByActiveTrue();

    List<Service> findByFeaturedTrueAndActiveTrue();

    Optional<Service> findByTitle(String title);

    Optional<Service> findBySlug(String slug);

    List<Service> findByCategoryIdAndActiveTrue(UUID categoryId);
}
