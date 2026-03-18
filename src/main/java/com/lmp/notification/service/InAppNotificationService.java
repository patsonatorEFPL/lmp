package com.lmp.notification.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lmp.auth.domain.User;
import com.lmp.auth.repository.UserRepository;
import com.lmp.notification.domain.InAppNotification;
import com.lmp.notification.dto.InAppNotificationDto;
import com.lmp.notification.repository.InAppNotificationRepository;

/**
 * Service pour la gestion des notifications in-app persistées.
 */
@Service
@Transactional
public class InAppNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(InAppNotificationService.class);

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public InAppNotificationService(InAppNotificationRepository notificationRepository,
                                     UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crée et persiste une notification in-app pour un utilisateur.
     * Uses REQUIRES_NEW to guarantee commit even when called from a read-only transaction context.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InAppNotification createNotification(String userId, String type, String message,
                                                  String orderId, String serviceName, Double amount) {
        UUID userUuid = UUID.fromString(userId);
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        InAppNotification notification = new InAppNotification();
        notification.setUser(user);
        notification.setType(type != null ? type : "INFO");
        notification.setMessage(message != null ? message : "Nouvelle notification");
        notification.setOrderId(orderId);
        notification.setServiceName(serviceName);
        if (amount != null) {
            notification.setAmount(BigDecimal.valueOf(amount));
        }
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notification = notificationRepository.save(notification);
        logger.info("Notification in-app créée pour user {} : type={}, id={}", userId, type, notification.getId());
        return notification;
    }

    /**
     * Récupère toutes les notifications d'un utilisateur (max 50, les plus récentes).
     */
    @Transactional(readOnly = true)
    public List<InAppNotificationDto> getUserNotifications(UUID userId) {
        List<InAppNotification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        // Limit to 50 max
        if (notifications.size() > 50) {
            notifications = notifications.subList(0, 50);
        }

        return notifications.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Compte les notifications non lues d'un utilisateur.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Marque une notification comme lue.
     */
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
            logger.debug("Notification {} marquée comme lue", notificationId);
        });
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues.
     */
    public void markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        logger.info("{} notifications marquées comme lues pour user {}", updated, userId);
    }

    /**
     * Supprime une notification.
     */
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
        logger.debug("Notification {} supprimée", notificationId);
    }

    /**
     * Supprime toutes les notifications d'un utilisateur.
     */
    public void deleteAllForUser(UUID userId) {
        int deleted = notificationRepository.deleteAllByUserId(userId);
        logger.info("{} notifications supprimées pour user {}", deleted, userId);
    }

    private InAppNotificationDto toDto(InAppNotification notification) {
        // Format timestamp as ISO-8601 with UTC 'Z' suffix for correct frontend parsing
        String timestamp = notification.getCreatedAt()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        return new InAppNotificationDto(
                notification.getId().toString(),
                notification.getType(),
                notification.getMessage(),
                notification.getOrderId(),
                notification.getServiceName(),
                notification.getAmount() != null ? notification.getAmount().doubleValue() : null,
                notification.isRead(),
                timestamp
        );
    }
}
