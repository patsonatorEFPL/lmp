package com.lmp.domain.entity;

import java.time.LocalDateTime;
import java.util.Set;

import com.lmp.domain.enums.UserStatus;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name", nullable = true)
    private String firstName;
    
    @Column(name = "last_name", nullable = true)
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

    // Gender field for avatar display
    @Column(name = "gender")
    private String gender; // "MALE", "FEMALE", "OTHER", null

    // OAuth2 fields
    @Column(name = "oauth_provider")
    private String oauthProvider; // "google", "microsoft", null for local

    @Column(name = "oauth_provider_id")
    private String oauthProviderId;

    // Relationships
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Order> orders;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Review> reviews;
    
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderStatusHistory> orderStatusHistories;
    
    // Constructors
    public User() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }
    
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
    
    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public Boolean getAccountLocked() {
        return accountLocked;
    }
    
    public void setAccountLocked(Boolean accountLocked) {
        this.accountLocked = accountLocked;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
    
    public String getVerificationToken() {
        return verificationToken;
    }
    
    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
    
    public String getResetToken() {
        return resetToken;
    }
    
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
    
    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }
    
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    public Set<Order> getOrders() {
        return orders;
    }
    
    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }
    
    public Set<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(Set<Review> reviews) {
        this.reviews = reviews;
    }
    
    public Set<OrderStatusHistory> getOrderStatusHistories() {
        return orderStatusHistories;
    }
    
    public void setOrderStatusHistories(Set<OrderStatusHistory> orderStatusHistories) {
        this.orderStatusHistories = orderStatusHistories;
    }
    
    /**
     * Retourne le nom d'affichage de l'utilisateur.
     * Si firstName et lastName sont disponibles, les combine.
     * Sinon, extrait un pseudo de la partie avant @ de l'email.
     *
     * @return Le nom d'affichage généré
     */
    public String getDisplayName() {
        // Si les noms sont disponibles, les utiliser
        if (firstName != null && !firstName.trim().isEmpty() &&
            lastName != null && !lastName.trim().isEmpty()) {
            return firstName.trim() + " " + lastName.trim();
        }
        
        // Si seul le prénom est disponible, l'utiliser
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        }
        
        // Si seul le nom est disponible, l'utiliser
        if (lastName != null && !lastName.trim().isEmpty()) {
            return lastName.trim();
        }
        
        // Sinon, extraire un pseudo de l'email
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf("@"));
            
            // Remplacer les points, tirets, underscores par des espaces pour un affichage plus lisible
            String displayName = localPart.replace(".", " ")
                                          .replace("-", " ")
                                          .replace("_", " ");
            
            // Capitaliser la première lettre de chaque mot
            String[] words = displayName.split("\\s+");
            StringBuilder result = new StringBuilder();
            
            for (String word : words) {
                if (!word.isEmpty()) {
                    if (result.length() > 0) {
                        result.append(" ");
                    }
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase());
                }
            }
            
            return result.toString();
        }
        
        // Fallback si l'email est invalide
        return "Utilisateur";
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Returns a Font Awesome icon class based on gender.
     */
    public String getGenderIcon() {
        if (gender == null) return "fa-user";
        return switch (gender.toUpperCase()) {
            case "MALE" -> "fa-male";
            case "FEMALE" -> "fa-female";
            default -> "fa-user";
        };
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public void setOauthProvider(String oauthProvider) {
        this.oauthProvider = oauthProvider;
    }

    public String getOauthProviderId() {
        return oauthProviderId;
    }

    public void setOauthProviderId(String oauthProviderId) {
        this.oauthProviderId = oauthProviderId;
    }
}
