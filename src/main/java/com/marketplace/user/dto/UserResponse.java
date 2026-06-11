package com.marketplace.user.dto;

import com.marketplace.user.model.User;
import java.time.Instant;

public record UserResponse(
		String id,
		String email,
		String displayName,
		User.Role role,
		boolean isVerified,
		User.AuthenticationType authenticationType,
		Instant createdAt
) {
	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId().toString(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				user.isVerified(),
				user.getAuthenticationType(),
				user.getCreatedAt()
		);
	}
}
