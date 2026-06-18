package com.marketplace.user.controller;

import com.marketplace.image.dto.ImageResponse;
import com.marketplace.image.model.EntityType;
import com.marketplace.image.service.ImageService;
import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.user.dto.ChangePasswordRequest;
import com.marketplace.user.dto.UpdateProfileRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		UserResponse user = userService.getProfile(userId);
		return ResponseEntity.ok(ApiResponse.ok(user));
	}

	@PutMapping("/profile")
	public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		UserResponse user = userService.updateProfile(userId, request);
		return ResponseEntity.ok(ApiResponse.ok("Profile updated", user));
	}

	@PutMapping("/profile/password")
	public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		userService.changePassword(userId, request);
		return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
	}
}
