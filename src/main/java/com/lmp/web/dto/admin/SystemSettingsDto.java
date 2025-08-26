package com.lmp.web.dto.admin;

import jakarta.validation.constraints.*;

/**
 * DTO pour les paramètres système
 */
public class SystemSettingsDto {
    
    // Configuration Application
    @NotBlank(message = "Le nom de l'application est requis")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String appName;
    
    @NotBlank(message = "La version est requise")
    @Pattern(regexp = "\\d+\\.\\d+\\.\\d+", message = "Format de version invalide (ex: 1.0.0)")
    private String appVersion;
    
    @NotBlank(message = "L'URL de base est requise")
    @Size(max = 255, message = "L'URL ne peut pas dépasser 255 caractères")
    private String appBaseUrl;
    
    // Configuration Entreprise
    @NotBlank(message = "Le nom de l'entreprise est requis")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String companyName;
    
    @Email(message = "L'email doit être valide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String companyEmail;
    
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String companyPhone;
    
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String companyAddress;
    
    @Size(max = 255, message = "L'URL du site web ne peut pas dépasser 255 caractères")
    private String companyWebsite;
    
    // Configuration Stripe
    private String stripePublishableKey;
    private boolean stripeTestMode;
    
    // Configuration Email
    @NotBlank(message = "L'hôte mail est requis")
    private String mailHost;
    
    @NotBlank(message = "Le port mail est requis")
    @Pattern(regexp = "\\d+", message = "Le port doit être un nombre")
    private String mailPort;
    
    private String mailUsername;
    private boolean mailAuthEnabled;
    
    // Configuration Sécurité
    @Min(value = 6, message = "La longueur minimale doit être au moins 6")
    @Max(value = 50, message = "La longueur minimale ne peut pas dépasser 50")
    private int passwordMinLength;
    
    @Min(value = 1, message = "Au moins 1 tentative doit être autorisée")
    @Max(value = 10, message = "Maximum 10 tentatives autorisées")
    private int maxLoginAttempts;
    
    @Min(value = 60000, message = "La durée de verrouillage doit être au moins 1 minute")
    private long accountLockoutDuration;
    
    // Configuration Paiements
    @NotBlank(message = "La devise par défaut est requise")
    @Size(min = 3, max = 3, message = "La devise doit faire 3 caractères")
    private String defaultCurrency;
    
    @DecimalMin(value = "0.01", message = "Le montant minimum doit être positif")
    private double minAmount;
    
    @DecimalMin(value = "1.00", message = "Le montant maximum doit être au moins 1")
    private double maxAmount;
    
    // Constructeurs
    public SystemSettingsDto() {}
    
    // Getters et Setters
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    
    public String getAppBaseUrl() { return appBaseUrl; }
    public void setAppBaseUrl(String appBaseUrl) { this.appBaseUrl = appBaseUrl; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    
    public String getStripePublishableKey() { return stripePublishableKey; }
    public void setStripePublishableKey(String stripePublishableKey) { this.stripePublishableKey = stripePublishableKey; }
    
    public boolean isStripeTestMode() { return stripeTestMode; }
    public void setStripeTestMode(boolean stripeTestMode) { this.stripeTestMode = stripeTestMode; }
    
    public String getMailHost() { return mailHost; }
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }
    
    public String getMailPort() { return mailPort; }
    public void setMailPort(String mailPort) { this.mailPort = mailPort; }
    
    public String getMailUsername() { return mailUsername; }
    public void setMailUsername(String mailUsername) { this.mailUsername = mailUsername; }
    
    public boolean isMailAuthEnabled() { return mailAuthEnabled; }
    public void setMailAuthEnabled(boolean mailAuthEnabled) { this.mailAuthEnabled = mailAuthEnabled; }
    
    public int getPasswordMinLength() { return passwordMinLength; }
    public void setPasswordMinLength(int passwordMinLength) { this.passwordMinLength = passwordMinLength; }
    
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }
    
    public long getAccountLockoutDuration() { return accountLockoutDuration; }
    public void setAccountLockoutDuration(long accountLockoutDuration) { this.accountLockoutDuration = accountLockoutDuration; }
    
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    
    public double getMinAmount() { return minAmount; }
    public void setMinAmount(double minAmount) { this.minAmount = minAmount; }
    
    public double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(double maxAmount) { this.maxAmount = maxAmount; }
}