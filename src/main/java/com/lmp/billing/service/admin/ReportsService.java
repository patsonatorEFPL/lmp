package com.lmp.billing.service.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.RefundRepository;
import com.lmp.auth.repository.UserRepository;
import com.lmp.billing.dto.admin.OrderReportDto;

/**
 * Service pour la génération de rapports et statistiques des commandes.
 * Fournit des analyses détaillées sur les ventes, tendances et performances.
 */
@Service
@Transactional(readOnly = true)
public class ReportsService {

    private static final Logger logger = LoggerFactory.getLogger(ReportsService.class);

        private final OrderRepository orderRepository;

        private final RefundRepository refundRepository;

        private final UserRepository userRepository;


    public ReportsService(OrderRepository orderRepository,
                           RefundRepository refundRepository,
                           UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
    }

    /**
     * Génère un rapport complet pour la période spécifiée
     */
    public OrderReportDto generateOrderReport(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Génération rapport commandes: {} à {}", startDate, endDate);

        OrderReportDto report = new OrderReportDto();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedAt(LocalDateTime.now());

        // Statistiques globales
        generateGlobalStatistics(report, startDate, endDate);

        // Statistiques par statut
        generateStatusStatistics(report, startDate, endDate);

        // Statistiques temporelles
        generateTimeBasedStatistics(report, startDate, endDate);

        // Statistiques par service
        generateServiceStatistics(report, startDate, endDate);

        // Statistiques clients
        generateCustomerStatistics(report, startDate, endDate);

        // Statistiques de performance
        generatePerformanceStatistics(report, startDate, endDate);

        // Statistiques de remboursement
        generateRefundStatistics(report, startDate, endDate);

        logger.info("Rapport généré avec succès pour la période {} - {}", startDate, endDate);
        return report;
    }

    /**
     * Génère un rapport de synthèse quotidien
     */
    public OrderReportDto generateDailySummary(LocalDateTime date) {
        LocalDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        return generateOrderReport(startOfDay, endOfDay);
    }

