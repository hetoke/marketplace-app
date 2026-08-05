package com.marketplace.user.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		UserResponse user,
		boolean requiresMfa,
		String mfaToken
) {
	public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
		this(accessToken, refreshToken, user, false, null);
	}
}
