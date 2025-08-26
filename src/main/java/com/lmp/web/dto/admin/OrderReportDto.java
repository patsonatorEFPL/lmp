package com.lmp.web.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO pour les rapports et statistiques des commandes dans l'administration.
 * Contient des données agrégées et des métriques pour l'analyse business.
 */
public class OrderReportDto {

    // Informations de période
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
    
    private String periodDescription; // "7 derniers jours", "Ce mois", "Cette année"
    
    // Métriques globales
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Long totalCustomers;
    private Long newCustomers;
    private Long returningCustomers;
    
    // Métriques par statut
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;
    private Long refundedOrders;
    
    // Revenus par statut
    private BigDecimal pendingRevenue;
    private BigDecimal confirmedRevenue;
    private BigDecimal processingRevenue;
    private BigDecimal shippedRevenue;
    private BigDecimal deliveredRevenue;
    private BigDecimal refundedAmount;
    
    // Métriques de performance
    private Double conversionRate; // % de commandes livrées
    private Double cancelationRate; // % de commandes annulées
    private Double refundRate; // % de commandes remboursées
    private Double averageProcessingTime; // Temps moyen en heures
    
    // Top services
    private List<ServiceStatsDto> topServices;
    
    // Données temporelles pour graphiques
    private List<DailyStatsDto> dailyStats;
    private List<MonthlyStatsDto> monthlyStats;
    
    // Données de comparaison
    private OrderReportDto previousPeriod;
    private Double revenueGrowth; // % de croissance du chiffre d'affaires
    private Double orderGrowth; // % de croissance des commandes
    
    // Constructeurs
    public OrderReportDto() {}
    
