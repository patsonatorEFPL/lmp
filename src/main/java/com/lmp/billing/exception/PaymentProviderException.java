package com.lmp.billing.exception;

/**
 * Exception levée lors d'erreurs spécifiques aux fournisseurs de paiement
 * Cette exception encapsule les erreurs provenant des APIs des fournisseurs
 * et fournit des informations détaillées pour le debugging et la gestion d'erreurs
 */
public class PaymentProviderException extends PaymentProcessingException {
    
    private final String httpStatus;
    private final String requestId;
    private final String providerMessage;
    private final boolean retryable;
    
    public PaymentProviderException(String message, String paymentProvider) {
        super(message, null, paymentProvider);
        this.httpStatus = null;
        this.requestId = null;
        this.providerMessage = null;
        this.retryable = false;
    }
    
    public PaymentProviderException(String message, String errorCode, String providerErrorCode, 
                                  String paymentProvider, String transactionId) {
        super(message, errorCode, providerErrorCode, paymentProvider, transactionId);
        this.httpStatus = null;
        this.requestId = null;
        this.providerMessage = null;
        this.retryable = false;
    }
    
    public PaymentProviderException(String message, String errorCode, String providerErrorCode, 
                                  String paymentProvider, String transactionId, String httpStatus, 
                                  String requestId, String providerMessage, boolean retryable) {
        super(message, errorCode, providerErrorCode, paymentProvider, transactionId);
        this.httpStatus = httpStatus;
        this.requestId = requestId;
        this.providerMessage = providerMessage;
        this.retryable = retryable;
    }
    
    public PaymentProviderException(String message, String errorCode, String providerErrorCode, 
                                  String paymentProvider, String transactionId, String httpStatus, 
                                  String requestId, String providerMessage, boolean retryable, Throwable cause) {
        super(message, errorCode, providerErrorCode, paymentProvider, transactionId, cause);
        this.httpStatus = httpStatus;
        this.requestId = requestId;
        this.providerMessage = providerMessage;
        this.retryable = retryable;
    }
    
    public String getHttpStatus() {
        return httpStatus;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public String getProviderMessage() {
        return providerMessage;
    }
    
    public boolean isRetryable() {
        return retryable;
    }
    
    /**
     * Détermine si l'erreur est liée à un problème de réseau
     */
    public boolean isNetworkError() {
        return httpStatus != null && (
            httpStatus.startsWith("5") || // Erreurs serveur 5xx
            httpStatus.equals("408") ||   // Request Timeout
            httpStatus.equals("429")      // Too Many Requests
        );
    }
    
    /**
     * Détermine si l'erreur est liée à un problème d'authentification
     */
    public boolean isAuthenticationError() {
        return httpStatus != null && (
            httpStatus.equals("401") ||   // Unauthorized
            httpStatus.equals("403")      // Forbidden
        );
    }
    
    /**
     * Détermine si l'erreur est liée à des données invalides
     */
    public boolean isClientError() {
        return httpStatus != null && httpStatus.startsWith("4") && 
               !isAuthenticationError() && !httpStatus.equals("429");
    }
    
    @Override
    public String toString() {
        return "PaymentProviderException{" +
                "message='" + getMessage() + '\'' +
                ", errorCode='" + getErrorCode() + '\'' +
                ", providerErrorCode='" + getProviderErrorCode() + '\'' +
                ", paymentProvider='" + getPaymentProvider() + '\'' +
                ", transactionId='" + getTransactionId() + '\'' +
                ", httpStatus='" + httpStatus + '\'' +
                ", requestId='" + requestId + '\'' +
                ", providerMessage='" + providerMessage + '\'' +
                ", retryable=" + retryable +
                '}';
    }
}