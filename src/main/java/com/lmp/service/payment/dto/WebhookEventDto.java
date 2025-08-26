package com.lmp.service.payment.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour les événements de webhook des fournisseurs de paiement
 * Utilisé pour traiter les notifications asynchrones des fournisseurs
 */
public class WebhookEventDto {
    
    private String eventId;
    private String eventType;
    private String paymentProvider;
    private LocalDateTime eventTime;
    private String transactionId;
    private String providerTransactionId;
    private String status;
    private Map<String, Object> eventData;
    private String signature;
    private String rawPayload;
    private boolean processed = false;
    private LocalDateTime processedAt;
    private String processingError;
    
    // Constructeurs
    public WebhookEventDto() {}
    
    public WebhookEventDto(String eventId, String eventType, String paymentProvider) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.paymentProvider = paymentProvider;
        this.eventTime = LocalDateTime.now();
    }
    
    // Getters et Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
    
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
    
    public LocalDateTime getEventTime() {
        return eventTime;
    }
    
    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getProviderTransactionId() {
        return providerTransactionId;
    }
    
    public void setProviderTransactionId(String providerTransactionId) {
        this.providerTransactionId = providerTransactionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Map<String, Object> getEventData() {
        return eventData;
    }
    
    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
    }
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public String getRawPayload() {
        return rawPayload;
    }
    
    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void setProcessed(boolean processed) {
        this.processed = processed;
        if (processed && processedAt == null) {
            this.processedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public String getProcessingError() {
        return processingError;
    }
    
    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }
    
    public boolean hasError() {
        return processingError != null && !processingError.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "WebhookEventDto{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", eventTime=" + eventTime +
                ", transactionId='" + transactionId + '\'' +
                ", providerTransactionId='" + providerTransactionId + '\'' +
                ", status='" + status + '\'' +
                ", processed=" + processed +
                ", processedAt=" + processedAt +
                '}';
    }
}