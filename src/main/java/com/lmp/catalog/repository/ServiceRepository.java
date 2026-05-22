package com.lmp.catalog.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<Service> findByActiveTrue();

    @Query("SELECT s FROM Service s LEFT JOIN FETCH s.offers WHERE s.id = :id")
    Optional<Service> findByIdWithOffers(@Param("id") UUID id);

    @Query("SELECT DISTINCT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "WHERE s.featured = true AND s.active = true " +
           "ORDER BY s.displayOrder")
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<Service> findByFeaturedTrueAndActiveTrue();

    Optional<Service> findByTitle(String title);

    long countByActiveTrue();

    long countByFeaturedTrue();

    @Query("SELECT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "WHERE s.slug = :slug")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Optional<Service> findBySlug(@Param("slug") String slug);

    List<Service> findByCategoryIdAndActiveTrue(UUID categoryId);

    Optional<Service> findByExternalItemCode(String externalItemCode);

    @Query("SELECT DISTINCT s FROM Service s " +
           "LEFT JOIN FETCH s.category " +
           "LEFT JOIN FETCH s.benefits " +
           "LEFT JOIN FETCH s.offers " +
           "ORDER BY s.displayOrder")
    List<Service> findAllWithDetails();

    /**
     * Full-text search on active services via the V42 GIN-indexed
     * {@code search_vector}. {@code websearch_to_tsquery} accepts user-typed
     * queries safely.
     */
    @Query(value = """
            SELECT * FROM services
             WHERE active = true
               AND search_vector @@ websearch_to_tsquery('french', :query)
             ORDER BY ts_rank_cd(search_vector, websearch_to_tsquery('french', :query)) DESC,
                      display_order ASC
             LIMIT :max
            """, nativeQuery = true)
    @QueryHints({
            @QueryHint(name = "org.hibernate.readOnly", value = "true"),
            @QueryHint(name = "org.hibernate.fetchSize", value = "50")
    })
    List<Service> searchActive(@Param("query") String query, @Param("max") int max);
}
