package com.marketplace.notification.controller;

import com.marketplace.notification.dto.NotificationResponse;
import com.marketplace.notification.dto.NotificationSettingsRequest;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(Pageable pageable) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAllNotifications() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<NotificationResponse> notifications = notificationService.getAllUserNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID notificationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID notificationId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.deleteNotification(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearAllNotifications() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.clearAllNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications cleared", null));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @Valid @RequestBody NotificationSettingsRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Notification settings updated", null));
    }
}
