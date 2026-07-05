package com.marketplace.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationTokenRequest(
		@NotBlank @Email String email
) {}
