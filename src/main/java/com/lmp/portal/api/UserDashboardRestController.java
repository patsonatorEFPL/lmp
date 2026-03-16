package com.lmp.portal.api;

import com.lmp.auth.domain.User;
import com.lmp.auth.dto.UserResponse;
import com.lmp.auth.service.UserService;
import com.lmp.billing.domain.Order;
import com.lmp.billing.domain.OrderStatus;
import com.lmp.billing.domain.Review;
import com.lmp.billing.repository.OrderRepository;
import com.lmp.billing.repository.ReviewRepository;
import com.lmp.crm.domain.Appointment;
import com.lmp.crm.repository.AppointmentRepository;
import com.lmp.portal.dto.ChangePasswordRequest;
import com.lmp.portal.dto.DashboardStatsResponse;
import com.lmp.portal.dto.DashboardStatsResponse.RecentOrderDto;
import com.lmp.portal.dto.DashboardStatsResponse.RecentReviewDto;
import com.lmp.portal.dto.DashboardStatsResponse.UpcomingAppointmentDto;
import com.lmp.portal.dto.UpdateProfileRequest;
import com.lmp.shared.dto.ApiResponse;
import com.lmp.shared.service.WebSocketNotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API REST pour le tableau de bord utilisateur (Angular SPA).
 * Endpoints protégés — authentification requise.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Transactional(readOnly = true)
@Tag(name = "User Dashboard", description = "Statistiques et gestion du tableau de bord utilisateur")
public class UserDashboardRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserDashboardRestController.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserService userService;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public UserDashboardRestController(UserService userService,
                                        OrderRepository orderRepository,
                                        ReviewRepository reviewRepository,
                                        AppointmentRepository appointmentRepository,
                                        WebSocketNotificationService webSocketNotificationService) {
        this.userService = userService;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques du dashboard", description = "Retourne les KPIs, commandes récentes, avis et rendez-vous")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        // Orders
        List<Order> allOrders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        long totalOrders = allOrders.size();
        long completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .count();
        long inProgressOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS
                        || o.getStatus() == OrderStatus.PROCESSING
                        || o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.PENDING)
                .count();
        BigDecimal totalSpent = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recent orders (5 max)
        List<RecentOrderDto> recentOrders = allOrders.stream()
                .limit(5)
                .map(o -> new RecentOrderDto(
                        o.getId().toString(),
                        o.getServiceName(),
                        o.getStatus() != null ? o.getStatus().name() : null,
                        o.getTotalAmount(),
                        o.getCurrency(),
                        o.getCreatedAt() != null ? o.getCreatedAt().format(ISO_FORMATTER) : null))
                .collect(Collectors.toList());

        // Reviews
        List<Review> allReviews = reviewRepository.findByUser(user);
        long totalReviews = allReviews.size();
        List<RecentReviewDto> recentReviews = allReviews.stream()
                .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(r -> new RecentReviewDto(
                        r.getId().toString(),
                        r.getRating() != null ? r.getRating() : 0,
                        r.getComment(),
                        Boolean.TRUE.equals(r.getAdminApproved()),
                        r.getCreatedAt() != null ? r.getCreatedAt().format(ISO_FORMATTER) : null))
                .collect(Collectors.toList());

        // Appointments
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> upcoming = appointmentRepository.findUpcomingAppointments(user, now);
        long upcomingCount = upcoming.size();
        List<UpcomingAppointmentDto> upcomingList = upcoming.stream()
                .limit(5)
                .map(a -> new UpcomingAppointmentDto(
                        a.getId().toString(),
                        a.getSubject(),
                        a.getStatus() != null ? a.getStatus().name() : null,
                        a.getAppointmentDate() != null ? a.getAppointmentDate().format(ISO_FORMATTER) : null,
                        a.getDurationMinutes() != null ? a.getDurationMinutes() : 60))
                .collect(Collectors.toList());

        DashboardStatsResponse stats = new DashboardStatsResponse(
                totalOrders, completedOrders, inProgressOrders, totalReviews,
                upcomingCount, totalSpent,
                recentOrders, recentReviews, upcomingList);

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PutMapping("/profile")
    @Transactional
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour les informations du profil utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.companyName() != null) user.setCompanyName(request.companyName());
        if (request.city() != null) user.setCity(request.city());
        if (request.country() != null) user.setCountry(request.country());
        if (request.address() != null) user.setAddress(request.address());
        if (request.postalCode() != null) user.setPostalCode(request.postalCode());

        User saved = userService.save(user);
        logger.info("Profile updated for user: {}", saved.getEmail());

        // Reload with roles for response
        User withRoles = userService.findByEmailWithRoles(saved.getEmail()).orElse(saved);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", UserResponse.from(withRoles)));
    }

    @PutMapping("/password")
    @Transactional
    @Operation(summary = "Changer le mot de passe", description = "Change le mot de passe de l'utilisateur authentifié")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        if (!request.isPasswordMatching()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Les mots de passe ne correspondent pas"));
        }

        if (!userService.isPasswordStrong(request.newPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre"));
        }

        try {
            userService.changePasswordWithValidation(user.getId(), request.currentPassword(), request.newPassword());
            logger.info("Password changed for user: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("Mot de passe modifié avec succès", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ========== WebSocket Test Endpoint ==========

    @GetMapping("/test-notification")
    @Operation(summary = "Test WebSocket notification", description = "Sends a test WebSocket notification to the authenticated user")
    public ResponseEntity<ApiResponse<String>> testNotification(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }

        try {
            webSocketNotificationService.notifyUserNewPendingOrder(
                    user.getId().toString(),
                    "test-" + System.currentTimeMillis(),
                    "Test Notification Service",
                    99.99);
            logger.info("Test WebSocket notification sent to user: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.ok("Notification WebSocket envoyée", "OK"));
        } catch (Exception e) {
            logger.error("Failed to send test WebSocket notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
