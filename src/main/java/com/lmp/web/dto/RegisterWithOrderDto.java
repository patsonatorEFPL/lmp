package com.lmp.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO pour l'inscription d'un utilisateur avec création simultanée d'une
 * commande.
 * Utilisé pour le flux d'achat intégré où l'utilisateur s'inscrit et commande
 * en une seule étape.
 * Inscription simplifiée : seuls email et password sont obligatoires pour
 * l'utilisateur.
 * Les informations d'adresse restent obligatoires pour la facturation et
 * livraison.
 */
public class RegisterWithOrderDto {

    // Informations utilisateur (optionnelles pour l'inscription simplifiée)
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstName;

    @Size(max = 50, message = "Le nom ne peut pas dépasser 50 caractères")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir entre 6 et 100 caractères")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;

    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String phone;

    // Informations d'adresse (optionnelles pour l'inscription simplifiée,
    // collectées lors du checkout Stripe)
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @Size(max = 50, message = "La ville ne peut pas dépasser 50 caractères")
    private String city;

    @Size(max = 20, message = "Le code postal ne peut pas dépasser 20 caractères")
    private String postalCode;

    @Size(max = 50, message = "Le pays ne peut pas dépasser 50 caractères")
    private String country;

    // Informations entreprise (optionnelles)
    @Size(max = 100, message = "Le nom de l'entreprise ne peut pas dépasser 100 caractères")
    private String companyName;

    // Informations de commande
    @NotBlank(message = "Le nom du service est obligatoire")
    @Size(max = 100, message = "Le nom du service ne peut pas dépasser 100 caractères")
    private String serviceName;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotBlank(message = "La devise est obligatoire")
    @Size(max = 3, message = "La devise ne peut pas dépasser 3 caractères")
    private String currency = "EUR";

    // ID de l'offre pour validation côté serveur (sécurisation prix)
    private Long offerId;

    // Conditions d'utilisation
    @NotNull(message = "Vous devez accepter les conditions d'utilisation")
    private Boolean acceptTerms = false;

    // Constructeurs
    public RegisterWithOrderDto() {
    }

    // Getters et Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public Boolean getAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(Boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    /**
     * Vérifie si les mots de passe correspondent.
     * 
     * @return true si les mots de passe correspondent, false sinon
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Convertit ce DTO en RegisterDto pour la création de l'utilisateur.
     * 
     * @return RegisterDto équivalent
     */
    public RegisterDto toRegisterDto() {
        RegisterDto registerDto = new RegisterDto();
        registerDto.setFirstName(this.firstName);
        registerDto.setLastName(this.lastName);
        registerDto.setEmail(this.email);
        registerDto.setPassword(this.password);
        registerDto.setConfirmPassword(this.confirmPassword);
        registerDto.setPhone(this.phone);
        registerDto.setAddress(this.address);
        registerDto.setCity(this.city);
        registerDto.setPostalCode(this.postalCode);
        registerDto.setCountry(this.country);
        registerDto.setCompanyName(this.companyName);
        registerDto.setAcceptTerms(this.acceptTerms);
        return registerDto;
    }

    @Override
    public String toString() {
        return "RegisterWithOrderDto{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", companyName='" + companyName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", offerId=" + offerId +
                ", acceptTerms=" + acceptTerms +
                '}';
    }
}