package com.marketplace.notification.dto;

import com.marketplace.notification.model.Notification;
import com.marketplace.notification.model.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String message,
    UUID referenceId,
    String referenceType,
    boolean isRead,
    Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getReferenceId(),
            notification.getReferenceType(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
