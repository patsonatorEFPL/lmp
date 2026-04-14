package com.lmp.shared.monitoring;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository pour les rapports quotidiens de santé API.
 */
public interface ApiHealthReportRepository extends JpaRepository<ApiHealthReport, Long> {

    Optional<ApiHealthReport> findByReportDate(LocalDate reportDate);

    List<ApiHealthReport> findAllByOrderByReportDateDesc();

    @Modifying
    @Query("DELETE FROM ApiHealthReport r WHERE r.generatedAt < :cutoff")
    int deleteByGeneratedAtBefore(@Param("cutoff") Instant cutoff);
}
