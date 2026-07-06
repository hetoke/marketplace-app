package com.marketplace.user.dto;

import java.util.List;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		UserResponse user,
		boolean requiresMfa,
		String mfaToken,
		List<String> recoveryCodes
) {
	public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
		this(accessToken, refreshToken, user, false, null, null);
	}
}
