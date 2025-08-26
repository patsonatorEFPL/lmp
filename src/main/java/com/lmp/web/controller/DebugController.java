package com.lmp.web.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lmp.domain.entity.User;
import com.lmp.repository.UserRepository;

/**
 * Contrôleur de debug pour vérifier les données
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Liste tous les utilisateurs avec leurs rôles (pour debug)
     */
    @GetMapping("/users")
    public ResponseEntity<List<String>> getAllUsersDebug() {
        List<User> users = userRepository.findAll();
        
        List<String> userInfos = users.stream()
            .map(user -> {
                String roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.joining(", "));
                
                return String.format("%s | %s %s | %s | Verrouillé: %s | Rôles: %s",
                    user.getEmail(),
                    user.getFirstName() != null ? user.getFirstName() : "N/A",
                    user.getLastName() != null ? user.getLastName() : "N/A",
                    user.getStatus(),
                    user.getAccountLocked(),
                    roles);
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(userInfos);
    }

    /**
     * Liste uniquement les administrateurs
     */
    @GetMapping("/admins")
    public ResponseEntity<List<String>> getAdminsDebug() {
        List<User> users = userRepository.findAll();
        
        List<String> adminInfos = users.stream()
            .filter(user -> user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName())))
            .map(user -> String.format("🔑 ADMIN: %s - %s %s - Statut: %s - Verrouillé: %s",
                user.getEmail(),
                user.getFirstName() != null ? user.getFirstName() : "N/A",
                user.getLastName() != null ? user.getLastName() : "N/A",
                user.getStatus(),
                user.getAccountLocked()))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(adminInfos);
    }
}