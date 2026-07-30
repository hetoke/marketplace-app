package com.marketplace.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marketplace.user.model.User;
import java.time.Instant;

@lombok.Builder
public record UserResponse(
		String id,
		String email,
		String displayName,
		String profilePictureUrl,
		User.Role role,
		boolean isVerified,
		boolean mfaEnabled,
		User.AuthenticationType authenticationType,
		Instant createdAt,
		String defaultStreet,
		String defaultProvince,
		String defaultDistrict,
		String defaultWard
) {
	public static UserResponse from(User user) {
		return UserResponse.builder()
				.id(user.getId().toString())
				.email(user.getEmail())
				.displayName(user.getDisplayName())
				.profilePictureUrl(user.getProfilePictureUrl())
				.role(user.getRole())
				.isVerified(user.isVerified())
				.mfaEnabled(user.isMfaEnabled())
				.authenticationType(user.getAuthenticationType())
				.createdAt(user.getCreatedAt())
				.defaultStreet(user.getDefaultStreet())
				.defaultProvince(user.getDefaultProvince())
				.defaultDistrict(user.getDefaultDistrict())
				.defaultWard(user.getDefaultWard())
				.build();
	}
}
