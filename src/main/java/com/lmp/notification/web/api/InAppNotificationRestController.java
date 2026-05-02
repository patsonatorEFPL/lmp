package com.lmp.notification.web.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.notification.dto.InAppNotificationDto;
import com.lmp.notification.service.InAppNotificationService;
import com.lmp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API REST pour les notifications in-app.
 * Endpoints authentifiés pour récupérer, marquer comme lues et supprimer les notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications in-app")
public class InAppNotificationRestController {

    private final InAppNotificationService notificationService;
    private final UserService userService;

    public InAppNotificationRestController(InAppNotificationService notificationService,
                                            UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Récupère les notifications de l'utilisateur connecté")
    public ResponseEntity<ApiResponse<List<InAppNotificationDto>>> getNotifications(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        List<InAppNotificationDto> notifications = notificationService.getUserNotifications(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notifications récupérées", notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Récupère le nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Compteur récupéré", Map.of("count", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marque une notification comme lue")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marquée comme lue", null));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marque toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Toutes les notifications marquées comme lues", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime une notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification supprimée", null));
    }

    @DeleteMapping("/all")
    @Operation(summary = "Supprime toutes les notifications de l'utilisateur")
    public ResponseEntity<ApiResponse<Void>> deleteAll(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
        }

        notificationService.deleteAllForUser(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Toutes les notifications supprimées", null));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByLogin(authentication.getName()).orElse(null);
    }
}
