package com.marketplace.user.dto;

public record TokenResponse(
		String accessToken,
		String refreshToken
) {}
