package com.marketplace.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.notification.dto.NotificationResponse;
import com.marketplace.notification.model.Notification;
import com.marketplace.notification.model.NotificationType;
import com.marketplace.notification.repository.NotificationRepository;
import com.marketplace.shared.exception.BusinessException;
import com.marketplace.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    private Notification createTestNotification(UUID userId) {
        Notification notification = new Notification(
                userId,
                NotificationType.ORDER_UPDATE,
                "Order Placed",
                "Your order has been placed."
        );
        notification.setId(UUID.randomUUID());
        return notification;
    }

    @Test
    void createNotification_createsNotificationSuccessfully() {
        UUID userUuid = UUID.fromString(USER_ID);
        Notification notification = createTestNotification(userUuid);

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        Notification result = notificationService.createNotification(
                userUuid,
                NotificationType.ORDER_UPDATE,
                "Order Placed",
                "Your order has been placed."
        );

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Order Placed");
        assertThat(result.getType()).isEqualTo(NotificationType.ORDER_UPDATE);
    }

    @Test
    void getUnreadCount_returnsCount() {
        UUID userUuid = UUID.fromString(USER_ID);

        when(notificationRepository.countByUserIdAndReadFalse(userUuid))
                .thenReturn(5L);

        Long count = notificationService.getUnreadCount(USER_ID);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void markAsRead_marksNotificationAsRead() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID notificationId = UUID.randomUUID();

        when(notificationRepository.markAsRead(notificationId, userUuid))
                .thenReturn(1);

        notificationService.markAsRead(USER_ID, notificationId);

        verify(notificationRepository).markAsRead(notificationId, userUuid);
    }

    @Test
    void markAsRead_throwsException_whenNotFound() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID notificationId = UUID.randomUUID();

        when(notificationRepository.markAsRead(notificationId, userUuid))
                .thenReturn(0);

        assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, notificationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllAsRead_marksAllNotificationsAsRead() {
        UUID userUuid = UUID.fromString(USER_ID);

        when(notificationRepository.markAllAsRead(userUuid))
                .thenReturn(5);

        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).markAllAsRead(userUuid);
    }

    @Test
    void deleteNotification_deletesNotification() {
        UUID userUuid = UUID.fromString(USER_ID);
        Notification notification = createTestNotification(userUuid);

        when(notificationRepository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        notificationService.deleteNotification(USER_ID, notification.getId());

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_throwsException_whenNotOwner() {
        UUID userUuid = UUID.fromString(USER_ID);
        UUID otherUserId = UUID.randomUUID();
        Notification notification = createTestNotification(otherUserId);

        when(notificationRepository.findById(notification.getId()))
                .thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(USER_ID, notification.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void deleteNotification_throwsException_whenNotFound() {
        UUID notificationId = UUID.randomUUID();

        when(notificationRepository.findById(notificationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(USER_ID, notificationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clearAllNotifications_clearsAllNotifications() {
        UUID userUuid = UUID.fromString(USER_ID);

        notificationService.clearAllNotifications(USER_ID);

        verify(notificationRepository).deleteByUserId(userUuid);
    }

    @Test
    void getUserNotifications_returnsNotifications() {
        UUID userUuid = UUID.fromString(USER_ID);
        Notification notification = createTestNotification(userUuid);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userUuid, Pageable.unpaged()))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(USER_ID, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Order Placed");
    }
}
