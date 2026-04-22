package com.lmp.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lmp.catalog.domain.Service;
import com.lmp.catalog.domain.ServiceCategory;

import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<Service, UUID> {
    List<Service> findByCategory(ServiceCategory category);

    @Query("SELECT DISTINCT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "WHERE s.active = true " +
           "ORDER BY s.displayOrder")
    List<Service> findByActiveTrue();

    @Query("SELECT DISTINCT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "WHERE s.featured = true AND s.active = true " +
           "ORDER BY s.displayOrder")
    List<Service> findByFeaturedTrueAndActiveTrue();

    Optional<Service> findByTitle(String title);

    @Query("SELECT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "WHERE s.slug = :slug")
    Optional<Service> findBySlug(@Param("slug") String slug);

    List<Service> findByCategoryIdAndActiveTrue(UUID categoryId);

    Optional<Service> findByExternalItemCode(String externalItemCode);

    @Query("SELECT DISTINCT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "ORDER BY s.displayOrder")
    List<Service> findAllWithDetails();
}
