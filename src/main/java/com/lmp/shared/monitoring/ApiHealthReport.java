package com.lmp.shared.monitoring;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Rapport quotidien agrégé de santé des APIs externes.
 * Conservé 7 jours en base.
 */
@Entity
@Table(name = "api_health_reports")
public class ApiHealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_data", nullable = false, columnDefinition = "jsonb")
    private String reportData;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected ApiHealthReport() {}

    public ApiHealthReport(LocalDate reportDate, String reportData) {
        this.reportDate = reportDate;
        this.reportData = reportData;
        this.generatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public LocalDate getReportDate() { return reportDate; }
    public String getReportData() { return reportData; }
    public Instant getGeneratedAt() { return generatedAt; }
}
