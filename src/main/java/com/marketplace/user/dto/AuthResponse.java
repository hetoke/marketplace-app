package com.marketplace.user.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		UserResponse user
) {}
