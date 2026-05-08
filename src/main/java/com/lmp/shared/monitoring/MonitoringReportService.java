package com.lmp.shared.monitoring;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Génère des rapports quotidiens à partir des métriques API persistées
 * et gère la purge des données périmées.
 *
 * <p>Exécution programmée :
 * <ul>
 *   <li>Minuit (00:05 UTC) : agrège les données des dernières 24h → rapport JSON</li>
 *   <li>Purge des records &gt; 24h et des rapports &gt; 7 jours</li>
 * </ul>
 */
@Service
public class MonitoringReportService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringReportService.class);

    private static final long RECORDS_RETENTION_HOURS = 24;
    private static final long REPORTS_RETENTION_DAYS = 7;

    private final ApiHealthRecordRepository recordRepository;
    private final ApiHealthReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public MonitoringReportService(ApiHealthRecordRepository recordRepository,
                                   ApiHealthReportRepository reportRepository,
                                   ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Tâche planifiée : génère le rapport quotidien et purge les anciennes données.
     * Exécutée à 00:05 UTC chaque jour.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @SchedulerLock(name = "MonitoringReportService.generateDailyReportAndPurge",
                   lockAtMostFor = "PT15M", lockAtLeastFor = "PT5M")
    @Transactional
    public void generateDailyReportAndPurge() {
        logger.info("[MONITORING-REPORT] Début de la génération du rapport quotidien");

        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant periodStart = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // 1. Vérifier qu'un rapport n'existe pas déjà pour cette date
        if (reportRepository.findByReportDate(yesterday).isPresent()) {
            logger.info("[MONITORING-REPORT] Rapport déjà existant pour {}, ignoré", yesterday);
        } else {
            // 2. Récupérer les records de la période
            List<ApiHealthRecord> records = recordRepository
                    .findByRecordedAtBetweenOrderByRecordedAtAsc(periodStart, periodEnd);

            // 3. Agréger et sauvegarder le rapport
            String reportJson = aggregateToReport(yesterday, periodStart, periodEnd, records);
            reportRepository.save(new ApiHealthReport(yesterday, reportJson));
            logger.info("[MONITORING-REPORT] Rapport généré pour {} ({} records agrégés)",
                    yesterday, records.size());
        }

        // 4. Purge des anciens records (> 24h)
        Instant recordsCutoff = Instant.now().minus(RECORDS_RETENTION_HOURS, ChronoUnit.HOURS);
        int deletedRecords = recordRepository.deleteByRecordedAtBefore(recordsCutoff);
        if (deletedRecords > 0) {
            logger.info("[MONITORING-REPORT] {} records purgés (> {}h)", deletedRecords, RECORDS_RETENTION_HOURS);
        }

        // 5. Purge des anciens rapports (> 7 jours)
        Instant reportsCutoff = Instant.now().minus(REPORTS_RETENTION_DAYS, ChronoUnit.DAYS);
        int deletedReports = reportRepository.deleteByGeneratedAtBefore(reportsCutoff);
        if (deletedReports > 0) {
            logger.info("[MONITORING-REPORT] {} rapports purgés (> {}j)", deletedReports, REPORTS_RETENTION_DAYS);
        }

        logger.info("[MONITORING-REPORT] Génération terminée");
    }

    /**
     * Agrège une liste de records en un rapport JSON structuré.
     */
    private String aggregateToReport(LocalDate date, Instant from, Instant to,
                                      List<ApiHealthRecord> records) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportDate", date.toString());
        report.put("period", Map.of("from", from.toString(), "to", to.toString()));

        // Grouper par API
        Map<String, List<ApiHealthRecord>> byApi = records.stream()
                .collect(Collectors.groupingBy(ApiHealthRecord::getApiName, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> apiSummaries = new ArrayList<>();

        for (var entry : byApi.entrySet()) {
            String apiName = entry.getKey();
            List<ApiHealthRecord> apiRecords = entry.getValue();

            long totalCalls = apiRecords.size();
            long successCount = apiRecords.stream().filter(ApiHealthRecord::isSuccess).count();
            double successRate = totalCalls > 0 ? Math.round((double) successCount / totalCalls * 1000.0) / 10.0 : 0;
            long avgLatency = totalCalls > 0
                    ? Math.round(apiRecords.stream().mapToLong(ApiHealthRecord::getLatencyMs).average().orElse(0))
                    : 0;
            long maxLatency = apiRecords.stream().mapToLong(ApiHealthRecord::getLatencyMs).max().orElse(0);
            long p95Latency = computeP95(apiRecords);
            long errorCount = totalCalls - successCount;

            // Top erreurs (distinctes, max 5)
            List<String> topErrors = apiRecords.stream()
                    .filter(r -> !r.isSuccess() && r.getError() != null)
                    .map(ApiHealthRecord::getError)
                    .distinct()
                    .limit(5)
                    .toList();

            // Répartition horaire
            List<Map<String, Object>> hourlyBreakdown = computeHourlyBreakdown(apiRecords);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("name", apiName);
            summary.put("totalCalls", totalCalls);
            summary.put("successCount", successCount);
            summary.put("successRate", successRate);
            summary.put("avgLatencyMs", avgLatency);
            summary.put("maxLatencyMs", maxLatency);
            summary.put("p95LatencyMs", p95Latency);
            summary.put("errorCount", errorCount);
            summary.put("topErrors", topErrors);
            summary.put("hourlyBreakdown", hourlyBreakdown);

            apiSummaries.add(summary);
        }

        // Trier par nombre d'appels décroissant
        apiSummaries.sort(Comparator.<Map<String, Object>, Long>comparing(
                m -> (Long) m.get("totalCalls")).reversed());

        report.put("apis", apiSummaries);
        report.put("totalRecords", records.size());

        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            logger.error("[MONITORING-REPORT] Erreur de sérialisation JSON", e);
            return "{}";
        }
    }

    private long computeP95(List<ApiHealthRecord> records) {
        if (records.isEmpty()) return 0;
        List<Long> latencies = records.stream()
                .map(r -> (long) r.getLatencyMs())
                .sorted()
                .toList();
        int index = (int) Math.ceil(latencies.size() * 0.95) - 1;
        return latencies.get(Math.max(0, Math.min(index, latencies.size() - 1)));
    }

    private List<Map<String, Object>> computeHourlyBreakdown(List<ApiHealthRecord> records) {
        // Grouper par heure UTC
        Map<Integer, List<ApiHealthRecord>> byHour = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRecordedAt().atZone(ZoneOffset.UTC).getHour()));

        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            List<ApiHealthRecord> hourRecords = byHour.getOrDefault(hour, List.of());
            if (hourRecords.isEmpty()) continue;

            long calls = hourRecords.size();
            long successes = hourRecords.stream().filter(ApiHealthRecord::isSuccess).count();
            double rate = Math.round((double) successes / calls * 1000.0) / 10.0;
            long avgMs = Math.round(hourRecords.stream().mapToLong(ApiHealthRecord::getLatencyMs).average().orElse(0));

            Map<String, Object> h = new HashMap<>();
            h.put("hour", hour);
            h.put("calls", calls);
            h.put("successRate", rate);
            h.put("avgLatencyMs", avgMs);
            breakdown.add(h);
        }
        return breakdown;
    }
}
