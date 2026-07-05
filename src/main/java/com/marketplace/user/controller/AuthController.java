package com.marketplace.user.controller;

import com.marketplace.shared.dto.ApiResponse;
import com.marketplace.user.dto.AuthResponse;
import com.marketplace.user.dto.ForgotPasswordRequest;
import com.marketplace.user.dto.LoginRequest;
import com.marketplace.user.dto.RefreshTokenRequest;
import com.marketplace.user.dto.RegisterRequest;
import com.marketplace.user.dto.ResendVerificationTokenRequest;
import com.marketplace.user.dto.ResetPasswordRequest;
import com.marketplace.user.dto.TokenResponse;
import com.marketplace.user.dto.UserResponse;
import com.marketplace.user.dto.VerifyEmailRequest;
import com.marketplace.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse user = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok("Registration successful", user));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse auth = authService.login(request);
		return ResponseEntity.ok(ApiResponse.ok("Login successful", auth));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		TokenResponse auth = authService.refreshToken(request);
		return ResponseEntity.ok(ApiResponse.ok("Token refreshed", auth));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout() {
		// userId is set by SecurityContext via JWT filter
		String userId = org.springframework.security.core.context.SecurityContextHolder
				.getContext().getAuthentication().getName();
		authService.logout(userId);
		return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
	}

	@PostMapping("/verify-email")
	public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request);
		return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", null));
	}

	@PostMapping("/resend-verification")
	public ResponseEntity<ApiResponse<Void>> resendVerificationToken(
			@Valid @RequestBody ResendVerificationTokenRequest request) {
		authService.resendVerificationToken(request);
		return ResponseEntity.ok(ApiResponse.ok("If the email exists and is unverified, a new verification token has been sent", null));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent", null));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
	}
}
