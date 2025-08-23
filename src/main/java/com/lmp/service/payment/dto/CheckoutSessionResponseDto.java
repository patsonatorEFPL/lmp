package com.lmp.service.payment.dto;

import com.lmp.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour les réponses de session Stripe Checkout
 * Contient les informations retournées après création d'une session
 */
public class CheckoutSessionResponseDto {
    
    private String sessionId;
    private String sessionUrl;
    private String paymentStatus;
    private PaymentStatus status;
    private String paymentIntentId;
    private BigDecimal amount;
    private String currency;
    private String customerEmail;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
    private String errorMessage;
    private boolean successful;
    
    // Constructeurs
    public CheckoutSessionResponseDto() {}
    
    public CheckoutSessionResponseDto(String sessionId, String sessionUrl) {
        this.sessionId = sessionId;
        this.sessionUrl = sessionUrl;
        this.successful = true;
        this.createdAt = LocalDateTime.now();
    }
    
    // Méthodes de construction statiques
    public static CheckoutSessionResponseDto success(String sessionId, String sessionUrl, 
                                                   BigDecimal amount, String currency) {
        CheckoutSessionResponseDto response = new CheckoutSessionResponseDto(sessionId, sessionUrl);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setStatus(PaymentStatus.PENDING);
        response.setPaymentStatus("pending");
        return response;
    }
    
    public static CheckoutSessionResponseDto failure(String errorMessage) {
        CheckoutSessionResponseDto response = new CheckoutSessionResponseDto();
        response.setSuccessful(false);
        response.setErrorMessage(errorMessage);
        response.setStatus(PaymentStatus.FAILED);
        response.setPaymentStatus("failed");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
    
    // Getters et Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getSessionUrl() { return sessionUrl; }
    public void setSessionUrl(String sessionUrl) { this.sessionUrl = sessionUrl; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public boolean isSuccessful() { return successful; }
    public void setSuccessful(boolean successful) { this.successful = successful; }
    
    public boolean isFailed() { return !successful; }
    
    // Méthodes utilitaires
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }
    
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }
}