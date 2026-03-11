package com.lmp.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO pour les requêtes de session Stripe Checkout
 * Simplifie l'intégration avec l'API Checkout de Stripe
 */
public class CheckoutSessionRequestDto {
    
    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;
    
    @NotBlank(message = "La devise est requise")
    private String currency;
    
    @NotBlank(message = "La description du produit est requise")
    private String productName;
    
    private String productDescription;
    
    @NotBlank(message = "L'email du client est requis")
    private String customerEmail;
    
    private String customerName;
    
    @NotBlank(message = "L'URL de succès est requise")
    private String successUrl;
    
    @NotBlank(message = "L'URL d'annulation est requise")
    private String cancelUrl;
    
    private Map<String, String> metadata;
    
    private boolean collectShippingAddress = false;
    
    private boolean allowPromotionCodes = true;
    
    private String paymentMethodTypes = "card"; // Par défaut seulement cartes
    
    // Mode de paiement : "payment" (unique) ou "subscription" (récurrent)
    private String mode = "payment";
    
    // Constructeurs
    public CheckoutSessionRequestDto() {}
    
    public CheckoutSessionRequestDto(BigDecimal amount, String currency, String productName, 
                                   String customerEmail, String successUrl, String cancelUrl) {
        this.amount = amount;
        this.currency = currency;
        this.productName = productName;
        this.customerEmail = customerEmail;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }
    
    // Getters et Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
    
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
    
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public boolean isCollectShippingAddress() { return collectShippingAddress; }
    public void setCollectShippingAddress(boolean collectShippingAddress) { 
        this.collectShippingAddress = collectShippingAddress; 
    }
    
    public boolean isAllowPromotionCodes() { return allowPromotionCodes; }
    public void setAllowPromotionCodes(boolean allowPromotionCodes) { 
        this.allowPromotionCodes = allowPromotionCodes; 
    }
    
    public String getPaymentMethodTypes() { return paymentMethodTypes; }
    public void setPaymentMethodTypes(String paymentMethodTypes) { 
        this.paymentMethodTypes = paymentMethodTypes; 
    }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}