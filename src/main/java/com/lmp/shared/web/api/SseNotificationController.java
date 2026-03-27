package com.lmp.shared.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.lmp.auth.domain.User;
import com.lmp.auth.service.UserService;
import com.lmp.shared.service.SseEmitterManager;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

/**
 * SSE endpoints for real-time notifications.
 * Replaces WebSocket/STOMP endpoints.
 *
 * - GET /api/v1/sse/notifications → per-user notification stream (authenticated)
 * - GET /api/v1/sse/admin/events  → admin broadcast stream (ADMIN role)
 */
@RestController
@RequestMapping("/api/v1/sse")
@Tag(name = "SSE Notifications", description = "Server-Sent Events for real-time notifications")
public class SseNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationController.class);

    private final SseEmitterManager emitterManager;
    private final UserService userService;

    public SseNotificationController(SseEmitterManager emitterManager, UserService userService) {
        this.emitterManager = emitterManager;
        this.userService = userService;
    }

    /**
     * User notification SSE stream.
     * EventSource sends cookies automatically (same-origin), so session auth works out of the box.
     * Response headers disable proxy buffering for Nginx compatibility.
     */
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE notification stream", description = "Streams real-time notifications to the authenticated user")
    public SseEmitter streamNotifications(Authentication authentication, HttpServletResponse response) {
        User user = getAuthenticatedUser(authentication);
        if (user == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }

        setSseHeaders(response);

        String userId = user.getId().toString();
        SseEmitter emitter = emitterManager.createEmitter(userId);

        logger.info("SSE notification stream opened for user {} ({})", user.getEmail(), userId);
        return emitter;
    }

    /**
     * Admin broadcast SSE stream.
     * Streams admin-specific events (order updates, dashboard refresh, stats).
     */
    @GetMapping(value = "/admin/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "SSE admin event stream", description = "Streams real-time admin events (orders, dashboard, stats)")
    public SseEmitter streamAdminEvents(Authentication authentication, HttpServletResponse response) {
        setSseHeaders(response);

        SseEmitter emitter = emitterManager.createAdminEmitter();

        logger.info("SSE admin event stream opened for {}", authentication.getName());
        return emitter;
    }

    /**
     * Sets response headers required for SSE through Nginx and other reverse proxies.
     */
    private void setSseHeaders(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Connection", "keep-alive");
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
