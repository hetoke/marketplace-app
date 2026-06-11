package com.marketplace.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(min = 1, max = 255) String displayName,
		@Size(max = 512) String profilePictureUrl
) {}