    /**
     * Génère un rapport mensuel
     */
    public OrderReportDto generateMonthlySummary(int year, int month) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);
        
        return generateOrderReport(startOfMonth, endOfMonth);
    }

    /**
     * Export des données de commande au format CSV
     */
    public List<String[]> exportOrdersToCSV(LocalDateTime startDate, LocalDateTime endDate) {
        List<String[]> csvData = new ArrayList<>();
        
        // Headers
        csvData.add(new String[]{
            "ID", "Date", "Client", "Service", "Montant", "Devise", "Statut", 
            "Statut Paiement", "Date Paiement", "Date Expédition", "Date Livraison"
        });

        // Données
        orderRepository.findOrdersForExport(startDate, endDate, null).forEach(order -> {
            csvData.add(new String[]{
                order.getId().toString(),
                order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                order.getUser() != null ? order.getUser().getEmail() : "",
                order.getServiceName(),
                order.getTotalAmount().toString(),
                order.getCurrency(),
                order.getStatus().toString(),
                order.getPaymentStatus(),
                order.getPaidAt() != null ? order.getPaidAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
                order.getShippedAt() != null ? order.getShippedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "",
                order.getDeliveredAt() != null ? order.getDeliveredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : ""
            });
        });

        return csvData;
    }

    /**
     * Analyse des tendances par période
     */
    public Map<String, Object> analyzeTrends(LocalDateTime startDate, LocalDateTime endDate, String period) {
        Map<String, Object> trends = new HashMap<>();
        
        List<Object[]> data;
        switch (period.toLowerCase()) {
            case "daily":
                data = orderRepository.getDailyOrderStats(startDate, endDate);
                break;
            case "monthly":
                data = orderRepository.getMonthlyOrderStats(startDate);
                break;
            default:
                throw new IllegalArgumentException("Période non supportée: " + period);
        }

        // Calcul des tendances
        List<OrderReportDto.TrendData> trendList = data.stream()
            .map(row -> {
                OrderReportDto.TrendData trend = new OrderReportDto.TrendData();
                trend.setPeriod(row[0].toString());
                trend.setOrderCount(((Number) row[1]).longValue());
                trend.setRevenue((BigDecimal) row[2]);
                return trend;
            })
            .collect(Collectors.toList());

        trends.put("data", trendList);
        trends.put("totalPeriods", trendList.size());
        
        // Calcul de la croissance
        if (trendList.size() >= 2) {
            OrderReportDto.TrendData current = trendList.get(0);
            OrderReportDto.TrendData previous = trendList.get(1);
            
            BigDecimal revenueGrowth = calculateGrowthRate(previous.getRevenue(), current.getRevenue());
            Long orderGrowth = calculateGrowthRate(previous.getOrderCount(), current.getOrderCount());
            
            trends.put("revenueGrowth", revenueGrowth);
            trends.put("orderGrowth", orderGrowth);
        }

        return trends;
    }

    /**
     * Analyse de la performance par service
     */
    public List<OrderReportDto.ServicePerformance> analyzeServicePerformance(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> topByRevenue = orderRepository.getTopServicesByRevenue(startDate, PageRequest.of(0, 10));
        List<Object[]> topByCount = orderRepository.getTopServicesByOrderCount(startDate, PageRequest.of(0, 10));
        
        Map<String, OrderReportDto.ServicePerformance> serviceMap = new HashMap<>();
        
        // Traitement des données de chiffre d'affaires
        topByRevenue.forEach(row -> {
            String serviceName = (String) row[0];
            Long orderCount = ((Number) row[1]).longValue();
            BigDecimal revenue = (BigDecimal) row[2];
            
            OrderReportDto.ServicePerformance perf = new OrderReportDto.ServicePerformance();
            perf.setServiceName(serviceName);
            perf.setOrderCount(orderCount);
            perf.setRevenue(revenue);
            perf.setAverageOrderValue(revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP));
            
            serviceMap.put(serviceName, perf);
        });

        return new ArrayList<>(serviceMap.values());
    }

    // ========== Méthodes privées de génération des statistiques ==========

    private void generateGlobalStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] stats = orderRepository.getOrderStatsSince(startDate);
        
        OrderReportDto.GlobalStats globalStats = new OrderReportDto.GlobalStats();
        globalStats.setTotalOrders(((Number) stats[0]).longValue());
        globalStats.setTotalRevenue((BigDecimal) stats[1]);
        globalStats.setAverageOrderValue((BigDecimal) stats[2]);
        globalStats.setTotalCustomers(((Number) stats[3]).longValue());
        
        // Calcul du taux de conversion (approximatif)
        Long totalVisitors = userRepository.countByRegistrationDateBetween(startDate, endDate);
        if (totalVisitors > 0) {
            BigDecimal conversionRate = BigDecimal.valueOf(globalStats.getTotalOrders())
                .divide(BigDecimal.valueOf(totalVisitors), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            globalStats.setConversionRate(conversionRate);
        }
        
        report.setGlobalStats(globalStats);
    }

    private void generateStatusStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> statusData = orderRepository.getOrderStatsByStatus();
        
        List<OrderReportDto.StatusStats> statusStats = statusData.stream()
            .map(row -> {
                OrderReportDto.StatusStats stats = new OrderReportDto.StatusStats();
                stats.setStatus(row[0].toString());
                stats.setCount(((Number) row[1]).longValue());
                stats.setRevenue((BigDecimal) row[2]);
                return stats;
            })
            .collect(Collectors.toList());
            
        report.setStatusStats(statusStats);
    }

    private void generateTimeBasedStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> dailyData = orderRepository.getDailyOrderStats(startDate, endDate);
        
        List<OrderReportDto.TrendData> trends = dailyData.stream()
            .map(row -> {
                OrderReportDto.TrendData trend = new OrderReportDto.TrendData();
                trend.setPeriod(row[0].toString());
                trend.setOrderCount(((Number) row[1]).longValue());
                trend.setRevenue((BigDecimal) row[2]);
                return trend;
            })
            .collect(Collectors.toList());
            
        report.setTrends(trends);
    }

    private void generateServiceStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        List<OrderReportDto.ServicePerformance> servicePerf = analyzeServicePerformance(startDate, endDate);
        report.setServicePerformance(servicePerf);
    }

    private void generateCustomerStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] customerStats = orderRepository.getCustomerTypeStats(startDate, endDate);
        
        OrderReportDto.CustomerStats custStats = new OrderReportDto.CustomerStats();
        custStats.setNewCustomers(((Number) customerStats[0]).longValue());
        custStats.setReturningCustomers(((Number) customerStats[1]).longValue());
        
        Long totalCustomers = custStats.getNewCustomers() + custStats.getReturningCustomers();
        if (totalCustomers > 0) {
            custStats.setRetentionRate(
                BigDecimal.valueOf(custStats.getReturningCustomers())
                    .divide(BigDecimal.valueOf(totalCustomers), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
            );
        }
        
        report.setCustomerStats(custStats);
    }

    private void generatePerformanceStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] processingTimes = orderRepository.getAverageProcessingTimes(startDate);
        
        OrderReportDto.PerformanceStats perfStats = new OrderReportDto.PerformanceStats();
        if (processingTimes != null && processingTimes.length >= 3) {
            perfStats.setAveragePaymentTime((BigDecimal) processingTimes[0]);
            perfStats.setAverageShippingTime((BigDecimal) processingTimes[1]);
            perfStats.setAverageDeliveryTime((BigDecimal) processingTimes[2]);
        }
        
        report.setPerformanceStats(perfStats);
    }

    private void generateRefundStatistics(OrderReportDto report, LocalDateTime startDate, LocalDateTime endDate) {
        Object[] refundStats = refundRepository.getRefundStatsSince(startDate);
        
        OrderReportDto.RefundStats refStats = new OrderReportDto.RefundStats();
        refStats.setTotalRefunds(((Number) refundStats[0]).longValue());
        refStats.setTotalRefundAmount((BigDecimal) refundStats[1]);
        refStats.setAverageRefundAmount((BigDecimal) refundStats[2]);
        
        // Calcul du taux de remboursement
        if (report.getGlobalStats() != null && report.getGlobalStats().getTotalOrders() > 0) {
            BigDecimal refundRate = BigDecimal.valueOf(refStats.getTotalRefunds())
                .divide(BigDecimal.valueOf(report.getGlobalStats().getTotalOrders()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            refStats.setRefundRate(refundRate);
        }
        
        report.setRefundStats(refStats);
    }

    private BigDecimal calculateGrowthRate(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    private Long calculateGrowthRate(Long previous, Long current) {
        if (previous == null || previous == 0) {
            return 0L;
        }
        
        return ((current - previous) * 100) / previous;
    }
}