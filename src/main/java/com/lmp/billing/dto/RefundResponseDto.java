package com.lmp.billing.dto;

import com.lmp.billing.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour les réponses de remboursement
 * Contient le résultat du traitement d'un remboursement
 */
public class RefundResponseDto {
    
    private String refundId;
    private String providerRefundId;
    private String originalTransactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private LocalDateTime processedAt;
    private LocalDateTime expectedAt;
    private String message;
    private String errorCode;
    private String errorMessage;
    private String paymentProvider;
    private Map<String, Object> providerResponse;
    private String receiptUrl;
    private boolean isPartialRefund;
    private BigDecimal remainingAmount;
    
    // Constructeurs
    public RefundResponseDto() {}
    
    public RefundResponseDto(String refundId, PaymentStatus status, BigDecimal amount, String currency) {
        this.refundId = refundId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = LocalDateTime.now();
    }
    
    // Factory methods pour créer des réponses communes
    public static RefundResponseDto success(String refundId, String providerRefundId, 
                                          BigDecimal amount, String currency, String paymentProvider) {
        RefundResponseDto response = new RefundResponseDto();
        response.setRefundId(refundId);
        response.setProviderRefundId(providerRefundId);
        response.setStatus(PaymentStatus.REFUNDED);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setMessage("Remboursement traité avec succès");
        return response;
    }
    
    public static RefundResponseDto failure(String errorCode, String errorMessage, String paymentProvider) {
        RefundResponseDto response = new RefundResponseDto();
        response.setStatus(PaymentStatus.FAILED);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setMessage("Échec du traitement du remboursement");
        return response;
    }
    
    public static RefundResponseDto pending(String refundId, String paymentProvider, LocalDateTime expectedAt) {
        RefundResponseDto response = new RefundResponseDto();
        response.setRefundId(refundId);
        response.setStatus(PaymentStatus.PENDING);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setExpectedAt(expectedAt);
        response.setMessage("Remboursement en cours de traitement");
        return response;
    }
    
    // Getters et Setters
    public String getRefundId() {
        return refundId;
    }
    
    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }
    
    public String getProviderRefundId() {
        return providerRefundId;
    }
    
    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }
    
    public String getOriginalTransactionId() {
        return originalTransactionId;
    }
    
    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public LocalDateTime getExpectedAt() {
        return expectedAt;
    }
    
    public void setExpectedAt(LocalDateTime expectedAt) {
        this.expectedAt = expectedAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
    
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
    
    public Map<String, Object> getProviderResponse() {
        return providerResponse;
    }
    
    public void setProviderResponse(Map<String, Object> providerResponse) {
        this.providerResponse = providerResponse;
    }
    
    public String getReceiptUrl() {
        return receiptUrl;
    }
    
    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }
    
    public boolean isPartialRefund() {
        return isPartialRefund;
    }
    
    public void setPartialRefund(boolean partialRefund) {
        isPartialRefund = partialRefund;
    }
    
    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }
    
    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }
    
    public boolean isSuccessful() {
        return status == PaymentStatus.REFUNDED;
    }
    
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    @Override
    public String toString() {
        return "RefundResponseDto{" +
                "refundId='" + refundId + '\'' +
                ", providerRefundId='" + providerRefundId + '\'' +
                ", originalTransactionId='" + originalTransactionId + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", processedAt=" + processedAt +
                ", message='" + message + '\'' +
                '}';
    }
}