package com.marketplace.admin.dto;

import com.marketplace.user.model.User;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String email,
    String displayName,
    String role,
    boolean verified,
    String status,
    Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRole().name(),
            user.isVerified(),
            user.getStatus().name(),
            user.getCreatedAt()
        );
    }
}
