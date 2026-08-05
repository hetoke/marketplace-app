package com.marketplace.user.dto;

import java.util.List;

public record MfaSetupResponse(
		UserResponse user,
		List<String> recoveryCodes
) {}
