package com.lmp.billing.dto;

import com.lmp.billing.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour les réponses de paiement
 * Contient le résultat du traitement d'un paiement
 */
public class PaymentResponseDto {

    private String transactionId;
    private String providerTransactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentProvider;
    private LocalDateTime processedAt;
    private String message;
    private String errorCode;
    private String errorMessage;
    private BigDecimal processingFees;
    private Map<String, Object> providerResponse;
    private String receiptUrl;
    private String authorizationCode;
    private boolean requiresAction;
    private String nextActionUrl;

    // Nouveaux champs pour Stripe Checkout
    private String redirectUrl;
    private boolean requiresRedirect;
    private String paymentIntentId;

    // Constructeurs
    public PaymentResponseDto() {
    }

    public PaymentResponseDto(String transactionId, PaymentStatus status, BigDecimal amount, String currency) {
        this.transactionId = transactionId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.processedAt = LocalDateTime.now();
    }

    // Factory methods pour créer des réponses communes
    public static PaymentResponseDto success(String transactionId, String providerTransactionId,
            BigDecimal amount, String currency, String paymentProvider) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setTransactionId(transactionId);
        response.setProviderTransactionId(providerTransactionId);
        response.setStatus(PaymentStatus.COMPLETED);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setMessage("Paiement traité avec succès");
        return response;
    }

    public static PaymentResponseDto failure(String errorCode, String errorMessage, String paymentProvider) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus(PaymentStatus.FAILED);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        response.setMessage("Échec du traitement du paiement");
        return response;
    }

    public static PaymentResponseDto pending(String transactionId, String paymentProvider) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setTransactionId(transactionId);
        response.setStatus(PaymentStatus.PENDING);
        response.setPaymentProvider(paymentProvider);
        response.setProcessedAt(LocalDateTime.now());
        response.setMessage("Paiement en cours de traitement");
        return response;
    }

    // Getters et Setters
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
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

    public BigDecimal getProcessingFees() {
        return processingFees;
    }

    public void setProcessingFees(BigDecimal processingFees) {
        this.processingFees = processingFees;
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

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public String getNextActionUrl() {
        return nextActionUrl;
    }

    public void setNextActionUrl(String nextActionUrl) {
        this.nextActionUrl = nextActionUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public boolean isRequiresRedirect() {
        return requiresRedirect;
    }

    public void setRequiresRedirect(boolean requiresRedirect) {
        this.requiresRedirect = requiresRedirect;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    @Override
    public String toString() {
        return "PaymentResponseDto{" +
                "transactionId='" + transactionId + '\'' +
                ", providerTransactionId='" + providerTransactionId + '\'' +
                ", status=" + status +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", processedAt=" + processedAt +
                ", message='" + message + '\'' +
                '}';
    }
}