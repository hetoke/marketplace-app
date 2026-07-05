package com.marketplace.notification.service;

import com.marketplace.notification.dto.NotificationResponse;
import com.marketplace.notification.dto.NotificationSettingsRequest;
import com.marketplace.notification.model.Notification;
import com.marketplace.notification.model.NotificationType;
import com.marketplace.notification.repository.NotificationRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title, String message) {
        return createNotification(userId, type, title, message, null, null);
    }

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title, String message,
                                           UUID referenceId, String referenceType) {
        Notification notification = new Notification(userId, type, title, message, referenceId, referenceType);
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification created for user {}: {}", userId, title);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        UUID userUuid = UUID.fromString(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userUuid, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllUserNotifications(String userId) {
        UUID userUuid = UUID.fromString(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userUuid).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(String userId) {
        UUID userUuid = UUID.fromString(userId);
        return notificationRepository.countByUserIdAndReadFalse(userUuid);
    }

    @Transactional
    public void markAsRead(String userId, UUID notificationId) {
        UUID userUuid = UUID.fromString(userId);
        int updated = notificationRepository.markAsRead(notificationId, userUuid);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }
    }

    @Transactional
    public void markAllAsRead(String userId) {
        UUID userUuid = UUID.fromString(userId);
        notificationRepository.markAllAsRead(userUuid);
    }

    @Transactional
    public void deleteNotification(String userId, UUID notificationId) {
        UUID userUuid = UUID.fromString(userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUserId().equals(userUuid)) {
            throw new BusinessException("Notification does not belong to this user");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void clearAllNotifications(String userId) {
        UUID userUuid = UUID.fromString(userId);
        notificationRepository.deleteByUserId(userUuid);
    }

    @Transactional
    public void updateSettings(String userId, NotificationSettingsRequest request) {
        log.debug("Notification settings updated for user {}: {}", userId, request);
    }
}
