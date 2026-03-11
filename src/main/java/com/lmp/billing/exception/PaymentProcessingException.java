package com.lmp.billing.exception;

/**
 * Exception levée lors d'erreurs de traitement de paiement
 * Cette exception est utilisée pour tous les problèmes liés au traitement des paiements
 * avec les fournisseurs externes (Stripe, PayPal, etc.)
 */
public class PaymentProcessingException extends Exception {
    
    private final String errorCode;
    private final String providerErrorCode;
    private final String paymentProvider;
    private final String transactionId;
    
    public PaymentProcessingException(String message) {
        super(message);
        this.errorCode = null;
        this.providerErrorCode = null;
        this.paymentProvider = null;
        this.transactionId = null;
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.providerErrorCode = null;
        this.paymentProvider = null;
        this.transactionId = null;
    }
    
    public PaymentProcessingException(String message, String errorCode, String paymentProvider) {
        super(message);
        this.errorCode = errorCode;
        this.providerErrorCode = null;
        this.paymentProvider = paymentProvider;
        this.transactionId = null;
    }
    
    public PaymentProcessingException(String message, String errorCode, String providerErrorCode, 
                                    String paymentProvider, String transactionId) {
        super(message);
        this.errorCode = errorCode;
        this.providerErrorCode = providerErrorCode;
        this.paymentProvider = paymentProvider;
        this.transactionId = transactionId;
    }
    
    public PaymentProcessingException(String message, String errorCode, String providerErrorCode, 
                                    String paymentProvider, String transactionId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.providerErrorCode = providerErrorCode;
        this.paymentProvider = paymentProvider;
        this.transactionId = transactionId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getProviderErrorCode() {
        return providerErrorCode;
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    @Override
    public String toString() {
        return "PaymentProcessingException{" +
                "message='" + getMessage() + '\'' +
                ", errorCode='" + errorCode + '\'' +
                ", providerErrorCode='" + providerErrorCode + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}