package com.lmp.web.controller.admin;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.domain.entity.User;
import com.lmp.repository.UserRepository;

/**
 * Contrôleur pour la gestion des utilisateurs (Administration)
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Liste tous les utilisateurs avec leurs rôles
     */
    @GetMapping
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        List<UserInfo> userInfos = users.stream()
            .map(user -> new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus().toString(),
                user.getAccountLocked(),
                user.getEmailVerified(),
                user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(userInfos);
    }

    /**
     * DTO pour les informations utilisateur
     */
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String status;
        private Boolean accountLocked;
        private Boolean emailVerified;
        private List<String> roles;

        public UserInfo(Long id, String email, String firstName, String lastName, 
                       String status, Boolean accountLocked, Boolean emailVerified, List<String> roles) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.status = status;
            this.accountLocked = accountLocked;
            this.emailVerified = emailVerified;
            this.roles = roles;
        }

        // Getters
        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getStatus() { return status; }
        public Boolean getAccountLocked() { return accountLocked; }
        public Boolean getEmailVerified() { return emailVerified; }
        public List<String> getRoles() { return roles; }
    }
}