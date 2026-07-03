package com.marketplace.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(min = 1, max = 255) String displayName,
		@Size(max = 500) String defaultStreet,
		@Size(max = 255) String defaultProvince,
		@Size(max = 255) String defaultDistrict,
		@Size(max = 255) String defaultWard
) {}
