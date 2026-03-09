package com.lmp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.Service;
import com.lmp.domain.entity.ServiceCategory;

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
