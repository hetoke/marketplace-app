package com.marketplace.user.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.ChangePasswordRequest;
import com.marketplace.user.dto.UpdateProfileRequest;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.model.User;
import com.marketplace.user.repository.UserRepository;
import com.marketplace.user.service.MFAService;
import com.marketplace.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;
	private final MFAService mfaService;
	private final UserRepository userRepository;

	public UserController(UserService userService, MFAService mfaService, UserRepository userRepository) {
		this.userService = userService;
		this.mfaService = mfaService;
		this.userRepository = userRepository;
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

	@PostMapping("/mfa/setup")
	public ResponseEntity<ApiResponse<Map<String, String>>> setupMFA() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new com.marketplace.shared.exception.ResourceNotFoundException("User", "id", userId));
		mfaService.setupMFA(user);
		return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email", Map.of("message", "OTP sent to your email")));
	}

	@PostMapping("/mfa/verify")
	public ResponseEntity<ApiResponse<AuthResponse>> verifyMFASetup(@RequestBody Map<String, String> body) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		String otp = body.get("otp");
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new com.marketplace.shared.exception.ResourceNotFoundException("User", "id", userId));
		AuthResponse response = mfaService.verifySetup(user, otp);
		return ResponseEntity.ok(ApiResponse.ok("MFA enabled successfully", response));
	}

	@DeleteMapping("/mfa")
	public ResponseEntity<ApiResponse<Void>> disableMFA(@RequestBody Map<String, String> body) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		String otp = body.get("otp");
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new com.marketplace.shared.exception.ResourceNotFoundException("User", "id", userId));
		mfaService.disableMFA(user, otp);
		return ResponseEntity.ok(ApiResponse.ok("MFA disabled successfully", null));
	}

	@PostMapping("/mfa/disable/send-otp")
	public ResponseEntity<ApiResponse<Map<String, String>>> sendDisableOTP() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new com.marketplace.shared.exception.ResourceNotFoundException("User", "id", userId));
		mfaService.sendDisableOTP(user);
		return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email", Map.of("message", "OTP sent to your email")));
	}

	@GetMapping("/mfa/status")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getMFAStatus() {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userRepository.findById(UUID.fromString(userId))
				.orElseThrow(() -> new com.marketplace.shared.exception.ResourceNotFoundException("User", "id", userId));
		long recoveryCodesRemaining = mfaService.getRecoveryCodesRemaining(user);
		return ResponseEntity.ok(ApiResponse.ok(Map.of(
				"mfaEnabled", user.isMfaEnabled(),
				"recoveryCodesRemaining", recoveryCodesRemaining
		)));
	}
}
