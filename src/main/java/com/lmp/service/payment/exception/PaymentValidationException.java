package com.lmp.service.payment.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception levée lors d'erreurs de validation des données de paiement
 * Cette exception est utilisée pour signaler des problèmes de validation
 * des données avant le traitement du paiement
 */
public class PaymentValidationException extends Exception {
    
    private final String field;
    private final Object rejectedValue;
    private final List<String> validationErrors;
    private final Map<String, String> fieldErrors;
    
    public PaymentValidationException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.validationErrors = null;
        this.fieldErrors = null;
    }
    
    public PaymentValidationException(String message, String field, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.validationErrors = null;
        this.fieldErrors = null;
    }
    
    public PaymentValidationException(String message, List<String> validationErrors) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.validationErrors = validationErrors;
        this.fieldErrors = null;
    }
    
    public PaymentValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.validationErrors = null;
        this.fieldErrors = fieldErrors;
    }
    
    public PaymentValidationException(String message, String field, Object rejectedValue, Throwable cause) {
        super(message, cause);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.validationErrors = null;
        this.fieldErrors = null;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
    
    public boolean hasValidationErrors() {
        return validationErrors != null && !validationErrors.isEmpty();
    }
    
    @Override
    public String toString() {
        return "PaymentValidationException{" +
                "message='" + getMessage() + '\'' +
                ", field='" + field + '\'' +
                ", rejectedValue=" + rejectedValue +
                ", validationErrors=" + validationErrors +
                ", fieldErrors=" + fieldErrors +
                '}';
    }
}