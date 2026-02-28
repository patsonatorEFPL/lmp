package com.lmp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lmp.domain.entity.ServiceCategory;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    java.util.Optional<ServiceCategory> findBySlug(String slug);

    java.util.List<ServiceCategory> findAllByOrderByDisplayOrderAsc();
}