    public OrderReportDto(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters et Setters
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getPeriodDescription() {
        return periodDescription;
    }
    
    public void setPeriodDescription(String periodDescription) {
        this.periodDescription = periodDescription;
    }
    
    public Long getTotalOrders() {
        return totalOrders;
    }
    
    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }
    
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }
    
    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }
    
    public Long getTotalCustomers() {
        return totalCustomers;
    }
    
    public void setTotalCustomers(Long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }
    
    public Long getNewCustomers() {
        return newCustomers;
    }
    
    public void setNewCustomers(Long newCustomers) {
        this.newCustomers = newCustomers;
    }
    
    public Long getReturningCustomers() {
        return returningCustomers;
    }
    
    public void setReturningCustomers(Long returningCustomers) {
        this.returningCustomers = returningCustomers;
    }
    
    public Long getPendingOrders() {
        return pendingOrders;
    }
    
    public void setPendingOrders(Long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }
    
    public Long getConfirmedOrders() {
        return confirmedOrders;
    }
    
    public void setConfirmedOrders(Long confirmedOrders) {
        this.confirmedOrders = confirmedOrders;
    }
    
    public Long getProcessingOrders() {
        return processingOrders;
    }
    
    public void setProcessingOrders(Long processingOrders) {
        this.processingOrders = processingOrders;
    }
    
    public Long getShippedOrders() {
        return shippedOrders;
    }
    
    public void setShippedOrders(Long shippedOrders) {
        this.shippedOrders = shippedOrders;
    }
    
    public Long getDeliveredOrders() {
        return deliveredOrders;
    }
    
    public void setDeliveredOrders(Long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }
    
    public Long getCancelledOrders() {
        return cancelledOrders;
    }
    
    public void setCancelledOrders(Long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }
    
    public Long getRefundedOrders() {
        return refundedOrders;
    }
    
    public void setRefundedOrders(Long refundedOrders) {
        this.refundedOrders = refundedOrders;
    }
    
    public BigDecimal getPendingRevenue() {
        return pendingRevenue;
    }
    
    public void setPendingRevenue(BigDecimal pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }
    
    public BigDecimal getConfirmedRevenue() {
        return confirmedRevenue;
    }
    
    public void setConfirmedRevenue(BigDecimal confirmedRevenue) {
        this.confirmedRevenue = confirmedRevenue;
    }
    
    public BigDecimal getProcessingRevenue() {
        return processingRevenue;
    }
    
    public void setProcessingRevenue(BigDecimal processingRevenue) {
        this.processingRevenue = processingRevenue;
    }
    
    public BigDecimal getShippedRevenue() {
        return shippedRevenue;
    }
    
    public void setShippedRevenue(BigDecimal shippedRevenue) {
        this.shippedRevenue = shippedRevenue;
    }
    
    public BigDecimal getDeliveredRevenue() {
        return deliveredRevenue;
    }
    
    public void setDeliveredRevenue(BigDecimal deliveredRevenue) {
        this.deliveredRevenue = deliveredRevenue;
    }
    
    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }
    
    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }
    
    public Double getConversionRate() {
        return conversionRate;
    }
    
    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }
    
    public Double getCancelationRate() {
        return cancelationRate;
    }
    
    public void setCancelationRate(Double cancelationRate) {
        this.cancelationRate = cancelationRate;
    }
    
    public Double getRefundRate() {
        return refundRate;
    }
    
    public void setRefundRate(Double refundRate) {
        this.refundRate = refundRate;
    }
    
    public Double getAverageProcessingTime() {
        return averageProcessingTime;
    }
    
    public void setAverageProcessingTime(Double averageProcessingTime) {
        this.averageProcessingTime = averageProcessingTime;
    }
    
    public List<ServiceStatsDto> getTopServices() {
        return topServices;
    }
    
    public void setTopServices(List<ServiceStatsDto> topServices) {
        this.topServices = topServices;
    }
    
    public List<DailyStatsDto> getDailyStats() {
        return dailyStats;
    }
    
    public void setDailyStats(List<DailyStatsDto> dailyStats) {
        this.dailyStats = dailyStats;
    }
    
    public List<MonthlyStatsDto> getMonthlyStats() {
        return monthlyStats;
    }
    
    public void setMonthlyStats(List<MonthlyStatsDto> monthlyStats) {
        this.monthlyStats = monthlyStats;
    }
    
    public OrderReportDto getPreviousPeriod() {
        return previousPeriod;
    }
    
    public void setPreviousPeriod(OrderReportDto previousPeriod) {
        this.previousPeriod = previousPeriod;
    }
    
    public Double getRevenueGrowth() {
        return revenueGrowth;
    }
    
    public void setRevenueGrowth(Double revenueGrowth) {
        this.revenueGrowth = revenueGrowth;
    }
    
    public Double getOrderGrowth() {
        return orderGrowth;
    }
    
    public void setOrderGrowth(Double orderGrowth) {
        this.orderGrowth = orderGrowth;
    }
    
    // Classes internes pour les statistiques
    
    public static class ServiceStatsDto {
        private String serviceName;
        private Long orderCount;
        private BigDecimal revenue;
        private Double percentage;
        
        public ServiceStatsDto() {}
        
        public ServiceStatsDto(String serviceName, Long orderCount, BigDecimal revenue) {
            this.serviceName = serviceName;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
        
        // Getters et Setters
        
        public String getServiceName() {
            return serviceName;
        }
        
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public Long getOrderCount() {
            return orderCount;
        }
        
        public void setOrderCount(Long orderCount) {
            this.orderCount = orderCount;
        }
        
        public BigDecimal getRevenue() {
            return revenue;
        }
        
        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
        
        public Double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
    
    public static class DailyStatsDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime date;
        private Long orderCount;
        private BigDecimal revenue;
        
        public DailyStatsDto() {}
        
        public DailyStatsDto(LocalDateTime date, Long orderCount, BigDecimal revenue) {
            this.date = date;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
        
        // Getters et Setters
        
        public LocalDateTime getDate() {
            return date;
        }
        
        public void setDate(LocalDateTime date) {
            this.date = date;
        }
        
        public Long getOrderCount() {
            return orderCount;
        }
        
        public void setOrderCount(Long orderCount) {
            this.orderCount = orderCount;
        }
        
        public BigDecimal getRevenue() {
            return revenue;
        }
        
        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }
    
    public static class MonthlyStatsDto {
        @JsonFormat(pattern = "yyyy-MM")
        private LocalDateTime month;
        private Long orderCount;
        private BigDecimal revenue;
        
        public MonthlyStatsDto() {}
        
        public MonthlyStatsDto(LocalDateTime month, Long orderCount, BigDecimal revenue) {
            this.month = month;
            this.orderCount = orderCount;
            this.revenue = revenue;
        }
        
        // Getters et Setters
        
        public LocalDateTime getMonth() {
            return month;
        }
        
        public void setMonth(LocalDateTime month) {
            this.month = month;
        }
        
        public Long getOrderCount() {
            return orderCount;
        }
        
        public void setOrderCount(Long orderCount) {
            this.orderCount = orderCount;
        }
        
        public BigDecimal getRevenue() {
            return revenue;
        }
        
        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }
    }
    
    // Classes internes pour les statistiques avancées
    
    public static class TrendData {
        private String period;
        private Long value;
        private BigDecimal amount;
        private Double percentage;
        private Long orderCount;
        private BigDecimal revenue;
        
        public TrendData() {}
        
        public TrendData(String period, Long value, BigDecimal amount) {
            this.period = period;
            this.value = value;
            this.amount = amount;
        }
        
        // Getters et Setters
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public Long getValue() { return value; }
        public void setValue(Long value) { this.value = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
        
        // Méthodes additionnelles pour ReportsService
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }
    
    public static class ServicePerformance {
        private String serviceName;
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private Double averageRating;
        private Double completionRate;
        private Long orderCount;
        private BigDecimal revenue;
        private BigDecimal averageOrderValue;
        
        public ServicePerformance() {}
        
        public ServicePerformance(String serviceName, Long totalOrders, BigDecimal totalRevenue) {
            this.serviceName = serviceName;
            this.totalOrders = totalOrders;
            this.totalRevenue = totalRevenue;
        }
        
        // Getters et Setters
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        public Double getCompletionRate() { return completionRate; }
        public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
        
        // Méthodes additionnelles pour ReportsService
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public BigDecimal getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(BigDecimal averageOrderValue) { this.averageOrderValue = averageOrderValue; }
    }
    
    public static class GlobalStats {
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private Long totalCustomers;
        private Double averageOrderValue;
        private BigDecimal conversionRate;
        
        public GlobalStats() {}
        
        // Getters et Setters
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public Long getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(Long totalCustomers) { this.totalCustomers = totalCustomers; }
        public Double getAverageOrderValue() { return averageOrderValue; }
        public void setAverageOrderValue(Double averageOrderValue) { this.averageOrderValue = averageOrderValue; }
        public void setAverageOrderValue(BigDecimal averageOrderValue) {
            this.averageOrderValue = averageOrderValue != null ? averageOrderValue.doubleValue() : null;
        }
        
        // Méthodes additionnelles pour ReportsService
        public BigDecimal getConversionRate() { return conversionRate; }
        public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    }
    
    public static class StatusStats {
        private String status;
        private Long count;
        private BigDecimal revenue;
        private Double percentage;
        
        public StatusStats() {}
        
        public StatusStats(String status, Long count, BigDecimal revenue) {
            this.status = status;
            this.count = count;
            this.revenue = revenue;
        }
        
        // Getters et Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }
    
    public static class CustomerStats {
        private Long newCustomers;
        private Long returningCustomers;
        private Double retentionRate;
        private BigDecimal averageLifetimeValue;
        
        public CustomerStats() {}
        
        // Getters et Setters
        public Long getNewCustomers() { return newCustomers; }
        public void setNewCustomers(Long newCustomers) { this.newCustomers = newCustomers; }
        public Long getReturningCustomers() { return returningCustomers; }
        public void setReturningCustomers(Long returningCustomers) { this.returningCustomers = returningCustomers; }
        public Double getRetentionRate() { return retentionRate; }
        public void setRetentionRate(Double retentionRate) { this.retentionRate = retentionRate; }
        public void setRetentionRate(BigDecimal retentionRate) {
            this.retentionRate = retentionRate != null ? retentionRate.doubleValue() : null;
        }
        public BigDecimal getAverageLifetimeValue() { return averageLifetimeValue; }
        public void setAverageLifetimeValue(BigDecimal averageLifetimeValue) { this.averageLifetimeValue = averageLifetimeValue; }
    }
    
    public static class PerformanceStats {
        private Double averageProcessingTime;
        private Double fulfillmentRate;
        private Double customerSatisfaction;
        private Double onTimeDeliveryRate;
        private BigDecimal averagePaymentTime;
        private BigDecimal averageShippingTime;
        private BigDecimal averageDeliveryTime;
        
        public PerformanceStats() {}
        
        // Getters et Setters
        public Double getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(Double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
        public Double getFulfillmentRate() { return fulfillmentRate; }
        public void setFulfillmentRate(Double fulfillmentRate) { this.fulfillmentRate = fulfillmentRate; }
        public Double getCustomerSatisfaction() { return customerSatisfaction; }
        public void setCustomerSatisfaction(Double customerSatisfaction) { this.customerSatisfaction = customerSatisfaction; }
        public Double getOnTimeDeliveryRate() { return onTimeDeliveryRate; }
        public void setOnTimeDeliveryRate(Double onTimeDeliveryRate) { this.onTimeDeliveryRate = onTimeDeliveryRate; }
        
        // Méthodes additionnelles pour ReportsService
        public BigDecimal getAveragePaymentTime() { return averagePaymentTime; }
        public void setAveragePaymentTime(BigDecimal averagePaymentTime) { this.averagePaymentTime = averagePaymentTime; }
        public BigDecimal getAverageShippingTime() { return averageShippingTime; }
        public void setAverageShippingTime(BigDecimal averageShippingTime) { this.averageShippingTime = averageShippingTime; }
        public BigDecimal getAverageDeliveryTime() { return averageDeliveryTime; }
        public void setAverageDeliveryTime(BigDecimal averageDeliveryTime) { this.averageDeliveryTime = averageDeliveryTime; }
    }
    
    public static class RefundStats {
        private Long totalRefunds;
        private BigDecimal totalRefundAmount;
        private Double refundRate;
        private BigDecimal averageRefundAmount;
        
        public RefundStats() {}
        
        // Getters et Setters
        public Long getTotalRefunds() { return totalRefunds; }
        public void setTotalRefunds(Long totalRefunds) { this.totalRefunds = totalRefunds; }
        public BigDecimal getTotalRefundAmount() { return totalRefundAmount; }
        public void setTotalRefundAmount(BigDecimal totalRefundAmount) { this.totalRefundAmount = totalRefundAmount; }
        public Double getRefundRate() { return refundRate; }
        public void setRefundRate(Double refundRate) { this.refundRate = refundRate; }
        public void setRefundRate(BigDecimal refundRate) {
            this.refundRate = refundRate != null ? refundRate.doubleValue() : null;
        }
        public BigDecimal getAverageRefundAmount() { return averageRefundAmount; }
        public void setAverageRefundAmount(BigDecimal averageRefundAmount) { this.averageRefundAmount = averageRefundAmount; }
    }
    
    // Champs pour les classes internes
    private GlobalStats globalStats;
    private List<StatusStats> statusStats;
    private List<TrendData> trends;
    private List<ServicePerformance> servicePerformances;
    private CustomerStats customerStats;
    private PerformanceStats performanceStats;
    private RefundStats refundStats;
    private LocalDateTime generatedAt;
    
    // Getters et Setters pour les nouveaux champs
    public GlobalStats getGlobalStats() { return globalStats; }
    public void setGlobalStats(GlobalStats globalStats) { this.globalStats = globalStats; }
    public List<StatusStats> getStatusStats() { return statusStats; }
    public void setStatusStats(List<StatusStats> statusStats) { this.statusStats = statusStats; }
    public List<TrendData> getTrends() { return trends; }
    public void setTrends(List<TrendData> trends) { this.trends = trends; }
    public List<ServicePerformance> getServicePerformances() { return servicePerformances; }
    public void setServicePerformances(List<ServicePerformance> servicePerformances) { this.servicePerformances = servicePerformances; }
    public List<ServicePerformance> getServicePerformance() { return servicePerformances; }
    public void setServicePerformance(List<ServicePerformance> servicePerformance) { this.servicePerformances = servicePerformance; }
    public CustomerStats getCustomerStats() { return customerStats; }
    public void setCustomerStats(CustomerStats customerStats) { this.customerStats = customerStats; }
    public PerformanceStats getPerformanceStats() { return performanceStats; }
    public void setPerformanceStats(PerformanceStats performanceStats) { this.performanceStats = performanceStats; }
    public RefundStats getRefundStats() { return refundStats; }
    public void setRefundStats(RefundStats refundStats) { this.refundStats = refundStats; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    // Méthodes utilitaires
    
    /**
     * Calcule et met à jour les taux de performance
     */
    public void calculateRates() {
        if (totalOrders != null && totalOrders > 0) {
            // Taux de conversion (commandes livrées / total)
            if (deliveredOrders != null) {
                this.conversionRate = (deliveredOrders.doubleValue() / totalOrders.doubleValue()) * 100;
            }
            
            // Taux d'annulation
            if (cancelledOrders != null) {
                this.cancelationRate = (cancelledOrders.doubleValue() / totalOrders.doubleValue()) * 100;
            }
            
            // Taux de remboursement
            if (refundedOrders != null) {
                this.refundRate = (refundedOrders.doubleValue() / totalOrders.doubleValue()) * 100;
            }
        }
        
        // Valeur moyenne des commandes
        if (totalOrders != null && totalOrders > 0 && totalRevenue != null) {
            this.averageOrderValue = totalRevenue.divide(new BigDecimal(totalOrders), 2, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Calcule la croissance par rapport à la période précédente
     */
    public void calculateGrowth() {
        if (previousPeriod != null) {
            // Croissance du chiffre d'affaires
            if (previousPeriod.getTotalRevenue() != null && previousPeriod.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = totalRevenue.subtract(previousPeriod.getTotalRevenue())
                    .divide(previousPeriod.getTotalRevenue(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100));
                this.revenueGrowth = growth.doubleValue();
            }
            
            // Croissance des commandes
            if (previousPeriod.getTotalOrders() != null && previousPeriod.getTotalOrders() > 0) {
                double growth = ((totalOrders.doubleValue() - previousPeriod.getTotalOrders().doubleValue()) 
                    / previousPeriod.getTotalOrders().doubleValue()) * 100;
                this.orderGrowth = growth;
            }
        }
    }
    
    /**
     * Vérifie si les données sont valides
     */
    public boolean isValid() {
        return startDate != null && endDate != null && 
               startDate.isBefore(endDate) && 
               totalOrders != null;
    }
    
    @Override
    public String toString() {
        return "OrderReportDto{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalOrders=" + totalOrders +
                ", totalRevenue=" + totalRevenue +
                ", averageOrderValue=" + averageOrderValue +
                ", conversionRate=" + conversionRate +
                '}';
    }
}