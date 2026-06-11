package com.marketplace.user.dto;

import com.marketplace.user.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 128) String password,
		@NotBlank @Size(min = 1, max = 255) String displayName,
		@NotNull User.Role role
) {}
