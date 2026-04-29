package com.lmp.auth.domain;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.lmp.auth.domain.UserStatus;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatusHistory;
import com.lmp.billing.domain.Review;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    private String phone;
    
    private String address;
    
    private String city;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    private String country;
    
    @Column(name = "company_name")
    private String companyName;
    
    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;
    
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "account_locked")
    private Boolean accountLocked = false;
    
    @Column(name = "email_verified")
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "reset_token")
    private String resetToken;
    
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "gender")
    private String gender;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_provider_id")
    private String oauthProviderId;

    /**
     * Le client déclare être assujetti à l'autoliquidation TVA (auto-reverse) / exonération liée en B2B.
     */
    @Column(name = "vat_reverse_charge", nullable = false)
    private Boolean vatReverseCharge = false;

    @Column(name = "vat_number", length = 64)
    private String vatNumber;

    // Relationships
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    
    /** Pas de REMOVE : sinon delete(user) supprime commandes/avis malgré anonymisation (user_id NULL). */
    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private Set<Order> orders;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private Set<Review> reviews;

    @OneToMany(mappedBy = "createdBy", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    private Set<OrderStatusHistory> orderStatusHistories;
    
    // Constructors
    public User() {}
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public LocalDateTime getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(LocalDateTime lastLoginDate) { this.lastLoginDate = lastLoginDate; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public Boolean getAccountLocked() { return accountLocked; }
    public void setAccountLocked(Boolean accountLocked) { this.accountLocked = accountLocked; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    
    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }
    
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    
    public Set<Order> getOrders() { return orders; }
    public void setOrders(Set<Order> orders) { this.orders = orders; }
    
    public Set<Review> getReviews() { return reviews; }
    public void setReviews(Set<Review> reviews) { this.reviews = reviews; }
    
    public Set<OrderStatusHistory> getOrderStatusHistories() { return orderStatusHistories; }
    public void setOrderStatusHistories(Set<OrderStatusHistory> orderStatusHistories) { this.orderStatusHistories = orderStatusHistories; }
    
    public String getDisplayName() {
        if (firstName != null && !firstName.trim().isEmpty() &&
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        }
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf("@"));
            String displayName = localPart.replace(".", " ").replace("-", " ").replace("_", " ");
            String[] words = displayName.split("\\s+");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) result.append(" ");
                    result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                }
            }
            return result.toString();
        }
        return "Utilisateur";
    }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getGenderIcon() {
        if (gender == null) return "fa-user";
        return switch (gender.toUpperCase()) {
            case "MALE" -> "fa-male";
            case "FEMALE" -> "fa-female";
            default -> "fa-user";
        };
    }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthProviderId() { return oauthProviderId; }
    public void setOauthProviderId(String oauthProviderId) { this.oauthProviderId = oauthProviderId; }

    public Boolean getVatReverseCharge() { return vatReverseCharge; }
    public void setVatReverseCharge(Boolean vatReverseCharge) { this.vatReverseCharge = vatReverseCharge; }

    public String getVatNumber() { return vatNumber; }
    public void setVatNumber(String vatNumber) { this.vatNumber = vatNumber; }

    // --- Champs de liaison système externe (agnostique ERP) ---

    @Column(name = "external_customer_id", length = 140)
    private String externalCustomerId;

    @Column(name = "external_contact_id", length = 140)
    private String externalContactId;

    @Column(name = "external_address_id", length = 140)
    private String externalAddressId;

    @Column(name = "external_erp_user_id", length = 140)
    private String externalErpUserId;

    public String getExternalCustomerId() { return externalCustomerId; }
    public void setExternalCustomerId(String externalCustomerId) { this.externalCustomerId = externalCustomerId; }

    public String getExternalContactId() { return externalContactId; }
    public void setExternalContactId(String externalContactId) { this.externalContactId = externalContactId; }

    public String getExternalAddressId() { return externalAddressId; }
    public void setExternalAddressId(String externalAddressId) { this.externalAddressId = externalAddressId; }

    public String getExternalErpUserId() { return externalErpUserId; }
    public void setExternalErpUserId(String externalErpUserId) { this.externalErpUserId = externalErpUserId; }

    public boolean isStaff() {
        if (roles == null) return false;
        return roles.stream().anyMatch(r -> {
            String name = r.getName();
            return "STAFF".equals(name) || "ADMIN".equals(name);
        });
    }
}
